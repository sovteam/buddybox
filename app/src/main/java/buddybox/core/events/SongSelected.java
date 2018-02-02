package buddybox.core.events;

import buddybox.core.Dispatcher;

public class SongSelected extends Dispatcher.Event {
    public final String hash;

    public SongSelected(String hash) {
        super("SongSelected: " + hash);
        this.hash = hash;
    }
}
