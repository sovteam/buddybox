package buddybox.core.events;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;

public class ArtistInfoError extends Dispatcher.Event {
    public final Artist artist;

    public ArtistInfoError(Artist artist) {
        super("ArtistInfoError " + artist.name);
        this.artist = artist;
    }
}
