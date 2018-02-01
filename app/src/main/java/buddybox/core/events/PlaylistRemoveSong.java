package buddybox.core.events;

import buddybox.core.Dispatcher;

public class PlaylistRemoveSong extends Dispatcher.Event {
    public final String songHash;
    public final Long playlistId;

    public PlaylistRemoveSong(String songHash, Long playlistId) {
        super("PlaylistRemoveSong songId: " + songHash + ", playlist: " + playlistId);
        this.songHash = songHash;
        this.playlistId = playlistId;
    }

}