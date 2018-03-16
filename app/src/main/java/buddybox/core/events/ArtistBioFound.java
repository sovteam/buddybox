package buddybox.core.events;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;

public class ArtistBioFound extends Dispatcher.Event {
    public final Artist artist;
    public final String content;

    public ArtistBioFound(Artist artist, String about) {
        super("ArtistBioFound " + artist.name);
        this.artist = artist;
        this.content = about;
    }
}
