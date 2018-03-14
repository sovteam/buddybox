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
        List<File> mp3Files = SongUtils.listLibraryMp3Files();

        Map<String, Song> songByPath = new HashMap<>();
        if (state != null)
            for (Song song : state.allSongsPlaylist.songs)
                songByPath.put(song.filePath, song);

        for (File mp3 : mp3Files) {
            Song song = songByPath.remove(mp3.getPath());
            if (song == null || song.fileLength != mp3.length() || song.lastModified != mp3.lastModified())
                dispatch(new SongFound(SongUtils.readSong(mp3)));
        }

        for (Song missing : songByPath.values())
            dispatch(new SongMissing(missing));

        dispatch(SYNC_LIBRARY_FINISHED);
        Log.i(TAG,"SYNC finished: " + (System.currentTimeMillis() - start));
    }
}
