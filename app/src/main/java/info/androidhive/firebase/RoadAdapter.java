package info.androidhive.firebase;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.sql.Date;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.List;


public class RoadAdapter extends ArrayAdapter<Road> {
    FirebaseUser user;
    public static final int RC_SIGN_IN = 1;


    public RoadAdapter(Context context, int resource, List<Road> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_road, parent, false);
        }

        TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);
        TextView markTextView = (TextView) convertView.findViewById(R.id.markTextView);

        Road road = getItem(position);
        nameTextView.setText(road.getName());

        try{
            String formattedString = String.format("%.02f", ((double)road.getMarks()/road.getCountMarks()));
            markTextView.setText("mark: " + formattedString);
        }
        catch (Exception e){
            markTextView.setText("mark: " + 0.0);
        }

        return convertView;
    }
}
