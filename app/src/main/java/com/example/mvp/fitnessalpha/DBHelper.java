package com.example.mvp.fitnessalpha;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by MVP on 11/3/2017.
 */
public class DBHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "myprovider";
    static final int DATABASE_VERSION = 2;

    DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        UserTable.onCreate(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        UserTable.onUpgrade(sqLiteDatabase, oldVersion, newVersion);
    }
}