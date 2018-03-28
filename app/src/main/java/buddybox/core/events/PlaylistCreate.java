package buddybox.core.events;

import buddybox.core.Dispatcher;

public class PlaylistCreate extends Dispatcher.Event {
    public final String playlistName;
    public final String songHash;

    public PlaylistCreate(String playlistName, String songHash) {
        super("PlaylistCreate playlistName: " + playlistName + ", songHash: " + songHash);
        this.playlistName = playlistName;
        this.songHash = songHash;
    }
}
