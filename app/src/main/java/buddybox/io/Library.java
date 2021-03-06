package buddybox.io;

import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;
import buddybox.core.events.SongDeleted;
import buddybox.core.events.SongFound;
import buddybox.core.events.SongMissing;
import utils.Daemon;

import static buddybox.core.Dispatcher.dispatch;
import static buddybox.core.events.Library.SYNC_LIBRARY_FINISHED;
import static buddybox.ui.ModelProxy.addStateListener;

public class Library {

    private static String TAG = "Library";
    private static State state;
    private static ExecutorService service = Executors.newCachedThreadPool();

    public static void init() {
        addStateListener(new IModel.StateListener() { @Override public void update(final State state) {
            updateState(state);
        }});
    }

    private static void updateState(final State state) {
        boolean wasSyncing = Library.state != null && Library.state.syncLibraryPending;
        if (state.syncLibraryPending && !wasSyncing)
            service.submit(new Runnable() {
                @Override
                public void run() {
                    startSynchronizingLibrary();
                }
            });

        boolean shawDeleteSong = state.deleteSong != null &&
                (Library.state == null || Library.state.deleteSong != state.deleteSong);
        if (shawDeleteSong)
            startDeleteSong(state.deleteSong);

        Library.state = state;
    }

    private static void startDeleteSong(final Song song) {
        new Daemon("Library.deleteSong") { @Override public void run() {
            deleteSong(song);
        }};
    }

    private static void deleteSong(Song song) {
        Log.i(TAG,"DELETE song");
        if (SongUtils.deleteSong(song))
            dispatch(new SongDeleted(song));
    }

    private static void startSynchronizingLibrary() {
        synchronizeLibrary();
    }

    private static void synchronizeLibrary() {
        Log.i(TAG,"SYNC started");
        long start = System.currentTimeMillis();
        List<SongMedia> mp3Files = SongUtils.listSongMediaFiles();

        Map<Long, Song> songByMediaId = new HashMap<>();
        if (state != null)
            for (Song song : state.allSongs)
                songByMediaId.put(song.mediaId, song);

        for (SongMedia mp3 : mp3Files) {
            Song song = songByMediaId.remove(mp3.getMediaId());
            if (song == null || song.duration() != mp3.getDuration() || song.lastModified != mp3.getModified())
                dispatch(new SongFound(SongUtils.readSong(mp3)));
        }

        for (Song missing : songByMediaId.values())
            dispatch(new SongMissing(missing));

        dispatch(SYNC_LIBRARY_FINISHED);
        Log.i(TAG,"SYNC finished: " + (System.currentTimeMillis() - start));
    }
}
