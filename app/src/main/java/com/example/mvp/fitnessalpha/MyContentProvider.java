package com.example.mvp.fitnessalpha;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class MyContentProvider extends ContentProvider {

    private final static String TAG = MyContentProvider.class.getSimpleName();

    static final String PROVIDER = "com.example.mvp.fitnessalpha";

    static private SQLiteDatabase db;
    static final String DATABASE_NAME = "myprovider";
    static final String TABLE_NAME = "user";
    static final int DATABASE_VERSION = 1;
    static final String CREATE_DB_TABLE =
            "CREATE TABLE " + TABLE_NAME
            + " _id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + " name TEXT NOT NULL, "
            + " gender TEXT NOT NULL, "
            + " weight DOUBLE NOT NULL, "
            + " avgDistannce DOUBLE, "
            + " avgTime TEXT, "
            + " avgWorkouts INTEGER, "
            + " avgCaloriesBurned DOUBLE, "
            + " allTimeDistannce DOUBLE, "
            + " allTimeTime TEXT, "
            + " allTimeWorkouts INTEGER, "
            + " allTimeCaloriesBurned DOUBLE);";

    private static class DB extends SQLiteOpenHelper {

        DB(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            db.execSQL(CREATE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    public MyContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO: Implement this to handle requests to insert a new row.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {
        // TODO: Implement this to initialize your content provider on startup.
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        // TODO: Implement this to handle query requests from clients.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
