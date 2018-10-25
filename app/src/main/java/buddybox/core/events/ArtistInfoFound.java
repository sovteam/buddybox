package buddybox.core.events;

import android.graphics.Bitmap;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;

public class ArtistInfoFound extends Dispatcher.Event {
    public final Artist artist;
    public final Bitmap pic;
    public final String bio;

    public ArtistInfoFound(Artist artist, Bitmap pic, String bio) {
        super("ArtistInfoFound " + artist.name);
        this.artist = artist;
        this.pic = pic;
        this.bio = bio;
    }
}
