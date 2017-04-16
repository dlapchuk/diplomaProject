package info.androidhive.firebase;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.maps.model.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

public class RatingActivity extends Activity {

    private RatingBar ratingBar;
    private TextView txtRatingValue;
    private Button btnSubmit;
    private String goalId, userId;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mark;
    private ChildEventListener mChildEventListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);
        ratingBar = (RatingBar) findViewById(R.id.ratingBar);
        txtRatingValue = (TextView) findViewById(R.id.txtRatingValue);
        goalId = getIntent().getExtras().getString("key");
        ratingBar.setRating(0);

        addListenerOnRatingBar();
        addListenerOnButton();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }
        mark = mDatabase.child("goals").child(goalId).child("marks").child(userId);
        attachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    try{
                        Long mark = (Long) dataSnapshot.getValue();
                        ratingBar.setRating(mark);
                    }
                    catch (Exception e){
                        Double mark =(Double) dataSnapshot.getValue();
                        ratingBar.setRating((int)mark.floatValue());
                    }

                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                public void onCancelled(DatabaseError databaseError) {
                }
            };
            mark.addChildEventListener(mChildEventListener);
        }
    }


    public void saveRating(float rating){
        mark.child("mark").setValue(rating);
    }



    public void addListenerOnRatingBar() {
        //if rating value is changed,
        //display the current rating value in the result (textview) automatically
        ratingBar.setOnRatingBarChangeListener(new OnRatingBarChangeListener() {
            public void onRatingChanged(RatingBar ratingBar, float rating,
                                        boolean fromUser) {

                txtRatingValue.setText(String.valueOf(rating));

            }
        });
    }

    public void addListenerOnButton() {

        ratingBar = (RatingBar) findViewById(R.id.ratingBar);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);

        //if click on me, then display the current rating value.
        btnSubmit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                float rating = ratingBar.getRating();
                if(rating == 0){
                    Toast.makeText(RatingActivity.this, "Rating must not be null",
                            Toast.LENGTH_SHORT).show();
                }
                else{
                    saveRating(rating);
                    Toast.makeText(RatingActivity.this,
                            String.valueOf(ratingBar.getRating()),
                            Toast.LENGTH_SHORT).show();
                }
            }

        });

    }
}
