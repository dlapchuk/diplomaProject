package info.androidhive.firebase;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import javax.xml.datatype.Duration;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    Polyline line;
    Date startDate;
    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    LatLng mLastLocation;
    Marker mCurrLocationMarker;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    Location prev = new Location("");
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    DatabaseReference goals = mDatabase.child("goals");
    float totalDistance = 0;
    boolean isFirstLocation = true;
    String loggingTime;
    final List<LatLng> coordList = new ArrayList<>();
    Map mGoals;
    LinkedList<LatLng> mLocations;
    DatabaseReference roadDB = mDatabase.child("roads");
    DatabaseReference markDB = mDatabase.child("marks");
    LinkedList<LinkedList> roads= new LinkedList<>();
    private ChildEventListener mChildEventListener;
    private ChildEventListener locChildEventListener;
    private ChildEventListener mDangerEventListener, mShopEventListener, mStationEventListener;
    private LinkedList <LatLng> dangerous, shops, stations;
    private FirebaseAnalytics firebaseAnalytics;
    private Switch trafficSwitch, mapTypeSwitch;
    Button startButton, stopButton;
    TextView stopwatch;
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L;
    Handler handler;
    int Seconds, Minutes, MilliSeconds;

    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        getNames();
        dangerous = new LinkedList<>();
        shops = new LinkedList<>();
        stations = new LinkedList<>();
        getDangerMarks();
        getShopMarks();
        getStationMarks();
        super.onCreate(savedInstanceState);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        setContentView(R.layout.activity_maps);
        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
        startButton=(Button)findViewById(R.id.button);
        stopButton = (Button)findViewById(R.id.stopButton);
        stopwatch = (TextView) findViewById(R.id.stopwatch);
        stopwatch.setText("00:00:00");
        trafficSwitch = (Switch) findViewById(R.id.trafficSwitch);
        mapTypeSwitch = (Switch) findViewById(R.id.mapTypeSwitch);
        setSwitchListener();
        stopButton.setVisibility(View.INVISIBLE);
        handler = new Handler();
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    public void setSwitchListener(){
        trafficSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) //Line A
            {
                if(isChecked){
                    mGoogleMap.setTrafficEnabled(true);
                }
                else{
                    mGoogleMap.setTrafficEnabled(false);
                }
            }
        });
        mapTypeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) //Line A
            {
                if(isChecked){
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                }
                else{
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        //stop location updates when Activity is no longer active
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        //Initialize Google Play Services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                buildGoogleApiClient();
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(true);
        }

        for(int i = 0; i < roads.size(); i++){
            PolylineOptions options = new PolylineOptions().width(20).color(Color.BLUE).geodesic(true);
            LinkedList<com.google.android.gms.maps.model.LatLng> road = roads.get(i);
            for (LatLng point: road) {
                 options.add(point);
            }
            mGoogleMap.addPolyline(options); //add Polyline
        }

        LatLng latLng = new LatLng(50.4501, 30.5234);
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(10));
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {

        float[] results = new float[1];
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        for(int i = 0; i < dangerous.size(); i++){
            mGoogleMap.addMarker(new MarkerOptions().position(dangerous.get(i))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.danger)));
        }
        for(int i = 0; i < shops.size(); i++){
            mGoogleMap.addMarker(new MarkerOptions().position(shops.get(i))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.shop)));
        }
        for(int i = 0; i < stations.size(); i++){
            mGoogleMap.addMarker(new MarkerOptions().position(stations.get(i))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.velostation)));
        }

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mLastLocation = latLng;
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);
        saveCoordinate();
        if (isFirstLocation) {
            startDate = new Date();
            isFirstLocation = false;

        } else {
            redrawLine();
            Location temp = new Location("");
            temp.setLatitude(latLng.latitude);
            temp.setLongitude(latLng.longitude);
            totalDistance += temp.distanceTo(prev);
        }
        prev.setLatitude(latLng.latitude);
        prev.setLongitude(latLng.longitude);
        System.out.println("\n\n\n" + latLng.latitude);
        System.out.println("\n\n\n" + latLng.longitude);
        //move map camera
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(18));

    }

    void getNames()
    {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String key = dataSnapshot.getKey();
                    getLocations(key);
                }
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            roadDB.addChildEventListener(mChildEventListener);
        }
    }

    void getDangerMarks()
    {
        if (mDangerEventListener == null) {
            mDangerEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    info.androidhive.firebase.LatLng loc = dataSnapshot.getValue(info.androidhive.firebase.LatLng.class);

                    com.google.android.gms.maps.model.LatLng latLng =
                            new com.google.android.gms.maps.model.LatLng(loc.getLatitude(), loc.getLongitude());
                    dangerous.add(latLng);
                }
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            markDB.child("danger").addChildEventListener(mDangerEventListener);
        }
    }

    void getShopMarks()
    {
        if (mShopEventListener == null) {
            mShopEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    info.androidhive.firebase.LatLng loc = dataSnapshot.getValue(info.androidhive.firebase.LatLng.class);

                    com.google.android.gms.maps.model.LatLng latLng =
                            new com.google.android.gms.maps.model.LatLng(loc.getLatitude(), loc.getLongitude());
                    shops.add(latLng);
                }
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            markDB.child("shop").addChildEventListener(mShopEventListener);
        }
    }

    void getStationMarks()
    {
        if (mStationEventListener == null) {
            mStationEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    info.androidhive.firebase.LatLng loc = dataSnapshot.getValue(info.androidhive.firebase.LatLng.class);

                    com.google.android.gms.maps.model.LatLng latLng =
                            new com.google.android.gms.maps.model.LatLng(loc.getLatitude(), loc.getLongitude());
                    stations.add(latLng);
                }
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            markDB.child("station").addChildEventListener(mStationEventListener);
        }
    }

    LinkedList<com.google.android.gms.maps.model.LatLng> getLocations(String key){
        final DatabaseReference locations = mDatabase.child("roads").child(key).child("locations");
        final LinkedList<com.google.android.gms.maps.model.LatLng> locationsList = new LinkedList<>();

        locChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                info.androidhive.firebase.LatLng loc = dataSnapshot.getValue(info.androidhive.firebase.LatLng.class);

                com.google.android.gms.maps.model.LatLng latLng =
                        new com.google.android.gms.maps.model.LatLng(loc.getLatitude(), loc.getLongitude());
                locationsList.add(latLng);
                int size = locationsList.size();
                if(size > 1){
                    Polyline line = mGoogleMap.addPolyline(new PolylineOptions()
                            .add(locationsList.get(size -2), locationsList.get(size-1))
                            .width(8)
                            .color(Color.BLUE));
                }
            }

            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            public void onCancelled(DatabaseError databaseError) {}
        };

        locations.addChildEventListener(locChildEventListener);
        return locationsList;
    }

    private void redrawLine() {
        //mGoogleMap.clear();  //clears all Markers and Polylines
        PolylineOptions options = new PolylineOptions().width(5).color(Color.RED).geodesic(true);
        for (int i = 0; i < mLocations.size(); i++) {
            double latitude = mLocations.get(i).latitude;
            double longitude = mLocations.get(i).longitude;
            LatLng point = new LatLng(latitude, longitude);
            options.add(point);
        }

        line = mGoogleMap.addPolyline(options); //add Polyline
    }

    private void saveToFirebase() {
        boolean isFirst = true;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = "anonymous";
        if (user != null) {
            name = user.getUid();
        }
        Date endDate = new Date();

        long diff = (endDate.getTime() - startDate.getTime()) / 1000;
        Goal goal = new Goal(totalDistance / diff, totalDistance, diff, startDate, endDate, name, mLocations);

        String key = goals.push().getKey();
        goals.child(key).setValue(goal);
        DatabaseReference locations = mDatabase.child("goals").child(key).child("locations");

        for (LatLng pos : mLocations) {
            locations.push().setValue(pos);
        }
        startButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.INVISIBLE);
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        Bundle bundle = new Bundle();
        switch (day) {
            case Calendar.SATURDAY:
            case Calendar.SUNDAY:
                bundle.putString("day", "weekend");
                break;
            default:
                bundle.putString("day", "workday");
                break;
        }

        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, key);
        bundle.putFloat("distance", totalDistance);
        bundle.putInt("time", (int)diff);
        Toast.makeText(MapsActivity.this,"add road event", Toast.LENGTH_SHORT).show();
        //Logs an app event.
        firebaseAnalytics.logEvent("add_road", bundle);
    }

    private void saveCoordinate() {
        mLocations.add(mLastLocation);
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
    }

    public void startTracking(View v) {
        mGoals = new HashMap();
        mLocations = new LinkedList();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        startButton.setVisibility(View.INVISIBLE);
        stopButton.setVisibility(View.VISIBLE);
        StartTime = SystemClock.uptimeMillis();
        handler.postDelayed(runnable, 0);
    }

    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            stopwatch.setText("" + Minutes + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds));

            handler.postDelayed(this, 0);
        }

    };

    public void stopTracking(View v) {
        LocationServices.FusedLocationApi.removeLocationUpdates(

                mGoogleApiClient, this);
        saveToFirebase();
        Intent intent = new Intent(MapsActivity.this, ShowGoalsListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        MillisecondTime = 0L;
        StartTime = 0L;
        TimeBuff = 0L;
        UpdateTime = 0L;
        Seconds = 0;
        Minutes = 0;
        MilliSeconds = 0;
        stopwatch.setText("00:00:00");
        startActivity(intent);
        finish();
    }

    public void markLocation(View v){
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MapsActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.dialog_spinner, null);
        mBuilder.setTitle("Spinner in custom dialog");
        final Spinner mSpinner = (Spinner) mView.findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MapsActivity.this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.markList));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        mSpinner.setAdapter(adapter);
        mBuilder.setPositiveButton("Ok",

                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int i) {
                        Toast.makeText(MapsActivity.this,
                                mSpinner.getSelectedItem().toString(),
                                Toast.LENGTH_SHORT)
                                .show();
                        switch (mSpinner.getSelectedItem().toString()){
                            case "danger":
                                mGoogleMap.addMarker(new MarkerOptions().position(mLastLocation)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.danger)));
                                saveMarkToDB("danger");
                                break;
                            case "shop":
                                mGoogleMap.addMarker(new MarkerOptions().position(mLastLocation)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.shop)));
                                saveMarkToDB("shop");
                                break;
                            case "station":
                                mGoogleMap.addMarker(new MarkerOptions().position(mLastLocation)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.velostation)));
                                saveMarkToDB("station");
                                break;

                        }

                        dialog.dismiss();
                    }
                });
        mBuilder.setNegativeButton("Dismiss",

                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });
        mBuilder.setView(mView);
        AlertDialog dialog = mBuilder.create();
        dialog.show();
    }

    public void saveMarkToDB(String type){
        String key = goals.push().getKey();
        markDB.child(type).child(key).setValue(mLastLocation);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mGoogleMap.setMyLocationEnabled(true);
                    }
                } else {

                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }

            }
        }
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Maps Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }
}
