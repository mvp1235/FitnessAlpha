package com.example.mvp.fitnessalpha;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
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
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


public class MainScreenActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String ACTION_FOO = "com.example.mvp.fitnessalpha.action.FOO";
    public static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 10001;
    public static final int DEFAULT_ZOOM = 15;

    //Google Map and location-related
    private GoogleMap mMap;
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;

    //UI Elements
    private Button workoutButton;

    private boolean workoutStarted = false;
    private int currentStepCount;
    private int sensorStepCount;
    private TextView tv;

    //Duration timer
    Handler handler = new Handler();
    long startTime = 0L, timeInMilliseconds = 0L, elapsedTime = 0L;
    private final int REFRESH_RATE = 1000;
    private boolean restart = false;
    TextView durationTV;

    //Thread management
    private Thread updateUIThread;
    private boolean threatStarted = false;
    private boolean threadRunning = true;
    private Runnable startTimer;

    //Remote Service
    MyAIDLInterface remoteService;
    RemoteConnection remoteConnection = null;

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


        tv = (TextView) findViewById(R.id.ex);
        currentStepCount = 0;
        sensorStepCount = WorkoutService.currentStepCount;

        //Setting up location services
        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);
        // Construct a PlaceDetectionClient.
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this, null);
        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //Initial service
        remoteConnection = new RemoteConnection();
        Intent intent = new Intent();
        intent.setClassName("com.example.mvp.fitnessalpha", com.example.mvp.fitnessalpha.WorkoutService.class.getName());
        if (!bindService(intent, remoteConnection, BIND_AUTO_CREATE)) {
            Toast.makeText(this, "Fail to bind the remote service.", Toast.LENGTH_SHORT).show();
        }

        startTimer = new Runnable() {
            public void run() {
                elapsedTime = System.currentTimeMillis() - startTime;
                updateTimer(elapsedTime);
                handler.postDelayed(this,REFRESH_RATE);
            }
        };

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

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

        //Update UI every 1 second
        updateUIThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (threadRunning) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int count = 0;
                                try {
                                    count = remoteService.getStepCount();

                                } catch (RemoteException e){
                                    e.printStackTrace();
                                }
                                currentStepCount = count - sensorStepCount;
                                tv.setText(String.valueOf(currentStepCount));
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        threadRunning = true;
        if (!threatStarted) {
            updateUIThread.start();
            threatStarted = true;
        }

        //Handling clock UI
        if (!restart) {
            startTime = System.currentTimeMillis() - elapsedTime;
        } else {
            startTime = System.currentTimeMillis();
        }
        handler.removeCallbacks(startTimer);
        handler.postDelayed(startTimer, 0);

    }

    public void stopWorkout() {
        workoutStarted = false;
        workoutButton.setText(R.string.startWorkout);
        workoutButton.setBackgroundColor(getResources().getColor(R.color.green_start_color));

        //Clock UI
        restart = true;
        handler.removeCallbacks(startTimer);
        durationTV.setText("00:00:00");

        //
        threadRunning = false;
        threatStarted = false;
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
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
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


}
