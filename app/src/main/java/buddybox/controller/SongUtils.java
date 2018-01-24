package buddybox.controller;

import android.media.MediaMetadataRetriever;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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

import buddybox.core.Hash;
import buddybox.core.Song;

public class SongUtils {

    private static final String UNKNOWN_GENRE = "Unknown Genre";
    private static final String UNKNOWN_ARTIST = "Unknown Artist";

    private static Map<String, String> genreByCode;
    private static File musicFolder;

    public static File musicFolder() {
        if (musicFolder == null) {
            musicFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            if (!musicFolder.exists())
                if (!musicFolder.mkdirs())
                    System.out.println("Unable to create folder: " + musicFolder);
        }
        return musicFolder;
    }

    public static List<Song> listSongs(File folder) {
        List<File> mp3Files = listMp3Files(folder);

        List<Song> ret = new ArrayList<>();
        for (File mp3 : mp3Files) {
            ret.add(readSong(mp3));
        }
        return ret;
    }

    private static Hash mp3Hash(File mp3) {
        MessageDigest sha256 = getMessageDigest();
        if (sha256 == null)
            throw new RuntimeException("Missing SHA-256 algorithm");

        Hash ret = null;
        byte[] raw = rawMP3(mp3);
        if (raw != null) {
            byte[] hashBytes = Arrays.copyOf(sha256.digest(raw), 16); // 128 bits is enough
            ret = new Hash(hashBytes);
        }
        return ret;
    }


    public static byte[] rawMP3(File file) {
        byte[] ret = null;
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(file), 8 * 1024);
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

    public static List<File> listLibraryMp3Files() {
        return listMp3Files(musicFolder());
    }

    private static List<File> listMp3Files(File directory) {
        List<File> ret = new ArrayList<>();

        if (!directory.exists()) {
            System.out.println("Directory does not exist: " + directory);
            return ret;
        }

        File[] files = directory.listFiles();
        if (files == null)
            return ret;

        for (File file : files) {
            if (file.isDirectory())
                ret.addAll(listMp3Files(file));
            else if (isMP3(file))
                ret.add(file);
        }
        return ret;
    }

    private static boolean isMP3(File file) {
        return file.getName().toLowerCase().endsWith(".mp3");
    }

    public static Map<String, String> readMp3Metadata(File mp3) {
        Map<String, String> ret = new HashMap<>();

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(mp3.getPath());

        String name = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (name == null || name.trim().isEmpty())
            name = mp3.getName().substring(0, mp3.getName().length() - 4);
        else
            name = name.trim();

        String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        if (artist == null || artist.trim().isEmpty())
            artist = UNKNOWN_ARTIST;
        else
            artist = artist.trim();

        String genre = formatSongGenre(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
        String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

        ret.put("name", name);
        ret.put("artist", artist);
        ret.put("genre", genre);
        ret.put("duration", duration);

        return ret;
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

    private static Map<String, String> genreMap() {
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

    public static Song readSong(File mp3) {
        Map<String, String> metadata = SongUtils.readMp3Metadata(mp3);
        Hash hash = SongUtils.mp3Hash(mp3);

        Integer duration = null;
        String durationStr = metadata.get("duration");
        if (durationStr != null)
            duration = Integer.parseInt(durationStr);

        return new Song(hash, metadata.get("name"), metadata.get("artist"), metadata.get("genre"), duration, mp3.getPath(), mp3.length(), mp3.lastModified());
    }
}
