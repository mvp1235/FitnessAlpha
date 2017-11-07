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


public class WorkoutDetailActivity extends AppCompatActivity {

    private static final long REFRESH_RATE = 2000;
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
                updateChartUI();
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

        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);

        // add empty data
        mChart.setData(data);

        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLACK);

        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.BLACK);
        xl.setDrawGridLines(false);
        xl.setAxisMinimum(0f);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);
        xl.setDrawLabels(true);
        xl.setGranularity(1f);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!MainScreenActivity.workoutStarted) {
            handler.removeCallbacks(uiUpdate);
        }
    }

    private void updateChartUI() {
        LineData data = mChart.getData();

        if (data != null) {

            ILineDataSet caloriesSet = data.getDataSetByIndex(0);
            ILineDataSet stepsSet = data.getDataSetByIndex(1);
            // set.addEntry(...); // can be called as well

            if (caloriesSet == null || stepsSet == null) {
                caloriesSet = createCaloriesSet();
                stepsSet = createStepsSet();
                data.addDataSet(caloriesSet);
                data.addDataSet(stepsSet);

                Entry e = new Entry(0f, 0);
                data.addEntry(e, 0);
                data.addEntry(e, 1);
            }

            //Add new data entries to Calories Set
            int currentCaloriesSetSize = caloriesSet.getEntryCount();
            for (int i=currentCaloriesSetSize; i<MainScreenActivity.caloriesEntries.size(); i++) {
                Entry e = MainScreenActivity.caloriesEntries.get(i);
                data.addEntry(e, 0);
            }

            //Add new data entries to Steps Set
            int currentStepCountSetSize = stepsSet.getEntryCount();
            for (int i=currentStepCountSetSize; i<MainScreenActivity.stepsEntries.size(); i++) {
                Entry e = MainScreenActivity.stepsEntries.get(i);
                data.addEntry(e, 1);
            }


            data.notifyDataChanged();

            // let the chart know it's data has changed
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(10);
            // mChart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            if (data.getEntryCount() >= 5)
                mChart.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // mChart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private LineDataSet createCaloriesSet() {

        LineDataSet set = new LineDataSet(null, "Calories Burned");

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.BLUE);
        set.setCircleColor(Color.BLUE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(Color.BLUE);
        set.setHighLightColor(Color.rgb(117, 117, 117));
        set.setValueTextColor(Color.BLUE);
        set.setValueTextSize(9f);
        return set;
    }

    private LineDataSet createStepsSet() {
        LineDataSet set = new LineDataSet(null, "Step Counts");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.RED);
        set.setCircleColor(Color.RED);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setFillAlpha(65);
        set.setFillColor(Color.RED);
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.RED);
        set.setValueTextSize(9f);
        return set;
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
