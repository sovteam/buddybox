package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class SongFound extends Dispatcher.Event {
    public final Song song;

    public SongFound(Song song) {
        super("SongFound " + song.name);
        this.song = song;
    }
}
