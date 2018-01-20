package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class SongAdded extends Dispatcher.Event {
    public final Song song;

    public SongAdded(Song song) {
        super("SongAdded " + song.name);
        this.song = song;
    }

}
