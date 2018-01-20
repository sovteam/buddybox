package buddybox.core.events;

import buddybox.core.Dispatcher;

public class AddSongToPlaylist extends Dispatcher.Event {
    private final String songId;
    private final String playlistName;

    public AddSongToPlaylist(String songId, String playlistName) {
        super("AddSongToPlaylist songId: " + songId + ", playlist: " + playlistName);
        this.songId = songId;
        this.playlistName = playlistName;
    }

}