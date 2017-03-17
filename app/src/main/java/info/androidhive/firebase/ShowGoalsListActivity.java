package info.androidhive.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.LinkedList;


public class ShowGoalsListActivity extends AppCompatActivity {
    ArrayAdapter<Goal> goalAdapter;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private ChildEventListener mChildEventListener;
    private ChildEventListener locChildEventListener;
    DatabaseReference goals = mDatabase.child("goals");
    ArrayList<Goal> NameList = new ArrayList<>();;

    LinkedList<LinkedList> roads;

    public void onCreate(Bundle saveInstanceState)
    {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.goals_list);
        ListView animalList=(ListView)findViewById(R.id.listViewAnimals);

        getNames();
        System.out.println(NameList.size());
        goalAdapter = new GoalAdapter(this,android.R.layout.simple_list_item_1, NameList);
        animalList.setAdapter(goalAdapter);

        animalList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> arg0, View v,int position, long arg3)
            {
                Goal goal = NameList.get(position);
                LinkedList<com.google.android.gms.maps.model.LatLng> road = roads.get(position);
                Intent intent = new Intent(ShowGoalsListActivity.this, GoalMapActivity.class);
                intent.putExtra("road", road);
                intent.putExtra("key", goal.getKey());
                startActivity(intent);
            }
        });

    }

     LinkedList<com.google.android.gms.maps.model.LatLng> getLocations(String key){
        DatabaseReference locations = mDatabase.child("goals").child(key).child("locations");
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

    void getNames()
    {
        roads = new LinkedList<>();
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Goal goal = dataSnapshot.getValue(Goal.class);
                    String key = dataSnapshot.getKey();
                    roads.add(getLocations(key));
                    goal.setKey(key);
                    goalAdapter.add(goal);
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            goals.addChildEventListener(mChildEventListener);
        }
    }
}

