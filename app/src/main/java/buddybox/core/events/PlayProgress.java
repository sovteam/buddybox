package buddybox.core.events;

import buddybox.core.Dispatcher;

public class PlayProgress extends Dispatcher.Event {
    public final int position;

    public PlayProgress(int position) {
        super("PlayProgress" + position);
        this.position = position;
    }
}