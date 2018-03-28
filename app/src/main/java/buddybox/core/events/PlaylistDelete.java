package buddybox.core.events;

import buddybox.core.Dispatcher;

public class PlaylistDelete extends Dispatcher.Event {
    public final Long playlistId;

    public PlaylistDelete(Long playlistId) {
        super("PlaylistDelete " + playlistId);
        this.playlistId = playlistId;
    }
}
