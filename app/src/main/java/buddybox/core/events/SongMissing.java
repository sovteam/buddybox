package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class SongMissing extends Dispatcher.Event {
    public final Song song;

    public SongMissing(Song song) {
        super("SongMissing " + song.name);
        this.song = song;
    }
}
