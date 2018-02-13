package buddybox.core.events;

import buddybox.core.Dispatcher;

public class PlaylistChangeSongPosition extends Dispatcher.Event {
    public final int fromPosition;
    public final int toPosition;

    public PlaylistChangeSongPosition(int fromPosition, int toPosition) {
        super("PlaylistChangeSongPosition from: " + fromPosition + " to: " + toPosition);
        this.fromPosition = fromPosition;
        this.toPosition = toPosition;
    }
}
