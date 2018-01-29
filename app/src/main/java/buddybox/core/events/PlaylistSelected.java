package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Playlist;

public class PlaylistSelected extends Dispatcher.Event {
    public final Playlist playlist;

    public PlaylistSelected(Playlist playlist) {
        super("PlaylistSelected: " + playlist.name);
        this.playlist = playlist;
    }
}
