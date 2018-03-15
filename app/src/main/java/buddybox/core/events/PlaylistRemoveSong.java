package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Playlist;
import buddybox.core.Song;

public class PlaylistRemoveSong extends Dispatcher.Event {

    public final Playlist playlist;
    public final Song song;

    public PlaylistRemoveSong(Playlist playlist, Song song) {
        super("PlaylistRemoveSong playlist: " + playlist.name + ", song: " + song.name);
        this.playlist = playlist;
        this.song = song;
    }
}