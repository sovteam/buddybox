package buddybox.controller;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import buddybox.ui.ModelProxy;
import buddybox.core.IModel;
import buddybox.core.State;
import buddybox.core.Song;
import buddybox.core.events.SongFound;
import buddybox.core.events.SongMissing;
import utils.Daemon;

import static buddybox.core.Dispatcher.dispatch;

public class Library {

    private static State state;

    public static void init() {
        ModelProxy.addStateListener(new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }});
    }

    synchronized
    private static void updateState(State state) {
        if (!state.hasWriteExternalStoragePermission)
            return;

        boolean needsSync = Library.state == null;
        Library.state = state;
        if (needsSync)
            startSynchronizingLibrary();
    }

    synchronized
    private static State getState() {
        return Library.state;
    }

    private static void startSynchronizingLibrary() {
        new Daemon("Library Sync") { @Override public void run() {
            synchronizeLibrary();
        }};
    }

    private static void synchronizeLibrary() {
        System.out.println("222 sync lib");
        List<File> mp3Files = SongUtils.listLibraryMp3Files();

        Map<String, Song> songByPath = new HashMap<>();
        for (Song song : getState().allSongsPlaylist.songs) {
            System.out.println(">>> songByPath path " + song.relativePath);
            if (!song.isMissing)
                songByPath.put(song.relativePath, song);
        }

        for (File mp3 : mp3Files) {
            Song song = songByPath.remove(mp3.getPath());
            if (song == null || song.fileLength != mp3.length() || song.lastModified != mp3.lastModified())
                dispatch(new SongFound(SongUtils.readSong(mp3)));
        }

        for (Song missing : songByPath.values()) {
            System.out.println("222 song missing " + missing.name);
            dispatch(new SongMissing(missing));
        }
    }
}
