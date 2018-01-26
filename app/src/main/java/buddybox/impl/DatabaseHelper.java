package buddybox.impl;

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
        System.out.println("$$$$$$$$$$$$$$$$$ DB onCreate");

        db.execSQL("CREATE TABLE IF NOT EXISTS SONGS (" +
                "HASH TEXT PRIMARY KEY," +
                "NAME TEXT," +
                "ARTIST TEXT," +
                "GENRE TEXT," +
                "DURATION INTEGER," +
                "RELATIVE_PATH TEXT," +
                "FILE_LENGTH INTEGER," +
                "LAST_MODIFIED INTEGER," +
                "IS_MISSING BOOLEAN)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
