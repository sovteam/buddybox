package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.model.AlbumInfo;

public class AlbumArtNotFound extends Dispatcher.Event {
    public final AlbumInfo albumInfo;

    public AlbumArtNotFound(AlbumInfo albumInfo) {
        super("AlbumArtNotFound, artist" + albumInfo.artist + ", album: " + albumInfo.name);
        this.albumInfo = albumInfo;
    }
}
