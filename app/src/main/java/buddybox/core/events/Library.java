package buddybox.core.events;

import buddybox.core.Dispatcher;
import buddybox.core.Playlist;

public class Library {
    public static final Dispatcher.Event SYNC_LIBRARY = new Dispatcher.Event("SyncLibrary");
    public static final Dispatcher.Event SYNC_LIBRARY_FINISHED = new Dispatcher.Event("SyncLibraryFinished");
}