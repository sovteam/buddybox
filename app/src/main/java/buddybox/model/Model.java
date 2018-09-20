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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import buddybox.core.Album;
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
import buddybox.core.events.ArtistInfoFound;
import buddybox.core.events.ArtistPictureFound;
import buddybox.core.events.ArtistSelected;
import buddybox.core.events.ArtistSelectedByName;
import buddybox.core.events.Play;
import buddybox.core.events.PlayPlaylist;
import buddybox.core.events.PlaylistAddSong;
import buddybox.core.events.PlaylistChangeSongPosition;
import buddybox.core.events.PlaylistCreate;
import buddybox.core.events.PlaylistDelete;
import buddybox.core.events.PlaylistRemoveSong;
import buddybox.core.events.PlaylistSelected;
import buddybox.core.events.PlaylistSetName;
import buddybox.core.events.SamplerDelete;
import buddybox.core.events.SamplerHate;
import buddybox.core.events.SamplerLove;
import buddybox.core.events.SamplerUpdated;
import buddybox.core.events.Search;
import buddybox.core.events.SeekTo;
import buddybox.core.events.SetBluetoothVolume;
import buddybox.core.events.SetHeadphonesVolume;
import buddybox.core.events.SetSpeakerVolume;
import buddybox.core.events.SongDeleteRequest;
import buddybox.core.events.SongDeleted;
import buddybox.core.events.SongEmbeddedArtResult;
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
    public static final String ALL_SONGS = "%%All%%Songs%%";

    SQLiteDatabase db;
    private List<StateListener> listeners = new ArrayList<>();

    private File musicDirectory;
    private Playlist currentPlaylist;
    private Integer currentSongIndex;

    private boolean isSampling = false;
    private Playlist samplerPlaylist;

    private List<Song> allSongs;
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
    private Map<String,Integer> volumeSettings;
    private boolean hasAudioFocus = false;
    private boolean showDuration = true;

    private HashMap<String, Artist> artists;
    private Artist artistSelected;
    private Map<String, Map<String, Album>> albumsByArtist;
    private ArrayList<Playable> searchResults = new ArrayList<>();
    private Deque<Song> songsToCheckArtEmbedded = new LinkedList<>();
    private Deque<AlbumInfo> albumsToFindArt;
    private Deque<Artist> artistsToFindInfo;

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
        if (cls == PlayPlaylist.class) playPlaylist((PlayPlaylist) event);
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
        if (cls == PlaylistCreate.class) createPlaylist((PlaylistCreate) event);
        if (cls == PlaylistDelete.class) deletePlaylist((PlaylistDelete) event);
        if (cls == PlaylistAddSong.class) addSongToPlaylist((PlaylistAddSong) event);
        if (cls == PlaylistRemoveSong.class) removeSongFromPlaylist((PlaylistRemoveSong) event);
        if (cls == PlaylistSetName.class) setPlaylistName((PlaylistSetName) event);
        if (cls == PlaylistSelected.class) playlistSelected((PlaylistSelected) event);
        if (cls == PlaylistChangeSongPosition.class) playlistChangeSongPosition((PlaylistChangeSongPosition) event);

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
        if (cls == ArtistInfoFound.class) artistInfoFound((ArtistInfoFound) event);
        if (cls == SongEmbeddedArtResult.class) songEmbeddedArtResult((SongEmbeddedArtResult) event);

        // artist
        if (cls == ArtistSelected.class) artistSelected((ArtistSelected) event);
        if (cls == ArtistSelectedByName.class) artistSelectedByName((ArtistSelectedByName) event);

        // search
        if (cls == Search.class) search((Search) event);

        updateListeners();
    }

    private void songEmbeddedArtResult(SongEmbeddedArtResult event) {
        event.song.setHasEmbeddedArt(event.hasArt);
        ContentValues values = new ContentValues();
        values.put("HAS_EMBEDDED_ART", event.hasArt);
        db.update("SONGS", values, "HASH=?", new String[]{event.song.hash.toString()});
        songsToCheckArtEmbedded.remove(event.song);
    }

    private void artistInfoFound(ArtistInfoFound event) {
        Log.i(TAG, "info found: " + event.artist.name);
        ContentValues values = new ContentValues();
        values.put("BIO", event.bio);
        values.put("LAST_INFO", System.currentTimeMillis());
        db.update("ARTISTS", values,"ID=?", new String[]{Long.toString(event.artist.getId())});
        event.artist.setBio(event.bio);
        event.artist.setPicture(event.pic);

        artistsToFindInfo.remove(event.artist);
    }

    private void search(Search event) {
        final String text = event.searchText.trim().toLowerCase();
        searchResults = new ArrayList<>();

        if (text.isEmpty())
            return;

        for (Song song: allSongs()) {
            if (song.name().toLowerCase().contains(text))
                searchResults.add(song);
            if (song.album.toLowerCase().contains(text))
                searchResults.add(albumsByArtist.get(song.artist).get(song.album));
        }

        for (Artist artist: allArtistsAvailable())
            if (artist.name().toLowerCase().contains(text))
                searchResults.add(artist);

        for (Playlist playlist: playlists())
            if (playlist.name().toLowerCase().contains(text))
                searchResults.add(playlist);

        Collections.sort(searchResults, new Comparator<Playable>() {
            @Override
            public int compare(Playable p1, Playable p2) {
                return p1.name().toLowerCase().indexOf(text) - p2.name().toLowerCase().indexOf(text);
            }
        });
    }

    private void play(Play event) {
        if (event.playable.getClass() == Song.class) {
            Playlist all = allSongsPlaylist();
            int songIndex = all.indexOf((Song) event.playable, isShuffle);
            doPlay(all, songIndex);
        } else {
            doPlay((Playlist) event.playable, 0);
        }
        updateLastPlayed(event.playable);
    }

    private Playlist allSongsPlaylist() {
        List<Song> songs = new ArrayList<>(allSongs);
        Collections.sort(songs, new Comparator<Song>() {
            @Override
            public int compare(Song s1, Song s2) {
                return s2.lastPlayed().compareTo(s1.lastPlayed());
            }
        });
        return new Playlist(0L, ALL_SONGS, 1L, songs);
    }

    private void artistBioFound(ArtistBioFound event) {
        Log.i(TAG, "bio found: " + event.content);
        ContentValues values = new ContentValues();
        values.put("BIO", event.content);
        db.update("ARTISTS", values,"ID=?", new String[]{Long.toString(event.artist.getId())});
        event.artist.setBio(event.content);
    }

    private void artistSelected(ArtistSelected event) {
        artistSelected = event.artist;
    }

    private void artistSelectedByName(ArtistSelectedByName event) {
        artistSelected = artists.get(event.name);
    }

    private void artistPictureFound(ArtistPictureFound event) {
        event.artist.setPicture(event.picture);
    }

    private void albumArtFound(AlbumArtFound event) {
        for (Song song : allSongs)
            if (song.getArt() == null
                    && song.artist.equals(event.album.artist)
                    && song.album.equals(event.album.name))
                song.setArt(event.art);

        albumsToFindArt.remove(event.album);
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
        Playlist playlist = event.playlist;

        /* UPDATE DB */
        if (event.fromPosition > event.toPosition) {
            // Slide +1 position songs >= toPosition and < fromPosition
            db.execSQL(
                    "UPDATE PLAYLIST_SONG " +
                            "SET POSITION = POSITION +1 " +
                            "WHERE  PLAYLIST_ID = " + playlist.getId() + " " +
                            "AND    POSITION <  " + event.fromPosition + " " +
                            "AND    POSITION >= " + event.toPosition);
        } else {
            // Slide -1 position songs <= toPosition and > fromPosition
            db.execSQL(
                    "UPDATE PLAYLIST_SONG " +
                            "SET POSITION = POSITION -1 " +
                            "WHERE  PLAYLIST_ID = " + playlist.getId() + " " +
                            "AND    POSITION >  " + event.fromPosition + " " +
                            "AND    POSITION <= " + event.toPosition);
        }

        // Update moved song position
        Song song = playlist.song(event.fromPosition, isShuffle);
        db.execSQL(
                "UPDATE PLAYLIST_SONG " +
                        "SET POSITION = " + event.toPosition + " " +
                        "WHERE  PLAYLIST_ID = " + playlist.getId() + " " +
                        "AND    SONG_HASH = '" + song.hash.toString() + "'");

        // check table
        Cursor w = db.rawQuery("SELECT * FROM PLAYLIST_SONG", null, null);
        while(w.moveToNext()) {
            System.out.println(
                    "PLAYLIST: " + w.getString(w.getColumnIndex("PLAYLIST_ID")) + ", " +
                    "SONG: " + songsByHash.get(w.getString(w.getColumnIndex("SONG_HASH"))).name + ", " +
                    "position: " + w.getString(w.getColumnIndex("POSITION")));
        }
        w.close();

        /* UPDATE OBJECTS */
        Song currentSong = null;
        if (currentSongIndex != null && currentPlaylist == playlist)
            currentSong = playlist.song(currentSongIndex, isShuffle);

        playlist.changeSongPosition(event.fromPosition, event.toPosition);

        if (currentSong != null && currentPlaylist == playlist)
            currentSongIndex = playlist.indexOf(currentSong, isShuffle);
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
                new String[]{Long.toString(selectedPlaylist.getId())});
        if (rows == 1)
            selectedPlaylist.setName(event.playlistName);
    }

    private void shuffle() {
        Song song = currentPlaylist.song(currentSongIndex, isShuffle);
        currentSongIndex = currentPlaylist.indexOf(song, !isShuffle);
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
        while (currentPlaylist.song(nextSongIndex, isShuffle).isMissing && !loop) {
            nextSongIndex = currentPlaylist.songAfter(nextSongIndex, 1);
            loop = nextSongIndex == firstSongIndex;
        }

        // Pause if all songs are missing
        if (loop) {
            playPauseCurrent();
            currentPlaylist = null;
            return;
        }

        updateLastPlayed(currentPlaylist);
        doPlay(currentPlaylist, nextSongIndex);
    }

    private void shufflePlayArtist() {
        selectedPlaylist = artistSelected;
        updateLastPlayed(selectedPlaylist);
        shufflePlay();
    }

    private void removeSongFromPlaylist(PlaylistRemoveSong event) {
        String songHash = event.song.hash.toString();
        Playlist playlist = event.playlist;
        int songPosition = playlist.songs.indexOf(event.song);

        // delete from associations table
        db.delete("PLAYLIST_SONG", "PLAYLIST_ID=? AND SONG_HASH=?", new String[]{Long.toString(playlist.getId()), songHash});

        // update position-1 for relations.position > song index
        db.execSQL(
                "UPDATE PLAYLIST_SONG " +
                        "SET POSITION = POSITION -1 " +
                        "WHERE PLAYLIST_ID = " + playlist.getId() + " " +
                        "AND SONG_HASH = '" + songHash + "' " +
                        "AND POSITION > " + songPosition);

        // remove from playlistsBySong
        List<Playlist> songPlaylists = playlistsBySong.get(songHash);
        songPlaylists.remove(playlist);
        playlistsBySong.put(songHash, songPlaylists);

        // remove from object
        playlist.removeSong(event.song);

        // keep activity_playing the current song
        if (currentPlaylist == playlist && currentSongIndex > songPosition)
            currentSongIndex--;
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
    }

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
        removeSongFromAlbum(song);
    }

    private void removeSongFromAlbum(Song song) {
        Album album = getAlbum(song.album, song.artist);
        if (album == null)
            return;

        album.removeSong(song);
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
    }

    private void addSong(Song song) {
        allSongs.add(song);
        songsByHash.put(song.hash.toString(), song);
        if (!song.isMissing) {
            updateMediaStorageUsed(song.fileLength);
            addSongToArtist(song);
            addSongToAlbum(song);
            if (song.hasEmbeddedArt == null) {
                songsToCheckArtEmbedded.add(song);
            }
        }
    }

    private void addSongToAlbum(Song song) {
        Album album = getAlbum(song.album, song.artist);
        if (!album.hasSong(song))
            album.addSong(song);
    }

    private Album getAlbum(String album, String artist) {
        Map<String, Album> albums = albumsByArtist().get(artist);
        if (albums == null)
            albums = new HashMap<>();

        Album ret = albums.get(album);
        if (ret == null) {
            // create new album
            ContentValues values = new ContentValues();
            values.put("NAME", album);
            values.put("ARTIST", artist);
            long newId = db.insert("ALBUMS", null, values);
            ret = new Album(newId, album, null);
            albums.put(album, ret);
            albumsByArtist.put(artist, albums);
        }
        return ret;
    }

    private Map<String, Map<String, Album>> albumsByArtist() {
        if (albumsByArtist == null)
            loadAlbums();
        return albumsByArtist;
    }

    private void loadAlbums() {
        albumsByArtist = new HashMap<>();
        Cursor cursor = db.rawQuery("SELECT * FROM ALBUMS", null);
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex("ID"));
            String name = cursor.getString(cursor.getColumnIndex("NAME"));
            String artist = cursor.getString(cursor.getColumnIndex("ARTIST"));
            Long lastPlayed = cursor.getLong(cursor.getColumnIndex("LAST_PLAYED"));
            Album album = new Album(id, name, lastPlayed == 0 ? null : lastPlayed);

            Map<String, Album> albums = albumsByArtist.get(artist);
            if (albums == null)
                albums = new HashMap<>();
            albums.put(name, album);
            albumsByArtist.put(artist, albums);
        }
        cursor.close();
    }

    private void addSongToArtist(Song song) {
        Artist artist = getArtist(song.artist);
        if (!artist.hasSong(song))
            artist.addSong(song);
    }

    private Artist getArtist(String artistName) {
        Artist ret = artists().get(artistName);
        if (ret == null) {
            // create new artist
            ContentValues values = new ContentValues();
            values.put("NAME", artistName);
            long newId = db.insert("ARTISTS", null, values);
            ret = new Artist(newId, artistName, null, null);
            artists.put(artistName, ret);
        }
        return ret;
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
        ContentValues newSong = songContents(song);
        newSong.put("LAST_PLAYED", song.lastPlayed());
        long id = db.insert("SONGS", null, newSong);
        song.setId(id);
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
        samplerPlaylist = new Playlist(666, "Sampler", System.currentTimeMillis(), event.samples);
    }

    private List<Artist> artistsPlayed() {
        List<Artist> ret = new ArrayList<>();
        for (Artist artist: allArtistsAvailable())
            if (artist.lastPlayed() != null && !artist.isEmpty())
                ret.add(artist);
        return ret;
    }

    private List<Album> albumsPlayed() {
        List<Album> ret = new ArrayList<>();
        for (Map<String, Album> albums : albumsByArtist().values())
            for (Album album : albums.values())
                if (album.lastPlayed() != null && !album.isEmpty())
                    ret.add(album);
        return ret;
    }

    private List<Artist> allArtistsAvailable() {
        List<Artist> ret = new ArrayList<>();
        for (Artist artist : artists().values())
            if (artist.size() > 0)
                ret.add(artist);

        Collections.sort(ret, new Comparator<Artist>() {
            @Override
            public int compare(Artist a1, Artist a2) {
                return a1.name.toLowerCase().compareTo(a2.name.toLowerCase());
            }
        });

        return ret;
    }

    private HashMap<String, Artist> artists() {
        if (artists == null) {
            loadArtists();
        }
        return artists;
    }

    private void loadArtists() {
        artists = new HashMap<>();
        Cursor cursor = db.rawQuery("SELECT * FROM ARTISTS", null);
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex("NAME"));
            Long lastPlayed = cursor.getLong(cursor.getColumnIndex("LAST_PLAYED"));
            Artist artist = new Artist(
                    cursor.getLong(cursor.getColumnIndex("ID")),
                    cursor.getString(cursor.getColumnIndex("NAME")),
                    cursor.getString(cursor.getColumnIndex("BIO")),
                    lastPlayed == 0 ? null : lastPlayed
            );
            artists.put(name, artist);
        }
        cursor.close();
    }

    private void finishedPlaying() {
        if (isSampling)
            doPlay(samplerPlaylist, 0);
        else if (!isStopped)
            if (repeatSong)
                doPlay(currentPlaylist, currentSongIndex);
            else if (currentPlaylist.isLastSong(currentSongIndex, isShuffle) && !repeatAll)
                pause();
            else
                skip(+1);
    }

    private void createPlaylist(PlaylistCreate event) {
        // Avoid playlists with same name
        Playlist playlist = findPlaylistByName(event.playlistName);

        if (playlist == null) {
            // Creates playlist
            long now = System.currentTimeMillis();
            ContentValues contents = new ContentValues();
            contents.put("NAME", event.playlistName);
            contents.put("LAST_PLAYED", now);
            long playlistId = db.insert("PLAYLISTS", null, contents);

            if (playlistId == -1) {
                Log.d("Model.createPlaylist", "Insert Playlist Error");
                return;
            }
            playlist = new Playlist(playlistId, event.playlistName, now, new ArrayList<Song>());

            // Add new playlist
            addPlaylist(playlist);
        } else {
            // Avoid playlist with duplicated songs
            if (playlist.hasSong(songsByHash.get(event.songHash)))
                return;
        }

        // Associates songs with playlist
        insertAssociationSongToPlaylist(event.songHash, playlist.getId());
    }

    private Playlist findPlaylistByName(String name) {
        for (Playlist p : playlists)
            if (Objects.equals(p.name, name))
                return p;
        return null;
    }

    private List<Playable> recent() {
        List<Playable> ret = new ArrayList<>();
        ret.addAll(allSongsAvailable());
        ret.addAll(playlists());
        ret.addAll(artistsPlayed());
        ret.addAll(albumsPlayed());
        Collections.sort(ret, new Comparator<Playable>() {
            @Override
            public int compare(Playable p1, Playable p2) {
                return p2.lastPlayed().compareTo(p1.lastPlayed());
            }
        });
        return ret;
    }

    private void deletePlaylist(PlaylistDelete event) {
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
        playlistsById.remove(playlist.getId());
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
        for (Song song : lovedPlaylist().songs)
            if (!song.isLovedViewed())
                song.setLovedViewed();
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
        if (samplerPlaylist.size() > 0)
            doPlay(samplerPlaylist, 0);
    }

    private void skip(int step) {
        if (currentPlaylist.size() == 1) {
            seekTo = 0;
            doPlay(currentPlaylist, 0);
            return;
        }

        // Skip all songs not available
        // todo stack overflow when all songs are missing (&& currentSongIndex != songAfter)
        Integer songAfter = currentPlaylist.songAfter(currentSongIndex, step);
        while (songAfter != null && currentPlaylist.song(songAfter, isShuffle).isMissing) {
            songAfter = currentPlaylist.songAfter(songAfter, step);
        }

        if (Objects.equals(songAfter, currentSongIndex)) {
            seekTo = 0;
        }

        if (songAfter != null)
            doPlay(currentPlaylist, songAfter);
    }

    private void playPlaylist(PlayPlaylist event) {
        updateLastPlayed(event.playlist);
        doPlay(event.playlist, event.songIndex);
    }

    private void updateLastPlayed(Playable playable) {
        long now = System.currentTimeMillis();
        ContentValues nowVal = new ContentValues();
        nowVal.put("LAST_PLAYED", now);
        String table = playable.getClass().getSimpleName().toUpperCase() + "S";

        Long id = playable.getId();
        if (id != null) {
            // external song just added
            db.update(table, nowVal, "ID=?", new String[]{Long.toString(id)});
            playable.updateLastPlayed(now);
        }
    }

    private void doPlay(Playlist playlist, int songIndex) {
        Song song = playlist.song(songIndex, isShuffle);
        currentSongIndex = songIndex;
        if (song != null && song.isMissing) {
            isPaused = true;
            return;
        }
        isStopped = false;
        isPaused = false;
        currentPlaylist = playlist;
    }

    private void playPauseCurrent() {
        Song song = currentSong();
        if (song != null && !song.isMissing)
            isPaused = !isPaused;
    }

    private void pause() {
        isPaused = true;
    }

    private void updateListeners() {
        final State state = getState();
        for (StateListener listener : listeners)
            updateListener(listener, state);
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
                recent(),
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
                searchResults,
                1,
                getAvailableMemorySize(),
                getMediaStorageUsed(),
                allSongsAvailable(),
                allArtistsAvailable(),
                syncLibraryRequested,
                deleteSong,
                selectedPlaylist,
                songSelected,
                getOutputConnected(),
                getVolumeSettings(),
                hasAudioFocus,
                artistSelected,
                artistAlbums(),
                songToCheckArtEmbedded(),
                albumToFindArt(),
                artistToFindInfo()
        );
    }

    private Artist artistToFindInfo() {
        // TODO insert into artistsToFindInfo when new SongFound
        if (albumToFindArt() != null)
            return null;

        if (artistsToFindInfo == null) {
            artistsToFindInfo = new LinkedList<>();
            for (Map.Entry<String, Artist> artistEntry : artists.entrySet())
                artistsToFindInfo.offer(artistEntry.getValue());
        }
        return artistsToFindInfo.peek();
    }

    private AlbumInfo albumToFindArt() {
        // TODO insert into albumsToFindArt when new SongFound
        if (songToCheckArtEmbedded() != null)
            return null;

        if (albumsToFindArt == null) {
            // collect albums by artist
            Map<String,Set<String>> artistsAlbums = new HashMap<>();
            for (Song song : allSongs) {
                if (song.hasEmbeddedArt())
                    continue;

                Set<String> albums = artistsAlbums.get(song.artist);
                if (albums == null)
                    albums = new HashSet<>();
                albums.add(song.album);
                artistsAlbums.put(song.artist, albums);
            }

            // offer albums to find art
            albumsToFindArt = new LinkedList<>();
            for (String artistName : artistsAlbums.keySet())
                for (String albumName : artistsAlbums.get(artistName))
                    albumsToFindArt.offer(new AlbumInfo(albumName, artistName));
        }
        return albumsToFindArt.peek();
    }

    private Song songToCheckArtEmbedded() {
        return songsToCheckArtEmbedded.peek();
    }

    private Map<String, Album> artistAlbums() {
        if (artistSelected == null)
            return null;

        return albumsByArtist.get(artistSelected.name);
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
                        : currentPlaylist.song(currentSongIndex, isShuffle);
    }

    private List<Song> allSongsAvailable() {
        List<Song> ret = new ArrayList<>();
        for (Song song : allSongs())
           if (!song.isMissing)
               ret.add(song);

        Collections.sort(ret, new Comparator<Playable>() {
            @Override
            public int compare(Playable p1, Playable p2) {
                return p2.lastPlayed().compareTo(p1.lastPlayed());
            }
        });

        return ret;
    }

    private List<Song> allSongs() {
        if (allSongs == null) {
            long start = System.currentTimeMillis();
            allSongs = new ArrayList<>();
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
                        cursor.getInt(cursor.getColumnIndex("IS_DELETED")) == 1,
                        cursor.getLong(cursor.getColumnIndex("LAST_PLAYED")),
                        cursor.getInt(cursor.getColumnIndex("HAS_EMBEDDED_ART")));
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
                Playlist playlist = new Playlist(
                        playlistId,
                        cursor.getString(cursor.getColumnIndex("NAME")),
                        cursor.getLong(cursor.getColumnIndex("LAST_PLAYED")),
                        new ArrayList<Song>());
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
        playlistsById.put(playlist.getId(), playlist);
        System.out.println("added new playlist");
    }

    private Playlist lovedPlaylist() {
        List<Song> lovedSongs = new ArrayList<>();

        for (Song song : allSongs())
            if (song.isLoved())
                lovedSongs.add(song);

        // Sort by most recent loved
        Collections.sort(lovedSongs, new Comparator<Song>() { @Override public int compare(Song songA, Song songB) {
            return songB.loved.compareTo(songA.loved);
        }});

        return new Playlist(69, "Loved", 1L, lovedSongs);
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
            for (Song song : allSongs)
                if (!song.isMissing)
                    updateMediaStorageUsed(song.fileLength);
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
