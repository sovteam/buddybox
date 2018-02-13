package buddybox.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper INSTANCE;

    private static final String DATABASE_NAME = "buddybox_database_v1";
    private static final int DATABASE_VERSION = 1;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (INSTANCE == null)
            INSTANCE = new DatabaseHelper(context.getApplicationContext());

        return INSTANCE;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        System.out.println("$$$$$$$$$$$ DB OPEN");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        System.out.println(">>> DB onCreate >>> CREATE TABLES");

        db.execSQL("CREATE TABLE IF NOT EXISTS SONGS (" +
                "HASH TEXT," +
                "NAME TEXT," +
                "ARTIST TEXT," +
                "ALBUM TEXT," +
                "GENRE TEXT," +
                "DURATION INTEGER," +
                "FILE_PATH TEXT," +
                "FILE_LENGTH INTEGER," +
                "LAST_MODIFIED INTEGER," +
                "IS_MISSING INTEGER," +
                "IS_DELETED INTEGER)");

        db.execSQL("CREATE TABLE IF NOT EXISTS PLAYLISTS (ID INTEGER PRIMARY KEY, NAME TEXT UNIQUE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS PLAYLIST_SONG (" +
                "SONG_HASH TEXT," +
                "PLAYLIST_ID INTEGER," +
                "POSITION INTEGER NOT NULL," +
                "PRIMARY KEY (SONG_HASH, PLAYLIST_ID, POSITION)," +
                "FOREIGN KEY(SONG_HASH) REFERENCES SONGS(HASH)," +
                "FOREIGN KEY(PLAYLIST_ID) REFERENCES PLAYLISTS(ID))");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {

    }
}
