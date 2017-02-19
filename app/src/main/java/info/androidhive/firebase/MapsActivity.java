package info.androidhive.firebase;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class MapsActivity extends FragmentActivity implements  OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    //String mLastUpdateTime;
    Polyline line;
    Date startDate;
    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    LatLng mLastLocation;
    Marker mCurrLocationMarker;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    // myRef = database.getReference("message");
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    DatabaseReference locations = mDatabase.child("goals");
    float totalDistance = 0;
    boolean isFirstLocation = true;
    String loggingTime;
    final List <LatLng> coordList = new ArrayList<>();
    Map mGoals;
    List mLocations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);
        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
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
        mGoogleMap=googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
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
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    @Override
    public void onLocationChanged(Location location) {

        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
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
        if(isFirstLocation){
            startDate = new Date();
            isFirstLocation = false;
        }
        else{
            redrawLine();
        }
        //lastLocation = location;
        System.out.println("\n\n\n"+latLng.latitude);
        System.out.println("\n\n\n"+latLng.longitude);
        //move map camera
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(18));

    }

    private void redrawLine(){

        mGoogleMap.clear();  //clears all Markers and Polylines

        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        for (int i = 0; i < mLocations.size(); i++) {
            double latitude = (double)((HashMap)(((HashMap)mLocations.get(i)).get("location"))).get("latitude");
            double longitude = (double)((HashMap)(((HashMap)mLocations.get(i)).get("location"))).get("longitude");
            LatLng point = new LatLng(latitude, longitude);
            options.add(point);
        }

        line = mGoogleMap.addPolyline(options); //add Polyline
    }

    private void saveToFirebase(){
        Date endDate = new Date();
        long diff = TimeUnit.SECONDS.toMinutes(endDate.getTime() - startDate.getTime());
        Goal goal = new Goal(totalDistance/diff, totalDistance, diff, startDate, endDate, "Daryna", mLocations);

        locations.push().setValue(goal);
    }

    private void saveCoordinate() {
        Map mLocation = new HashMap();
        //mLocation.put("timestamp", mLastUpdateTime);
        Map mCoordinate = new HashMap();
        mCoordinate.put("latitude", mLastLocation.latitude);
        mCoordinate.put("longitude", mLastLocation.longitude);
        mLocation.put("location", mCoordinate);
        mLocations.add(mLocation);
        //locations.push().setValue(mLocations);
//        myRef.setValue(mLocations);
        //myRef.setValue("Hello, World!");
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    public void startTracking(View v){
        mGoals = new HashMap();
        mLocations = new LinkedList();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    public void stopTracking(View v){
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    public void drawLocations(View v) {
        saveToFirebase();
        //getRoadData();
        //drawPolyline();
    }

    public void getRoadData(){
        // Get only latest logged locations - since 'START' button clicked
        Query queryRef =
                locations.orderByChild("timestamp").startAt(loggingTime);
        // Add listener for a child added at the data at this location
        queryRef.addChildEventListener(new ChildEventListener() {
            LatLngBounds bounds;
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Map  data = (Map ) dataSnapshot.getValue();
                String timestamp = (String) data.get("timestamp");
                // Get recorded latitude and longitude
                Map  mCoordinate = (HashMap)data.get("location");
                double latitude = (double) (mCoordinate.get("latitude"));
                double longitude = (double) (mCoordinate.get("longitude"));
                // Create LatLng for each locations
                LatLng mLatlng = new LatLng(latitude, longitude);
                // Make sure the map boundary contains the location
                builder.include(mLatlng);
                bounds = builder.build();
                coordList.add(mLatlng);
                //Zoom map to the boundary that contains every logged location
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds,
                        18));
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot){
            }
            @Override
            public void onCancelled(DatabaseError databaseError){}
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s1){}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s2){}
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mGoogleMap.setMyLocationEnabled(true);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }

            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}