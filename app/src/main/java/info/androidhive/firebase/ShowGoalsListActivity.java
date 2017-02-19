package info.androidhive.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ShowGoalsListActivity extends AppCompatActivity {
    ArrayAdapter<Goal> arrayAdapter;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    DatabaseReference locations = mDatabase.child("messages");
    ArrayList<Goal> NameList = new ArrayList<>();;
    public void onCreate(Bundle saveInstanceState)
    {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.goals_list);

        ListView animalList=(ListView)findViewById(R.id.listViewAnimals);

        getNames();
        System.out.println(NameList.size());
        arrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, NameList);
        animalList.setAdapter(arrayAdapter);

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

    void getNames()
    {

        Query queryRef = locations.orderByChild("average speed");
        queryRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Goal goal = dataSnapshot.getValue(Goal.class);
                NameList.add(goal);
                arrayAdapter.notifyDataSetChanged();
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
}

