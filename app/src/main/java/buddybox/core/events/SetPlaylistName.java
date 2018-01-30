package buddybox.core.events;

import buddybox.core.Dispatcher;

public class SetPlaylistName extends Dispatcher.Event {
    public final String playlistName;

    public SetPlaylistName(String playlistName) {
        super("SetPlaylistName newName: " + playlistName);
        this.playlistName = playlistName;
    }
}
