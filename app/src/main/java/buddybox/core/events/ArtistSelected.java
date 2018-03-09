package buddybox.core.events;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;

public class ArtistSelected extends Dispatcher.Event {
    public final Artist artist;

    public ArtistSelected(Artist artist) {
        super("ArtistSelected " + artist.name);
        this.artist = artist;
    }
}
