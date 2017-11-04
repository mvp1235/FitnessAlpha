package com.example.mvp.fitnessalpha;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by MVP on 11/3/2017.
 */

public class UserTable {
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

    static final String TABLE_NAME = "user";
    static final String CREATE_DB_TABLE =
            "CREATE TABLE " + TABLE_NAME
                    + "( _id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL, "
                    + "gender TEXT NOT NULL, "
                    + "weight DOUBLE NOT NULL, "
                    + "avgDistannce DOUBLE, "
                    + "avgTime TEXT, "
                    + "avgWorkouts INTEGER, "
                    + "avgCaloriesBurned DOUBLE, "
                    + "allTimeDistannce DOUBLE, "
                    + "allTimeTime TEXT, "
                    + "allTimeWorkouts INTEGER, "
                    + "allTimeCaloriesBurned DOUBLE);";

    public static void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_DB_TABLE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.w(UserTable.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(database);
    }
}
