package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class SongDeleted extends Dispatcher.Event {
    public final Song song;

    public SongDeleted(Song song) {
        super("SongDeleted " + song.name);
        this.song = song;
    }

}