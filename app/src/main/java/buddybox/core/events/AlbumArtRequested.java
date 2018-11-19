package buddybox.core.events;

import buddybox.core.Dispatcher;

public class AlbumArtRequested extends Dispatcher.Event {

    public final String artist;
    public final String album;

    public AlbumArtRequested(String artist, String album) {
        super("AlbumArtRequested artist: " + artist + ", album: " + album);
        this.artist = artist;
        this.album = album;
    }
}
