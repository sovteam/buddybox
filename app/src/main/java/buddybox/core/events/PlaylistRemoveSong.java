package buddybox.core.events;

import buddybox.core.Dispatcher;

public class PlaylistRemoveSong extends Dispatcher.Event {
    public final String songHash;

    public PlaylistRemoveSong(String songHash) {
        super("PlaylistRemoveSong songId: " + songHash);
        this.songHash = songHash;
    }

}