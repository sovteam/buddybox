package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class SongUpdate extends Dispatcher.Event {
    public final Song song;
    public final String name;
    public final String artist;
    public final String album;
    public final String genre;

    public SongUpdate(Song song, String name, String artist, String album, String genre) {
        super("SongUpdate hash: " + song.hash.toString());
        this.song = song;
        this.name = name;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
    }
}
