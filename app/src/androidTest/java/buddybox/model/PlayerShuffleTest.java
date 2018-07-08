package buddybox.model;

import android.util.Base64;

import org.junit.Before;
import org.junit.Test;

import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.events.PlayPlaylist;
import buddybox.core.events.PlaylistAddSong;
import buddybox.core.events.PlaylistCreate;
import buddybox.core.events.PlaylistSelected;
import buddybox.core.events.SongFound;
import sov.Hash;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Play.SHUFFLE;
import static buddybox.core.events.Play.SHUFFLE_PLAY;
import static buddybox.core.events.Play.SKIP_NEXT;
import static buddybox.core.events.Play.SKIP_PREVIOUS;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class PlayerShuffleTest extends ModelTest {

    @Before
    public void setup() {
        super.setup();

        Playlist.setRandomSeed(42);

        Hash hash = new Hash(Base64.encode("IsThisLoveStream".getBytes(), 1));
        Song song = new Song(null, hash, "Is This Love", "Bob Marley", "Legend", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8236L , 2L,false, false, 1L);
        dispatch(new SongFound(song));

        hash = new Hash(Base64.encode("StirItUpStream".getBytes(), 1));
        song = new Song(null, hash, "Stir It Up", "Bob Marley", "Legend Deluxe Edition", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8236L , 2L,false, false, 1L);
        dispatch(new SongFound(song));

        hash = new Hash(Base64.encode("CanYouFeelItStream".getBytes(), 1));
        song = new Song(null, hash, "Can You Feel It", "The Jacksons", "The Very Best Of", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8235L , 3L,false, false, 1L);
        dispatch(new SongFound(song));

        dispatch(new PlaylistCreate("My Playlist 1", getSong("Stir It Up").hash.toString()));
        dispatch(new PlaylistAddSong(getSong("Can You Feel It").hash.toString(), getPlaylist("My Playlist 1").getId()));
        dispatch(new PlaylistAddSong(getSong("Is This Love").hash.toString(), getPlaylist("My Playlist 1").getId()));
    }

    @Test
    public void dispatchShufflePlay_modelShufflesPlaylist() {
        dispatch(new PlaylistSelected(getPlaylist("My Playlist 1")));
        assertFalse(lastState.isShuffle);

        dispatch(SHUFFLE_PLAY);
        assertTrue(lastState.isShuffle);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        dispatch(SKIP_NEXT);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        dispatch(SKIP_NEXT);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);

        dispatch(SKIP_NEXT);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        dispatch(SKIP_NEXT);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        dispatch(SKIP_NEXT);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);

        dispatch(SKIP_NEXT);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);
    }

    @Test
    public void switchShuffleOnOff_modelPlaylist() {
        // check shuffle OFF sequence skipping next
        assertFalse(lastState.isShuffle);
        dispatch(new PlayPlaylist(getPlaylist("My Playlist 1"), 0));
        assertFalse(lastState.isShuffle);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        dispatch(SKIP_NEXT);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);

        dispatch(SKIP_NEXT);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        // check shuffle ON sequence skipping next
        dispatch(SHUFFLE);
        assertTrue(lastState.isShuffle);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        dispatch(SKIP_NEXT);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);

        dispatch(SKIP_NEXT);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        dispatch(SKIP_NEXT);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        // check shuffle ON sequence skipping previous
        dispatch(SKIP_PREVIOUS);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        dispatch(SKIP_PREVIOUS);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);

        dispatch(SKIP_PREVIOUS);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        dispatch(SKIP_PREVIOUS);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        // check shuffle OFF sequence skipping previous
        dispatch(SHUFFLE);
        assertFalse(lastState.isShuffle);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        dispatch(SKIP_PREVIOUS);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        dispatch(SKIP_PREVIOUS);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);

        dispatch(SKIP_PREVIOUS);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);
    }
}
