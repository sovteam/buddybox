package buddybox.core.events;

import android.graphics.Bitmap;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;

public class AlbumArtFound extends Dispatcher.Event {
    public final String artist;
    public final String album;
    public final Bitmap art;

    public AlbumArtFound(String artist, String album, Bitmap art) {
        super("AlbumArtFound artist: " + artist + ", album: " + album);
        this.artist = artist;
        this.album = album;
        this.art = art;
    }
}
