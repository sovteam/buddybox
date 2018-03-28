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

        hash = new Hash(Base64.encode("IsThisLoveStream".getBytes(), 1));
        song = new Song(null, hash, "Is This Love", "Bob Marley", "Legend", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8236L , 2L,false, false);
        dispatch(new SongFound(song));

        hash = new Hash(Base64.encode("StirItUpStream".getBytes(), 1));
        song = new Song(null, hash, "Stir It Up", "Bob Marley", "Legend Deluxe Edition", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8236L , 2L,false, false);
        dispatch(new SongFound(song));

        hash = new Hash(Base64.encode("CanYouFeelItStream".getBytes(), 1));
        song = new Song(null, hash, "Can You Feel It", "The Jacksons", "The Very Best Of", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8235L , 3L,false, false);
        dispatch(new SongFound(song));
    }

    @Test
    public void albumArtFound() {
        assertEquals(5, updateCount); // 1 subscribe + 4 SongFound
        for (int i = 0; i < 4; i++) {
            assertEquals(null, lastState.allSongsPlaylist.songs.get(i).getArt());
        }

        String artString = "legend album art";
        Bitmap legendCoverArt = BitmapFactory.decodeByteArray(artString.getBytes(), 0, 16);
        dispatch(new AlbumArtFound("Bob Marley", "Legend", legendCoverArt));

        assertEquals(6, updateCount);

        // same album which art was found
        assertEquals(legendCoverArt, getSong("Jamming").getArt());
        assertEquals(legendCoverArt, getSong("Is This Love").getArt());

        // same artist but different album
        assertEquals(null, getSong("Stir It Up").getArt());

        // different album and artist
        assertEquals(null, getSong("Can You Feel It").getArt());
    }

    @Test
    public void embeddedAlbumArtFound() {
        String artString = "legend album art embedded";
        Bitmap embeddedArt = BitmapFactory.decodeByteArray(artString.getBytes(), 0, 16);
        dispatch(new AlbumArtEmbeddedFound(getSong("Jamming"), embeddedArt));

        assertEquals(6, updateCount);
        assertEquals(embeddedArt, getSong("Jamming").getArt());
        assertEquals(null, getSong("Is This Love").getArt());
        assertEquals(null, getSong("Stir It Up").getArt());
        assertEquals(null, getSong("Can You Feel It").getArt());

    }

    @Test
    public void albumArtFoundCannotOverrideEmbeddedArt() {
        String embeddedString = "art embedded";
        Bitmap embeddedArt = BitmapFactory.decodeByteArray(embeddedString.getBytes(), 0, 12);
        dispatch(new AlbumArtEmbeddedFound(getSong("Jamming"), embeddedArt));

        String albumArtString = "legend album art";
        Bitmap albumArt = BitmapFactory.decodeByteArray(albumArtString.getBytes(), 0, 16);
        dispatch(new AlbumArtFound("Bob Marley", "Legend", albumArt));

        assertEquals(7, updateCount);
        assertEquals(embeddedArt,   getSong("Jamming").getArt());
        assertEquals(albumArt,      getSong("Is This Love").getArt());
        assertEquals(null, getSong("Stir It Up").getArt());
        assertEquals(null, getSong("Can You Feel It").getArt());
    }
}
