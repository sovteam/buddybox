package buddybox.core.events;

import buddybox.core.Dispatcher;

public class CreatePlaylist extends Dispatcher.Event {
    public final String playlistName;
    public final String songHash;

    public CreatePlaylist(String playlistName, String songHash) {
        super("CreatePlaylist playlistName: " + playlistName + ", songHash: " + songHash);
        this.playlistName = playlistName;
        this.songHash = songHash;
    }
}
