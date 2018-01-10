package buddybox.impl.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

public class Database {

    private static SQLiteDatabase sqlite;

    public static SQLiteDatabase initDatabase(Context context) {

        File dbFile = new File(context.getFilesDir(), "database.sqlite");
        SQLiteDatabase ret = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

        System.out.println(">> create table");
        ret.execSQL("CREATE TABLE IF NOT EXISTS BANANA (ID INTEGER PRIMARY KEY, NAME TEXT)");

        System.out.println(">> inserts");
        ContentValues vals = new ContentValues(2);
        vals.put("ID", 1);
        vals.put("NAME", "Klaus :-)");
        ret.insert("BANANA", null, vals);

        ContentValues vals2 = new ContentValues(2);
        vals2.put("ID", 2);
        vals2.put("NAME", "jU9's B-)");
        ret.insert("BANANA", null, vals2);

        System.out.println(">> query");
        Cursor c = ret.query("BANANA", new String[]{"ID", "NAME"}, "ID = ?", new String[]{"1"}, null, null, "ID");
        System.out.println(">> query result ID: " + c.getString(0) + " NAME: " + c.getString(1));
        c.moveToNext();
        System.out.println(">> query result ID: " + c.getString(0) + " NAME: " + c.getString(1));


        return ret;
    }


}
