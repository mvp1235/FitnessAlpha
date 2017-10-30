package com.example.mvp.fitnessalpha;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class WorkoutService extends Service implements SensorEventListener {

    MyAIDLInterface.Stub mBinder;

    //SensorManager and Sensors
    private SensorManager mySensors;
    private Sensor stepCounterSensor;

    static int currentStepCount;

    public WorkoutService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        currentStepCount = 0;

        mBinder = new MyAIDLInterface.Stub() {
            public int getStepCount() {
                return currentStepCount;
            }
        };

        //Retrieve an instance of SensorManager to access sensors
        mySensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //Step Counter Sensor
        if (mySensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            stepCounterSensor = mySensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }

        mySensors.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values;
        currentStepCount = (int)values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
