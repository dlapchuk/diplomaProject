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

import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.Serializable;
import java.util.ArrayList;


public class ShowGoalsListActivity extends AppCompatActivity {
    ArrayAdapter<Goal> goalAdapter;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private ChildEventListener mChildEventListener;
    private ChildEventListener locChildEventListener;
    DatabaseReference goals = mDatabase.child("goals");
    ArrayList<Goal> NameList = new ArrayList<>();;
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
                Intent intent = new Intent(ShowGoalsListActivity.this, GoalActivity.class);
                intent.putExtra("goal", (Serializable) goal);
                startActivity(intent);
                //Toast.makeText(getApplicationContext(), "Animal Selected : "+goal,   Toast.LENGTH_LONG).show();
            }
        });

    }

    void getLocations(String key){
        DatabaseReference locations = mDatabase.child("goals").child(key).child("locations");
        if (locChildEventListener == null) {
            locChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    LatLng loc = dataSnapshot.getValue(LatLng.class);
                    String key = dataSnapshot.getKey();
                    System.out.println(loc.getLatitude());
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            locations.addChildEventListener(locChildEventListener);
        }
    }

    void getNames()
    {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Goal goal = dataSnapshot.getValue(Goal.class);
                    String key = dataSnapshot.getKey();
                    System.out.println(goal.getName());
                    getLocations(key);
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

