package buddybox.core.events;

import buddybox.core.Dispatcher;

public class Library {
    public static final Dispatcher.Event SYNC_LIBRARY = new Dispatcher.Event("SyncLibrary");
    public static final Dispatcher.Event SYNC_LIBRARY_FINISHED = new Dispatcher.Event("SyncLibraryFinished");
}