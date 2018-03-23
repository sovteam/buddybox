package buddybox.model;

import android.database.sqlite.SQLiteDatabase;

import org.junit.Before;

import buddybox.core.IModel;
import buddybox.core.State;

public abstract class ModelTest {

    protected Model model;
    protected State lastState;
    protected int updateCount;
    protected IModel.StateListener listener;
    protected SQLiteDatabase db;

    @Before
    public void setup() {
        db = DatabaseHelper.getInstance(null).getReadableDatabase();
        initialize();
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
        db = model.db;
        model.removeStateListener(listener);
        initialize();
    }
}
