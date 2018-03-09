package buddybox.core.events;

import buddybox.core.Artist;
import buddybox.core.Dispatcher;

public class ArtistPictureDownloadCompleted extends Dispatcher.Event {
    public final String fileName;
    public final Artist artist;

    public ArtistPictureDownloadCompleted(String fileName, Artist artist) {
        super ("ArtistPictureDownloadCompleted file: " + fileName);

        this.fileName = fileName;
        this.artist = artist;
    }
}
