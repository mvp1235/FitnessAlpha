package com.example.mvp.fitnessalpha;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;


import java.lang.reflect.Array;
import java.util.ArrayList;


public class WorkoutDetailActivity extends AppCompatActivity {

    private static final long REFRESH_RATE = 1000;
    static final String CURRENT_AVG_VALUE = "currentAvgValue";
    static final String CURRENT_MIN_VALUE = "currentMinValue";
    static final String CURRENT_MAX_VALUE = "currentMaxValue";
    static final String ITERATIONS = "iterations";
    static final String SUM_TOTAL = "sumTotal";
    private TextView avgValueTV, minValueTV, maxValueTV;

    private Runnable uiUpdate;
    private Handler handler;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    private LineChart mChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_detail);

        sharedPref = getSharedPreferences(MainScreenActivity.MY_PREFERENCE, Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        handler = new Handler();

        MainScreenActivity.currentAvgValue = sharedPref.getFloat(CURRENT_AVG_VALUE, 0);
        MainScreenActivity.currentMinValue = sharedPref.getFloat(CURRENT_MIN_VALUE, 0);
        MainScreenActivity.currentMaxValue = sharedPref.getFloat(CURRENT_MAX_VALUE, 0);

        avgValueTV = (TextView) findViewById(R.id.detailAvgValue);
        minValueTV = (TextView) findViewById(R.id.detailMinValue);
        maxValueTV = (TextView) findViewById(R.id.detailMaxValue);
        avgValueTV.setText("0:00");
        minValueTV.setText("0:00");
        maxValueTV.setText("0:00");

        uiUpdate = new Runnable() {
            public void run() {
                //Only start calculating after user ran for a little bit
                if (MainScreenActivity.totalDistance > 0.01) {

                    double value = calculateAvgValue();
                    if (value < MainScreenActivity.currentMaxValue || MainScreenActivity.currentMaxValue == 0) {
                        MainScreenActivity.currentMaxValue = value;
                        editor.putFloat(CURRENT_MAX_VALUE, (float)value);
                    }
                    if (value > MainScreenActivity.currentMinValue || MainScreenActivity.currentMinValue == 0) {
                        MainScreenActivity.currentMinValue = value;
                        editor.putFloat(CURRENT_MIN_VALUE, (float)value);
                    }
                    MainScreenActivity.currentAvgValue = value; // for now, let avg display current min/mile rate
                    editor.putFloat(CURRENT_AVG_VALUE, (float)value);
                    editor.commit();

                    avgValueTV.setText(getMinuteSeconds(MainScreenActivity.currentAvgValue));
                    minValueTV.setText(getMinuteSeconds(MainScreenActivity.currentMinValue));
                    maxValueTV.setText(getMinuteSeconds(MainScreenActivity.currentMaxValue));
                }
                handler.postDelayed(this, REFRESH_RATE);
            }
        };

        handler.postDelayed(uiUpdate, REFRESH_RATE);

        //Setting up elements for the chart
        mChart = (LineChart) findViewById(R.id.chart);

        // enable description text
        mChart.getDescription().setEnabled(true);
        // enable touch gestures
        mChart.setTouchEnabled(true);
        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);
        // set an alternative background color
        mChart.setBackgroundColor(Color.WHITE);

        


    }


    public double calculateAvgValue() {
        double value = (double) MainScreenActivity.secondsPassed / MainScreenActivity.totalDistance;    // seconds/mile
        value = value / 60; // min/mile

        MainScreenActivity.iterations++;
        MainScreenActivity.sumValue += value;
        editor.putInt(ITERATIONS, MainScreenActivity.iterations);
        editor.putFloat(SUM_TOTAL, (float)MainScreenActivity.sumValue);
        editor.commit();

        double result = 0;
        result = (double) sharedPref.getFloat(SUM_TOTAL, 0) / sharedPref.getInt(ITERATIONS, 1);

        return result;
    }

    public String getMinuteSeconds(double minute) {
        String result = "";
        long fullMinute = (long) Math.floor(minute);

        double remainder =  minute - fullMinute;

        remainder = remainder * 60;
        long fullSecond = (long)Math.floor(remainder);
        String minStr = String.valueOf(fullMinute);
        String secStr = "";
        if (fullSecond < 10) {
            secStr = "0" + fullSecond;
        } else {
            secStr = String.valueOf(fullSecond);
        }
        result = minStr + ":" + secStr;

        return result;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            finish();
        }
    }
}
