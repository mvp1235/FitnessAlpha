package com.example.mvp.fitnessalpha;

/**
 * List of to do items
 * check if device supports all sensors required
 * unregistering listener event for step counter???
 *
 */

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;



public class MainScreenActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String ACTION_FOO = "com.example.mvp.fitnessalpha.action.FOO";
    private TextView tv;

    //Google Map and location-related
    private GoogleMap mMap;
    private LocationManager locationManager;

    //UI Elements
    private Button workoutButton;

    private boolean workoutStarted = false;
    private MyReceiver receiver;
    private int stepCount;

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (ACTION_FOO.equals(action) && workoutStarted) {
                Log.i("RECEIVED", "yolo " + intent.getIntExtra("count", 0));
                stepCount = intent.getIntExtra("count", 0);
                tv.setText(String.valueOf(stepCount));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        final IntentFilter filter = new IntentFilter(ACTION_FOO);
        receiver = new MyReceiver();


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        tv = (TextView) findViewById(R.id.ex);
        stepCount = 0;

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
        final Intent intentService = new Intent(this, WorkoutSessionService.class);
        workoutButton = (Button) findViewById(R.id.startStopBtn);
        workoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!workoutStarted) {
                    startWorkout(intentService);
                    registerReceiver(receiver, filter);
                } else {
                    stopWorkout(intentService);
                    unregisterReceiver(receiver);
                }
            }
        });
    }


    /**
     *
     */
    public void startWorkout(Intent intent) {
        workoutStarted = true;
        workoutButton.setText(R.string.stopWorkout);
        workoutButton.setBackgroundColor(getResources().getColor(R.color.red_stop_color));

        //Start intent service
        startService(intent);
    }

    /**
     *
     */
    public void stopWorkout(Intent intent) {
        workoutStarted = false;
        workoutButton.setText(R.string.startWorkout);
        workoutButton.setBackgroundColor(getResources().getColor(R.color.green_start_color));

        //Stop intent service
        stopService(intent);
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

        //Setting up location services
        Location location;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String locationProvider = locationManager.getBestProvider(criteria, true);

        //Check for Permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_DENIED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_DENIED) {
            //Get the most recent last known location
            location = locationManager.getLastKnownLocation(locationProvider);

        } else {
            location = null;
        }

        // Add a marker in Sydney and move the camera
        if (location != null) {
            LatLng here = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.addMarker(new MarkerOptions().position(here).title("Current Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(here));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        } else {
            Toast.makeText(getApplicationContext(), "Cannot obtain permission to retrieve the current location...", Toast.LENGTH_SHORT).show();
        }
    }

}
