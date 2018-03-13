package buddybox.core.events;

import android.graphics.Bitmap;

import buddybox.core.Dispatcher;
import buddybox.core.Song;

public class AlbumArtEmbeddedFound extends Dispatcher.Event {
    public final Song song;
    public final Bitmap art;

    public AlbumArtEmbeddedFound(Song song, Bitmap art) {
        super("AlbumArtEmbeddedFound " + song.name);
        this.song = song;
        this.art = art;
    }
}
