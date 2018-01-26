package buddybox.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import buddybox.core.events.AddSongToPlaylist;
import buddybox.core.Artist;
import buddybox.core.events.CreatePlaylist;
import buddybox.core.Dispatcher;
import buddybox.core.Hash;
import buddybox.core.IModel;
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

import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.core.events.Play.SKIP_NEXT;
import static buddybox.core.events.Play.SKIP_PREVIOUS;
import static buddybox.core.events.Play.FINISHED_PLAYING;

import static buddybox.core.events.Sampler.*;

public class Model implements IModel {

    private final Context context;
    private final Handler handler = new Handler();
    private List<StateListener> listeners = new ArrayList<>();

    private File musicDirectory;
    private Playlist currentPlaylist;
    private Integer currentSongIndex;

    private boolean isSampling = false;
    private Playlist samplerPlaylist;

    private ArrayList<Playlist> playlists;
    private List<Song> allSongs;

    private boolean isPaused;
    private Boolean hasWriteExternalStoragePermission;

    public Model(Context context) {
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
            songFound((SongFound)event);

        if (event.getClass() == SongMissing.class)
            songMissing((SongMissing)event);

        updateListeners();
    }

    private void songMissing(SongMissing event) {
        ContentValues values = new ContentValues();
        values.put("IS_MISSING", true);
        DatabaseHelper.getInstance(context).getReadableDatabase().update("SONGS", values, "HASH=?", new String[]{event.song.hash.toString()});
        event.song.setMissing();
    }

    private void songFound(SongFound event) {
        System.out.println("111 song found");

        Song song = findSongByHash(event.song.hash);
        if (song == null)
            insertNewSong(event.song);
        else
            updateSong(event.song);

        allSongs.add(event.song);
    }

    private Song findSongByHash(Hash hash) {
        for (Song song : allSongs) {
            if (song.hash.equals(hash))
                return song;
        }
        return null;
    }

    private void insertNewSong(Song song) {
        ContentValues newSong = songContents(song);
        newSong.put("HASH", song.hash.toString());
        DatabaseHelper.getInstance(context).getReadableDatabase().insert("SONGS", null, newSong);
    }

    private void updateSong(Song song) {
        DatabaseHelper.getInstance(context).getReadableDatabase().update("SONGS", songContents(song), "HASH=?", new String[]{song.hash.toString()});
    }

    private ContentValues songContents(Song song) {
        ContentValues ret = new ContentValues();
        ret.put("NAME", song.name);
        ret.put("GENRE", song.genre);
        ret.put("ARTIST", song.artist);
        ret.put("DURATION", song.duration);
        ret.put("RELATIVE_PATH", song.relativePath);
        ret.put("FILE_LENGTH", song.fileLength);
        ret.put("LAST_MODIFIED", song.lastModified);
        ret.put("IS_MISSING", song.isMissing ? 1 : 0);
        return ret;
    }

    private void samplerUpdate(SamplerUpdated event) {
        samplerPlaylist = new Playlist(666, "Sampler", event.samples);
    }

    private ArrayList<Artist> artists() {
        Map<String, Artist> artistsMap = new HashMap<>();
        for (Song song : allSongs()) {
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
            hasWriteExternalStoragePermission = event.granted;
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

    private boolean hasWriteExternalStoragePermission() {
        return hasWriteExternalStoragePermission != null && hasWriteExternalStoragePermission;
    }

    private State getState() {
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
                hasWriteExternalStoragePermission());
    }

    private Playlist playlistAllSongs() {
        return new Playlist(0, "Recent", allSongsAvailable());
    }

    private List<Song> allSongsAvailable() {
        List<Song> ret = new ArrayList<>();
        for (Song song : allSongs())
           if (!song.isMissing)
               ret.add(song);
        return ret;
    }


    private List<Song> allSongs() {
        if (allSongs == null) {
            allSongs = new ArrayList<>();
            Cursor cursor = DatabaseHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM SONGS", null);
            while(cursor.moveToNext()) {
                Song song = new Song(
                        new Hash(cursor.getString(cursor.getColumnIndex("HASH"))),
                        cursor.getString(cursor.getColumnIndex("NAME")),
                        cursor.getString(cursor.getColumnIndex("GENRE")),
                        cursor.getString(cursor.getColumnIndex("ARTIST")),
                        cursor.getInt(cursor.getColumnIndex("DURATION")),
                        cursor.getString(cursor.getColumnIndex("RELATIVE_PATH")),
                        cursor.getLong(cursor.getColumnIndex("FILE_LENGTH")),
                        cursor.getLong(cursor.getColumnIndex("LAST_MODIFIED")),
                        cursor.getInt(cursor.getColumnIndex("IS_MISSING")) == 1);
                allSongs.add(song);
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

        for (Song song : allSongs()) {
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
