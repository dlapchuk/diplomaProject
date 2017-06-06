package info.androidhive.firebase;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
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

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
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
import com.google.api.services.bigquery.model.TableCell;
import com.google.api.services.bigquery.model.TableRow;
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

public class AllRoadsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    Polyline line;
    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    DatabaseReference goals = mDatabase.child("goals");
    Map mGoals;
    LinkedList<LatLng> mLocations;
    DatabaseReference roadDB = mDatabase.child("roads");
    LinkedList<LinkedList> roads= new LinkedList<>();
    private ChildEventListener mChildEventListener;
    private ChildEventListener locChildEventListener;
    String age = "any", day = "any", time = "any", season = "any";
    Spinner mSpinnerAge, mSpinnerDay, mSpinnerSeason, mSpinnerTime;
    TextView maxDistance, maxTime, minDistance, minTime, averegeDistance, averegeTime;
    View mView, sView;
    double max_distance, min_distance, avg_time, avg_distance;
    int min_time, max_time;
    float rainfall[] = {98.8f, 123.8f, 162.4f, 25.9f};
    String monthNames[] = {"Jan", "Febr", "March", "Apr"};


    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //getNames();
        //executeQuery("evening", "young", "spring");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_roads);
        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        String query = addParamsToQuery(composeMainQuery(), time, age, season, day);
        mView = getLayoutInflater().inflate(R.layout.all_road_statistic, null);
        sView = getLayoutInflater().inflate(R.layout.all_roads_dialog_spinner, null);

        executeMainQuery(query);
    }

    @Override
    public void onPause() {
        super.onPause();
        //stop location updates when Activity is no longer active
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

    public void markLocation(View v){
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(AllRoadsActivity.this);
        sView = getLayoutInflater().inflate(R.layout.all_roads_dialog_spinner, null);
        mBuilder.setTitle("Spinner in custom dialog");

        mSpinnerAge = (Spinner) sView.findViewById(R.id.spinnerAge);
        ArrayAdapter<String> adapterAge = new ArrayAdapter<String>(AllRoadsActivity.this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.ageList));
        adapterAge.setDropDownViewResource(android.R.layout.simple_spinner_item);
        mSpinnerAge.setAdapter(adapterAge);


        mSpinnerSeason = (Spinner) sView.findViewById(R.id.spinnerSeason);
        ArrayAdapter<String> adapterSeason = new ArrayAdapter<String>(AllRoadsActivity.this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.seasonList));
        adapterSeason.setDropDownViewResource(android.R.layout.simple_spinner_item);
        mSpinnerSeason.setAdapter(adapterSeason);

        mSpinnerDay = (Spinner) sView.findViewById(R.id.spinnerDay);
        ArrayAdapter<String> adapterDay = new ArrayAdapter<String>(AllRoadsActivity.this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.dayList));
        adapterDay.setDropDownViewResource(android.R.layout.simple_spinner_item);
        mSpinnerDay.setAdapter(adapterDay);

        mSpinnerTime = (Spinner) sView.findViewById(R.id.spinnerTime);
        ArrayAdapter<String> adapterTime = new ArrayAdapter<String>(AllRoadsActivity.this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.timeList));
        adapterTime.setDropDownViewResource(android.R.layout.simple_spinner_item);
        mSpinnerTime.setAdapter(adapterTime);

        //int spinnerPosition = adapterAge.getPosition(age);
        //mSpinnerAge.setSelection(spinnerPosition);
        int spinnerPosition;
        spinnerPosition = adapterDay.getPosition(day);
        mSpinnerDay.setSelection(spinnerPosition);
        spinnerPosition = adapterSeason.getPosition(season);
        mSpinnerSeason.setSelection(spinnerPosition);
        spinnerPosition = adapterTime.getPosition(time);
        mSpinnerTime.setSelection(spinnerPosition);

        mBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int i) {
                        //time = mSpinnerTime.getSelectedItem().toString();
                        age = mSpinnerAge.getSelectedItem().toString();
                        season = mSpinnerSeason.getSelectedItem().toString();
                        day = mSpinnerDay.getSelectedItem().toString();
                        mGoogleMap.clear();
                        String query = addParamsToQuery(composeMainQuery(), time, age, season, day);
                        executeMainQuery(query);
                        dialog.dismiss();
                    }
                });
        mBuilder.setNegativeButton("Dismiss",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });
        mBuilder.setView(sView);
        AlertDialog dialog = mBuilder.create();
        dialog.show();
    }

    public List<String> getIds(List<TableRow> rows){
        List<String> keys = new ArrayList<>();
        if(rows != null){
            for (TableRow row : rows) {
                for (TableCell field : row.getF()) {
                    keys.add((String)field.getV());
                }
            }
        }

        return keys;
    }

    public void getStatistic(View view){
        AlertDialog.Builder mBuilder = new AlertDialog.Builder(AllRoadsActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.all_road_statistic, null);
        mBuilder.setTitle("Spinner in custom dialog");

        String query = addParamsToQuery(composeStatisticQuery(), time, age, season, day);
        executeStatisticQuery(query);
        maxDistance = (TextView) mView.findViewById(R.id.maxDistance);
        minDistance = (TextView) mView.findViewById(R.id.minDistance);
        maxTime = (TextView) mView.findViewById(R.id.maxTime);
        minTime = (TextView) mView.findViewById(R.id.minTime);
        averegeDistance = (TextView) mView.findViewById(R.id.avgDistance);
        averegeTime = (TextView) mView.findViewById(R.id.avgTime);

        maxDistance.setText("Max distance: " + Double.toString(max_distance));
        minDistance.setText("Min distance: " + Double.toString(min_distance));
        maxTime.setText("Max time: " + Integer.toString(max_time));
        minTime.setText("Min time: " + Integer.toString(min_time));
        averegeDistance.setText("Average distance: " + Double.toString(avg_distance));
        averegeTime.setText("Average time: " + Double.toString(avg_time));


        List<PieEntry> pieEntry = new ArrayList<>();
        for(int i=0; i< rainfall.length; i++){
            pieEntry.add(new PieEntry(rainfall[i], monthNames[i]));
        }
        PieDataSet dataSet = new PieDataSet(pieEntry, "Rainfall");
        PieData data = new PieData(dataSet);
        PieChart chart = (PieChart) mView.findViewById(R.id.chart);

        chart.setData(data);
        chart.invalidate();

        mBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int i) {
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

    public void executeStatisticQuery(final String query){

        Thread thread = new Thread(){
            public void run(){
                AssetManager am = getAssets();
                Context context = getApplicationContext();
                BigQueryConnector bigQuery = new BigQueryConnector(am, query, context);
                try {
                    List<TableRow> rows = bigQuery.start_bigquery();
                    if(rows != null){
                        for(TableRow row: rows){
                            max_distance = Double.valueOf((String) row.getF().get(0).getV());
                            max_time = Integer.valueOf((String) row.getF().get(1).getV());
                            min_distance = Double.valueOf((String) row.getF().get(2).getV());
                            min_time = Integer.valueOf((String) row.getF().get(3).getV());
                            avg_distance = Double.valueOf((String) row.getF().get(4).getV());
                            avg_time = Double.valueOf((String) row.getF().get(5).getV());
                            setStatisticValues(max_distance, max_time, min_distance, min_time, avg_distance, avg_time);
                            //maxDistance = (TextView) mView.findViewById(R.id.maxDistanceValue);
                            //maxDistance.setText("Max distance: " + Double.toString(max_distance));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
        thread.start();
    }

    private void setStatisticValues(final double max_distance, final int max_time,
                                    final double min_distance, final int min_time,
                                    final double avg_distance, final double avg_time) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                maxDistance = (TextView) mView.findViewById(R.id.maxDistance);
                maxDistance.setText(Double.toString(max_distance));
            }
        });


        String a = Double.toString(max_distance);
        System.out.println(a);
    }

    public void executeMainQuery(final String query){

        Thread thread = new Thread(){
            public void run(){
                AssetManager am = getAssets();
                Context context = getApplicationContext();
                BigQueryConnector bigQuery = new BigQueryConnector(am, query, context);
                try {
                    List<TableRow> rows = bigQuery.start_bigquery();
                    List <String> keys = getIds(rows);
                    for(String key: keys){
                        getLocations(key);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
        thread.start();
    }

    public String composeStatisticQuery(){
        String query = "SELECT\n" +
                "  MAX(IF(param.key = \"distance\", param.value.double_value, NULL)) AS max_distance,\n" +
                "  MAX(IF(param.key = \"time\", param.value.int_value, NULL)) AS max_time,\n" +
                "  MIN(IF(param.key = \"distance\", param.value.double_value, NULL)) AS min_distance,\n" +
                "  MIN(IF(param.key = \"time\", param.value.int_value, NULL)) AS min_time,\n" +
                "  AVG(IF(param.key = \"distance\", param.value.double_value, NULL)) AS avg_distance,\n" +
                "  AVG(IF(param.key = \"time\", param.value.int_value, NULL)) AS avg_time\n" +
                "FROM \n" +
                "  `ukrbikeapp.info_androidhive_firebase_ANDROID.app_events_*`,\n" +
                "  UNNEST(event_dim) as event,\n" +
                "  UNNEST(event.params) as param,\n" +
                "  UNNEST(user_dim.user_properties) as user_prop,\n" +
                "  UNNEST([EXTRACT(HOUR FROM TIMESTAMP_MICROS(event.timestamp_micros))]) AS hr \n" +
                "WHERE event.name = \"add_road\" \n" +
                "  AND user_prop.key = \"age\" \n";
        return query;
    }

    public String composeMainQuery(){
        String query = "SELECT event_param.value.String_value \n" +
                "FROM `ukrbikeapp.info_androidhive_firebase_ANDROID.app_events_*`,\n" +
                "  UNNEST(event_dim) as event,\n" +
                "  UNNEST(event.params) as event_param,\n" +
                "  UNNEST(user_dim.user_properties) as user_prop,\n" +
                "  UNNEST([EXTRACT(HOUR FROM TIMESTAMP_MICROS(event.timestamp_micros))]) AS hr\n" +
                "WHERE event.name = \"add_road\" \n" +
                "AND user_prop.key = \"age\" AND event_param.key = \"item_id\" \n";
        return query;
    }

    public String addParamsToQuery(String query, String time, String age, String season, String day){
        switch (time){
            case "morning":
                query += " AND hr >= 6 AND hr < 11\n";
                break;
            case "afternoon":
                query += " AND hr >= 11 AND hr < 17\n";
                break;
            case "evening":
                query += " AND hr >= 17 AND hr < 24\n";
                break;
            case "night":
                query += " AND hr >= 0 AND hr < 6\n";
                break;
        }
        switch (age){
            case "child":
                query += " AND (CAST(user_prop.value.value.string_value as FLOAT64)) <= 12\n";
                break;
            case "teen":
                query += " AND (CAST(user_prop.value.value.string_value as FLOAT64)) > 12" +
                        " AND (CAST(user_prop.value.value.string_value as FLOAT64)) <= 18\n";
                break;
            case "young":
                query += " AND (CAST(user_prop.value.value.string_value as FLOAT64)) > 18" +
                        " AND (CAST(user_prop.value.value.string_value as FLOAT64)) <= 30\n";
                break;
            case "middle":
                query += " AND (CAST(user_prop.value.value.string_value as FLOAT64)) > 30" +
                        " AND (CAST(user_prop.value.value.string_value as FLOAT64)) <= 60\n";
                break;
            case "senior":
                query += " AND (CAST(user_prop.value.value.string_value as FLOAT64)) > 60\n";
                break;
        }
        switch (season){
            case "summer":
                query += "AND (_TABLE_SUFFIX LIKE '201_06__' OR _TABLE_SUFFIX LIKE '201_07__' OR _TABLE_SUFFIX LIKE '201_08__')\n";
                break;
            case "autumn":
                query += "AND (_TABLE_SUFFIX LIKE '201_09__' OR _TABLE_SUFFIX LIKE '201_10__' OR _TABLE_SUFFIX LIKE '201_11__')\n";
                break;
            case "winter":
                query += "AND (_TABLE_SUFFIX LIKE '201_12__' OR _TABLE_SUFFIX LIKE '201_01__' OR _TABLE_SUFFIX LIKE '201_02__')\n";
                break;
            case "spring":
                query += "AND (_TABLE_SUFFIX LIKE '201_05__' OR _TABLE_SUFFIX LIKE '201_04__' OR _TABLE_SUFFIX LIKE '201_03__')\n";
                break;
        }
        switch(day){
            case "workday":
                query += "AND (SELECT COUNTIF(key = 'day' AND value.string_value = \"workday\") FROM UNNEST(event.params)) > 0";
                break;
            case "weekend":
                query += "AND (SELECT COUNTIF(key = 'day' AND value.string_value = \"weekend\") FROM UNNEST(event.params)) > 0";
                break;
        }
        System.out.println(query);
        return query;
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
            goals.addChildEventListener(mChildEventListener);
        }
    }


    LinkedList<com.google.android.gms.maps.model.LatLng> getLocations(String key){
        final DatabaseReference locations = mDatabase.child("goals").child(key).child("locations");
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
                            .width(5)
                            .color(Color.BLUE));
                    System.out.println("line added!");
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
                                ActivityCompat.requestPermissions(AllRoadsActivity.this,
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
