package buddybox.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.util.LongSparseArray;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;
import buddybox.core.IModel;
import buddybox.core.Playable;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.AlbumArtEmbeddedFound;
import buddybox.core.events.AlbumArtFound;
import buddybox.core.events.ArtistBioFound;
import buddybox.core.events.ArtistPictureFound;
import buddybox.core.events.ArtistSelected;
import buddybox.core.events.CreatePlaylist;
import buddybox.core.events.DeletePlaylist;
import buddybox.core.events.Play;
import buddybox.core.events.PlaylistAddSong;
import buddybox.core.events.PlaylistChangeSongPosition;
import buddybox.core.events.PlaylistRemoveSong;
import buddybox.core.events.PlaylistSelected;
import buddybox.core.events.PlaylistSetName;
import buddybox.core.events.SamplerDelete;
import buddybox.core.events.SamplerHate;
import buddybox.core.events.SamplerLove;
import buddybox.core.events.SamplerUpdated;
import buddybox.core.events.SeekTo;
import buddybox.core.events.SetBluetoothVolume;
import buddybox.core.events.SetHeadphonesVolume;
import buddybox.core.events.SetSpeakerVolume;
import buddybox.core.events.SongDeleteRequest;
import buddybox.core.events.SongDeleted;
import buddybox.core.events.SongFound;
import buddybox.core.events.SongMissing;
import buddybox.core.events.SongSelected;
import buddybox.core.events.SongUpdate;
import sov.Hash;

import static buddybox.core.events.AudioFocus.AUDIO_FOCUS_GAIN;
import static buddybox.core.events.AudioFocus.AUDIO_FOCUS_LOSS;
import static buddybox.core.events.AudioFocus.AUDIO_FOCUS_LOSS_TRANSIENT;
import static buddybox.core.events.Bluetooth.BLUETOOTH_CONNECT;
import static buddybox.core.events.Bluetooth.BLUETOOTH_DISCONNECT;
import static buddybox.core.events.Library.SYNC_LIBRARY;
import static buddybox.core.events.Library.SYNC_LIBRARY_FINISHED;
import static buddybox.core.events.Play.FINISHED_PLAYING;
import static buddybox.core.events.Play.PAUSE;
import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.core.events.Play.REPEAT;
import static buddybox.core.events.Play.SHUFFLE;
import static buddybox.core.events.Play.SHUFFLE_PLAY;
import static buddybox.core.events.Play.SHUFFLE_PLAY_ARTIST;
import static buddybox.core.events.Play.SKIP_NEXT;
import static buddybox.core.events.Play.SKIP_PREVIOUS;
import static buddybox.core.events.Play.TOGGLE_DURATION_REMAINING;
import static buddybox.core.events.Sampler.LOVED_VIEWED;
import static buddybox.core.events.Sampler.SAMPLER_START;
import static buddybox.core.events.Sampler.SAMPLER_STOP;
import static buddybox.core.events.SetHeadphonesVolume.HEADPHONES_CONNECTED;
import static buddybox.core.events.SetHeadphonesVolume.HEADPHONES_DISCONNECTED;

/**
 * The Model is modified only through dispatched events, handled sequentially.
 * Only the Model handles the database.
 */
public class Model implements IModel {

    private static final String TAG = "Model";

    public static final String HEADPHONES = "headphones";
    public static final String SPEAKER = "speaker";
    public static final String BLUETOOTH = "bluetooth";

    public SQLiteDatabase db;
    private List<StateListener> listeners = new ArrayList<>();

    private File musicDirectory;
    private Playlist currentPlaylist;
    private Integer currentSongIndex;

    private boolean isSampling = false;
    private Playlist samplerPlaylist;

    private Set<Song> allSongs;
    private Map<String, Song> songsByHash;
    private ArrayList<Playlist> playlists;
    private LongSparseArray<Playlist> playlistsById = new LongSparseArray<>();

    private boolean isPaused = true;
    private boolean isStopped = true;
    private boolean isShuffle = false;
    private boolean repeatAll = true;
    private boolean repeatSong = false;

    private Playlist selectedPlaylist;
    private Map<String, List<Playlist>> playlistsBySong;

    private boolean syncLibraryRequested = false;
    private Song deleteSong;
    private Song songSelected;
    private Integer seekTo;
    private Long mediaStorageUsed;
    private boolean isHeadphoneConnected = false;

    private boolean wasPlayingBeforeLostAudioFocus = false;
    private boolean isBluetoothConnected;
    private Map<String,Integer>  volumeSettings;
    private boolean hasAudioFocus = false;
    private boolean showDuration = true;

    private ArrayList<Artist> artists;
    private Artist artistSelected;
    private HashMap<String, String> bios;

    public Model(Context context) {
        if (context != null)
            this.db = DatabaseHelper.getInstance(context).getReadableDatabase();

        // db.execSQL("delete from PLAYLISTS");
        // db.execSQL("delete from PLAYLIST_SONG");

        Dispatcher.addListener(new Dispatcher.Listener() { @Override public void onEvent(Dispatcher.Event event) {
            handle(event);
        }});
    }

    private void handle(Dispatcher.Event event) {
        Class<? extends Dispatcher.Event> cls = event.getClass();

        if (event.getClass() != Dispatcher.Event.class && event.getClass().isAssignableFrom(Dispatcher.Event.class))
            Log.i(TAG, "EVENT " + cls);
        else
            Log.i(TAG, "EVENT " + event.type);

        // player
        if (cls == Play.class) play((Play) event);
        if (cls == SeekTo.class) seekTo((SeekTo) event);
        if (event == SHUFFLE_PLAY) shufflePlay();
        if (event == SHUFFLE_PLAY_ARTIST) shufflePlayArtist();
        if (event == PLAY_PAUSE_CURRENT) playPauseCurrent();
        if (event == PAUSE) pause();
        if (event == SKIP_NEXT) skip(+1);
        if (event == SKIP_PREVIOUS) skip(-1);
        if (event == REPEAT) repeat();
        if (event == SHUFFLE) shuffle();
        if (event == FINISHED_PLAYING) finishedPlaying();
        if (event == TOGGLE_DURATION_REMAINING) toggleDurationRemaining();

        // sound output
        if (event == HEADPHONES_CONNECTED) headphonesConnected();
        if (event == HEADPHONES_DISCONNECTED) headphonesDisconnected();
        if (cls == SetBluetoothVolume.class) setBluetoothVolume((SetBluetoothVolume) event);
        if (cls == SetSpeakerVolume.class) setSpeakerVolume((SetSpeakerVolume) event);
        if (cls == SetHeadphonesVolume.class) setHeadphonesVolume((SetHeadphonesVolume) event);

        // playlist
        if (cls == CreatePlaylist.class) createPlaylist((CreatePlaylist) event);
        if (cls == DeletePlaylist.class) deletePlaylist((DeletePlaylist) event);
        if (cls == PlaylistAddSong.class) addSongToPlaylist((PlaylistAddSong) event);
        if (cls == PlaylistRemoveSong.class) removeSongFromPlaylist((PlaylistRemoveSong) event);
        if (cls == PlaylistSetName.class) setPlaylistName((PlaylistSetName) event);
        if (cls == PlaylistSelected.class) playlistSelected((PlaylistSelected) event);
        if (cls == PlaylistChangeSongPosition.class)
            playlistChangeSongPosition((PlaylistChangeSongPosition) event);

        // sampler
        if (cls == SamplerUpdated.class) samplerUpdate((SamplerUpdated) event);
        if (event == SAMPLER_START) samplerStart();
        if (event == SAMPLER_STOP) samplerStop();
        if (cls == SamplerHate.class) samplerHate((SamplerHate) event);
        if (cls == SamplerDelete.class) samplerDelete((SamplerDelete) event);
        if (cls == SamplerLove.class) samplerLove((SamplerLove) event);

        if (event == LOVED_VIEWED) lovedViewed();

        // library
        if (cls == SongFound.class) songFound((SongFound) event);
        if (cls == SongMissing.class) songMissing((SongMissing) event);
        if (cls == SongDeleted.class) songDeleted((SongDeleted) event);
        if (cls == SongDeleteRequest.class) songDeleteRequest((SongDeleteRequest) event);
        if (cls == SongSelected.class) songSelected((SongSelected) event);
        if (cls == SongUpdate.class) songUpdate((SongUpdate) event);
        if (event == SYNC_LIBRARY) syncLibrary();
        if (event == SYNC_LIBRARY_FINISHED) syncLibraryStarted();

        // audio focus
        if (event == AUDIO_FOCUS_LOSS) audioFocusLoss();
        if (event == AUDIO_FOCUS_GAIN) audioFocusGain();
        if (event == AUDIO_FOCUS_LOSS_TRANSIENT) audioFocusLossTransient();

        // bluetooth
        if (event == BLUETOOTH_CONNECT) bluetoothConnect();
        if (event == BLUETOOTH_DISCONNECT) bluetoothDisconnect();

        // media info
        if (cls == AlbumArtFound.class) albumArtFound((AlbumArtFound) event);
        if (cls == AlbumArtEmbeddedFound.class) albumArtEmbeddedFound((AlbumArtEmbeddedFound) event);
        if (cls == ArtistPictureFound.class) artistPictureFound((ArtistPictureFound) event);
        if (cls == ArtistBioFound.class) artistBioFound((ArtistBioFound) event);

        // artist
        if (cls == ArtistSelected.class) artistSelected((ArtistSelected) event);

        updateListeners();
    }

    private void artistBioFound(ArtistBioFound event) {
        ContentValues values = new ContentValues();
        values.put("ARTIST_NAME", event.artist.name);
        values.put("CONTENT", event.content);

        if (event.artist.bio == null)
            db.insert(
                    "ARTIST_BIO",
                    null,
                    values);
        else
            db.update(
                "ARTIST_BIO",
                values,
                "ARTIST_NAME=?",
                new String[]{event.artist.name});

        bios.put(event.artist.name, event.content);
        event.artist.setBio(event.content);
    }


    private void artistSelected(ArtistSelected event) {
        artistSelected = event.artist;
    }

    private void artistPictureFound(ArtistPictureFound event) {
        event.artist.setPicture(event.picture);
    }

    private void albumArtFound(AlbumArtFound event) {
        for (Song song : allSongs)
            if (song.getArt() == null
                    && song.artist.equals(event.artist)
                    && song.album.equals(event.album))
                song.setArt(event.art);
    }

    private void albumArtEmbeddedFound(AlbumArtEmbeddedFound event) {
        event.song.setEmbeddedArt(event.art);
    }

    private void toggleDurationRemaining() {
        showDuration = !showDuration;
    }

    private void audioFocusLossTransient() {
        wasPlayingBeforeLostAudioFocus = !isPaused;
        if (!isPaused)
            playPauseCurrent();
    }

    private void audioFocusGain() {
        hasAudioFocus = true;
        if (wasPlayingBeforeLostAudioFocus && isPaused)
            playPauseCurrent();
        wasPlayingBeforeLostAudioFocus = false;
    }

    private void audioFocusLoss() {
        hasAudioFocus = false;
        isPaused = true; // should stop?
    }

    private void bluetoothDisconnect() {
        isBluetoothConnected = false;
    }

    private void bluetoothConnect() {
        isBluetoothConnected = true;
    }

    private void playlistChangeSongPosition(PlaylistChangeSongPosition event) {
        /* UPDATE DB */
        if (event.fromPosition > event.toPosition) {
            // Slide +1 position songs >= toPosition and < fromPosition
            db.execSQL(
                    "UPDATE PLAYLIST_SONG " +
                            "SET POSITION = POSITION +1 " +
                            "WHERE  PLAYLIST_ID = " + selectedPlaylist.id + " " +
                            "AND    POSITION <  " + event.fromPosition + " " +
                            "AND    POSITION >= " + event.toPosition);
        } else {
            // Slide -1 position songs <= toPosition and > fromPosition
            db.execSQL(
                    "UPDATE PLAYLIST_SONG " +
                            "SET POSITION = POSITION -1 " +
                            "WHERE  PLAYLIST_ID = " + selectedPlaylist.id + " " +
                            "AND    POSITION >  " + event.fromPosition + " " +
                            "AND    POSITION <= " + event.toPosition);
        }

        // Update moved song position
        Song song = selectedPlaylist.song(event.fromPosition);
        db.execSQL(
                "UPDATE PLAYLIST_SONG " +
                        "SET POSITION = " + event.toPosition + " " +
                        "WHERE  PLAYLIST_ID = " + selectedPlaylist.id + " " +
                        "AND    SONG_HASH = '" + song.hash.toString() + "'");

        /* UPDATE OBJECTS */
        Song currentSong = null;
        if (currentSongIndex != null && currentPlaylist == selectedPlaylist)
            currentSong = selectedPlaylist.song(currentSongIndex);

        selectedPlaylist.changeSongPosition(event.fromPosition, event.toPosition);

        if (currentSong != null && currentPlaylist == selectedPlaylist)
            currentSongIndex = selectedPlaylist.songs.indexOf(currentSong);
    }

    private void seekTo(SeekTo event) {
        seekTo = event.position;
    }

    private void songUpdate(SongUpdate event) {
        // TODO update artists if event.artist != song.artist
        Song song = event.song;
        song.setName(event.name);
        song.setArtist(event.artist);
        song.setAlbum(event.album);
        song.setGenre(event.genre);
        updateSong(song);
    }

    private void setPlaylistName(PlaylistSetName event) {
        ContentValues values = new ContentValues();
        values.put("NAME", event.playlistName);
        int rows = db.update(
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
        if (selectedPlaylist.isEmpty()) {
            if (currentPlaylist == selectedPlaylist && !isPaused) {
                playPauseCurrent();
                currentPlaylist = null;
            }
            return;
        }

        isShuffle = true;
        currentPlaylist = selectedPlaylist;

        // Skip missing songs
        int firstSongIndex = currentPlaylist.firstShuffleIndex();
        int nextSongIndex = firstSongIndex;
        boolean loop = false;
        while (currentPlaylist.song(nextSongIndex).isMissing && !loop) {
            nextSongIndex = currentPlaylist.songAfter(nextSongIndex, 1, true);
            loop = nextSongIndex == firstSongIndex;
        }

        // Pause if all songs are missing
        if (loop) {
            playPauseCurrent();
            currentPlaylist = null;
            return;
        }

        doPlay(currentPlaylist, nextSongIndex);
    }

    private void shufflePlayArtist() {
        // TODO auto create/update a playlist for each artist
        // do not show artist playlist at custom playlists
        selectedPlaylist = new Playlist(System.currentTimeMillis(), artistSelected.name, new ArrayList<>(artistSelected.songs));
        shufflePlay();
    }

    private void removeSongFromPlaylist(PlaylistRemoveSong event) {
        String songHash = event.song.hash.toString();
        Playlist playlist = event.playlist;
        int songPosition = playlist.songs.indexOf(event.song);

        // update position-1 for relations.position > song index
        db.execSQL(
                "UPDATE PLAYLIST_SONG " +
                        "SET POSITION = POSITION -1 " +
                        "WHERE PLAYLIST_ID = " + playlist.id + " " +
                        "AND SONG_HASH = '" + songHash + "' " +
                        "AND POSITION > " + songPosition);

        // delete from associations table
        db.delete("PLAYLIST_SONG", "PLAYLIST_ID=? AND SONG_HASH=?", new String[]{Long.toString(playlist.id), songHash});

        // remove from playlistsBySong
        List<Playlist> songPlaylists = playlistsBySong.get(songHash);
        songPlaylists.remove(playlist);
        playlistsBySong.put(songHash, songPlaylists);

        // keep activity_playing the current song
        if (currentPlaylist == playlist && currentSongIndex > songPosition)
            currentSongIndex--;

        // remove from object
        playlist.removeSong(event.song);
    }

    private void playlistSelected(PlaylistSelected event) {
        selectedPlaylist = event.playlist;
    }

    private void syncLibrary() { syncLibraryRequested = true; }

    private void syncLibraryStarted() {
        syncLibraryRequested = false;
    }

    private void songDeleteRequest(SongDeleteRequest event) {
        deleteSong = songsByHash.get(event.songHash);
    }

    private void songSelected(SongSelected event) {
        songSelected = songsByHash.get(event.hash);
    }

    private void songDeleted(SongDeleted event) {
        Song song = event.song;
        ContentValues values = new ContentValues();
        values.put("IS_DELETED", true);
        values.put("IS_MISSING", true);
        db.update("SONGS", values, "HASH=?", new String[]{song.hash.toString()});
        song.setDeleted();

        Song current = currentSong();
        if (current != null && current.equals(song))
            currentSongIndex = null;

        updateMediaStorageUsed(-song.fileLength);
        removeSongFromArtist(song);

        // printDBSongs();
    }

    /*private void printDBSongs() {
        Cursor cursor = db.rawQuery("SELECT * FROM SONGS", null);
        while(cursor.moveToNext()) {
            System.out.println(
                "HASH: " + cursor.getString(cursor.getColumnIndex("HASH")) + ", " +
                "NAME: " + cursor.getString(cursor.getColumnIndex("NAME")) + ", " +
                "GENRE: " + cursor.getString(cursor.getColumnIndex("GENRE")) + ", " +
                "ARTIST: " + cursor.getString(cursor.getColumnIndex("ARTIST")) + ", " +
                "DURATION: " + cursor.getInt(cursor.getColumnIndex("DURATION")) + ", " +
                "FILE_PATH: " + cursor.getString(cursor.getColumnIndex("FILE_PATH")) + ", " +
                "FILE_LENGTH: " + cursor.getLong(cursor.getColumnIndex("FILE_LENGTH")) + ", " +
                "LAST_MODIFIED: " + cursor.getLong(cursor.getColumnIndex("LAST_MODIFIED")) + ", " +
                "IS_MISSING: " + (cursor.getInt(cursor.getColumnIndex("IS_MISSING")) == 1) + ", " +
                "IS_DELETED: " + (cursor.getInt(cursor.getColumnIndex("IS_DELETED")) == 1));
        }
        cursor.close();
    }*/

    private void repeat() {
        if (repeatSong) {
            repeatSong = false;
            return;
        }

        if (repeatAll) {
            repeatAll = false;
            repeatSong = true;
            return;
        }

        repeatAll = true;
        repeatSong = false;
    }

    private void songMissing(SongMissing event) {
        ContentValues values = new ContentValues();
        values.put("IS_MISSING", true);
        db.update("SONGS", values, "HASH=?", new String[]{event.song.hash.toString()});
        event.song.setMissing();
        updateMediaStorageUsed(-event.song.fileLength);
        removeSongFromArtist(event.song);
    }

    private void removeSongFromArtist(Song song) {
        Artist artist = getArtist(song.artist);
        if (artist == null)
            return;

        artist.removeSong(song);
        if (artist.songs.isEmpty()) {
            artists.remove(artist);
        }
    }

    private void songFound(SongFound event) {
        Song song = findSongByHash(event.song.hash);
        if (song == null) {
            insertNewSong(event.song);
        } else {
            updateSong(event.song);

            // remove old song from caches
            allSongs.remove(song);
            if (!event.song.artist.equals(song.artist))
                removeSongFromArtist(song);
        }
        addSong(event.song);
        addSongToArtist(event.song);
    }

    private void addSongToArtist(Song song) {
        // add song to artist
        Artist artist = getArtist(song.artist);
        if (!artist.hasSong(song))
            artist.addSong(song);
    }

    private void addSong(Song song) {
        allSongs.add(song);
        songsByHash.put(song.hash.toString(), song);
        if (!song.isMissing) {
            updateMediaStorageUsed(song.fileLength);
        }
    }

    private Artist getArtist(String artist) {
        // find first
        for (Artist a : artists)
            if (a.name.equals(artist))
                return a;

        // create new
        Artist newArtist = new Artist(artist);
        newArtist.setBio(bios.get(artist));
        artists.add(newArtist);

        return newArtist;
    }

    private void updateMediaStorageUsed(long fileLength) {
        if (mediaStorageUsed == null)
            mediaStorageUsed = 0L;
        mediaStorageUsed += fileLength;
    }

    private Song findSongByHash(Hash hash) {
        for (Song song : allSongs)
            if (song.hash.equals(hash))
                return song;
        return null;
    }

    private void insertNewSong(Song song) {
        db.insert("SONGS", null, songContents(song));
    }

    private void updateSong(Song song) {
        db.update("SONGS", songContents(song), "HASH=?", new String[]{song.hash.toString()});
    }

    private ContentValues songContents(Song song) {
        ContentValues ret = new ContentValues();
        ret.put("NAME", song.name);
        ret.put("HASH", song.hash.toString());
        ret.put("GENRE", song.genre);
        ret.put("ARTIST", song.artist);
        ret.put("ALBUM", song.album);
        ret.put("DURATION", song.duration);
        ret.put("FILE_PATH", song.filePath);
        ret.put("FILE_LENGTH", song.fileLength);
        ret.put("LAST_MODIFIED", song.lastModified);
        ret.put("IS_MISSING", song.isMissing ? 1 : 0);
        ret.put("IS_DELETED", song.isDeleted ? 1 : 0);
        return ret;
    }

    private void samplerUpdate(SamplerUpdated event) {
        samplerPlaylist = new Playlist(666, "Sampler", event.samples);
    }

    private ArrayList<Artist> artists() {
        if (artists == null) {
            loadBios();
            initializeArtists();
        }
        return artists;
    }

    private void loadBios() {
        bios = new HashMap<>();
        Cursor cursor = db.rawQuery("SELECT * FROM ARTIST_BIO", null);
        while (cursor.moveToNext())
            bios.put(cursor.getString(cursor.getColumnIndex("ARTIST_NAME")),
                    cursor.getString(cursor.getColumnIndex("CONTENT")));
        cursor.close();
    }

    private void initializeArtists() {
        Map<String, Artist> artistsByName = new HashMap<>();
        for (Song song : allSongs()) {
            if (song.isMissing)
                continue;
            Artist artist = artistsByName.get(song.artist);
            if (artist == null) {
                artist = new Artist(song.artist);
                artist.setBio(bios.get(song.artist));
                artistsByName.put(song.artist, artist);
            }
            artist.addSong(song);
        }
        artists = new ArrayList<>(artistsByName.values());
    }

    private void finishedPlaying() {
        if (isSampling)
            doPlay(samplerPlaylist, 0);
        else if (!isStopped)
            if (repeatSong)
                doPlay(currentPlaylist, currentSongIndex);
            else
                skip(+1);
    }

    private void createPlaylist(CreatePlaylist event) {
        // Avoid playlists with same name
        Playlist playlist = findPlaylistByName(event.playlistName);

        if (playlist == null) {
            // Creates playlist
            ContentValues contents = new ContentValues();
            contents.put("NAME", event.playlistName);
            long playlistId = db.insert("PLAYLISTS", null, contents);

            if (playlistId == -1) {
                Log.d("Model.createPlaylist", "Insert Playlist Error");
                return;
            }
            playlist = new Playlist(playlistId, event.playlistName, new ArrayList<Song>());

            // Add new playlist
            addPlaylist(playlist);
        } else {
            // Avoid playlist with duplicated songs
            if (playlist.hasSong(songsByHash.get(event.songHash)))
                return;
        }

        // Associates songs with playlist
        insertAssociationSongToPlaylist(event.songHash, playlist.id);
    }

    private Playlist findPlaylistByName(String name) {
        for (Playlist p : playlists)
            if (Objects.equals(p.name, name))
                return p;
        return null;
    }

    private void deletePlaylist(DeletePlaylist event) {
        Playlist playlist = playlistsById.get(event.playlistId);

        // delete from table
        int rowsP = db.delete("PLAYLISTS", "ID=?", new String[]{Long.toString(event.playlistId)});
        if (rowsP != 1) {
            Log.d("Model.deletePlaylist", "Unable to delete playlist in DB");
            return;
        }

        // delete associations to songs
        int rowsA = db.delete("PLAYLIST_SONG", "PLAYLIST_ID=?", new String[]{Long.toString(event.playlistId)});
        if (rowsA != playlist.songs.size()) {
            Log.d("Model.deletePlaylist", "Unable to delete all playlists-song associations");
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
    }

    private void addSongToPlaylist(PlaylistAddSong event) {
        insertAssociationSongToPlaylist(event.songHash, event.playlistId);
    }

    private void insertAssociationSongToPlaylist(String songHash, long playlistId) {
        ContentValues playlistSong = new ContentValues();
        playlistSong.put("PLAYLIST_ID", playlistId);
        playlistSong.put("SONG_HASH", songHash);
        playlistSong.put("POSITION", playlistsById.get(playlistId).size());
        db.insert("PLAYLIST_SONG", null, playlistSong);

        addSongToPlaylist(songHash, playlistId);
    }

    private void lovedViewed() {
        // TODO move to Sampler?
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

        if (currentPlaylist.size() == 1) {
            seekTo = 0;
            doPlay(currentPlaylist, 0);
            return;
        }

        // Skip all songs missing
        Integer songAfter = currentPlaylist.songAfter(currentSongIndex, step, isShuffle);
        while (songAfter != null && currentPlaylist.song(songAfter).isMissing) {
            songAfter = currentPlaylist.songAfter(songAfter, step, isShuffle);
        }

        if (Objects.equals(songAfter, currentSongIndex)) {
            seekTo = 0;
        }

        if (songAfter != null)
            doPlay(currentPlaylist, songAfter);
    }

    private void play(Play event) {
        doPlay(event.playlist, event.songIndex);
    }

    private void doPlay(Playlist playlist, int songIndex) {
        Song song = playlist.song(songIndex);
        if (song != null && song.isMissing) {
            isPaused = true;
            currentSongIndex = null;
            return;
        }
        isStopped = false;
        isPaused = false;
        currentSongIndex = songIndex;
        currentPlaylist = playlist;
    }

    private void playPauseCurrent() {
        isPaused = !isPaused;
    }

    private void pause() {
        isPaused = true;
    }

    private void updateListeners() {
        final State state = getState();
        /*Runnable update = new Runnable() { @Override public void run() {
            for (StateListener listener : listeners) {
                updateListener(listener, state);
            }
        }};
        handler.post(update);*/
        for (StateListener listener : listeners) {
            updateListener(listener, state);
        }
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
                reportSeekTo(),
                isStopped,
                isPaused,
                isShuffle,
                repeatAll,
                repeatSong,
                showDuration,
                playlistsBySong,
                isSampling,
                samplerPlaylist,
                lovedPlaylist(),
                playlists(),
                null,
                1,
                getAvailableMemorySize(),
                getMediaStorageUsed(),
                playlistAllSongs(),
                artists(),
                syncLibraryRequested,
                deleteSong,
                selectedPlaylist,
                songSelected,
                getOutputConnected(),
                getVolumeSettings(),
                hasAudioFocus,
                artistSelected);
    }

    private Integer reportSeekTo() {
        Integer position = seekTo;
        seekTo = null;
        return position;
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

        Collections.sort(ret, new Comparator<Playable>() {
            @Override
            public int compare(Playable p1, Playable p2) {
                int subtitleCompare = p1.subtitle().compareTo(p2.subtitle());
                if (subtitleCompare == 0)
                    return p1.name().compareTo(p2.name());
                return subtitleCompare;
            }
        });

        return ret;
    }


    private Set<Song> allSongs() {
        if (allSongs == null) {
            long start = System.currentTimeMillis();
            allSongs = new HashSet<>();
            songsByHash = new HashMap<>();
            Cursor cursor = db.rawQuery("SELECT * FROM SONGS", null);
            while (cursor.moveToNext()) {
                Song song = new Song(
                        cursor.getLong(cursor.getColumnIndex("ID")),
                        new Hash(cursor.getString(cursor.getColumnIndex("HASH"))),
                        cursor.getString(cursor.getColumnIndex("NAME")),
                        cursor.getString(cursor.getColumnIndex("ARTIST")),
                        cursor.getString(cursor.getColumnIndex("ALBUM")),
                        cursor.getString(cursor.getColumnIndex("GENRE")),
                        cursor.getInt(cursor.getColumnIndex("DURATION")),
                        cursor.getString(cursor.getColumnIndex("FILE_PATH")),
                        cursor.getLong(cursor.getColumnIndex("FILE_LENGTH")),
                        cursor.getLong(cursor.getColumnIndex("LAST_MODIFIED")),
                        cursor.getInt(cursor.getColumnIndex("IS_MISSING")) == 1,
                        cursor.getInt(cursor.getColumnIndex("IS_DELETED")) == 1);
                addSong(song);
            }
            cursor.close();
            Log.i(TAG, "initiate allSongs: " + (System.currentTimeMillis() - start) + " ms");
        }
        return allSongs;
    }

    private List<Playlist> playlists() {
        if (playlists == null) {
            playlists = new ArrayList<>();

            // Create playlist map
            Cursor cursor = db.rawQuery("SELECT * FROM PLAYLISTS", null);
            while(cursor.moveToNext()) {
                long playlistId = cursor.getLong(cursor.getColumnIndex("ID"));
                Playlist playlist = new Playlist(playlistId, cursor.getString(cursor.getColumnIndex("NAME")), new ArrayList<Song>());
                addPlaylist(playlist);
            }
            cursor.close();

            // Associates songs to playlists
            playlistsBySong = new HashMap<>();
            Cursor cursorAssoc = db.rawQuery("SELECT * FROM PLAYLIST_SONG ORDER BY PLAYLIST_ID, POSITION", null);
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

    private Long getMediaStorageUsed() {
        if (mediaStorageUsed == null) {
            mediaStorageUsed = 0L;
            for (Song song : allSongs) {
                if (!song.isMissing)
                    updateMediaStorageUsed(song.fileLength);
            }
        }
        return mediaStorageUsed;
    }

    private File musicDirectory() {
        if (musicDirectory == null) {
            musicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            if (!musicDirectory.exists())
                if (!musicDirectory.mkdirs())
                    Log.d("Model.musicDirectory()", "Unable to create folder: " + musicDirectory);
        }
        return musicDirectory;
    }

    @Override
    public void addStateListener(final StateListener listener) {
        this.listeners.add(listener);
        /*Runnable update = new Runnable() { @Override public void run() {
            updateListener(listener);
        }};
        handler.post(update);*/

        updateListener(listener);
    }

    @Override
    public void removeStateListener(StateListener listener) {
        this.listeners.remove(listener);
    }

    private String getOutputConnected() {
        return isHeadphoneConnected
                ? HEADPHONES
                : isBluetoothConnected
                    ? BLUETOOTH
                    : SPEAKER;
    }

    private void headphonesConnected() {
        isHeadphoneConnected = true;
    }

    private void headphonesDisconnected() {
        isHeadphoneConnected = false;
    }

    // Volume Settings

    private void setSpeakerVolume(SetSpeakerVolume event) {
        updateVolumeSettings(SPEAKER, event.volume);
    }

    private void setHeadphonesVolume(SetHeadphonesVolume event) {
        updateVolumeSettings(HEADPHONES, event.volume);
    }

    private void setBluetoothVolume(SetBluetoothVolume event) {
        updateVolumeSettings(BLUETOOTH, event.volume);
    }

    private Map<String, Integer> getVolumeSettings() {
        if (volumeSettings == null) {
            volumeSettings = new HashMap<>();
            Cursor cursor = db.rawQuery("SELECT * FROM VOLUME_SETTINGS", null);
            while (cursor.moveToNext()) {
                String output = cursor.getString(cursor.getColumnIndex("OUTPUT"));
                Integer volume = cursor.getInt(cursor.getColumnIndex("VOLUME"));
                volumeSettings.put(output, volume);
            }
            cursor.close();
        }
        return volumeSettings;
    }

    private void updateVolumeSettings(String output, Integer volume) {
        getVolumeSettings().put(output, volume);

        ContentValues values = new ContentValues();
        values.put("VOLUME", volume);
        db.update("VOLUME_SETTINGS", values, "OUTPUT=?", new String[]{output});
    }

    void setDatabase(SQLiteDatabase database) {
        // FORT TEST ONLY!!!
        db = database;
    }
}
