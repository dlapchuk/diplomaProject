package info.androidhive.firebase;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.gms.maps.model.*;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.LinkedList;

public class GoalActivity extends AppCompatActivity {

    private TextView name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        name = (TextView) findViewById(R.id.nametxt);

        ArrayList<LatLng> road =
                (ArrayList< LatLng>)getIntent().getSerializableExtra("road");
        String langitudes = "";
        for(LatLng latLng: road){
            langitudes += latLng.latitude + " ";
        }
        name.setText(name.getText().toString() + " " + langitudes);
    }
}
