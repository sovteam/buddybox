package buddybox.core.events;

import buddybox.core.Dispatcher;

public class PlaylistAddSong extends Dispatcher.Event {
    public final String songHash;
    public final Long playlistId;

    public PlaylistAddSong(String songHash, Long playlistId) {
        super("PlaylistAddSong songId: " + songHash + ", playlist: " + playlistId);
        this.songHash = songHash;
        this.playlistId = playlistId;
    }

}