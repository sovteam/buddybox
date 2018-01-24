package buddybox.impl.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper INSTANCE;

    private static final String DATABASE_NAME = "buddybox_database_v0";
    private static final int DATABASE_VERSION = 1;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (INSTANCE == null) {
            System.out.println("$$$$$$$$$$$$$$$ instantiate DB");
            INSTANCE = new DatabaseHelper(context.getApplicationContext());
        }
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
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        System.out.println("$$$$$$$$$$$$$$$$$ DB onCreate");

        SQLiteDatabase db = INSTANCE.getReadableDatabase();

        db.execSQL("CREATE TABLE IF NOT EXISTS SONGS (" +
                "HASH TEXT PRIMARY KEY," +
                "NAME TEXT," +
                "ARTIST TEXT," +
                "GENRE TEXT," +
                "DURATION INTEGER)");

        System.out.println(">> inserts");
        ContentValues vals = new ContentValues(2);
        vals.put("HASH", "A0");
        vals.put("NAME", "A1");
        vals.put("GENRE", "A2");
        vals.put("ARTIST", "A3");
        vals.put("DURATION", 1);
        db.insert("SONGS", null, vals);

        ContentValues vals2 = new ContentValues(2);
        vals.put("HASH", "B0");
        vals.put("NAME", "B1");
        vals.put("GENRE", "B2");
        vals.put("ARTIST", "B3");
        vals.put("DURATION", 2);
        db.insert("SONGS", null, vals2);

        System.out.println(">> query");
        Cursor c = db.query("SONGS", new String[]{"HASH", "NAME", "ARTIST", "GENRE", "DURATION"}, "HASH = ?", new String[]{"A0"}, null, null, "NAME");
        c.moveToFirst();
        System.out.println(">> query result HASH: " + c.getString(0) + " NAME: " + c.getString(1) + " GENRE: " + c.getString(2) + " ARTIST: " + c.getString(3) + " DURATION: " + c.getString(4));
        // c.moveToNext();
        c.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
