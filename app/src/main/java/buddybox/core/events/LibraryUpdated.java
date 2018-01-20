package buddybox.core.events;

import java.util.List;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class LibraryUpdated extends Dispatcher.Event {

    public final List<Song> allSongs;
    public final List<Artist> allArtists;

    public LibraryUpdated(List<Song> allSongs, List<Artist> allArtists) {
        super("Library Synchronized - Songs: " + allSongs.size() + ", Artists: " + allArtists.size());
        this.allSongs = allSongs;
        this.allArtists = allArtists;
    }
}
