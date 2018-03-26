package buddybox.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.junit.Before;
import org.junit.Test;

import buddybox.core.Song;
import buddybox.core.events.AlbumArtEmbeddedFound;
import buddybox.core.events.AlbumArtFound;
import buddybox.core.events.SongFound;
import sov.Hash;

import static buddybox.core.Dispatcher.dispatch;
import static junit.framework.Assert.assertEquals;


public class AlbumArtFoundTest extends ModelTest {

    @Before
    public void setup() {
        super.setup();
        Hash hash = new Hash(Base64.encode("JammingStream".getBytes(), 1));
        Song song = new Song(null, hash, "Jamming", "Bob Marley", "Legend", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8235L , 1L,false, false);
        dispatch(new SongFound(song));
    }

    @Test
    public void albumArtFound() {
        assertEquals(2, updateCount); // subscribe + SongFound

        String artString = "legend album art";
        Bitmap legendCoverArt = BitmapFactory.decodeByteArray(artString.getBytes(), 0, 16);

        Song song = lastState.allSongsPlaylist.songs.get(0);
        assertEquals(song.getArt(), null);
        dispatch(new AlbumArtFound(song.artist, song.album, legendCoverArt));

        assertEquals(3, updateCount);
        song = lastState.allSongsPlaylist.songs.get(0);
        assertEquals(legendCoverArt, song.getArt());
    }

    @Test
    public void embeddedAlbumArtFound() {
        assertEquals(2, updateCount); // subscribe + SongFound

        String artString = "legend album art embedded";
        Bitmap embeddedArt = BitmapFactory.decodeByteArray(artString.getBytes(), 0, 25);

        Song song = lastState.allSongsPlaylist.songs.get(0);
        assertEquals(song.getArt(), null);
        dispatch(new AlbumArtEmbeddedFound(song, embeddedArt));

        assertEquals(3, updateCount);
        song = lastState.allSongsPlaylist.songs.get(0);
        assertEquals(embeddedArt, song.getArt());
    }
}
