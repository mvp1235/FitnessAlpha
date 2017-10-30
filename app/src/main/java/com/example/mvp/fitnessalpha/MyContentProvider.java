package com.example.mvp.fitnessalpha;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Scanner;

import static android.provider.CalendarContract.CalendarCache.URI;

public class MyContentProvider extends ContentProvider {

    static final String TAG = MyContentProvider.class.getSimpleName();
    static final int DEVICES = 1;
    static final int DEVICE_ID = 2;
    static final String PROVIDER = "com.example.mvp.fitnessalpha";

    static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER, "devices", DEVICES);
        uriMatcher.addURI(PROVIDER, "devices/#", DEVICE_ID);
    }

    Context mContext;
    private static HashMap<String, String> DEVICES_PROJECTION_MAP;

    static final String _ID = "_id";
    static final String NAME = "name";
    static final String GENDER = "gender";
    static final String WEIGHT = "weight";
    static final String AVG_DISTANCE = "avgDistannce";
    static final String AVG_TIME = "avgTime";
    static final String AVG_WORKOUTS = "avgWorkouts";
    static final String AVG_CALORIES_BURNED = "avgCaloriesBurned";
    static final String ALL_TIME_DISTANCE = "allTimeDistannce";
    static final String ALL_TIME_TIME = "allTimeTime";
    static final String ALL_TIME_WORKOUTS = "allTimeWorkouts";
    static final String ALL_TIME_CALORIES_BURNED = "allTimeCaloriesBurned";

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
//            db.execSQL(CREATE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }

    public MyContentProvider() {
    }

    private void notifyChange(Uri uri) {
        ContentResolver resolver = mContext.getContentResolver();
        if (resolver != null) {
            resolver.notifyChange(uri, null);
        }
    }

    private int getMatchedID(Uri uri) {
        int matchedID = uriMatcher.match(uri);
        if (!(matchedID == DEVICES || matchedID == DEVICE_ID)) {
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        return matchedID;
    }

    private String getIdString(Uri uri) {
        return (_ID + " = " + uri.getPathSegments().get(1));
    }

    private String getSelectionWithID(Uri uri, String selection) {
        String sel_str = getIdString(uri);
        if (!TextUtils.isEmpty(selection)) {
            sel_str += " AND (" + selection + ")";
        }
        return sel_str;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.v(TAG, "content provider: delete()");

        int count = 0;

        String sel_str = (getMatchedID(uri) == DEVICE_ID) ? getSelectionWithID(uri, selection) : selection;
        count = db.delete(TABLE_NAME, sel_str, selectionArgs);
        notifyChange(uri);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        Log.v(TAG, "content provider: getType()");

        if (getMatchedID(uri) == DEVICES) {
            return "vnd.android.cursor.dir/vnd.mvp.fitnessalpha";
        } else {
            return "vnd.android.cursor.item/vnd.mvp.fitnessalpha";
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.v(TAG, "content provider: insert()");

        long row = db.insert(TABLE_NAME, "", values);

        if (row > 0) {
            Uri _uri = ContentUris.withAppendedId(URI, row);
            notifyChange(_uri);
            return _uri;
        }
        throw new SQLException("Failed to add a record into " + uri);
    }

    @Override
    public boolean onCreate() {
        Log.v(TAG, "content provider: onCreate()");

        mContext = getContext();
        if (mContext == null) {
            Log.e(TAG, "Fail to retrieve the context");
            return false;
        }
        DB dbHelper = new DB(mContext);
        db = dbHelper.getWritableDatabase();
        if (db == null) {
            Log.e(TAG, "Failed to create a writable database");
            return false;
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Log.v(TAG, "content provider: query()");

        SQLiteQueryBuilder sqLiteQueryBuilder = new SQLiteQueryBuilder();
        sqLiteQueryBuilder.setTables(TABLE_NAME);

        if (getMatchedID(uri) == DEVICES) {
            sqLiteQueryBuilder.setProjectionMap(DEVICES_PROJECTION_MAP);
        } else {
            sqLiteQueryBuilder.appendWhere(getIdString(uri));
        }

        if (sortOrder == null || sortOrder == "") {
            sortOrder = NAME;
        }

        Cursor c = sqLiteQueryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        c.setNotificationUri(mContext.getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        Log.v(TAG, "content provider: update()");

        int count = 0;
        int matchedID = getMatchedID(uri);

        String sel_str = (matchedID == DEVICE_ID) ? getSelectionWithID(uri, selection) : selection;

        count = db.update(TABLE_NAME, values, sel_str, selectionArgs);
        notifyChange(uri);
        return count;
    }
}
