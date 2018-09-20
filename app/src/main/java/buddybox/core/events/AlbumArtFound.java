package buddybox.core.events;

import android.graphics.Bitmap;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;
import buddybox.model.AlbumInfo;

public class AlbumArtFound extends Dispatcher.Event {
    public final AlbumInfo album;
    public final Bitmap art;

    public AlbumArtFound(AlbumInfo album, Bitmap art) {
        super("AlbumArtFound artist: " + album.artist + ", album: " + album.name);
        this.album = album;
        this.art = art;
    }
}
