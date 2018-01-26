package buddybox.core.events;

import buddybox.core.Dispatcher;

public class AddSongToPlaylist extends Dispatcher.Event {
    public final String songHash;
    public final Long playlistId;

    public AddSongToPlaylist(String songHash, Long playlistId) {
        super("AddSongToPlaylist songId: " + songHash + ", playlist: " + playlistId);
        this.songHash = songHash;
        this.playlistId = playlistId;
    }

}