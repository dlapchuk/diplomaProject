package info.androidhive.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.roomorama.caldroid.CaldroidFragment;
import com.roomorama.caldroid.CaldroidListener;

import java.sql.SQLOutput;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

public class ShowGoalsListActivity extends AppCompatActivity {
    ArrayAdapter<Goal> goalAdapter;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private ChildEventListener locChildEventListener;
    DatabaseReference goals = mDatabase.child("goals");
    ArrayList<Goal> NameList = new ArrayList<>();;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    LinkedList<LinkedList> roads;
    ArrayList <Date> dates = new ArrayList<>();
    private CaldroidFragment caldroidFragment = new CaldroidFragment();

    public void onCreate(Bundle saveInstanceState)
    {
        final SimpleDateFormat formatter = new SimpleDateFormat("dd MMM yyyy");
        super.onCreate(saveInstanceState);
        setContentView(R.layout.goals_list);
        ListView animalList=(ListView)findViewById(R.id.listViewAnimals);
        //////////////////////////////////////////////////////
        Bundle args = new Bundle();
        Calendar cal = Calendar.getInstance();
        args.putInt(CaldroidFragment.MONTH, cal.get(Calendar.MONTH) + 1);
        args.putInt(CaldroidFragment.YEAR, cal.get(Calendar.YEAR));
        args.putInt(CaldroidFragment.START_DAY_OF_WEEK, CaldroidFragment.MONDAY);
        caldroidFragment.setArguments(args);

        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.calendar1, caldroidFragment);
        t.commit();

        final CaldroidListener listener = new CaldroidListener() {

            @Override
            public void onSelectDate(Date date, View view) {
                Toast.makeText(getApplicationContext(), formatter.format(date),
                        Toast.LENGTH_SHORT).show();
                goalAdapter.clear();
                getByDate(date);
            }

            @Override
            public void onChangeMonth(int month, int year) {

            }

            @Override
            public void onLongClickDate(Date date, View view) {
            }

            @Override
            public void onCaldroidViewCreated() {
                if (caldroidFragment.getLeftArrowButton() != null) {
                }
            }
        };
        caldroidFragment.setCaldroidListener(listener);
        /////////////////////////////////////////////////////////////
        getAllDates();
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

    void getAllDates()
    {
        Query queryRef =
                goals.orderByChild("name").equalTo(user.getUid());
        roads = new LinkedList<>();
            queryRef.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Goal goal = dataSnapshot.getValue(Goal.class);
                    dates.add(goal.getStartDate());
                    caldroidFragment.setSelectedDate(goal.getStartDate());
                    caldroidFragment.refreshView();
                }
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            });
    }

    void getByDate(final Date date)
    {
        roads.clear();
        Query queryRef =
                goals.orderByChild("name").equalTo(user.getUid());
        roads = new LinkedList<>();
        queryRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Goal goal = dataSnapshot.getValue(Goal.class);
                String key = dataSnapshot.getKey();

                goal.setKey(key);
                Date forCompare = goal.getStartDate();
                SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
                if(fmt.format(date).equals(fmt.format(forCompare))){
                    goalAdapter.add(goal);
                    roads.add(getLocations(key));
                }
            }
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            public void onCancelled(DatabaseError databaseError) {}
        });
    }
}

