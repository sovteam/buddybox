package buddybox.core.events;

import buddybox.core.Dispatcher;

public class DownloadCompleted extends Dispatcher.Event {
    public final String fileName;

    public DownloadCompleted(String fileName) {
        super ("DownloadCompleted file: " + fileName);

        this.fileName = fileName;
    }
}
