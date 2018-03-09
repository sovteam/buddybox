package buddybox.core.events;

import buddybox.core.Dispatcher;

public class AlbumArtDownloadCompleted extends Dispatcher.Event {
    public final String fileName;

    public AlbumArtDownloadCompleted(String fileName) {
        super ("AlbumArtDownloadCompleted file: " + fileName);

        this.fileName = fileName;
    }
}
