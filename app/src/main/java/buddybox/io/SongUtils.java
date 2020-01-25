package buddybox.io;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sov.Hash;
import buddybox.core.Song;

public class SongUtils {

    private static Application context;

    private static final String UNKNOWN_GENRE = "Unknown Genre";
    private static final String UNKNOWN_ARTIST = "Unknown Artist";
    private static final String UNKNOWN_ALBUM = "Unknown Album";

    private static Map<String, String> genreByCode;
    private static File musicFolder;

    public static void initializeContext(Application context) {
        SongUtils.context = context;
    }

    static File musicFolder() {
        // TODO: remove checks, new API29 method getExternalFilesDir creates missing folders
        if (musicFolder == null) {
            musicFolder = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            if (!musicFolder.exists())
                if (!musicFolder.mkdirs())
                    System.out.println("Unable to create folder: " + musicFolder);
        }
        return musicFolder;
    }

    static List<Song> listSongs() {
        List<SongMedia> mp3Files = listSongMediaFiles();
        List<Song> ret = new ArrayList<>();

        for (SongMedia songMedia : mp3Files)
            ret.add(readSong(songMedia));

        return ret;
    }

    private static Hash mp3Hash(SongMedia songMedia) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            InputStream inputStream = contentResolver.openInputStream(songMedia.getUri());
            return mp3Hash(inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Hash mp3Hash(InputStream inputStream) {
        MessageDigest sha256 = getMessageDigest();
        if (sha256 == null)
            throw new RuntimeException("Missing SHA-256 algorithm");

        Hash ret = null;
        byte[] raw = rawMP3(inputStream);
        if (raw != null) {
            byte[] hashBytes = Arrays.copyOf(sha256.digest(raw), 16); // 128 bits is enough
            ret = new Hash(hashBytes);
        }
        return ret;
    }

    private static byte[] rawMP3(InputStream inputStream) {
        byte[] ret = null;
        try {
            InputStream in = new BufferedInputStream(inputStream, 8 * 1024);
            in.mark(10);
            int headerEnd = readID3v2Header(in);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = in.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();

            ret = buffer.toByteArray();
            int end = ret.length;
            if (end > 128 && ret[end-128] == 84 && ret[end-127] == 65 && ret[end-126] == 71) // Detect TAG from ID3v1
                end -= 129;

            ret = Arrays.copyOfRange(ret, headerEnd, end); // Discard header (ID3 v2) and last 128 bytes (ID3 v1)
        } catch(IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    private static int readID3v2Header (InputStream in) throws IOException {
        byte[] id3header = new byte[4];
        int size = -10;
        in.read(id3header, 0, 3);
        // Look for ID3v2
        if (id3header[0] == 'I' && id3header[1] == 'D' && id3header[2] == '3') {
            in.read(id3header, 0, 3);
            in.read(id3header, 0, 4);
            size = (id3header[0] << 21) + (id3header[1] << 14) + (id3header[2] << 7) + id3header[3];
        }
        return size + 10;
    }

    static List<SongMedia> listSongMediaFiles() {
        List<SongMedia> ret = new ArrayList<>();

        ContentResolver contentResolver = context.getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        Cursor songCursor = contentResolver.query(songUri, null, selection, null, null);
        System.out.println("Total songs: " + songCursor.getCount());

        if (songCursor != null && songCursor.moveToFirst()) {
            int songId = songCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int songDuration = songCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int songAlbum = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int songModified = songCursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED);

            do {
                long currentId = songCursor.getLong(songId);
                String currentDuration = songCursor.getString(songDuration);
                String currentArtist = songCursor.getString(songArtist);
                String currentAlbum = songCursor.getString(songAlbum);
                String currentTitle = songCursor.getString(songTitle);
                long currentModified = songCursor.getLong(songModified);

                System.out.println("Song:" + currentId + " / " + currentTitle);

                Uri currentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, currentId);

                SongMedia songMedia = new SongMedia();
                songMedia.setMediaId(currentId);
                songMedia.setDuration(currentDuration);
                songMedia.setArtist(currentArtist);
                songMedia.setAlbum(currentAlbum);
                songMedia.setTitle(currentTitle);
                songMedia.setUri(contentResolver.canonicalize(currentUri));
                songMedia.setModified(currentModified);

                ret.add(songMedia);
            } while(songCursor.moveToNext());
        }

        return ret;
    }

    private static Map<String, String> readMp3Metadata(SongMedia songMedia) {
        Map<String, String> ret = new HashMap<>();

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, songMedia.getUri());

        String genre = formatSongGenre(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));

        ret.put("genre", genre);

        return ret;
    }

    private static byte[] getEmbeddedPicture(SongMedia songMedia) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(context, songMedia.getUri());

        return mmr.getEmbeddedPicture();
    }

    private static String formatSongGenre(String genreRaw) {
        if (genreRaw == null)
            return UNKNOWN_GENRE;

        String formatGenre = genreRaw.trim();
        if (formatGenre.matches("[0-9]+")) {
            formatGenre = getGenre(formatGenre);
        } else {
            // Try to find a code between parenthesis
            Matcher m = Pattern.compile("\\(([0-9]+)\\)+").matcher(formatGenre);
            while (m.find()) {
                formatGenre = getGenre(m.group(1));
            }
        }
        if (formatGenre == null)
            return UNKNOWN_GENRE;
        return formatGenre;
    }

    private static String getGenre(String key) {
        return genreMap().get(key);
    }

    public static Map<String, String> genreMap() {
        if (genreByCode == null) {
            genreByCode = new HashMap<>();
            genreByCode.put("0", "Blues");
            genreByCode.put("1", "Classic Rock");
            genreByCode.put("2", "Country");
            genreByCode.put("3", "Dance");
            genreByCode.put("4", "Disco");
            genreByCode.put("5", "Funk");
            genreByCode.put("6", "Grunge");
            genreByCode.put("7", "Hip-Hop");
            genreByCode.put("8", "Jazz");
            genreByCode.put("9", "Metal");
            genreByCode.put("10", "New Age");
            genreByCode.put("11", "Oldies");
            genreByCode.put("12", "Other");
            genreByCode.put("13", "Pop");
            genreByCode.put("14", "R&B");
            genreByCode.put("15", "Rap");
            genreByCode.put("16", "Reggae");
            genreByCode.put("17", "Rock");
            genreByCode.put("18", "Techno");
            genreByCode.put("19", "Industrial");
            genreByCode.put("20", "Alternative");
            genreByCode.put("21", "Ska");
            genreByCode.put("22", "Death Metal");
            genreByCode.put("23", "Pranks");
            genreByCode.put("24", "Soundtrack");
            genreByCode.put("25", "Euro-Techno");
            genreByCode.put("26", "Ambient");
            genreByCode.put("27", "Trip-Hop");
            genreByCode.put("28", "Vocal");
            genreByCode.put("29", "Jazz+Funk");
            genreByCode.put("30", "Fusion");
            genreByCode.put("31", "Trance");
            genreByCode.put("32", "Classical");
            genreByCode.put("33", "Instrumental");
            genreByCode.put("34", "Acid");
            genreByCode.put("35", "House");
            genreByCode.put("36", "Game");
            genreByCode.put("37", "Sound Clip");
            genreByCode.put("38", "Gospel");
            genreByCode.put("39", "Noise");
            genreByCode.put("40", "Alternative Rock");
            genreByCode.put("41", "Bass");
            genreByCode.put("42", "Soul");
            genreByCode.put("43", "Punk");
            genreByCode.put("44", "Space");
            genreByCode.put("45", "Meditative");
            genreByCode.put("46", "Instrumental Pop");
            genreByCode.put("47", "Instrumental Rock");
            genreByCode.put("48", "Ethnic");
            genreByCode.put("49", "Gothic");
            genreByCode.put("50", "Darkwave");
            genreByCode.put("51", "Techno-Industrial");
            genreByCode.put("52", "Electronic");
            genreByCode.put("53", "Pop-Folk");
            genreByCode.put("54", "Eurodance");
            genreByCode.put("55", "Dream");
            genreByCode.put("56", "Southern Rock");
            genreByCode.put("57", "Comedy");
            genreByCode.put("58", "Cult");
            genreByCode.put("59", "Gangsta");
            genreByCode.put("60", "Top 40");
            genreByCode.put("61", "Christian Rap");
            genreByCode.put("62", "Pop/Funk");
            genreByCode.put("63", "Jungle");
            genreByCode.put("64", "Native US");
            genreByCode.put("65", "Cabaret");
            genreByCode.put("66", "New Wave");
            genreByCode.put("67", "Psychadelic");
            genreByCode.put("68", "Rave");
            genreByCode.put("69", "Showtunes");
            genreByCode.put("70", "Trailer");
            genreByCode.put("71", "Lo-Fi");
            genreByCode.put("72", "Tribal");
            genreByCode.put("73", "Acid Punk");
            genreByCode.put("74", "Acid Jazz");
            genreByCode.put("75", "Polka");
            genreByCode.put("76", "Retro");
            genreByCode.put("77", "Musical");
            genreByCode.put("78", "Rock & Roll");
            genreByCode.put("79", "Hard Rock");
            genreByCode.put("80", "Folk");
            genreByCode.put("81", "Folk-Rock");
            genreByCode.put("82", "National Folk");
            genreByCode.put("83", "Swing");
            genreByCode.put("84", "Fast Fusion");
            genreByCode.put("85", "Bebob");
            genreByCode.put("86", "Latin");
            genreByCode.put("87", "Revival");
            genreByCode.put("88", "Celtic");
            genreByCode.put("89", "Bluegrass");
            genreByCode.put("90", "Avantgarde");
            genreByCode.put("91", "Gothic Rock");
            genreByCode.put("92", "Progressive Rock");
            genreByCode.put("93", "Psychedelic Rock");
            genreByCode.put("94", "Symphonic Rock");
            genreByCode.put("95", "Slow Rock");
            genreByCode.put("96", "Big Band");
            genreByCode.put("97", "Chorus");
            genreByCode.put("98", "Easy Listening");
            genreByCode.put("99", "Acoustic");
            genreByCode.put("100", "Humour");
            genreByCode.put("101", "Speech");
            genreByCode.put("102", "Chanson");
            genreByCode.put("103", "Opera");
            genreByCode.put("104", "Chamber Music");
            genreByCode.put("105", "Sonata");
            genreByCode.put("106", "Symphony");
            genreByCode.put("107", "Booty Bass");
            genreByCode.put("108", "Primus");
            genreByCode.put("109", "Porn Groove");
            genreByCode.put("110", "Satire");
            genreByCode.put("111", "Slow Jam");
            genreByCode.put("112", "Club");
            genreByCode.put("113", "Tango");
            genreByCode.put("114", "Samba");
            genreByCode.put("115", "Folklore");
            genreByCode.put("116", "Ballad");
            genreByCode.put("117", "Power Ballad");
            genreByCode.put("118", "Rhythmic Soul");
            genreByCode.put("119", "Freestyle");
            genreByCode.put("120", "Duet");
            genreByCode.put("121", "Punk Rock");
            genreByCode.put("122", "Drum Solo");
            genreByCode.put("123", "Acapella");
            genreByCode.put("124", "Euro-House");
            genreByCode.put("125", "Dance Hall");
            genreByCode.put("126", "Goa");
            genreByCode.put("127", "Drum & Bass");
            genreByCode.put("128", "Club - House");
            genreByCode.put("129", "Hardcore");
            genreByCode.put("130", "Terror");
            genreByCode.put("131", "Indie");
            genreByCode.put("132", "BritPop");
            genreByCode.put("133", "Negerpunk");
            genreByCode.put("134", "Polsk Punk");
            genreByCode.put("135", "Beat");
            genreByCode.put("136", "Christian Gangsta Rap");
            genreByCode.put("137", "Heavy Metal");
            genreByCode.put("138", "Black Metal");
            genreByCode.put("139", "Crossover");
            genreByCode.put("140", "Contemporary Christian");
            genreByCode.put("141", "Christian Rock");
            genreByCode.put("142", "Merengue");
            genreByCode.put("143", "Salsa");
            genreByCode.put("144", "Thrash Metal");
            genreByCode.put("145", "Anime");
            genreByCode.put("146", "JPop");
            genreByCode.put("147", "Synthpop");
        }
        return genreByCode;
    }

    private static MessageDigest getMessageDigest() {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return digest;
    }

    public static Song readSong(SongMedia songMedia) {
        Map<String, String> metadata = SongUtils.readMp3Metadata(songMedia);
        Hash hash = SongUtils.mp3Hash(songMedia);

        Integer duration = null;
        String durationStr = songMedia.getDuration();
        if (durationStr != null)
            duration = Integer.parseInt(durationStr);

        boolean hasEmbeddedArt = getEmbeddedPicture(songMedia) != null;

        return new Song(null, hash, songMedia.getTitle(), songMedia.getArtist(), songMedia.getAlbum(), metadata.get("genre"), duration, songMedia.getMediaId(), 0, songMedia.getModified(), false, false, System.currentTimeMillis(), hasEmbeddedArt, 0);
    }

    static boolean deleteSong(Song song) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.mediaId);
        return contentResolver.delete(uri, null, null) == 1;
    }
}
