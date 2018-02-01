package buddybox.core.events;

import buddybox.core.Dispatcher;

public class PlaylistSetName extends Dispatcher.Event {
    public final String playlistName;

    public PlaylistSetName(String playlistName) {
        super("PlaylistSetName newName: " + playlistName);
        this.playlistName = playlistName;
    }
}
