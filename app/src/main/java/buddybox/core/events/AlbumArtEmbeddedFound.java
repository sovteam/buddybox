package buddybox.core.events;

import android.graphics.Bitmap;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class AlbumArtEmbeddedFound extends Dispatcher.Event {
    public final Song song;
    public final boolean hasEmbeddedArt;

    public AlbumArtEmbeddedFound(Song song, boolean hasEmbeddedArt) {
        super("AlbumArtEmbeddedFound " + song.name);
        this.song = song;
        this.hasEmbeddedArt = hasEmbeddedArt;
    }
}
