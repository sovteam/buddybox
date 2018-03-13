package buddybox.core.events;

import android.graphics.Bitmap;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;

public class ArtistPictureFound extends Dispatcher.Event {
    public final Artist artist;
    public final Bitmap picture;

    public ArtistPictureFound(Artist artist, Bitmap picture) {
        super("ArtistPictureFound " + artist.name);
        this.artist = artist;
        this.picture = picture;
    }
}
