package buddybox.model;

import android.util.Base64;

import org.junit.Test;

import buddybox.core.Song;
import buddybox.core.events.SongFound;
import buddybox.core.events.SongMissing;
import sov.Hash;

import static buddybox.core.Dispatcher.dispatch;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SongMissingTest extends ModelTest {

    @Test
    public void songMissingDispatched_modelRemovesSongAndArtist() {
        // dispatch Jamming song found
        Hash hash = new Hash(Base64.encode("JammingStream".getBytes(), 1));
        Song song = new Song(null, hash, "Jamming", "Bob Marley", "Legend", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8235L , 1L,false, false);
        dispatch(new SongFound(song));

        // dispatch Jamming song missing
        dispatch(new SongMissing(song));

        // check if song and artist were removed
        assertEquals(3, updateCount);
        assertTrue(lastState.allSongsPlaylist.songs.isEmpty());
        assertTrue(lastState.artists.isEmpty());

        reinitialize();

        assertEquals(1, updateCount);
        assertTrue(lastState.allSongsPlaylist.songs.isEmpty());
        assertTrue(lastState.artists.isEmpty());
    }

    @Test
    public void songMissingDispatched_modelRemovesOnlySong() {
        // dispatch "Jamming" song found
        Hash hash1 = new Hash(Base64.encode("JammingStream".getBytes(), 1));
        Song song1 = new Song(null, hash1, "Jamming", "Bob Marley", "Legend", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8235L , 1L,false, false);
        dispatch(new SongFound(song1));

        // dispatch "Is this Love" song found
        Hash hash2 = new Hash(Base64.encode("IsThisLoveStream".getBytes(), 1));
        Song song2 = new Song(null, hash2, "Is this love", "Bob Marley", "Legend", "Reggae", 214000, "/bob/legend/is this love.mp3" , 6789L , 2L,false, false);
        dispatch(new SongFound(song2));

        // dispatch Jamming song missing
        dispatch(new SongMissing(song1));

        // check if song and artist were removed
        assertEquals(4, updateCount);
        assertEquals(1, lastState.artists.size());
        assertEquals("Bob Marley", lastState.artists.get(0).name);
        assertEquals("Is this love", lastState.artists.get(0).songs.get(0).name);
        assertEquals(1, lastState.allSongsPlaylist.songs.size());
        assertEquals("Is this love", lastState.allSongsPlaylist.songs.get(0).name);

        reinitialize();

        assertEquals(1, updateCount);
        assertEquals(1, lastState.allSongsPlaylist.songs.size());
        assertEquals("Is this love", lastState.allSongsPlaylist.songs.get(0).name);
    }
}
