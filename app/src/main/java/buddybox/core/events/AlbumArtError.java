package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.model.AlbumInfo;

public class AlbumArtError extends Dispatcher.Event {
    public final AlbumInfo albumInfo;

    public AlbumArtError(AlbumInfo albumInfo) {
        super("AlbumArtError, artist" + albumInfo.artist + ", album: " + albumInfo.name);
        this.albumInfo = albumInfo;
    }

}