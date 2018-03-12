package buddybox.io;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import static buddybox.ui.ModelProxy.removeStateListener;

public class Library extends Service {

    private State state;
    private IModel.StateListener stateListener;

    public static void init(Context context) {
        Intent intent = new Intent(context, Library.class);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        stateListener = new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }};
        addStateListener(stateListener);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean stopService(Intent name) {
        Log.d("Library", "stopService");
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        Log.d("Library", "Remove stateListener: " + (stateListener != null));
        removeStateListener(stateListener);
        super.onDestroy();
    }

    synchronized
    private void updateState(State state) {
        boolean wasSyncing = this.state != null && this.state.syncLibraryPending;
        if (state.syncLibraryPending && !wasSyncing)
            startSynchronizingLibrary();

        boolean shawDeleteSong = state.deleteSong != null &&
                (this.state == null || this.state.deleteSong != state.deleteSong);
        if (shawDeleteSong)
            startDeleteSong(state.deleteSong);

        this.state = state;
    }

    private void startDeleteSong(final Song song) {
        new Daemon("Delete Song") { @Override public void run() { // TODO should put in same thread of sync?
            deleteSong(song);
        }};
    }

    private void deleteSong(Song song) {
        if (SongUtils.deleteSong(song))
            dispatch(new SongDeleted(song));
    }

    synchronized
    private State getState() {
        return state;
    }

    private void startSynchronizingLibrary() {
        new Daemon("Library Sync") { @Override public void run() {
            synchronizeLibrary();
        }};
    }

    private void synchronizeLibrary() {
        long start = System.currentTimeMillis();
        List<File> mp3Files = SongUtils.listLibraryMp3Files();

        Map<String, Song> songByPath = new HashMap<>();
        for (Song song : getState().allSongsPlaylist.songs)
            songByPath.put(song.filePath, song);

        for (File mp3 : mp3Files) {
            Song song = songByPath.remove(mp3.getPath());
            if (song == null || song.fileLength != mp3.length() || song.lastModified != mp3.lastModified())
                dispatch(new SongFound(SongUtils.readSong(mp3)));
        }

        for (Song missing : songByPath.values())
            dispatch(new SongMissing(missing));

        dispatch(SYNC_LIBRARY_FINISHED);
        System.out.println(">>> SYNC LIB FINISHED: " + (System.currentTimeMillis() - start));
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
