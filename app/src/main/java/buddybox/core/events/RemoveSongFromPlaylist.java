package buddybox.core.events;

import buddybox.core.Dispatcher;

public class RemoveSongFromPlaylist extends Dispatcher.Event {
    public final String songHash;
    public final Long playlistId;

    public RemoveSongFromPlaylist(String songHash, Long playlistId) {
        super("RemoveSongFromPlaylist songId: " + songHash + ", playlist: " + playlistId);
        this.songHash = songHash;
        this.playlistId = playlistId;
    }

}