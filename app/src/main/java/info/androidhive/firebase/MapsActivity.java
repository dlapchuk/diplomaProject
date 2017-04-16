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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import javax.xml.datatype.Duration;

public class MapsActivity extends FragmentActivity implements  OnMapReadyCallback,
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
    LatLng prev = null;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    DatabaseReference goals = mDatabase.child("goals");
    float totalDistance = 0;
    boolean isFirstLocation = true;
    String loggingTime;
    final List <LatLng> coordList = new ArrayList<>();
    Map mGoals;
    LinkedList <LatLng> mLocations;

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

        saveMaps();

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
        float [] results = new float[1];
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
            Location.distanceBetween(prev.latitude/ 1e6, prev.longitude/ 1e6,
                    latLng.latitude/ 1e6, latLng.longitude/ 1e6, results);
            totalDistance += results[0];
        }
        prev = new LatLng(latLng.latitude, latLng.longitude);

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
            double latitude = mLocations.get(i).latitude;
            double longitude = mLocations.get(i).longitude;
            LatLng point = new LatLng(latitude, longitude);
            options.add(point);
        }

        line = mGoogleMap.addPolyline(options); //add Polyline
    }

    private void saveToFirebase(){
        boolean isFirst = true;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String name = "anonymous";
        if (user != null) {
            name = user.getUid();
        }
        Date endDate = new Date();

        long diff = (endDate.getTime() - startDate.getTime())/1000;
        Goal goal = new Goal(totalDistance/diff, totalDistance, diff, startDate, endDate, name, mLocations);

        String key = goals.push().getKey();
        goals.child(key).setValue(goal);
        DatabaseReference locations = mDatabase.child("goals").child(key).child("locations");

        for(LatLng pos: mLocations){
            locations.push().setValue(pos);
        }
    }

    private void saveMaps(){
        /*DatabaseReference roads = mDatabase.child("roads");
        LinkedList<LatLng> mLocationRoads = new LinkedList<>();
        mLocationRoads.add(new LatLng(50.455448, 30.352098));
        mLocationRoads.add(new LatLng(50.457225, 30.386935));
        mLocationRoads.add(new LatLng(50.427392, 30.566915));
        mLocationRoads.add(new LatLng(50.442280, 30.558851));
        mLocationRoads.add(new LatLng(50.451380, 30.543030));
        mLocationRoads.add(new LatLng(50.458269, 30.528230));
        mLocationRoads.add(new LatLng(50.467755, 30.524658));
        mLocationRoads.add(new LatLng(50.476006, 30.535069));
        mLocationRoads.add(new LatLng(50.482111, 30.537723));
        mLocationRoads.add(new LatLng(50.484514, 30.537212));
        mLocationRoads.add(new LatLng(50.486463, 30.528638));
        mLocationRoads.add(new LatLng(50.488411, 30.531701));
        mLocationRoads.add(new LatLng(50.490814, 30.529353));
        mLocationRoads.add(new LatLng(50.506007, 30.513022));
        mLocationRoads.add(new LatLng(50.509966, 30.512511));
        mLocationRoads.add(new LatLng(50.513212, 30.514042));
        mLocationRoads.add(new LatLng(50.520828, 30.521995));
        mLocationRoads.add(new LatLng(50.522719, 30.522541));
        mLocationRoads.add(new LatLng(50.529124, 30.518475));
        mLocationRoads.add(new LatLng(50.529278, 30.517504));
        mLocationRoads.add(new LatLng(50.532094, 30.528368));
        mLocationRoads.add(new LatLng(50.503925, 30.542861));
        mLocationRoads.add(new LatLng(50.503563, 30.542401));
        mLocationRoads.add(new LatLng(50.504500, 30.541557));
        mLocationRoads.add(new LatLng(50.508197, 30.540535));
        mLocationRoads.add(new LatLng(50.513157, 30.536226));
        mLocationRoads.add(new LatLng(50.514768, 30.536303));
        mLocationRoads.add(new LatLng(50.519231, 30.538622));
        mLocationRoads.add(new LatLng(50.505538, 30.584182));
        mLocationRoads.add(new LatLng(50.507864, 30.587283));
        mLocationRoads.add(new LatLng(50.511921, 30.598893));
        mLocationRoads.add(new LatLng(50.513999, 30.602676));
        mLocationRoads.add(new LatLng(50.520409, 30.608545));
        mLocationRoads.add(new LatLng(50.522032, 30.609232));
        mLocationRoads.add(new LatLng(50.523456, 30.609429));
        mLocationRoads.add(new LatLng(50.527045, 30.609888));

        HashMap<String, String> road = new HashMap<>();
        road.put("name", "Велодоріжка");
        road.put("mark", "0");

        String key = roads.push().getKey();
        roads.child(key).setValue(road);
        DatabaseReference roadLocations = roads.child(key).child("locations");
        for(LatLng ltnLng: mLocationRoads){
            roadLocations.push().setValue(ltnLng);
        }*/
    }

    private void saveCoordinate() {
        mLocations.add(mLastLocation);
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

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
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
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
}