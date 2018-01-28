package buddybox.model;

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
import java.util.concurrent.ConcurrentHashMap;

import buddybox.core.events.AddSongToPlaylist;
import buddybox.core.Artist;
import buddybox.core.events.CreatePlaylist;
import buddybox.core.Dispatcher;
import utils.Hash;
import buddybox.core.IModel;
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
import static buddybox.core.events.Play.REPEAT_SONG;
import static buddybox.core.events.Play.SKIP_NEXT;
import static buddybox.core.events.Play.SKIP_PREVIOUS;
import static buddybox.core.events.Play.FINISHED_PLAYING;
import static buddybox.core.events.Library.SYNC_LIBRARY;
import static buddybox.core.events.Library.SYNC_LIBRARY_FINISHED;

import static buddybox.core.events.Sampler.*;

/** The Model is modified only through dispatched events, handled sequentially.
 * Only the Model handles the database. */
public class Model implements IModel {

    private final Context context;
    private final Handler handler = new Handler();
    private List<StateListener> listeners = new ArrayList<>();

    private File musicDirectory;
    private Playlist currentPlaylist;
    private Integer currentSongIndex;

    private boolean isSampling = false;
    private Playlist samplerPlaylist;

    private List<Song> allSongs;
    private Map<String, Song> songsByHash;
    private ArrayList<Playlist> playlists;
    private HashMap<Long, Playlist> playlistsById;

    private boolean isPaused;
    private boolean repeatSong = false;
    private boolean syncLibraryRequested = false;

    public Model(Context context) {
        this.context = context;

        System.out.println(DatabaseHelper.getInstance(context));

        Dispatcher.addListener(new Dispatcher.Listener() { @Override public void onEvent(Dispatcher.Event event) {
            handle(event);
        }});
    }

    private void handle(Dispatcher.Event event) {
        Class<? extends Dispatcher.Event> cls = event.getClass();
        System.out.println("@@@ Event class " + cls);

        if (cls == SongFound.class)   songFound((SongFound)event);
        if (cls == SongMissing.class) songMissing((SongMissing)event);

        if (cls == Play.class) play((Play)event);

        if (event == PLAY_PAUSE_CURRENT) playPauseCurrent();
        if (event == SKIP_NEXT) skip(+1);
        if (event == SKIP_PREVIOUS) skip(-1);
        if (event == REPEAT_SONG) repeatSong();
        if (event == FINISHED_PLAYING) finishedPlaying();

        if (cls == CreatePlaylist.class)    createPlaylist((CreatePlaylist) event);
        if (cls == AddSongToPlaylist.class) addSongToPlaylist((AddSongToPlaylist) event);

        if (cls == SamplerUpdated.class) samplerUpdate((SamplerUpdated) event);
        if (event == SAMPLER_START)      samplerStart();
        if (event == SAMPLER_STOP)       samplerStop();
        if (cls == SamplerHate.class)    samplerHate((SamplerHate) event);
        if (cls == SamplerDelete.class)  samplerDelete((SamplerDelete) event);
        if (cls == SamplerLove.class)    samplerLove((SamplerLove) event);

        if (event == SYNC_LIBRARY)          syncLibrary();
        if (event == SYNC_LIBRARY_FINISHED) syncLibraryStarted();

        if (event == LOVED_VIEWED) lovedViewed();

        updateListeners();
    }

    private void syncLibraryStarted() {
        syncLibraryRequested = false;
    }

    private void syncLibrary() {
        syncLibraryRequested = true;
    }

    private void repeatSong() {
        repeatSong = !repeatSong;
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

        addSong(event.song);
    }

    private void addSong(Song song) {
        allSongs.add(song);
        songsByHash.put(song.hash.toString(), song);
    }

    private Song findSongByHash(Hash hash) {
        for (Song song : allSongs)
            if (song.hash.equals(hash))
                return song;
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
        ret.put("FILE_PATH", song.filePath);
        ret.put("FILE_LENGTH", song.fileLength);
        ret.put("LAST_MODIFIED", song.lastModified);
        ret.put("IS_MISSING", song.isMissing ? 1 : 0);
        return ret;
    }

    private void samplerUpdate(SamplerUpdated event) {
        samplerPlaylist = new Playlist(666, "Sampler", event.samples);
    }

    private ArrayList<Artist> artists() {
        Map<String, Artist> artistsByName = new HashMap<>();
        for (Song song : allSongs()) {
            Artist artist = artistsByName.get(song.artist);
            if (artist == null) {
                artist = new Artist(song.artist);
                artistsByName.put(song.artist, artist);
            }
            artist.addSong(song);
        }
        return new ArrayList<>(artistsByName.values());
    }

    private void finishedPlaying() {
        if (isSampling)
            doPlay(samplerPlaylist, 0);
        else {
            if (repeatSong)
                doPlay(currentPlaylist, currentSongIndex);
            else
                skip(+1);
        }

    }

    private void createPlaylist(CreatePlaylist event) {
        // Creates playlist
        ContentValues contents = new ContentValues();
        contents.put("NAME", event.playlistName);
        long playlistId = DatabaseHelper.getInstance(context).getReadableDatabase().insert("PLAYLISTS", null, contents);

        // Associates songs with playlist
        associateSongToPlaylist(event.songHash, playlistId);

        // Add new playlist
        List<Song> songs = new ArrayList<>();
        songs.add(songsByHash.get(event.songHash));
        Playlist playlist = new Playlist(playlistId, event.playlistName, songs);
        addPlaylist(playlist);

        System.out.println("@@@ Dispatched Event: createPlaylist. Song: " +event.songHash + ", playlist: " + event.playlistName);
    }

    private void addSongToPlaylist(AddSongToPlaylist event) {
        associateSongToPlaylist(event.songHash, event.playlistId);

        Playlist playlist = playlistsById.get(event.playlistId);
        playlist.addSong(songsByHash.get(event.songHash));

        System.out.println("@@@ Dispatched Event: addSongToPlaylist. Playlist id: " + event.playlistId + ", song: " + event.songHash);
    }

    private void associateSongToPlaylist(String songHash, long playlistId) {
        ContentValues playlistSong = new ContentValues();
        playlistSong.put("PLAYLIST_ID", playlistId);
        playlistSong.put("SONG_HASH", songHash);
        DatabaseHelper.getInstance(context).getReadableDatabase().insert("PLAYLIST_SONG", null, playlistSong);
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
                repeatSong,
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
                syncLibraryRequested);
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
            songsByHash = new HashMap<>();
            Cursor cursor = DatabaseHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM SONGS", null);
            while(cursor.moveToNext()) {
                Song song = new Song(
                        new Hash(cursor.getString(cursor.getColumnIndex("HASH"))),
                        cursor.getString(cursor.getColumnIndex("NAME")),
                        cursor.getString(cursor.getColumnIndex("GENRE")),
                        cursor.getString(cursor.getColumnIndex("ARTIST")),
                        cursor.getInt(cursor.getColumnIndex("DURATION")),
                        cursor.getString(cursor.getColumnIndex("FILE_PATH")),
                        cursor.getLong(cursor.getColumnIndex("FILE_LENGTH")),
                        cursor.getLong(cursor.getColumnIndex("LAST_MODIFIED")),
                        cursor.getInt(cursor.getColumnIndex("IS_MISSING")) == 1);
                addSong(song);
            }
            cursor.close();
        }
        return allSongs;
    }

    private List<Playlist> playlists() {
        if (playlists == null) {
            playlists = new ArrayList<>();
            playlistsById = new HashMap<>();
            Cursor cursor = DatabaseHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM PLAYLISTS", null);
            while(cursor.moveToNext()) {
                long playlistId = cursor.getLong(cursor.getColumnIndex("ID"));
                List<Song> songs = queryPlaylistSongs(playlistId);
                Playlist playlist = new Playlist(playlistId, cursor.getString(cursor.getColumnIndex("NAME")), songs);
                addPlaylist(playlist);
            }
            cursor.close();
        }
        return playlists;
    }

    synchronized
    private void addPlaylist(Playlist playlist) {
        playlists.add(playlist);
        playlistsById.put(playlist.id, playlist);
    }

    private List<Song> queryPlaylistSongs(long playlistId) {
        List<Song> songs = new ArrayList<>();
        Cursor cursor = DatabaseHelper.getInstance(context).getReadableDatabase().query("PLAYLIST_SONG", new String[]{"SONG_HASH"}, "PLAYLIST_ID=?", new String[]{Long.toString(playlistId)}, null, null, null);
        while(cursor.moveToNext()) {
            String hash = cursor.getString(cursor.getColumnIndex("SONG_HASH"));
            songs.add(songsByHash.get(hash));
        }
        cursor.close();
        return songs;
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
        updateListener(listener);
    }

    @Override
    public void removeStateListener(StateListener listener) {
        this.listeners.remove(listener);
    }
}
