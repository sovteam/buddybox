package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Playlist;

public class PlaylistChangeSongPosition extends Dispatcher.Event {
    public final Playlist playlist;
    public final int fromPosition;
    public final int toPosition;

    public PlaylistChangeSongPosition(Playlist playlist, int fromPosition, int toPosition) {
        super("PlaylistChangeSongPosition from: " + fromPosition + " to: " + toPosition);
        this.playlist = playlist;
        this.fromPosition = fromPosition;
        this.toPosition = toPosition;
    }
}
