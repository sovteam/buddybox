package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Playlist;

public class PlayPlaylist extends Dispatcher.Event {
    public final Playlist playlist;
    public final int songIndex;

    public PlayPlaylist(Playlist playlist, int songIndex) {
        super("PlayPlaylist name: " + playlist.name + ", songIndex: " + songIndex);
        this.playlist = playlist;
        this.songIndex = songIndex;
    }
}
