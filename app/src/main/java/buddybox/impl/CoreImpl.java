package buddybox.impl;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import buddybox.api.Core;
import buddybox.api.Play;
import buddybox.api.Playlist;
import buddybox.api.Song;
import buddybox.api.SongAdded;
import buddybox.api.VisibleState;

import static buddybox.api.Play.PLAY_PAUSE_CURRENT;
import static buddybox.api.Play.SKIP_NEXT;
import static buddybox.api.Play.SKIP_PREVIOUS;

import buddybox.impl.db.Database;

import static buddybox.api.Sampler.*;

public class CoreImpl implements Core {

    private static final String UNKNOWN_GENRE = "Unknown Genre";
    private static final String UNKNOWN_ARTIST = "Unknown Artist";
    private final Context context;
    private final MediaPlayer player;
    private final Handler handler = new Handler();
    private boolean isMediaPlayerPrepared = false;
    private StateListener listener;

    private File musicDirectory;
    private Playlist recentPlaylist;
    private int currentSongIndex;

    private final File samplerDirectory;
    private boolean isSampling = false;
    private Playlist samplerPlaylist;

    private int nextId;
//    private MyFileObserver fileObs;
    private HashMap<String, String> genreMap;

    public CoreImpl(Context context) {
        this.context = context;

        //System.out.println(Database.initDatabase(context));

        //samplerDirectory = this.context.getExternalFilesDir("SongSamples");
        samplerDirectory = this.context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (samplerDirectory != null)
            if (!samplerDirectory.exists() && !samplerDirectory.mkdirs())
                System.out.println("Unable to create folder: " + samplerDirectory);
        System.out.println(">>>>> sampler directory " + samplerDirectory);

        musicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        System.out.println(">>> Music directory: " + musicDirectory);

        // Folder observer !!! ONLY root
        /*try {
            fileObs = new MyFileObserver(musicDirectory.getCanonicalPath());
            fileObs.startWatching();
            // TODO track onDestroy main activity to call fileObs.stopWatching();
            // TODO track subdirs >> http://www.roman10.net/2011/08/06/android-fileobserverthe-underlying-inotify-mechanism-and-an-example/
        } catch (IOException e) {
            System.out.println(e.getStackTrace());
        }*/

        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { @Override public void onCompletion(MediaPlayer mediaPlayer) {
            if (isSampling)
                play(samplerPlaylist(), 0);
            else
                play(recentPlaylist(), recentPlaylist().songAfter(currentSongIndex, 1));
            updateListener();
        }});
    }

    @Override
    public void dispatch(Event event) {
        if (event == PLAY_PAUSE_CURRENT) playPauseCurrent();
        if (event == SKIP_NEXT) skip(+1);
        if (event == SKIP_PREVIOUS) skip(-1);
        if (event.getClass() == Play.class) play((Play)event);

        if (event == SAMPLER_START) samplerStart();
        if (event == SAMPLER_STOP) samplerStop();
        if (event == SAMPLER_LOVE) samplerLove();
        if (event == SAMPLER_HATE) samplerHate();
        if (event == SAMPLER_DELETE) samplerDelete();

        if (event.getClass() == SongAdded.class) addSong((SongAdded)event);
        updateListener();
    }

    private void samplerHate() {
        System.out.println(">>> Sampler HATE");

        // TODO register song hated
        samplerNextSong(false);
    }

    private void samplerLove() {
        System.out.println(">>> Sampler LOVE");

        // TODO persist song loved
        Song lovedSong = samplerPlaylist.song(0);
        recentPlaylist.songs.add(lovedSong);
        lovedSong.setLoved();

        samplerNextSong(true);
    }

    private void samplerDelete() {
        System.out.println(">>> Sampler delete");

        // TODO register song deleted
        samplerNextSong(false);
    }

    private void samplerNextSong(boolean moveSongToLibrary) {
        if (samplerPlaylist.isEmpty())
            return;

        playPauseCurrent();

        SongImpl song = samplerPlaylist.song(0);
        samplerPlaylist.removeSong(0);

        if (moveSongToLibrary) {
            File newFile = new File(musicDirectory + "/" + song.file.getName());
            boolean r = song.file.renameTo(newFile);
            song.file = newFile; // TODO switch to immutable
            System.out.println(">>> LOVE move file " + r);
        } else if (!song.file.delete())
            System.out.println("Unable to delete file: " + song.file);

        if (!samplerPlaylist.isEmpty())
            play(samplerPlaylist, 0);
    }

    private void addSong(SongAdded event) {
        System.out.println(">>> song added " +  event.filePath);
        Song song = tryToReadSong(new File(event.filePath));
        if (song == null)
            return;
        recentPlaylist.songs.add(song);
    }

    private void samplerStop() {
        if (isSampling)
            player.stop();
        isSampling = false;
        isMediaPlayerPrepared = false;
    }

    private void samplerStart() {
        isSampling = true;
        Playlist playlist = samplerPlaylist();
        if (playlist.isEmpty())
            return;
        play(playlist, 0);
    }

    @NonNull
    private Playlist samplerPlaylist() {
        if (samplerPlaylist == null)
            samplerPlaylist = new Playlist(666, "Sampler", listSongs(samplerDirectory));
        return samplerPlaylist;
    }

    private void skip(int step) {
        play(recentPlaylist, recentPlaylist.songAfter(currentSongIndex, step));
    }

    private void play(Play event) {
        play(event.playlist, event.songIndex);
    }

    private void play(Playlist playlist, Integer songIndex) {
        // TODO set currentPlaylist
        if (songIndex == null)
            return;

        // TODO differ sampler from library
        currentSongIndex = songIndex;
        try {
            Uri myUri = Uri.parse(playlist.song(songIndex).file.getCanonicalPath());
            player.pause();
            player.reset();
            player.setDataSource(context, myUri);
            player.prepare();
            player.start();
            isMediaPlayerPrepared = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playPauseCurrent() {
        if (player.isPlaying())
            player.pause();
        else if (isMediaPlayerPrepared)
            player.start();
        else
            play(recentPlaylist(), currentSongIndex);
    }


    private void updateListener() {
        System.out.println(">> updateListener");
        Runnable runnable = new Runnable() { @Override public void run() {
            Playlist playlist = recentPlaylist();

            /* All genres found
            Set<String> genres = new HashSet<>();
            for (Song song : playlist.songs) {
                genres.add(song.genre);
            }
            for (String g : genres) {
                System.out.println(">>># " + g);
            }
            */

            VisibleState state = new VisibleState(
                    1,
                    null,
                    isSampling ? null : playlist.song(currentSongIndex),
                    null,
                    !player.isPlaying(),
                    null,
                    samplerPlaylist(),
                    lovedPlaylist(),
                    null,
                    null,
                    1,
                    getAvailableMemorySize(),
                    playlist
            );
            listener.update(state);
        }};
        handler.post(runnable);
    }

    private Playlist lovedPlaylist() {
        // Select loved songs
        ArrayList<Song> lovedSongs = new ArrayList<>();
        for (Song song : recentPlaylist.songs) {
            if (song.isLoved()) {
                System.out.println(">> Love " + song.name);
                lovedSongs.add(song);
            }
        }

        // Sort by most recent loved
        Collections.sort(lovedSongs, new Comparator<Song>() { @Override public int compare(Song songA, Song songB) {
            return songB.loved.compareTo(songA.loved);
        }});

        return new Playlist(69, "Loved", lovedSongs);
    }

    private long getAvailableMemorySize() {
        StatFs stat = new StatFs(musicDirectory.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    private Playlist recentPlaylist() {
        if (recentPlaylist == null) {
            Long now = System.currentTimeMillis();
            recentPlaylist = new Playlist(0, "Recent", listSongs(musicDirectory));
            System.out.println(">>> seconds to list all songs: " + (System.currentTimeMillis() - now) / 1000);
        }
        return recentPlaylist;
    }

    private ArrayList<Song> listSongs(File directory) {
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
        System.out.println(">>> songs size: " + ret.size() + " at " + directory);
        return ret;
    }

    @Nullable
    private Song tryToReadSong(File file) {
        return file.getName().toLowerCase().endsWith(".mp3")
            ? readSongMetadata(nextId(), file)
            : null;
    }

    private int nextId() {
        return nextId++;
    }

    @NonNull
    private SongImpl readSongMetadata(int id, File file) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(file.getPath());

        String name = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if (name == null || name.trim().isEmpty())
            name = file.getName().substring(0, file.getName().length() - 4);
        else
            name = name.trim();

        String artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        if (artist == null || artist.trim().isEmpty())
            artist = UNKNOWN_ARTIST;
        else
            artist = artist.trim();

        String genre = formatSongGenre(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));

        return new SongImpl(id, name, artist, genre, file);
    }

    @Override
    public void setStateListener(StateListener listener) {
        this.listener = listener;
        updateListener();
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
