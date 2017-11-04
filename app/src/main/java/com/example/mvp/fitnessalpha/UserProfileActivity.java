    package com.example.mvp.fitnessalpha;

import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

    public class UserProfileActivity extends AppCompatActivity {

    private TextView userNameTV, genderTV, weightTV;
    private TextView avgDistanceTV, avgTimeTV, avgWorkoutsTV, avgCaloriesBurnedTV;
    private TextView allTimeDistanceTV, allTimeTimeTV, allTimeWorkoutsTV, allTimeCaloriesBurnedTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        //Reference all UI elements
        userNameTV = (TextView) findViewById(R.id.userNameValue);
        genderTV = (TextView) findViewById(R.id.genderValue);
        weightTV = (TextView) findViewById(R.id.weightValue);
        avgDistanceTV = (TextView) findViewById(R.id.avgDistanceValue);
        avgTimeTV = (TextView) findViewById(R.id.avgTimeValue);
        avgWorkoutsTV = (TextView) findViewById(R.id.avgWorkoutsValue);
        avgCaloriesBurnedTV = (TextView) findViewById(R.id.avgCaloriesBurnedValue);
        allTimeDistanceTV = (TextView) findViewById(R.id.allDistanceValue);
        allTimeTimeTV = (TextView) findViewById(R.id.allTimeValue);
        allTimeWorkoutsTV = (TextView) findViewById(R.id.allWorkoutsValue);
        allTimeCaloriesBurnedTV = (TextView) findViewById(R.id.allCaloriesBurnedValue);

        Cursor c = managedQuery(MyContentProvider.CONTENT_URI, null, null, null, UserTable._ID);

        if (c.moveToFirst()) {
            do {
                String name = c.getString(c.getColumnIndex(UserTable.NAME));
                String gender = c.getString(c.getColumnIndex(UserTable.GENDER));
                double weight = c.getDouble(c.getColumnIndex(UserTable.WEIGHT));
                double avgDistance = c.getDouble(c.getColumnIndex(UserTable.AVG_DISTANCE));
                String avgTime = c.getString(c.getColumnIndex(UserTable.AVG_TIME));
                int avgWorkouts = c.getInt(c.getColumnIndex(UserTable.AVG_WORKOUTS));
                double avgCaloriesBurned = c.getDouble(c.getColumnIndex(UserTable.AVG_CALORIES_BURNED));
                double allTimeDistance = c.getDouble(c.getColumnIndex(UserTable.ALL_TIME_DISTANCE));
                String allTimeTime = c.getString(c.getColumnIndex(UserTable.ALL_TIME_TIME));
                int allTimeWorkouts = c.getInt(c.getColumnIndex(UserTable.ALL_TIME_WORKOUTS));
                double allTimeCaloriesBurned = c.getDouble(c.getColumnIndex(UserTable.ALL_TIME_CALORIES_BURNED));

                userNameTV.setText(name);
                genderTV.setText(gender);
                weightTV.setText(String.valueOf(weight));
                avgDistanceTV.setText(String.valueOf(avgDistance));
                avgTimeTV.setText(avgTime);
                avgWorkoutsTV.setText(String.valueOf(avgWorkouts));
                avgCaloriesBurnedTV.setText(String.valueOf(avgCaloriesBurned));
                allTimeDistanceTV.setText(String.valueOf(allTimeDistance));
                allTimeTimeTV.setText(allTimeTime);
                allTimeWorkoutsTV.setText(String.valueOf(allTimeWorkouts));
                allTimeCaloriesBurnedTV.setText(String.valueOf(allTimeCaloriesBurned));


            } while (c.moveToNext());
        }
    }
}
//            "CREATE TABLE " + TABLE_NAME
//                    + " _id INTEGER PRIMARY KEY AUTOINCREMENT, "
//                    + " name TEXT NOT NULL, "
//                    + " gender TEXT NOT NULL, "
//                    + " weight DOUBLE NOT NULL, "
//                    + " avgDistannce DOUBLE, "
//                    + " avgTime TEXT, "
//                    + " avgWorkouts INTEGER, "
//                    + " avgCaloriesBurned DOUBLE, "
//                    + " allTimeDistannce DOUBLE, "
//                    + " allTimeTime TEXT, "
//                    + " allTimeWorkouts INTEGER, "
//                    + " allTimeCaloriesBurned DOUBLE);";