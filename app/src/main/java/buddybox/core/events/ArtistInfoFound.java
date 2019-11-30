package buddybox.core.events;

import android.graphics.Bitmap;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;

public class ArtistInfoFound extends Dispatcher.Event {
    public final Artist artist;
    public final String bio;

    public ArtistInfoFound(Artist artist, String bio) {
        super("ArtistInfoFound " + artist.name);
        this.artist = artist;
        this.bio = bio;
    }
}
