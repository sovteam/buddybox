package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class SongEmbeddedArtResult extends Dispatcher.Event {
    public final Song song;
    public final boolean hasArt;

    public SongEmbeddedArtResult(Song song, boolean hasArt) {
        super("SongEmbeddedArtResult " + song.name + ", hasArt: " + hasArt);
        this.song = song;
        this.hasArt = hasArt;
    }
}
