package com.example.mvp.fitnessalpha;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class UserProfileActivity extends AppCompatActivity {

    private static final long REFRESH_RATE = 1000;
    private static final int EDIT_USER_INFO_CODE = 1001;
    private TextView userNameTV, genderTV, weightTV;
    private TextView avgDistanceTV, avgTimeTV, avgWorkoutsTV, avgCaloriesBurnedTV;
    private TextView allTimeDistanceTV, allTimeTimeTV, allTimeWorkoutsTV, allTimeCaloriesBurnedTV;
    private ImageView profileIcon;

    private Runnable uiUpdate;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        handler = new Handler();

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
        profileIcon = (ImageView) findViewById(R.id.userProfileImgView);

        profileIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editUserInfo();
            }
        });

        initializeViews();
        updateViews();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_USER_INFO_CODE) {
            if (resultCode == RESULT_OK) {
                userNameTV.setText(data.getStringExtra(UserTable.NAME));
                genderTV.setText(data.getStringExtra(UserTable.GENDER));
                weightTV.setText(data.getStringExtra(UserTable.WEIGHT));
            }
        }
    }

    private void editUserInfo() {
        Intent intent = new Intent(this, EditUserInfoActivity.class);
        intent.putExtra(UserTable.NAME, userNameTV.getText().toString());
        intent.putExtra(UserTable.GENDER, genderTV.getText().toString());
        intent.putExtra(UserTable.WEIGHT, weightTV.getText().toString());

        startActivityForResult(intent, EDIT_USER_INFO_CODE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(uiUpdate);
    }

    public void initializeViews() {
        Cursor c = getContentResolver().query(MyContentProvider.CONTENT_URI, null, "_id = ?", new String[]{"1"}, UserTable._ID);

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

                avgDistanceTV.setText(String.format("%.1f", avgDistance) + " miles");
                avgTimeTV.setText(createTimeFormat(Long.parseLong(avgTime)));
                avgWorkoutsTV.setText(String.valueOf(avgWorkouts) + " times");
                avgCaloriesBurnedTV.setText(String.format("%.1f", avgCaloriesBurned) + " Cal");

                allTimeDistanceTV.setText(String.format("%.1f", allTimeDistance) + " miles");
                allTimeTimeTV.setText(createTimeFormat(Long.parseLong(allTimeTime)));
                allTimeWorkoutsTV.setText(String.valueOf(allTimeWorkouts) + " times");
                allTimeCaloriesBurnedTV.setText(String.format("%.1f", allTimeCaloriesBurned) + " Cal");
                break;

            } while (c.moveToNext());
        }
    }

    public void updateViews() {
        uiUpdate = new Runnable() {
            public void run() {
                Cursor c = getContentResolver().query(MyContentProvider.CONTENT_URI, null, "_id = ?", new String[]{"1"}, UserTable._ID);

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

                        avgDistanceTV.setText(String.format("%.1f", avgDistance) + " miles");
                        avgTimeTV.setText(createTimeFormat(Long.parseLong(avgTime)));
                        avgWorkoutsTV.setText(String.valueOf(avgWorkouts) + " times");
                        avgCaloriesBurnedTV.setText(String.format("%.1f", avgCaloriesBurned) + " Cal");

                        allTimeDistanceTV.setText(String.format("%.1f", allTimeDistance) + " miles");
                        allTimeTimeTV.setText(createTimeFormat(Long.parseLong(allTimeTime)));
                        allTimeWorkoutsTV.setText(String.valueOf(allTimeWorkouts) + " times");
                        allTimeCaloriesBurnedTV.setText(String.format("%.1f", allTimeCaloriesBurned) + " Cal");
                        break;

                    } while (c.moveToNext());
                }
                handler.postDelayed(this, REFRESH_RATE);
            }
        };

        handler.postDelayed(uiUpdate, REFRESH_RATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!MainScreenActivity.workoutStarted) {
            handler.removeCallbacks(uiUpdate);
        }
    }

    public String createTimeFormat(Long seconds) {
        String result = "";
        long days = seconds / (60 * 60 * 24);
        seconds -= days * (60 * 60 * 24);
        long hours = seconds / (60 * 60);
        seconds -= hours * (60 * 60);
        long minutes = seconds / 60;
        seconds -= minutes * 60;

        if (days > 0) {
            result += days + " day ";
        }
        if (hours > 0) {
            result += hours + " hr ";
        }
        if (minutes > 0) {
            result += minutes + " min ";
        }
        result += seconds + " sec";
        return result;
    }
}
