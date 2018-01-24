package buddybox.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
import buddybox.core.events.SongFound;
import buddybox.core.events.SongMissing;
import buddybox.impl.db.DatabaseHelper;

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

        System.out.println(DatabaseHelper.getInstance(context));

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

        if (event.getClass() == SongFound.class)
            addFound((SongFound)event);

        if (event.getClass() == SongMissing.class)
            songMissing((SongMissing)event);

        updateListeners();
    }

    private void songMissing(SongMissing event) {

    }

    private void addFound(SongFound event) {
        ContentValues newSong = new ContentValues(2);
        newSong.put("HASH", event.song.hash.bytes);
        newSong.put("NAME", event.song.name);
        newSong.put("GENRE", event.song.genre);
        newSong.put("ARTIST", event.song.artist);
        newSong.put("DURATION", event.song.duration);
        newSong.put("FILE_LENGTH", event.song.fileLength);
        newSong.put("LAST_MODIFIED", event.song.lastModified);
        DatabaseHelper.getInstance(context).getReadableDatabase().insert("SONGS", null, newSong);
    }

    private void samplerUpdate(SamplerUpdated event) {
        samplerPlaylist = new Playlist(666, "Sampler", event.samples);
    }

    private ArrayList<Artist> artists() {
        Map<String, Artist> artistsMap = new HashMap<>();
        for (Song song : allSongs) {
            Artist artist = artistsMap.get(song.artist);
            if (artist == null) {
                artist = new Artist(song.artist);
                artistsMap.put(song.artist, artist);
            }
            artist.addSong(song);
        }
        return new ArrayList<>(artistsMap.values());
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
                artists(),
                hasPermissionWriteExternalStorage);
    }

    private Playlist playlistAllSongs() {
        return new Playlist(0, "Recent", new ArrayList<>(allSongs));
    }

    private List<Song> allSongs() {
        if (allSongs == null) {
            allSongs = new ArrayList<>();
            Cursor cursor = DatabaseHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM SONGS", null);
            while(cursor.moveToNext()) {
                /*Song song = new Song(
                    cursor.getString(0)
                    cursor.getString(1)
                    cursor.getString(2)
                    cursor.getString(3)
                    cursor.getString(4)
                    cursor.getString(5));
                allSongs.add(song)*/
            }
            cursor.close();
        }
        return allSongs;
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

    @Override
    public void addStateListener(StateListener listener) {
        this.listeners.add(listener);
    }
}
