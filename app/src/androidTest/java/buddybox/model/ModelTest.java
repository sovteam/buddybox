package buddybox.model;

import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;

import buddybox.core.IModel;
import buddybox.core.Song;
import buddybox.core.State;

public abstract class ModelTest {

    private SQLiteDatabase db;

    private Model model;
    State lastState;
    int updateCount;
    private IModel.StateListener listener;

    @Before
    public void setup() {
        // resetDatabase();
        db = DatabaseHelper.getInstance(null).getReadableDatabase();
        initialize();
    }

    private void resetDatabase() {
        db.delete("SONGS", null, null);
        db.delete("PLAYLISTS", null, null);
        db.delete("PLAYLIST_SONG", null, null);
        db.delete("ARTIST_BIO", null, null);
    }

    @After
    public void finished() {
        model.removeStateListener(listener);
        model = null;
    }

    private void initialize() {
        model = new Model(null);
        model.setDatabase(db);

        updateCount = 0;
        listener = new IModel.StateListener() { @Override public void update(State state) {
            lastState = state;
            updateCount++;
        }};
        model.addStateListener(listener);
    }

    void reinitialize() {
        model.removeStateListener(listener);
        initialize();
    }

    Song getSong(String name) {
        for (Song song : lastState.allSongsPlaylist.songs) {
            if (song.name.equals(name))
                return song;
        }
        throw new IllegalStateException("Song " + name + " not found");
    }
}
