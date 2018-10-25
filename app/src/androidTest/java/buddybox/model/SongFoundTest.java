package buddybox.model;

import android.util.Base64;

import org.junit.Test;

import buddybox.core.Artist;
import buddybox.core.Song;
import buddybox.core.events.SongFound;
import buddybox.core.events.SongMissing;
import sov.Hash;

import static buddybox.core.Dispatcher.dispatch;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SongFoundTest extends ModelTest {

    @Test
    public void songFoundDispatched_modelAddsNewSong() {
        // check empty songs and artists
        assertEquals(1, updateCount);
        assertTrue(lastState.allSongs.isEmpty());
        assertTrue(lastState.artists.isEmpty());

        // dispatch new song found
        Hash hash = new Hash(Base64.encode("JammingStream".getBytes(), 1));
        Song newSong = new Song(null, hash, "Jamming", "Bob Marley", "Legend", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8235L , 1L,false, false, 1L, null);
        dispatch(new SongFound(newSong));

        // second update
        assertEquals(2, updateCount);

        // check song found
        assertEquals(1, lastState.allSongs.size());
        Song song = lastState.allSongs.get(0);
        assertEquals(hash, song.hash);
        assertEquals("Jamming", song.name);
        assertEquals("Bob Marley", song.artist);
        assertEquals("Legend", song.album);
        assertEquals("Reggae", song.genre);
        assertEquals((Integer) 213000, song.duration);
        assertEquals("/bob/legend/jamming.mp3", song.filePath);
        assertEquals(8235L, song.fileLength);
        assertEquals(1L, song.lastModified);
        assertEquals(false, song.isMissing);
        assertEquals(false, song.isDeleted);

        // check new artist
        assertEquals(1, lastState.artists.size());
        Artist artist = lastState.artists.get(0);
        assertEquals("Bob Marley", artist.name);
        assertEquals(1, artist.songs.size());
        assertEquals("Jamming", artist.songs.iterator().next().name);

        reinitialize();

        // check if song was persisted
        assertEquals(1, updateCount);
        assertEquals(1, lastState.allSongs.size());

        // check song
        song = lastState.allSongs.get(0);
        assertEquals(hash, song.hash);
        assertEquals("Jamming", song.name);
        assertEquals("Bob Marley", song.artist);
        assertEquals("Legend", song.album);
        assertEquals("Reggae", song.genre);
        assertEquals((Integer) 213000, song.duration);
        assertEquals("/bob/legend/jamming.mp3", song.filePath);
        assertEquals(8235L, song.fileLength);
        assertEquals(1L, song.lastModified);
        assertEquals(false, song.isMissing);
        assertEquals(false, song.isDeleted);

        // check if artist was persisted
        assertEquals(1, lastState.artists.size());
        artist = lastState.artists.get(0);
        assertEquals("Bob Marley", artist.name);
        assertEquals(1, artist.songs.size());
        assertEquals("Jamming", artist.songs.iterator().next().name);

    }

    @Test
    public void songFoundDispatched_modelUpdatesOldSong() {
        Hash hash = new Hash(Base64.encode("JammingStream".getBytes(), 1));

        // dispatch Jamming song found
        Song song = new Song(null, hash, "Jamming", "Bob Marley", "Legend", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8235L , 1L,false, false, 1L, null);
        dispatch(new SongFound(song));

        // dispatch Jamming song missing
        dispatch(new SongMissing(song));

        // dispatch Jamming song found again
        Song newSong = new Song(null, hash, "Jamming it!", "Bob", "I am Legend", "Classic", 42, "/bobby/jamming-it.mp3" , 133L , 2L,false, false, 1L, null);
        dispatch(new SongFound(newSong));

        // check song update
        assertEquals(4, updateCount);
        assertEquals(1, lastState.allSongs.size());
        Song result = lastState.allSongs.get(0);
        assertEquals(hash, result.hash);
        assertEquals("Jamming it!", result.name);
        assertEquals("Bob", result.artist);
        assertEquals("I am Legend", result.album);
        assertEquals("Classic", result.genre);
        assertEquals((Integer) 42, result.duration);
        assertEquals("/bobby/jamming-it.mp3", result.filePath);
        assertEquals(133L, result.fileLength);
        assertEquals(2L, result.lastModified);
        assertEquals(false, result.isMissing);
        assertEquals(false, result.isDeleted);

        // check artist update
        assertEquals(1, lastState.artists.size());
        Artist artist = lastState.artists.get(0);
        assertEquals("Bob", artist.name);
        assertEquals(1, artist.songs.size());
        assertEquals("Jamming it!", artist.songs.iterator().next().name);

        reinitialize();

        // check if song updates were persisted
        assertEquals(1, updateCount);
        assertEquals(1, lastState.allSongs.size());
        result = lastState.allSongs.get(0);
        assertEquals(hash, result.hash);
        assertEquals("Jamming it!", result.name);
        assertEquals("Bob", result.artist);
        assertEquals("I am Legend", result.album);
        assertEquals("Classic", result.genre);
        assertEquals((Integer) 42, result.duration);
        assertEquals("/bobby/jamming-it.mp3", result.filePath);
        assertEquals(133L, result.fileLength);
        assertEquals(2L, result.lastModified);
        assertEquals(false, result.isMissing);
        assertEquals(false, result.isDeleted);

    }
}
