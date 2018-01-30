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

import buddybox.core.events.AddSongToPlaylist;
import buddybox.core.Artist;
import buddybox.core.events.CreatePlaylist;
import buddybox.core.Dispatcher;
import buddybox.core.events.DeletePlaylist;
import buddybox.core.events.PlaylistSelected;
import buddybox.core.events.RemoveSongFromPlaylist;
import buddybox.core.events.SetPlaylistName;
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
import static buddybox.core.events.Play.REPEAT_ALL;
import static buddybox.core.events.Play.REPEAT_SONG;
import static buddybox.core.events.Play.SHUFFLE;
import static buddybox.core.events.Play.SHUFFLE_PLAY;
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
    private boolean repeatAll = true;
    private boolean repeatSong = false;
    private boolean syncLibraryRequested = false;
    private Playlist selectedPlaylist;
    private Map<String, List<Playlist>> playlistsBySong;
    private boolean isShuffle = false;

    public Model(Context context) {
        this.context = context;

        // DatabaseHelper.getInstance(context).getReadableDatabase().execSQL("delete from PLAYLISTS");
        // DatabaseHelper.getInstance(context).getReadableDatabase().execSQL("delete from PLAYLIST_SONG");

        System.out.println(DatabaseHelper.getInstance(context));

        Dispatcher.addListener(new Dispatcher.Listener() { @Override public void onEvent(Dispatcher.Event event) {
            handle(event);
        }});
    }

    private void handle(Dispatcher.Event event) {
        Class<? extends Dispatcher.Event> cls = event.getClass();
        System.out.println("@@@ Event class " + cls);

        // player
        if (cls == Play.class) play((Play)event);
        if (event == SHUFFLE_PLAY) shufflePlay();
        if (event == PLAY_PAUSE_CURRENT) playPauseCurrent();
        if (event == SKIP_NEXT) skip(+1);
        if (event == SKIP_PREVIOUS) skip(-1);
        if (event == REPEAT_SONG) repeatSong();
        if (event == REPEAT_ALL) repeatAll();
        if (event == SHUFFLE) shuffle();
        if (event == FINISHED_PLAYING) finishedPlaying();

        // playlist
        if (cls == CreatePlaylist.class)            createPlaylist((CreatePlaylist) event);
        if (cls == DeletePlaylist.class)            deletePlaylist((DeletePlaylist) event);
        if (cls == AddSongToPlaylist.class)         addSongToPlaylist((AddSongToPlaylist) event);
        if (cls == RemoveSongFromPlaylist.class)    removeSongFromPlaylist((RemoveSongFromPlaylist) event);
        if (cls == SetPlaylistName.class)           setPlaylistName((SetPlaylistName) event);
        if (cls == PlaylistSelected.class)          playlistSelected((PlaylistSelected) event);

        // sampler
        if (cls == SamplerUpdated.class) samplerUpdate((SamplerUpdated) event);
        if (event == SAMPLER_START)      samplerStart();
        if (event == SAMPLER_STOP)       samplerStop();
        if (cls == SamplerHate.class)    samplerHate((SamplerHate) event);
        if (cls == SamplerDelete.class)  samplerDelete((SamplerDelete) event);
        if (cls == SamplerLove.class)    samplerLove((SamplerLove) event);

        if (event == LOVED_VIEWED) lovedViewed();

        // library
        if (cls == SongFound.class)         songFound((SongFound)event);
        if (cls == SongMissing.class)       songMissing((SongMissing)event);
        if (event == SYNC_LIBRARY)          syncLibrary();
        if (event == SYNC_LIBRARY_FINISHED) syncLibraryStarted();

        updateListeners();
    }

    private void setPlaylistName(SetPlaylistName event) {
        ContentValues values = new ContentValues();
        values.put("NAME", event.playlistName);
        int rows = DatabaseHelper.getInstance(context).getReadableDatabase().update(
                "PLAYLISTS",
                values,
                "ID=?",
                new String[]{Long.toString(selectedPlaylist.id)});
        if (rows == 1)
            selectedPlaylist.setName(event.playlistName);
    }

    private void shuffle() {
        isShuffle = !isShuffle;
    }

    private void shufflePlay() {
        isShuffle = true;
        currentPlaylist = selectedPlaylist;
        doPlay(currentPlaylist, currentPlaylist.firstShuffleIndex());
    }

    private void removeSongFromPlaylist(RemoveSongFromPlaylist event) {
        Song song = songsByHash.get(event.songHash);
        Playlist playlist = playlistsById.get(event.playlistId);
        int songPosition = playlist.songs.indexOf(song);

        // update position-1 for relations.position > song index
        DatabaseHelper.getInstance(context).getReadableDatabase().execSQL(
                "UPDATE PLAYLIST_SONG " +
                        "SET POSITION = POSITION -1 " +
                        "WHERE PLAYLIST_ID = " + event.playlistId + " " +
                        "AND SONG_HASH = '" + event.songHash + "' " +
                        "AND POSITION > " + songPosition);

        // delete from associations table
        DatabaseHelper.getInstance(context).getReadableDatabase().delete("PLAYLIST_SONG", "PLAYLIST_ID=? AND SONG_HASH=?", new String[]{Long.toString(event.playlistId), event.songHash});

        // remove from playlistsBySong
        List<Playlist> songPlaylists = playlistsBySong.get(event.songHash);
        songPlaylists.remove(playlist);
        playlistsBySong.put(event.songHash, songPlaylists);

        // keep playing the current song
        if (currentPlaylist == playlist && currentSongIndex > songPosition)
            currentSongIndex--;

        // remove from object
        playlist.removeSong(song);
    }

    private void playlistSelected(PlaylistSelected event) {
        selectedPlaylist = event.playlist;
    }

    private void syncLibraryStarted() {
        syncLibraryRequested = false;
    }

    private void syncLibrary() {
        syncLibraryRequested = true;
    }

    private void repeatAll() {
        System.out.println("repeatAll: " + !repeatAll);
        repeatAll = !repeatAll;
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

        // Add new playlist
        addPlaylist(new Playlist(playlistId, event.playlistName, new ArrayList<Song>()));

        // Associates songs with playlist
        insertAssociationSongToPlaylist(event.songHash, playlistId);

        System.out.println("@@@ Dispatched Event: createPlaylist. Song: " +event.songHash + ", playlist: " + event.playlistName);
    }

    private void deletePlaylist(DeletePlaylist event) {
        Playlist playlist = playlistsById.get(event.playlistId);

        // delete from table
        int rowsP = DatabaseHelper.getInstance(context).getReadableDatabase().delete("PLAYLISTS", "ID=?", new String[]{Long.toString(event.playlistId)});
        if (rowsP != 1) {
            System.out.println("Unable to delete playlist in DB");
            return;
        }

        // delete associations to songs
        int rowsA = DatabaseHelper.getInstance(context).getReadableDatabase().delete("PLAYLIST_SONG", "PLAYLIST_ID=?", new String[]{Long.toString(event.playlistId)});
        if (rowsA != playlist.songs.size()) {
            System.out.println("Unable to delete all playlists-song associations");
            return;
        }

        // remove playlist from maps
        for (Song song : playlist.songs) {
            String songHash = song.hash.toString();
            List<Playlist> songPlaylists = playlistsBySong.get(songHash);
            songPlaylists.remove(playlist);
            playlistsBySong.put(songHash, songPlaylists);
        }
        playlists.remove(playlist);
        playlistsById.remove(playlist.id);
        selectedPlaylist = null;

        if (currentPlaylist == playlist) {
            currentPlaylist = null;
            currentSongIndex = null;
        }

        System.out.println(">>> Playlist deleted: " + playlist.name);
    }

    private void addSongToPlaylist(AddSongToPlaylist event) {
        insertAssociationSongToPlaylist(event.songHash, event.playlistId);
        System.out.println("@@@ Dispatched Event: addSongToPlaylist. Playlist id: " + event.playlistId + ", song: " + event.songHash);
    }

    private void insertAssociationSongToPlaylist(String songHash, long playlistId) {
        ContentValues playlistSong = new ContentValues();
        playlistSong.put("PLAYLIST_ID", playlistId);
        playlistSong.put("SONG_HASH", songHash);
        playlistSong.put("POSITION", playlistsById.get(playlistId).size());
        DatabaseHelper.getInstance(context).getReadableDatabase().insert("PLAYLIST_SONG", null, playlistSong);

        addSongToPlaylist(songHash, playlistId);
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
        if (step > 0 && currentPlaylist.isLastSong(currentSongIndex, isShuffle) && !repeatAll) {
            playPauseCurrent();
            return;
        }

        doPlay(currentPlaylist, currentPlaylist.songAfter(currentSongIndex, step, isShuffle));
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
                currentSong(),
                currentPlaylist,
                isPaused,
                isShuffle,
                repeatAll,
                repeatSong,
                playlistsBySong,
                isSampling,
                samplerPlaylist,
                lovedPlaylist(),
                playlists(),
                null,
                1,
                getAvailableMemorySize(),
                playlistAllSongs(),
                artists(),
                syncLibraryRequested,
                selectedPlaylist);
    }

    private Song currentSong() {
        return isSampling
                    ? samplerPlaylist.song(0)
                    : currentSongIndex == null
                        ? null
                        : currentPlaylist.song(currentSongIndex);
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

            // Create playlist map
            Cursor cursor = DatabaseHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM PLAYLISTS", null);
            while(cursor.moveToNext()) {
                long playlistId = cursor.getLong(cursor.getColumnIndex("ID"));
                Playlist playlist = new Playlist(playlistId, cursor.getString(cursor.getColumnIndex("NAME")), new ArrayList<Song>());
                addPlaylist(playlist);
            }
            cursor.close();

            // Associates songs to playlists
            playlistsBySong = new HashMap<>();
            Cursor cursorAssoc = DatabaseHelper.getInstance(context).getReadableDatabase().rawQuery("SELECT * FROM PLAYLIST_SONG ORDER BY PLAYLIST_ID, POSITION", null);
            while(cursorAssoc.moveToNext()) {
                String songHash = cursorAssoc.getString(cursorAssoc.getColumnIndex("SONG_HASH"));
                Long playlistId = cursorAssoc.getLong(cursorAssoc.getColumnIndex("PLAYLIST_ID"));
                addSongToPlaylist(songHash, playlistId);
            }
            cursorAssoc.close();
        }
        return playlists;
    }

    private void addSongToPlaylist(String songHash, Long playlistId) {
        Song song = songsByHash.get(songHash);
        Playlist playlist = playlistsById.get(playlistId);

        playlist.addSong(song);

        // add playlist to song map
        List<Playlist> songPlaylists = playlistsBySong.get(songHash);
        if (songPlaylists == null)
            songPlaylists = new ArrayList<>();
        songPlaylists.add(playlist);
        playlistsBySong.put(songHash, songPlaylists);
    }

    synchronized
    private void addPlaylist(Playlist playlist) {
        playlists.add(playlist);
        playlistsById.put(playlist.id, playlist);
        System.out.println("added new playlist");
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
