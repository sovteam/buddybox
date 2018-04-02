package buddybox.model;

import android.util.Base64;

import org.junit.Before;
import org.junit.Test;

import buddybox.core.Song;
import buddybox.core.events.Play;
import buddybox.core.events.PlaylistAddSong;
import buddybox.core.events.PlaylistCreate;
import buddybox.core.events.SongFound;
import sov.Hash;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Play.FINISHED_PLAYING;
import static buddybox.core.events.Play.PLAY_PAUSE_CURRENT;
import static buddybox.core.events.Play.REPEAT;
import static buddybox.core.events.Play.SKIP_NEXT;
import static buddybox.core.events.Play.SKIP_PREVIOUS;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class PlayerCycleTest extends ModelTest {

    @Before
    public void setup() {
        super.setup();

        Hash hash = new Hash(Base64.encode("IsThisLoveStream".getBytes(), 1));
        Song song = new Song(null, hash, "Is This Love", "Bob Marley", "Legend", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8236L , 2L,false, false);
        dispatch(new SongFound(song));

        hash = new Hash(Base64.encode("StirItUpStream".getBytes(), 1));
        song = new Song(null, hash, "Stir It Up", "Bob Marley", "Legend Deluxe Edition", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8236L , 2L,false, false);
        dispatch(new SongFound(song));

        hash = new Hash(Base64.encode("CanYouFeelItStream".getBytes(), 1));
        song = new Song(null, hash, "Can You Feel It", "The Jacksons", "The Very Best Of", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8235L , 3L,false, false);
        dispatch(new SongFound(song));

        dispatch(new PlaylistCreate("My Playlist", getSong("Stir It Up").hash.toString()));
        dispatch(new PlaylistAddSong(getSong("Can You Feel It").hash.toString(), getPlaylist("My Playlist").id));
        dispatch(new PlaylistAddSong(getSong("Is This Love").hash.toString(), getPlaylist("My Playlist").id));
    }

    @Test
    public void dispatchPlay_modelUpdatesPlayingState() {
        dispatch(new PlaylistCreate("Soul Party", getSong("Can You Feel It").hash.toString()));

        assertTrue(lastState.isPaused);
        assertEquals(null, lastState.songPlaying);
        assertEquals(null, lastState.playlistPlaying);

        dispatch(new Play(getPlaylist("My Playlist"), 0));
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        dispatch(new Play(getPlaylist("Soul Party"), 0));
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);
        assertEquals(getPlaylist("Soul Party"), lastState.playlistPlaying);

        dispatch(new Play(getPlaylist("My Playlist"), 2));
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);
    }

    @Test
    public void dispatchFinishedPlayingRepeatAllOn_modelSkipSongsAndLoopsAfterTheLastOne() {
        assertTrue(lastState.repeatAll); // default: repeat all on

        // Play last song
        dispatch(new Play(getPlaylist("My Playlist"), 2));
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // must skip to first song
        dispatch(FINISHED_PLAYING);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // must skip to second
        dispatch(FINISHED_PLAYING);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // must skip to third last song
        dispatch(FINISHED_PLAYING);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // must skip to first song again
        dispatch(FINISHED_PLAYING);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);
    }

    @Test
    public void dispatchFinishedPlayingRepeatAllOff_modelSkipSongsTillTheLastOne() {
        // default: repeat all on
        dispatch(REPEAT); // repeat only one
        dispatch(REPEAT); // repeat off
        assertFalse(lastState.repeatAll);

        // play second song
        dispatch(new Play(getPlaylist("My Playlist"), 1));
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // must skip to next song when finished
        dispatch(FINISHED_PLAYING);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // must stop playing when last song finishes
        dispatch(FINISHED_PLAYING);
        assertTrue(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);
    }

    @Test
    public void dispatchPlayPauseCurrent_modelUpdatesPlayingState() {
        // play second song
        dispatch(new Play(getPlaylist("My Playlist"), 1));
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // pause
        dispatch(PLAY_PAUSE_CURRENT);
        assertTrue(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // play
        dispatch(PLAY_PAUSE_CURRENT);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // pause
        dispatch(PLAY_PAUSE_CURRENT);
        assertTrue(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // play first song
        dispatch(new Play(getPlaylist("My Playlist"), 0));
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

    }

    @Test
    public void dispatchRepeat_modelUpdatesPlayingState() {
        // default: repeat all
        assertTrue(lastState.repeatAll);
        assertFalse(lastState.repeatSong);

        // repeat one
        dispatch(REPEAT);
        assertFalse(lastState.repeatAll);
        assertTrue(lastState.repeatSong);

        // repeat off
        dispatch(REPEAT);
        assertFalse(lastState.repeatAll);
        assertFalse(lastState.repeatSong);

        // repeat all
        dispatch(REPEAT);
        assertTrue(lastState.repeatAll);
        assertFalse(lastState.repeatSong);
    }

    @Test
    public void dispatchRepeatSameSong_modelLoopSameSong() {
        // default: repeat all
        // repeat one
        dispatch(REPEAT);
        assertFalse(lastState.repeatAll);
        assertTrue(lastState.repeatSong);

        dispatch(new Play(getPlaylist("My Playlist"), 2));
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        dispatch(FINISHED_PLAYING);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        dispatch(FINISHED_PLAYING);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // select another song
        dispatch(new Play(getPlaylist("My Playlist"), 1));
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        dispatch(FINISHED_PLAYING);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        dispatch(FINISHED_PLAYING);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);
    }

    @Test
    public void dispatchSkipNextTillLoop_modelUpdatesPlayingState() {
        // play last song
        dispatch(new Play(getPlaylist("My Playlist"), 2));
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        // skip next > play first song
        dispatch(SKIP_NEXT);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // skip next > play second song
        dispatch(SKIP_NEXT);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // skip next > play last song
        dispatch(SKIP_NEXT);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // skip next > skip first song
        dispatch(SKIP_NEXT);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);
    }

    @Test
    public void dispatchSkipPreviousTillLoop_modelUpdatesPlayingState() {
        // play first song
        dispatch(new Play(getPlaylist("My Playlist"), 0));
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        // skip previous > play last song
        dispatch(SKIP_PREVIOUS);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // skip previous > play second song
        dispatch(SKIP_PREVIOUS);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // skip previous > play first song
        dispatch(SKIP_PREVIOUS);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);

        // skip previous > skip to last song
        dispatch(SKIP_PREVIOUS);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);
        assertEquals(getPlaylist("My Playlist"), lastState.playlistPlaying);
    }

    @Test
    public void dispatchMultiplePlayerEvents() {
        // play first song
        dispatch(new Play(getPlaylist("My Playlist"), 0));

        // skip next > play second song
        dispatch(SKIP_NEXT);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);

        // skip previous > play first song
        dispatch(SKIP_PREVIOUS);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        // pause song
        dispatch(PLAY_PAUSE_CURRENT);
        assertTrue(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        // play song
        dispatch(PLAY_PAUSE_CURRENT);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        // skip previous > jump to last song
        dispatch(SKIP_PREVIOUS);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        // finished playing > jump to first song
        dispatch(FINISHED_PLAYING);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        // skip previous > jump to last song
        dispatch(SKIP_PREVIOUS);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        // disable repeat
        dispatch(REPEAT);
        dispatch(REPEAT);
        assertFalse(lastState.repeatAll);
        assertFalse(lastState.repeatSong);

        // finished playing > pause player
        dispatch(FINISHED_PLAYING);
        assertTrue(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        // skip previous > play second song
        dispatch(SKIP_PREVIOUS);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);

        // pause
        dispatch(PLAY_PAUSE_CURRENT);
        assertTrue(lastState.isPaused);
        assertEquals(getSong("Can You Feel It"), lastState.songPlaying);

        // skip next > play last song
        dispatch(SKIP_NEXT);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        // repeat one song
        dispatch(REPEAT);
        dispatch(REPEAT);
        assertFalse(lastState.repeatAll);
        assertTrue(lastState.repeatSong);

        // finished playing
        dispatch(FINISHED_PLAYING);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Is This Love"), lastState.songPlaying);

        // repeat all
        dispatch(REPEAT);
        dispatch(REPEAT);
        assertTrue(lastState.repeatAll);
        assertFalse(lastState.repeatSong);

        // finished playing > play first song
        dispatch(FINISHED_PLAYING);
        assertFalse(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);

        // pause
        dispatch(PLAY_PAUSE_CURRENT);
        assertTrue(lastState.isPaused);
        assertEquals(getSong("Stir It Up"), lastState.songPlaying);
    }
}
