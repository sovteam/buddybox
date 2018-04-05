package buddybox.model;

import android.util.Base64;

import org.junit.Before;
import org.junit.Test;

import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.events.PlaylistAddSong;
import buddybox.core.events.PlaylistChangeSongPosition;
import buddybox.core.events.PlaylistCreate;
import buddybox.core.events.PlaylistDelete;
import buddybox.core.events.PlaylistRemoveSong;
import buddybox.core.events.PlaylistSelected;
import buddybox.core.events.PlaylistSetName;
import buddybox.core.events.SongFound;
import sov.Hash;

import static buddybox.core.Dispatcher.dispatch;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class PlaylistCRUDTest extends ModelTest {

    @Before
    public void setup() {
        super.setup();

        Hash hash = new Hash(Base64.encode("IsThisLoveStream".getBytes(), 1));
        Song song = new Song(null, hash, "Is This Love", "Bob Marley", "Legend", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8236L , 2L,false, false, 1L);
        dispatch(new SongFound(song));

        hash = new Hash(Base64.encode("StirItUpStream".getBytes(), 1));
        song = new Song(null, hash, "Stir It Up", "Bob Marley", "Legend Deluxe Edition", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8236L , 2L,false, false, 1L);
        dispatch(new SongFound(song));

        hash = new Hash(Base64.encode("CanYouFeelItStream".getBytes(), 1));
        song = new Song(null, hash, "Can You Feel It", "The Jacksons", "The Very Best Of", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8235L , 3L,false, false, 1L);
        dispatch(new SongFound(song));
    }

    @Test
    public void createPlaylistDispatch_modelCreatePlaylist() {
        assertEquals(4, updateCount);
        assertTrue(lastState.playlists.isEmpty());

        dispatch(new PlaylistCreate("My Playlist 1", getSong("Stir It Up").hash.toString()));

        assertEquals(5, updateCount);
        assertEquals(1, lastState.playlists.size());
        assertEquals("My Playlist 1", lastState.playlists.get(0).name);
        assertEquals(1, lastState.playlists.get(0).songs.size());
        assertEquals("Stir It Up", lastState.playlists.get(0).songs.get(0).name);

        reinitialize();

        assertEquals(1, updateCount);
        assertEquals(1, lastState.playlists.size());
        assertEquals("My Playlist 1", lastState.playlists.get(0).name);
        assertEquals(1, lastState.playlists.get(0).songs.size());
        assertEquals("Stir It Up", lastState.playlists.get(0).songs.get(0).name);
    }

    @Test
    public void playlistAddSongDispatch_modelUpdatesPlaylist() {
        dispatch(new PlaylistCreate("My Playlist 2", getSong("Stir It Up").hash.toString()));
        dispatch(new PlaylistAddSong(getSong("Can You Feel It").hash.toString(), getPlaylist("My Playlist 2").getId()));

        Playlist mPlaylist = getPlaylist("My Playlist 2");
        assertEquals(6, updateCount);
        assertEquals(2, mPlaylist.size());
        assertEquals("Stir It Up", mPlaylist.song(0).name);
        assertEquals("Can You Feel It", mPlaylist.song(1).name);

        reinitialize();

        mPlaylist = getPlaylist("My Playlist 2");
        assertEquals(1, updateCount);
        assertEquals(2, mPlaylist.size());
        assertEquals("Stir It Up", mPlaylist.song(0).name);
        assertEquals("Can You Feel It", mPlaylist.song(1).name);
    }

    @Test
    public void playlistRemoveSongDispatch_modelUpdatesPlaylist() {
        dispatch(new PlaylistCreate("My Playlist 3", getSong("Stir It Up").hash.toString()));
        dispatch(new PlaylistAddSong(getSong("Can You Feel It").hash.toString(), getPlaylist("My Playlist 3").getId()));
        dispatch(new PlaylistAddSong(getSong("Is This Love").hash.toString(), getPlaylist("My Playlist 3").getId()));

        Playlist mPlaylist = getPlaylist("My Playlist 3");
        assertEquals(7, updateCount);
        assertFalse(mPlaylist.isEmpty());
        assertEquals(3, mPlaylist.size());
        assertEquals("Stir It Up", mPlaylist.song(0).name);
        assertEquals("Can You Feel It", mPlaylist.song(1).name);
        assertEquals("Is This Love", mPlaylist.song(2).name);

        dispatch(new PlaylistRemoveSong(mPlaylist, mPlaylist.song(1)));

        mPlaylist = getPlaylist("My Playlist 3");
        assertEquals(8, updateCount);
        assertEquals(2, mPlaylist.size());
        assertFalse(mPlaylist.isEmpty());
        assertEquals("Stir It Up", mPlaylist.song(0).name);
        assertEquals("Is This Love", mPlaylist.song(1).name);

        reinitialize();

        mPlaylist = getPlaylist("My Playlist 3");
        assertEquals(1, updateCount);
        assertEquals(2, mPlaylist.size());
        assertFalse(mPlaylist.isEmpty());
        assertEquals("Stir It Up", mPlaylist.song(0).name);
        assertEquals("Is This Love", mPlaylist.song(1).name);

        dispatch(new PlaylistRemoveSong(mPlaylist, getSong("Stir It Up")));

        mPlaylist = getPlaylist("My Playlist 3");
        assertEquals(2, updateCount);
        assertEquals(1, mPlaylist.size());
        assertFalse(mPlaylist.isEmpty());
        assertEquals("Is This Love", mPlaylist.song(0).name);

        dispatch(new PlaylistRemoveSong(mPlaylist, getSong("Is This Love")));

        mPlaylist = getPlaylist("My Playlist 3");
        assertEquals(3, updateCount);
        assertTrue(mPlaylist.isEmpty());
    }

    @Test
    public void playlistSetNameDispatch_modelUpdatesPlaylist() {
        dispatch(new PlaylistCreate("P1", getSong("Stir It Up").hash.toString()));
        dispatch(new PlaylistCreate("P2", getSong("Can You Feel It").hash.toString()));

        dispatch(new PlaylistSelected(getPlaylist("P2")));
        assertEquals(7, updateCount);
        assertEquals("P2", lastState.selectedPlaylist.name);

        dispatch(new PlaylistSetName("P2.1"));
        assertEquals(8, updateCount);
        assertEquals("P1", lastState.playlists.get(0).name);
        assertEquals("P2.1", lastState.playlists.get(1).name);

        reinitialize();

        assertEquals(1, updateCount);
        assertEquals("P1", lastState.playlists.get(0).name);
        assertEquals("P2.1", lastState.playlists.get(1).name);
    }

    @Test
    public void playlistDeleteDispatch_modelRemovesPlaylist() {
        dispatch(new PlaylistCreate("P1", getSong("Stir It Up").hash.toString()));
        dispatch(new PlaylistCreate("P2", getSong("Can You Feel It").hash.toString()));
        assertEquals(6, updateCount);
        assertEquals(2, lastState.playlists.size());

        dispatch(new PlaylistDelete(getPlaylist("P2").getId()));
        assertEquals(7, updateCount);
        assertEquals(1, lastState.playlists.size());
        assertEquals("P1", lastState.playlists.get(0).name);

        reinitialize();

        assertEquals(1, updateCount);
        assertEquals(1, lastState.playlists.size());
        assertEquals("P1", lastState.playlists.get(0).name);
    }

    @Test
    public void playlistChangSongPosition_modelUpdatesPlaylist() {
        dispatch(new PlaylistCreate("My Playlist 4", getSong("Stir It Up").hash.toString()));
        dispatch(new PlaylistAddSong(getSong("Can You Feel It").hash.toString(), getPlaylist("My Playlist 4").getId()));
        dispatch(new PlaylistAddSong(getSong("Is This Love").hash.toString(), getPlaylist("My Playlist 4").getId()));

        Playlist mPlaylist = getPlaylist("My Playlist 4");
        assertEquals(7, updateCount);
        assertEquals("Stir It Up", mPlaylist.song(0).name);
        assertEquals("Can You Feel It", mPlaylist.song(1).name);
        assertEquals("Is This Love", mPlaylist.song(2).name);

        // first position to last
        dispatch(new PlaylistChangeSongPosition(mPlaylist, 0, 2));
        mPlaylist = getPlaylist("My Playlist 4");
        assertEquals(8, updateCount);
        assertEquals("Can You Feel It", mPlaylist.song(0).name);
        assertEquals("Is This Love", mPlaylist.song(1).name);
        assertEquals("Stir It Up", mPlaylist.song(2).name);

        // check if model have persisted changes
        reinitialize();
        mPlaylist = getPlaylist("My Playlist 4");
        assertEquals(1, updateCount);
        assertEquals("Can You Feel It", mPlaylist.song(0).name);
        assertEquals("Is This Love", mPlaylist.song(1).name);
        assertEquals("Stir It Up", mPlaylist.song(2).name);

        // move last position to first
        dispatch(new PlaylistChangeSongPosition(mPlaylist, 2, 0));
        mPlaylist = getPlaylist("My Playlist 4");
        assertEquals(2, updateCount);
        assertEquals("Stir It Up", mPlaylist.song(0).name);
        assertEquals("Can You Feel It", mPlaylist.song(1).name);
        assertEquals("Is This Love", mPlaylist.song(2).name);

        // first position to second
        dispatch(new PlaylistChangeSongPosition(mPlaylist, 0, 1));
        mPlaylist = getPlaylist("My Playlist 4");
        assertEquals(3, updateCount);
        assertEquals("Can You Feel It", mPlaylist.song(0).name);
        assertEquals("Stir It Up", mPlaylist.song(1).name);
        assertEquals("Is This Love", mPlaylist.song(2).name);

        // second position to first
        dispatch(new PlaylistChangeSongPosition(mPlaylist, 1, 0));
        mPlaylist = getPlaylist("My Playlist 4");
        assertEquals(4, updateCount);
        assertEquals("Stir It Up", mPlaylist.song(0).name);
        assertEquals("Can You Feel It", mPlaylist.song(1).name);
        assertEquals("Is This Love", mPlaylist.song(2).name);

        // second position to last
        dispatch(new PlaylistChangeSongPosition(mPlaylist, 1, 2));
        mPlaylist = getPlaylist("My Playlist 4");
        assertEquals(5, updateCount);
        assertEquals("Stir It Up", mPlaylist.song(0).name);
        assertEquals("Is This Love", mPlaylist.song(1).name);
        assertEquals("Can You Feel It", mPlaylist.song(2).name);

        reinitialize();
        mPlaylist = getPlaylist("My Playlist 4");
        assertEquals(1, updateCount);
        assertEquals("Stir It Up", mPlaylist.song(0).name);
        assertEquals("Is This Love", mPlaylist.song(1).name);
        assertEquals("Can You Feel It", mPlaylist.song(2).name);
    }
}
