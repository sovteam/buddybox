package buddybox.core.events;

import android.util.Base64;

import org.junit.Before;
import org.junit.Test;

import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.model.Model;
import sov.Hash;

import static buddybox.core.Dispatcher.dispatch;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class SongFoundTest {

    private Model model;
    private State lastState;
    private int updateCount;
    private IModel.StateListener listener;

    @Before
    public void setup() {
        if (listener != null && model != null)
            model.removeStateListener(listener);

        model = new Model(null);
        updateCount = 0;
        listener = new IModel.StateListener() { @Override public void update(State state) {
            lastState = state;
            updateCount++;
        }};
        model.addStateListener(listener);
    }

    @Test
    public void songFoundEventDispatched_modelSavesSong() {
        // check empty songs
        assertEquals(1, updateCount);
        assertTrue(lastState.allSongsPlaylist.songs.isEmpty());

        // dispatch new song found
        Hash hash = new Hash(Base64.encode("JammingStream".getBytes(), 1));
        Song newSong = new Song(null, hash, "Jamming", "Bob Marley", "Legend", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8235L , 1L,false, false);
        dispatch(new SongFound(newSong));

        // second update: check song found
        assertEquals(2, updateCount);
        assertEquals(1, lastState.allSongsPlaylist.songs.size());
        Song song = lastState.allSongsPlaylist.songs.get(0);
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

        // reinitialize
        setup();

        // check if song was persisted
        assertEquals(1, updateCount);
        assertEquals(1, lastState.allSongsPlaylist.songs.size());
        song = lastState.allSongsPlaylist.songs.get(0);
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

    }
}
