package com.example.mvp.fitnessalpha;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.data.Entry;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;


public class MainScreenActivity extends FragmentActivity implements OnMapReadyCallback {

    static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 10001;
    static final int DEFAULT_ZOOM = 17;
    static final String MY_PREFERENCE = "MyPrefs";
    static final String SENSOR_STEP_COUNT = "sensorStepCount";
    static final double INCHES_PER_STEP_MEN = 30;
    static final double INCHES_PER_STEP_WOMEN = 26;
    static final double MILE_PER_INCH = 0.0000157828;

    //Calories Burned Assuming 2200 steps per mile
    //Use for calculating amount of calories burned
    static final double REF_WEIGHT_IN_LBS = 200;
    static final double REF_CALORIES_BURNED_1000_STEPS = 50;
    static final double REF_STEPS = 1000;

    //For workout detail screen
    static double currentAvgValue = 0, currentMinValue = 0, currentMaxValue = 0;
    static int secondsPassed = 0;
    static int iterations = 0;
    static double sumValue = 0;

    //Google Map and location-related
    private GoogleMap mMap;
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;
    private PolylineOptions polylineOptions;
    private ArrayList<LatLng> path;
    private Marker current;
    private Polyline line;
    private boolean firstMarker = true;

    //UI Elements
    private Button workoutButton;

    static boolean workoutStarted = false;
    private int currentStepCount;
    private int sensorStepCount;

    //Duration timer
    Handler handler = new Handler();
    long startTime = 0L, elapsedTime = 0L;
    private final int REFRESH_RATE = 1000;
    private final int MAP_REFRESH_RATE = 10000;
    private final int CALORIES_REFRESH_RATE = 5000;
    private final int STEPS_REFRESH_RATE = 5000;
    private boolean restart = false;
    private TextView durationTV;

    //Distance
    static double totalDistance;
    private TextView distanceTV;

    //Variables for updating database
    private double initialAllTimeDistance = 0;
    private double initialAllTimeCaloriesBurned = 0;

    //Thread management
    private Runnable startTimer;
    private Runnable databaseUpdate;
    private Runnable addCaloriesPer1Min;
    private Runnable addStepsCountPer5Min;
    private Runnable mapUpdate;

    //Remote Service
    MyAIDLInterface remoteService;
    RemoteConnection remoteConnection = null;

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    //For detail workout screen
    static ArrayList<Entry> caloriesEntries;
    static ArrayList<Entry> stepsEntries;

    class RemoteConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            remoteService = MyAIDLInterface.Stub.asInterface((IBinder) service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remoteService = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        caloriesEntries = new ArrayList<>();
        stepsEntries = new ArrayList<>();

        sharedPref = getSharedPreferences(MY_PREFERENCE, Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        //If running the app for the very first time, a default user profile will be created
        if (sharedPref.getBoolean("FIRST_RUN", true)) {
            populateDefaultDatabase();
            editor.putBoolean("FIRST_RUN", false);
            editor.commit();
        }

        sensorStepCount = sharedPref.getInt(SENSOR_STEP_COUNT, 0);

        currentStepCount = 0;
        totalDistance = 0;

        //Initial service
        remoteConnection = new RemoteConnection();
        Intent intent = new Intent();
        intent.setClassName("com.example.mvp.fitnessalpha", com.example.mvp.fitnessalpha.WorkoutService.class.getName());
        if (!bindService(intent, remoteConnection, BIND_AUTO_CREATE)) {
            Toast.makeText(this, "Fail to bind the remote service.", Toast.LENGTH_SHORT).show();
        }

        addCaloriesPer1Min = new Runnable() {
            @Override
            public void run() {
                double weight = Double.parseDouble(getDatabaseColumnValue(UserTable.WEIGHT));
                double calories = calculateCaloriesBurned(weight, currentStepCount);
                Entry e = new Entry(caloriesEntries.size()*5f, (float)calories);
                if (caloriesEntries.size() == 0) {
                    e = new Entry(0f, 0);   //add starting plot
                }
                caloriesEntries.add(e);
                handler.postDelayed(this, CALORIES_REFRESH_RATE);
            }
        };

        addStepsCountPer5Min = new Runnable() {
            @Override
            public void run() {
                Entry e = new Entry(stepsEntries.size()*5f, currentStepCount);
                if (stepsEntries.size() == 0) {
                    e = new Entry(0f,0);    //add starting plot
                }
                stepsEntries.add(e);
                handler.postDelayed(this, CALORIES_REFRESH_RATE);
            }
        };

        startTimer = new Runnable() {
            public void run() {
                secondsPassed++;
                elapsedTime = System.currentTimeMillis() - startTime;
                updateTimer(elapsedTime);

                handler.postDelayed(this, REFRESH_RATE);
            }
        };

        mapUpdate = new Runnable() {
            public void run() {
                getDeviceLocation();
                handler.postDelayed(this, MAP_REFRESH_RATE);
            }
        };


        databaseUpdate = new Runnable() {
            @Override
            public void run() {
                int count = 0;
                try {
                    count = remoteService.getStepCount();
                } catch (RemoteException e){
                    e.printStackTrace();
                }

                currentStepCount = count - sensorStepCount;

                //Updating Distance UI
                //Check whether user is male or female and use appropriate assumed average stride length
                if (getDatabaseColumnValue(UserTable.GENDER).equalsIgnoreCase("Male")) {
                    totalDistance = currentStepCount * INCHES_PER_STEP_MEN; // in inches
                } else {
                    totalDistance = currentStepCount * INCHES_PER_STEP_WOMEN; // in inches
                }

                totalDistance *= MILE_PER_INCH;
                distanceTV.setText(String.format("%.1f", totalDistance));

                double currentAllTimeDistance = Double.parseDouble(getDatabaseColumnValue(UserTable.ALL_TIME_DISTANCE));
                double distanceToBeAdded = totalDistance - initialAllTimeDistance;
                initialAllTimeDistance = totalDistance;


                //Updating Calories UI
                double currentAllTimeCaloriesBurned = Double.parseDouble(getDatabaseColumnValue(UserTable.ALL_TIME_CALORIES_BURNED));
                double weight = Double.parseDouble(getDatabaseColumnValue(UserTable.WEIGHT));
                double calories = calculateCaloriesBurned(weight, currentStepCount);
                double caloriesToBeAdded = calories - initialAllTimeCaloriesBurned;
                initialAllTimeCaloriesBurned = calories;

                //Updating time in database
                Long currentAllTimeTime = Long.parseLong(getDatabaseColumnValue(UserTable.ALL_TIME_TIME));
                currentAllTimeTime++;

                ContentValues contentValues = new ContentValues();
                contentValues.put(UserTable.ALL_TIME_DISTANCE, currentAllTimeDistance + distanceToBeAdded);
                contentValues.put(UserTable.ALL_TIME_CALORIES_BURNED, currentAllTimeCaloriesBurned + caloriesToBeAdded);
                contentValues.put(UserTable.ALL_TIME_TIME, String.valueOf(currentAllTimeTime));
                getContentResolver().update(MyContentProvider.CONTENT_URI, contentValues, "_id = ?", new String[] {"1"});

                //Update the value of step count returned by sensor
                editor.putInt(SENSOR_STEP_COUNT, count);
                editor.commit();

                handler.postDelayed(this, REFRESH_RATE);
            }
        };

        //Setting up location services
        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);
        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);
        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        polylineOptions = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        path = new ArrayList<>();

        //Setting up user profile button
        ImageButton profileBtn = (ImageButton) findViewById(R.id.userProfileBtn);
        profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainScreenActivity.this, UserProfileActivity.class);
                startActivity(intent);
            }
        });

        //Setting up start workout button
        workoutButton = (Button) findViewById(R.id.startStopBtn);
        workoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (workoutStarted) {
                    stopWorkout();
                } else {
                    startWorkout();
                }
            }
        });

        durationTV = (TextView) findViewById(R.id.recordDurationValue);
        distanceTV = (TextView) findViewById(R.id.recordDistanceValue);


    }

    @Override
    public void onBackPressed() {
        //do nothing, preventing users from exiting the activity with back button and stop the workout session
        //pressing home will not affect
    }

    /**
     * Return value of a certain column in the user table
     * @param columnName the column to be return
     * @return the value of the specified column name
     */
    public String getDatabaseColumnValue(String columnName) {
        Cursor c = getContentResolver().query(MyContentProvider.CONTENT_URI, null, "_id = ?", new String[] {"1"}, UserTable._ID);

        if (c.moveToFirst()) {
            do {
                if (columnName.equalsIgnoreCase(UserTable.NAME)) {
                    String name = c.getString(c.getColumnIndex(UserTable.NAME));
                    return name;
                } else if (columnName.equalsIgnoreCase(UserTable.GENDER)) {
                    String gender = c.getString(c.getColumnIndex(UserTable.GENDER));
                    return gender;
                } else if (columnName.equalsIgnoreCase(UserTable.WEIGHT)) {
                    double weight = c.getDouble(c.getColumnIndex(UserTable.WEIGHT));
                    return String.valueOf(weight);
                } else if (columnName.equalsIgnoreCase(UserTable.AVG_DISTANCE)) {
                    double avgDistance = c.getDouble(c.getColumnIndex(UserTable.AVG_DISTANCE));
                    return String.valueOf(avgDistance);
                } else if (columnName.equalsIgnoreCase(UserTable.AVG_TIME)) {
                    String avgTime = c.getString(c.getColumnIndex(UserTable.AVG_TIME));
                    return avgTime;
                } else if (columnName.equalsIgnoreCase(UserTable.AVG_WORKOUTS)) {
                    int avgWorkouts = c.getInt(c.getColumnIndex(UserTable.AVG_WORKOUTS));
                    return String.valueOf(avgWorkouts);
                } else if (columnName.equalsIgnoreCase(UserTable.AVG_CALORIES_BURNED)) {
                    double avgCaloriesBurned = c.getDouble(c.getColumnIndex(UserTable.AVG_CALORIES_BURNED));
                    return String.valueOf(avgCaloriesBurned);
                } else if (columnName.equalsIgnoreCase(UserTable.ALL_TIME_DISTANCE)) {
                    double allTimeDistance = c.getDouble(c.getColumnIndex(UserTable.ALL_TIME_DISTANCE));
                    return String.valueOf(allTimeDistance);
                } else if (columnName.equalsIgnoreCase(UserTable.ALL_TIME_TIME)) {
                    String allTimeTime = c.getString(c.getColumnIndex(UserTable.ALL_TIME_TIME));
                    return allTimeTime;
                } else if (columnName.equalsIgnoreCase(UserTable.ALL_TIME_WORKOUTS)) {
                    int allTimeWorkouts = c.getInt(c.getColumnIndex(UserTable.ALL_TIME_WORKOUTS));
                    return String.valueOf(allTimeWorkouts);
                } else if (columnName.equalsIgnoreCase(UserTable.ALL_TIME_CALORIES_BURNED)) {
                    double allTimeCaloriesBurned = c.getDouble(c.getColumnIndex(UserTable.ALL_TIME_CALORIES_BURNED));
                    return String.valueOf(allTimeCaloriesBurned);
                }

            } while (c.moveToNext());
        }
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Intent intent = new Intent(MainScreenActivity.this, WorkoutDetailActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(remoteConnection);
        remoteConnection = null;
    }

    public void startWorkout() {
        workoutStarted = true;
        workoutButton.setText(R.string.stopWorkout);
        workoutButton.setBackgroundColor(getResources().getColor(R.color.red_stop_color));

        sensorStepCount = sharedPref.getInt(SENSOR_STEP_COUNT, 0);

        //Handling clock UI
        if (!restart) {
            startTime = System.currentTimeMillis() - elapsedTime;
        } else {
            startTime = System.currentTimeMillis();
        }
        handler.removeCallbacks(startTimer);
        handler.postDelayed(startTimer, REFRESH_RATE);

        //Handling distance UI
        distanceTV.setText("0.0");
        handler.removeCallbacks(databaseUpdate);
        handler.postDelayed(databaseUpdate, REFRESH_RATE);
        currentStepCount = 0;
        totalDistance = 0;
        secondsPassed = 0;

        //Start workout detail threads
        handler.removeCallbacks(addCaloriesPer1Min);
        handler.removeCallbacks(addStepsCountPer5Min);
        handler.postDelayed(addStepsCountPer5Min, CALORIES_REFRESH_RATE);
        handler.postDelayed(addCaloriesPer1Min, CALORIES_REFRESH_RATE);

        //Map
        handler.removeCallbacks(mapUpdate);
        handler.postDelayed(mapUpdate, MAP_REFRESH_RATE);

        //Update all time workouts number
        int currentAllTimeWorkouts = Integer.parseInt(getDatabaseColumnValue(UserTable.ALL_TIME_WORKOUTS));
        currentAllTimeWorkouts += 1;
        ContentValues contentValues = new ContentValues();
        contentValues.put(UserTable.ALL_TIME_WORKOUTS, currentAllTimeWorkouts);
        getContentResolver().update(MyContentProvider.CONTENT_URI, contentValues, "_id = ?", new String[] {"1"});
    }

    public void stopWorkout() {
        secondsPassed = 0;
        workoutStarted = false;
        workoutButton.setText(R.string.startWorkout);
        workoutButton.setBackgroundColor(getResources().getColor(R.color.green_start_color));

        //Clock UI
        restart = true;
        handler.removeCallbacks(startTimer);
        durationTV.setText("00:00:00");

        iterations = 0;
        sumValue = 0;

        //Reset avg, min, and max in detail screen everytime a workout is finished
        editor.putFloat(WorkoutDetailActivity.CURRENT_AVG_VALUE, 0);
        editor.putFloat(WorkoutDetailActivity.CURRENT_MIN_VALUE, 0);
        editor.putFloat(WorkoutDetailActivity.CURRENT_MAX_VALUE, 0);
        editor.putInt(WorkoutDetailActivity.ITERATIONS, 0);
        editor.putFloat(WorkoutDetailActivity.SUM_TOTAL, 0);
        editor.commit();

        //Distance UI
        handler.removeCallbacks(databaseUpdate);

        //Detail workout UI
        handler.removeCallbacks(addCaloriesPer1Min);
        handler.removeCallbacks(addStepsCountPer5Min);

        //Map
        handler.removeCallbacks(mapUpdate);

        caloriesEntries = new ArrayList<>();
        stepsEntries = new ArrayList<>();
        path = new ArrayList<>();

    }


    public double calculateCaloriesBurned(double weight, int stepCount) {
        double result = 0;
        double caloriesBurnedFor1000Steps = (weight * REF_CALORIES_BURNED_1000_STEPS) / REF_WEIGHT_IN_LBS;

        result = (stepCount * caloriesBurnedFor1000Steps) / REF_STEPS;
        return result;
    }

    private void updateTimer(long time) {
        String secondStr, minuteStr, hourStr;
        long hours = ((time/1000)/60)/60;
        long mins = (time/1000)/60;
        long secs = time/1000;

        secs = secs % 60;
        secondStr = String.valueOf(secs);
        if(secs < 10){
            secondStr = "0" + secondStr;
        }

        mins = mins % 60;
        minuteStr = String.valueOf(mins);
        if (mins < 10) {
            minuteStr = "0" + minuteStr;
        }

        hourStr = String.valueOf(hours);
        if (hours < 10) {
            hourStr = "0" + hourStr;
        }

        durationTV.setText("" + hourStr + ":" + minuteStr + ":" + secondStr);
    }

    private void getLocationPermission() {
    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }


    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getDeviceLocation() {
    /*
     * Get the best and most recent location of the device, which may be null in rare
     * cases when a location is not available.
     */
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = (Location) task.getResult();
                            if (mLastKnownLocation != null) {
                                LatLng currentLocation = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());
                                polylineOptions.add(currentLocation);

                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(mLastKnownLocation.getLatitude(),
                                                mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                                if (firstMarker) {
                                    mMap.addMarker(new MarkerOptions()
                                            .position(currentLocation)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                                    firstMarker = false;

                                } else {
                                    if (current != null)
                                        current.remove();
                                    current =  mMap.addMarker(new MarkerOptions()
                                            .position(currentLocation)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
                                }
                                for (int i=0; i<path.size(); i++) {
                                    polylineOptions.add(path.get(i));
                                }
                                if (line != null)
                                    line.remove();
                                line = mMap.addPolyline(polylineOptions);
                            } else {
                                Toast.makeText(getApplicationContext(), "Cannot get current location. Please turn on GPS or allow permission request.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d("TAG", "Current location is null. Using defaults.");
                            Log.e("TAG", "Exception: %s", task.getException());
                            LatLng mDefaultLocation = new LatLng(37, -121); //Default is San Jose
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch(SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    /**
     * Insert a new user database, which initialize every values to 0, and a default name and weight
     */
    public void populateDefaultDatabase() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(UserTable.NAME, "John Doe");
        contentValues.put(UserTable.GENDER, "Male");
        contentValues.put(UserTable.WEIGHT, 140);
        contentValues.put(UserTable.AVG_DISTANCE, 0);
        contentValues.put(UserTable.AVG_TIME, "0");
        contentValues.put(UserTable.AVG_WORKOUTS, 0);
        contentValues.put(UserTable.AVG_CALORIES_BURNED, 0);
        contentValues.put(UserTable.ALL_TIME_DISTANCE, 0);
        contentValues.put(UserTable.ALL_TIME_TIME, "0");
        contentValues.put(UserTable.ALL_TIME_WORKOUTS, 0);
        contentValues.put(UserTable.ALL_TIME_CALORIES_BURNED, 0);

        getContentResolver().insert(MyContentProvider.CONTENT_URI, contentValues);
    }


}

