package buddybox.core.events;

import java.util.List;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;
import buddybox.core.Playlist;

public class LibraryUpdated extends Dispatcher.Event {

    public final Playlist allSongs;
    public final List<Artist> allArtists;

    public LibraryUpdated(Playlist allSongs, List<Artist> allArtists) {
        super("Library Synchronized - Songs: " + allSongs.size() + ", Artists: " + allArtists.size());
        this.allSongs = allSongs;
        this.allArtists = allArtists;
    }
}
