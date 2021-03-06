package buddybox.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.junit.Before;
import org.junit.Test;

import buddybox.core.Artist;
import buddybox.core.Song;
import buddybox.core.events.ArtistPictureFound;
import buddybox.core.events.SongFound;
import sov.Hash;

import static buddybox.core.Dispatcher.dispatch;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class ArtistInfoFoundTest extends ModelTest {

    @Before
    public void setup() {
        super.setup();

        Hash hash1 = new Hash(Base64.encode("StirItUpStream".getBytes(), 1));
        Song song1 = new Song(null, hash1, "Stir It Up", "Bob Marley", "Legend Deluxe Edition", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8236L , 2L,false, false, 1L, null, 0);
        dispatch(new SongFound(song1));

        Hash hash2 = new Hash(Base64.encode("CanYouFeelItStream".getBytes(), 1));
        Song song2 = new Song(null, hash2, "Can You Feel It", "The Jacksons", "The Very Best Of", "Reggae", 213000, "/bob/legend/jamming.mp3" , 8235L , 3L,false, false, 1L, null, 0);
        dispatch(new SongFound(song2));
    }

    @Test
    public void artistPictureFound_modelUpdateArtist() {
        assertEquals(3, updateCount);
        assertNull(getArtist("The Jacksons").picture);
        assertNull(getArtist("Bob Marley").picture);

        Bitmap jacksonsPic = BitmapFactory.decodeByteArray("Jacksons Picture".getBytes(), 0, 16);
        dispatch(new ArtistPictureFound(getArtist("The Jacksons"), jacksonsPic));

        assertEquals(4, updateCount);
        assertEquals(jacksonsPic, getArtist("The Jacksons").picture);
        assertNull(getArtist("Bob Marley").picture);

        Bitmap bobPic = BitmapFactory.decodeByteArray("Bob Marley Pic".getBytes(), 0, 14);
        dispatch(new ArtistPictureFound(getArtist("Bob Marley"), bobPic));

        assertEquals(5, updateCount);
        assertEquals(jacksonsPic, getArtist("The Jacksons").picture);
        assertEquals(bobPic, getArtist("Bob Marley").picture);
    }

    private Artist getArtist(String name) {
        for (Artist artist : lastState.artists)
            if (artist.name.equals(name))
                return artist;
        throw new IllegalStateException("Artist " + name + " not found");
    }

}
