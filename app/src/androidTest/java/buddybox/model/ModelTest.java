package buddybox.model;

import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;

import buddybox.core.Dispatcher;
import buddybox.core.IModel;
import buddybox.core.Playlist;
import buddybox.core.Song;
import buddybox.core.State;

public abstract class ModelTest {

    private IModel.StateListener listener;
    private SQLiteDatabase db;
    private Model model;

    State lastState;

    int updateCount;

    @Before
    public void setup() {
        db = DatabaseHelper.getInstance(null).getReadableDatabase();
        initialize();
    }

    @After
    public void finished() {
        model.removeStateListener(listener);
        model = null;
    }

    private void initialize() {
        Dispatcher.reset();
        model = new Model(null);
        model.setDatabase(db);
        updateCount = 0;
        listener = new IModel.StateListener() { @Override public void update(State state) {
            updateState(state);
        }};
        model.addStateListener(listener);
    }

    private void updateState(State state) {
        lastState = state;
        updateCount++;
    }

    void reinitialize() {
        model.removeStateListener(listener);
        initialize();
    }

    Song getSong(String name) {
        for (Song song : lastState.allSongs) {
            if (song.name.equals(name))
                return song;
        }
        throw new IllegalStateException("Song " + name + " not found");
    }

    Playlist getPlaylist(String name) {
        for (Playlist playlist : lastState.playlists)
            if (playlist.name.equals(name))
                return playlist;
        throw new IllegalStateException("Playlist " + name + " not found");
    }
}
