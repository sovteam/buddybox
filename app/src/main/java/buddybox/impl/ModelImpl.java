package buddybox.impl;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import buddybox.core.events.AddSongToPlaylist;
import buddybox.core.Artist;
import buddybox.core.events.CreatePlaylist;
import buddybox.core.Dispatcher;
import buddybox.core.Hash;
import buddybox.core.events.LibraryUpdated;
import buddybox.core.events.LovedUpdated;
import buddybox.core.Model;
import buddybox.core.events.Permission;
import buddybox.core.events.Play;
import buddybox.core.Playlist;
import buddybox.core.events.SamplerDelete;
import buddybox.core.events.SamplerHate;
import buddybox.core.events.SamplerLove;
import buddybox.core.events.SamplerUpdated;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.SongAdded;

import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.core.events.Play.SKIP_NEXT;
import static buddybox.core.events.Play.SKIP_PREVIOUS;
import static buddybox.core.events.Play.FINISHED_PLAYING;

import static buddybox.core.events.Sampler.*;

public class ModelImpl implements Model {

    private static final String UNKNOWN_GENRE = "Unknown Genre";
    private static final String UNKNOWN_ARTIST = "Unknown Artist";

    private final Context context;
    private final Handler handler = new Handler();
    private List<StateListener> listeners = new ArrayList<>();

    private File musicDirectory;
    private Playlist currentPlaylist;
    private Integer currentSongIndex;

    private boolean isSampling = false;
    private Playlist samplerPlaylist;
    private int nextId;

    private HashMap<String, String> genreMap;
    private ArrayList<Playlist> playlists;
    private List<Song> allSongs;
    private List<Artist> allArtists;
    private boolean isPaused;
    private Boolean hasPermissionWriteExternalStorage;

    public ModelImpl(Context context) {
        this.context = context;

        //System.out.println(Database.initDatabase(context));

        Dispatcher.addListener(new Dispatcher.Listener() { @Override public void onEvent(Dispatcher.Event event) {
            handle(event);
        }});
    }

    private void handle(Dispatcher.Event event) {
        System.out.println("@@@ Event class " + event.getClass());

        if (event == PLAY_PAUSE_CURRENT) playPauseCurrent();
        if (event == SKIP_NEXT) skip(+1);
        if (event == SKIP_PREVIOUS) skip(-1);
        if (event == FINISHED_PLAYING) finishedPlaying();

        if (event.getClass() == Play.class) play((Play) event);


        // Sampler Events
        if (event == SAMPLER_START) samplerStart();
        if (event == SAMPLER_STOP) samplerStop();

        if (event.getClass() == SamplerLove.class)
            samplerLove((SamplerLove) event);

        if (event.getClass() == SamplerHate.class)
            samplerHate((SamplerHate) event);

        if (event.getClass() == SamplerDelete.class)
            samplerDelete((SamplerDelete) event);

        if (event == LOVED_VIEWED) lovedViewed();

        if (event.getClass() == SamplerUpdated.class)
            samplerUpdate((SamplerUpdated) event);

        if (event.getClass() == Permission.class)
            updatePermission((Permission) event);

        if (event.getClass() == AddSongToPlaylist.class)
            addSongToPlaylist((AddSongToPlaylist) event);

        if (event.getClass() == CreatePlaylist.class)
            createPlaylist((CreatePlaylist) event);

        if (event.getClass() == LibraryUpdated.class)
            updateLibrary((LibraryUpdated) event);

        //if (event.getClass() == SongAdded.class) addSong((SongAdded)event);

        updateListeners();
    }

    private void samplerUpdate(SamplerUpdated event) {
        samplerPlaylist = new Playlist(666, "Sampler", event.samples);
    }

    private void updateLibrary(LibraryUpdated event) {
        allArtists = event.allArtists;
        allSongs = event.allSongs;
    }

    private void updatePermission(Permission event) {
        if (event.code == Permission.WRITE_EXTERNAL_STORAGE) {
            hasPermissionWriteExternalStorage = event.granted;
        }
    }

    private void finishedPlaying() {
        if (isSampling)
            doPlay(samplerPlaylist, 0);
        else
            skip(+1);
    }

    private void createPlaylist(CreatePlaylist event) {
        /* TODO implement
         * String name = playlistName.trim();
         * if (name.isEmpty()) {
         *      Toast("Playlist name can\'t be empty");
         *      return;
         * }
         * Playlist playlist = Playlist.findByName(name);
         * if (playlist != null)
         *      playlist.addSong(songId);
         * else
         *      Playlist.create(event.playlistName, [Song.findById(event.songId)]);
         */
        System.out.println("@@@ Dispatched Event: createPlaylist");
    }

    private void addSongToPlaylist(AddSongToPlaylist event) {
        /*TODO implement
        * Playlist playlist = Playlist.findByName(event.playlist);
        * if (playlist == null)
        *   Playlist.create(event.playlistName, [Song.findById(event.songId)]);
        * else
        *   playlist.addSong(songId);
        * */
        System.out.println("@@@ Dispatched Event: addSongToPlaylist");
    }

    private void lovedViewed() {
        // TODO move to Sampler?
        System.out.println(">>> Model Loved VIEWED");
        for (Song song : lovedPlaylist().songs) {
            if (!song.isLovedViewed())
                song.setLovedViewed();
        }
    }

    private void samplerHate(SamplerHate event) {
        System.out.println(">>> Sampler HATE sample");
        removeSample(event.song);
    }

    private void samplerLove(SamplerLove event) {
        System.out.println(">>> Model LOVE sample");
        removeSample(event.song);
    }

    private void samplerDelete(SamplerDelete event) {
        System.out.println(">>> Model DELETE sample");
        removeSample(event.song);
    }

    private void removeSample(Song song) {
        int idx = samplerPlaylist.songs.indexOf(song);
        samplerPlaylist.removeSong(idx);
    }

    private void samplerStop() {
        isSampling = false;
        doStop();
    }

    private void doStop() {
        currentPlaylist = null;
        currentSongIndex = null;
    }

    private void samplerStart() {
        isSampling = true;
        doPlay(samplerPlaylist, 0);
    }


    private void skip(int step) {
        doPlay(currentPlaylist, currentPlaylist.songAfter(currentSongIndex, step));
    }

    private void play(Play event) {
        doPlay(event.playlist, event.songIndex);
    }

    private void doPlay(Playlist playlist, int songIndex) {
        isPaused = false;
        currentSongIndex = songIndex;
        currentPlaylist = playlist;
    }

    private void playPauseCurrent() {
        isPaused = !isPaused;
    }

    private void updateListeners() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                State state = getState();
                for (StateListener listener : listeners) {
                    updateListener(listener, state);
                }
            }
        };
        handler.post(runnable);
    }

    private void updateListener(StateListener listener) {
        updateListener(listener, getState());
    }

    private void updateListener(StateListener listener, State state) {
        listener.update(state);
    }

    private State getState() {
        System.out.println("!!! isSampling " + isSampling);
        return new State(
                1,
                null,
                isSampling
                        ? samplerPlaylist.song(0)
                        : currentSongIndex == null
                            ? null
                            : currentPlaylist.song(currentSongIndex),
                currentPlaylist,
                isPaused,
                null,
                isSampling,
                samplerPlaylist,
                lovedPlaylist(),
                playlists(),
                null,
                1,
                getAvailableMemorySize(),
                playlistAllSongs(),
                allArtists,
                hasPermissionWriteExternalStorage);
    }

    private Playlist playlistAllSongs() {
        return new Playlist(0, "Recent", new ArrayList<>(allSongs));
    }

    private List<Playlist> playlists() {
        if (playlists == null) {
            playlists = new ArrayList<>();
            /*List<Song> songs = new ArrayList<>(allSongs.values());
            playlists.add(new Playlist(10, "My Rock", songs.subList(0, 1)));
            playlists.add(new Playlist(11, "70\'s", songs.subList(1, 3)));
            playlists.add(new Playlist(12, "Pagode do Tadeu", songs.subList(0, 4)));*/
        }
        return playlists;
    }

    private Playlist lovedPlaylist() {
        List<Song> lovedSongs = new ArrayList<>();

        for (Song song : allSongs) {
            if (song.isLoved())
                lovedSongs.add(song);
        }

        // Sort by most recent loved
        Collections.sort(lovedSongs, new Comparator<Song>() { @Override public int compare(Song songA, Song songB) {
            return songB.loved.compareTo(songA.loved);
        }});

        return new Playlist(69, "Loved", lovedSongs);
    }

    // TODO send to Library
    private long getAvailableMemorySize() {
        StatFs stat = new StatFs(musicDirectory().getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    private File musicDirectory() {
        if (musicDirectory == null) {
            musicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            if (!musicDirectory.exists())
                if (!musicDirectory.mkdirs())
                    System.out.println("Unable to create folder: " + musicDirectory);
        }
        return musicDirectory;
    }

    private Hash mp3Hash(File mp3) {
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

    private MessageDigest getMessageDigest() {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return digest;
    }


    private byte[] rawMP3(File file) {
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

    private int readID3v2Header (InputStream in) throws IOException {
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

    private List<Song> listSongs(File directory) {
        ArrayList<Song> ret = new ArrayList<>();

        if (!directory.exists()) {
            System.out.println("Directory does not exist: " + directory);
            return ret;
        }

        File[] files = directory.listFiles();
        if (files == null) return ret;

        for (File file : files) {
            if (file.isDirectory()) {
                ret.addAll(listSongs(file));
            } else {
                Song song = tryToReadSong(file);
                if (song == null) continue;
                ret.add(song);
            }
        }
        return ret;
    }

    @Nullable
    private Song tryToReadSong(File file) {
        return file.getName().toLowerCase().endsWith(".mp3")
            ? readSongMetadata(file)
            : null;
    }

    private int nextId() {
        return nextId++;
    }

    @NonNull
    private Song readSongMetadata(File file) {
        Map<String, String> map = readMp3Metadata(file);
        Integer duration = null;
        String durationStr = map.get("duration");
        if (durationStr != null)
            duration = Integer.parseInt(durationStr);
        return new Song(nextId(), mp3Hash(file), map.get("name"), map.get("artist"), map.get("genre"), duration, file.getPath(), file);
    }

    private Map<String, String> readMp3Metadata(File mp3) {
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

    @Override
    public void addStateListener(StateListener listener) {
        this.listeners.add(listener);
    }

    private String formatSongGenre(String genreRaw) {
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
    
    private String getGenre(String key) {
        return genreMap().get(key);
    }

    private Map<String, String> genreMap() {
        if (genreMap == null) {
            genreMap = new HashMap<>();
            genreMap.put("0", "Blues");
            genreMap.put("1", "Classic Rock");
            genreMap.put("2", "Country");
            genreMap.put("3", "Dance");
            genreMap.put("4", "Disco");
            genreMap.put("5", "Funk");
            genreMap.put("6", "Grunge");
            genreMap.put("7", "Hip-Hop");
            genreMap.put("8", "Jazz");
            genreMap.put("9", "Metal");
            genreMap.put("10", "New Age");
            genreMap.put("11", "Oldies");
            genreMap.put("12", "Other");
            genreMap.put("13", "Pop");
            genreMap.put("14", "R&B");
            genreMap.put("15", "Rap");
            genreMap.put("16", "Reggae");
            genreMap.put("17", "Rock");
            genreMap.put("18", "Techno");
            genreMap.put("19", "Industrial");
            genreMap.put("20", "Alternative");
            genreMap.put("21", "Ska");
            genreMap.put("22", "Death Metal");
            genreMap.put("23", "Pranks");
            genreMap.put("24", "Soundtrack");
            genreMap.put("25", "Euro-Techno");
            genreMap.put("26", "Ambient");
            genreMap.put("27", "Trip-Hop");
            genreMap.put("28", "Vocal");
            genreMap.put("29", "Jazz+Funk");
            genreMap.put("30", "Fusion");
            genreMap.put("31", "Trance");
            genreMap.put("32", "Classical");
            genreMap.put("33", "Instrumental");
            genreMap.put("34", "Acid");
            genreMap.put("35", "House");
            genreMap.put("36", "Game");
            genreMap.put("37", "Sound Clip");
            genreMap.put("38", "Gospel");
            genreMap.put("39", "Noise");
            genreMap.put("40", "Alternative Rock");
            genreMap.put("41", "Bass");
            genreMap.put("42", "Soul");
            genreMap.put("43", "Punk");
            genreMap.put("44", "Space");
            genreMap.put("45", "Meditative");
            genreMap.put("46", "Instrumental Pop");
            genreMap.put("47", "Instrumental Rock");
            genreMap.put("48", "Ethnic");
            genreMap.put("49", "Gothic");
            genreMap.put("50", "Darkwave");
            genreMap.put("51", "Techno-Industrial");
            genreMap.put("52", "Electronic");
            genreMap.put("53", "Pop-Folk");
            genreMap.put("54", "Eurodance");
            genreMap.put("55", "Dream");
            genreMap.put("56", "Southern Rock");
            genreMap.put("57", "Comedy");
            genreMap.put("58", "Cult");
            genreMap.put("59", "Gangsta");
            genreMap.put("60", "Top 40");
            genreMap.put("61", "Christian Rap");
            genreMap.put("62", "Pop/Funk");
            genreMap.put("63", "Jungle");
            genreMap.put("64", "Native US");
            genreMap.put("65", "Cabaret");
            genreMap.put("66", "New Wave");
            genreMap.put("67", "Psychadelic");
            genreMap.put("68", "Rave");
            genreMap.put("69", "Showtunes");
            genreMap.put("70", "Trailer");
            genreMap.put("71", "Lo-Fi");
            genreMap.put("72", "Tribal");
            genreMap.put("73", "Acid Punk");
            genreMap.put("74", "Acid Jazz");
            genreMap.put("75", "Polka");
            genreMap.put("76", "Retro");
            genreMap.put("77", "Musical");
            genreMap.put("78", "Rock & Roll");
            genreMap.put("79", "Hard Rock");
            genreMap.put("80", "Folk");
            genreMap.put("81", "Folk-Rock");
            genreMap.put("82", "National Folk");
            genreMap.put("83", "Swing");
            genreMap.put("84", "Fast Fusion");
            genreMap.put("85", "Bebob");
            genreMap.put("86", "Latin");
            genreMap.put("87", "Revival");
            genreMap.put("88", "Celtic");
            genreMap.put("89", "Bluegrass");
            genreMap.put("90", "Avantgarde");
            genreMap.put("91", "Gothic Rock");
            genreMap.put("92", "Progressive Rock");
            genreMap.put("93", "Psychedelic Rock");
            genreMap.put("94", "Symphonic Rock");
            genreMap.put("95", "Slow Rock");
            genreMap.put("96", "Big Band");
            genreMap.put("97", "Chorus");
            genreMap.put("98", "Easy Listening");
            genreMap.put("99", "Acoustic");
            genreMap.put("100", "Humour");
            genreMap.put("101", "Speech");
            genreMap.put("102", "Chanson");
            genreMap.put("103", "Opera");
            genreMap.put("104", "Chamber Music");
            genreMap.put("105", "Sonata");
            genreMap.put("106", "Symphony");
            genreMap.put("107", "Booty Bass");
            genreMap.put("108", "Primus");
            genreMap.put("109", "Porn Groove");
            genreMap.put("110", "Satire");
            genreMap.put("111", "Slow Jam");
            genreMap.put("112", "Club");
            genreMap.put("113", "Tango");
            genreMap.put("114", "Samba");
            genreMap.put("115", "Folklore");
            genreMap.put("116", "Ballad");
            genreMap.put("117", "Power Ballad");
            genreMap.put("118", "Rhythmic Soul");
            genreMap.put("119", "Freestyle");
            genreMap.put("120", "Duet");
            genreMap.put("121", "Punk Rock");
            genreMap.put("122", "Drum Solo");
            genreMap.put("123", "Acapella");
            genreMap.put("124", "Euro-House");
            genreMap.put("125", "Dance Hall");
            genreMap.put("126", "Goa");
            genreMap.put("127", "Drum & Bass");
            genreMap.put("128", "Club - House");
            genreMap.put("129", "Hardcore");
            genreMap.put("130", "Terror");
            genreMap.put("131", "Indie");
            genreMap.put("132", "BritPop");
            genreMap.put("133", "Negerpunk");
            genreMap.put("134", "Polsk Punk");
            genreMap.put("135", "Beat");
            genreMap.put("136", "Christian Gangsta Rap");
            genreMap.put("137", "Heavy Metal");
            genreMap.put("138", "Black Metal");
            genreMap.put("139", "Crossover");
            genreMap.put("140", "Contemporary Christian");
            genreMap.put("141", "Christian Rock");
            genreMap.put("142", "Merengue");
            genreMap.put("143", "Salsa");
            genreMap.put("144", "Thrash Metal");
            genreMap.put("145", "Anime");
            genreMap.put("146", "JPop");
            genreMap.put("147", "Synthpop");
        }
        return genreMap;
    }
}
