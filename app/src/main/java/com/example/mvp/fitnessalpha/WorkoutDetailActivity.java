package com.example.mvp.fitnessalpha;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.XAxis.XAxisPosition;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BubbleData;
import com.github.mikephil.charting.data.BubbleDataSet;
import com.github.mikephil.charting.data.BubbleEntry;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

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

    private CombinedChart mChart;
    private String[] mMonths = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};

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

        mChart = (CombinedChart) findViewById(R.id.chart);
        mChart.getDescription().setEnabled(false);
        mChart.setBackgroundColor(Color.WHITE);
        mChart.setDrawGridBackground(false);
        mChart.setDrawBarShadow(false);
        mChart.setHighlightFullBarEnabled(false);

        mChart.setDrawOrder(new DrawOrder[]{
                DrawOrder.BAR, DrawOrder.BUBBLE, DrawOrder.CANDLE, DrawOrder.LINE, DrawOrder.SCATTER
        });

        Legend l = mChart.getLegend();
        l.setWordWrapEnabled(true);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setDrawGridLines(false);
        rightAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)

        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxisPosition.BOTH_SIDED);
        xAxis.setAxisMinimum(0f);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return mMonths[(int) value % mMonths.length];
            }
        });

        CombinedData data = new CombinedData();
        data.setData(generateLineData());
        data.setData(generateBarData());

        xAxis.setAxisMaximum(data.getXMax() + 0.25f);

        mChart.setData(data);
        mChart.invalidate();

    }

    private LineData generateLineData() {

        LineData d = new LineData();

        ArrayList<Entry> entries = new ArrayList<Entry>();

        for (int index = 0; index < 12; index++)
            entries.add(new Entry(index + 0.5f, getRandom(15, 5)));

        LineDataSet set = new LineDataSet(entries, "Line DataSet");
        set.setColor(Color.rgb(240, 238, 70));
        set.setLineWidth(2.5f);
        set.setCircleColor(Color.rgb(240, 238, 70));
        set.setCircleRadius(5f);
        set.setFillColor(Color.rgb(240, 238, 70));
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(true);
        set.setValueTextSize(10f);
        set.setValueTextColor(Color.rgb(240, 238, 70));

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        d.addDataSet(set);

        return d;
    }

    private BarData generateBarData() {

        ArrayList<BarEntry> entries1 = new ArrayList<BarEntry>();
        ArrayList<BarEntry> entries2 = new ArrayList<BarEntry>();

        for (int index = 0; index < 12; index++) {
            entries1.add(new BarEntry(0, getRandom(25, 25)));

            // stacked
            entries2.add(new BarEntry(0, new float[]{getRandom(13, 12), getRandom(13, 12)}));
        }

        BarDataSet set1 = new BarDataSet(entries1, "Bar 1");
        set1.setColor(Color.rgb(60, 220, 78));
        set1.setValueTextColor(Color.rgb(60, 220, 78));
        set1.setValueTextSize(10f);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);

        BarDataSet set2 = new BarDataSet(entries2, "");
        set2.setStackLabels(new String[]{"Stack 1", "Stack 2"});
        set2.setColors(new int[]{Color.rgb(61, 165, 255), Color.rgb(23, 197, 255)});
        set2.setValueTextColor(Color.rgb(61, 165, 255));
        set2.setValueTextSize(10f);
        set2.setAxisDependency(YAxis.AxisDependency.LEFT);

        float groupSpace = 0.06f;
        float barSpace = 0.02f; // x2 dataset
        float barWidth = 0.45f; // x2 dataset
        // (0.45 + 0.02) * 2 + 0.06 = 1.00 -> interval per "group"

        BarData d = new BarData(set1, set2);
        d.setBarWidth(barWidth);

        // make this BarData object grouped
        d.groupBars(0, groupSpace, barSpace); // start at x = 0

        return d;
    }

    private float getRandom(int i, int i1) {
        return i * i1;
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
