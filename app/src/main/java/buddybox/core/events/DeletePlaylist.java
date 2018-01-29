package buddybox.core.events;

import buddybox.core.Dispatcher;

public class DeletePlaylist extends Dispatcher.Event {
    public final Long playlistId;

    public DeletePlaylist(Long playlistId) {
        super("DeletePlaylist " + playlistId);
        this.playlistId = playlistId;
    }
}
