package buddybox.controller;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import buddybox.ModelSingleton;
import buddybox.core.Model;
import buddybox.core.State;
import buddybox.core.Song;
import buddybox.core.events.SongFound;
import buddybox.core.events.SongMissing;

import static buddybox.core.Dispatcher.dispatch;

public class Library {

    private static State state;

    public static void init() {
        ModelSingleton.addStateListener(new Model.StateListener() { @Override public void update(State state) {
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
            synchronizeLibrary();
    }

    synchronized
    private static State getState() {
        return Library.state;
    }

    private static void synchronizeLibrary() {
        List<File> mp3Files = SongUtils.listLibraryMp3Files();

        Map<String, Song> songByPath = new HashMap<>();
        for (Song song : getState().allSongsPlaylist.songs)
            songByPath.put(song.relativePath, song);

        for (File mp3 : mp3Files) {
            Song song = songByPath.remove(mp3.getPath());
            if (song == null || song.fileLength != mp3.length() || song.lastModified != mp3.lastModified())
                dispatch(new SongFound(SongUtils.readSong(mp3)));
        }

        for (Song missing : songByPath.values())
            dispatch(new SongMissing(missing));
    }
}
