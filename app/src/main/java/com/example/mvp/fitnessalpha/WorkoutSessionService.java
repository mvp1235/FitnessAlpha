package com.example.mvp.fitnessalpha;

import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.Nullable;
import android.util.Log;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class WorkoutSessionService extends IntentService implements SensorEventListener {
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    public static final String ACTION_FOO = "com.example.mvp.fitnessalpha.action.FOO";

    //SensorManager and Sensors
    private SensorManager mySensors;
    private Sensor stepCounterSensor;

    private int initialCount;
    private int currentStepCount;
    private boolean firstRun = true;

    public WorkoutSessionService() {
        super("WorkoutSessionService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        currentStepCount = 0;
        initialCount = 0;

        //Retrieve an instance of SensorManager to access sensors
        mySensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //Step Counter Sensor
        if (mySensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            stepCounterSensor = mySensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        mySensors.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FOO.equals(action)) {
                handleActionFoo();
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo() {

        Intent intent = new Intent();
        intent.setAction(MainScreenActivity.ACTION_FOO);
        intent.putExtra("count", currentStepCount);
        sendBroadcast(intent);

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] values = sensorEvent.values;

        if (firstRun) {
            firstRun = false;
            initialCount = (int)values[0];
            currentStepCount = 0;
        } else {
            int added = (int)values[0] - initialCount;
            currentStepCount += added;
        }

        handleActionFoo();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
