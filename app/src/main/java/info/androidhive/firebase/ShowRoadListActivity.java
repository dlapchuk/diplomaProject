package info.androidhive.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

public class ShowRoadListActivity extends AppCompatActivity {
    ArrayAdapter<Road> roadAdapter;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private ChildEventListener locChildEventListener;
    DatabaseReference city_roads = mDatabase.child("roads");
    ArrayList<Road> NameList = new ArrayList<>();;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    LinkedList<LinkedList> roads;
    private FirebaseAnalytics firebaseAnalytics;

    public void onCreate(Bundle saveInstanceState)
    {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_show_road_list);
        ListView roadList=(ListView)findViewById(R.id.listViewRoads);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        getAllDates();
        roadAdapter = new RoadAdapter(this,android.R.layout.simple_list_item_1, NameList);
        roadList.setAdapter(roadAdapter);

        roadList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> arg0, View v,int position, long arg3)
            {
                Road road = NameList.get(position);
                LinkedList<com.google.android.gms.maps.model.LatLng> positions = roads.get(position);
                Intent intent = new Intent(ShowRoadListActivity.this, GoalMapActivity.class);
                intent.putExtra("road", positions);
                intent.putExtra("key", road.getKey());
                intent.putExtra("marks", road.getMarks());
                intent.putExtra("countMarks", road.getCountMarks());
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, user.getUid());
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, road.getName());
                Toast.makeText(ShowRoadListActivity.this,"select content event added", Toast.LENGTH_SHORT).show();
                //Logs an app event.
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                startActivity(intent);

            }
        });

    }

    LinkedList<com.google.android.gms.maps.model.LatLng> getLocations(String key){
        DatabaseReference locations = mDatabase.child("roads").child(key).child("locations");
        final LinkedList<com.google.android.gms.maps.model.LatLng> locationsList = new LinkedList<>();

        locChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                LatLng loc = dataSnapshot.getValue(LatLng.class);
                com.google.android.gms.maps.model.LatLng latLng =
                        new com.google.android.gms.maps.model.LatLng(loc.getLatitude(), loc.getLongitude());
                locationsList.add(latLng);

            }

            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            public void onCancelled(DatabaseError databaseError) {}
        };

        locations.addChildEventListener(locChildEventListener);
        return locationsList;
    }

    void getAllDates()
    {
        Query queryRef = city_roads.orderByChild("name");
        roads = new LinkedList<>();
        queryRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Road road = dataSnapshot.getValue(Road.class);
                String key = dataSnapshot.getKey();
                road.setKey(key);
                roadAdapter.add(road);
                roads.add(getLocations(key));
            }
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

}

