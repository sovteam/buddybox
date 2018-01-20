package buddybox.core.events;

import java.util.List;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class LovedUpdated extends Dispatcher.Event {
    public final List<Song> loved;

    public LovedUpdated(List<Song> loved) {
        super("Loved List Updated, size: " + loved.size());
        this.loved = loved;
    }
}
