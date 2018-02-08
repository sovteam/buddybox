package buddybox.core.events;

import buddybox.core.Dispatcher;

public class SeekTo extends Dispatcher.Event {
    public final int position;

    public SeekTo(int position) {
        super("SeekTo" + position);
        this.position = position;
    }
}