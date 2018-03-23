package buddybox.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "buddybox_database_v1";
    private static final int DATABASE_VERSION = 1;

    static synchronized DatabaseHelper getInstance(Context context) {
        return context == null
                ? new DatabaseHelper()
                : new DatabaseHelper(context.getApplicationContext());
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private DatabaseHelper() {
        // creates db in memory
        super(null, null, null, 1);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        System.out.println("$$$$$$$$$$$ DB OPEN");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        System.out.println(">>> DB onCreate >>> CREATE TABLES");

        db.execSQL("CREATE TABLE IF NOT EXISTS SONGS (" +
                "ID INTEGER PRIMARY KEY," +
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

        db.execSQL("CREATE TABLE IF NOT EXISTS ARTIST_BIO (" +
                "ARTIST_NAME TEXT PRIMARY KEY," +
                "CONTENT TEXT NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS VOLUME_SETTINGS (" +
                "OUTPUT TEXT PRIMARY KEY," +
                "VOLUME INTEGER NOT NULL)");

        ContentValues speaker = new ContentValues();
        speaker.put("OUTPUT", "speaker");
        speaker.put("VOLUME", 100);
        db.insert("VOLUME_SETTINGS", null, speaker);

        ContentValues headphones = new ContentValues();
        headphones.put("OUTPUT", "headphones");
        headphones.put("VOLUME", 50);
        db.insert("VOLUME_SETTINGS", null, headphones);

        ContentValues bluetooth = new ContentValues();
        bluetooth.put("OUTPUT", "bluetooth");
        bluetooth.put("VOLUME", 70);
        db.insert("VOLUME_SETTINGS", null, bluetooth);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {

    }
}
