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

/**
 * Created by user on 19.02.2017.
 */

public class GoalAdapter extends ArrayAdapter<Goal> {
    FirebaseUser user;
    public static final int RC_SIGN_IN = 1;


    public GoalAdapter(Context context, int resource, List<Goal> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_goal, parent, false);
        }
        String formattedString;
        TextView speedTextView = (TextView) convertView.findViewById(R.id.speedTextView);
        TextView distanceTextView = (TextView) convertView.findViewById(R.id.distanceTextView);
        TextView durationTextView = (TextView) convertView.findViewById(R.id.durationTextView);
        TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);

        Goal goal = getItem(position);
        Format formatter = new SimpleDateFormat("HH:mm:ss");
        String start = formatter.format(goal.getStartDate());
        String end = formatter.format(goal.getEndDate());
        speedTextView.setText(start + " - " + end);
        formattedString = String.format("%.2f", goal.getDistance());
        distanceTextView.setText("відстань: " + formattedString +" м");
        double time = goal.getDuration() / 60.0;
        formattedString = String.format("%.3f", time);
        durationTextView.setText("час: " + formattedString + " хв");
        //nameTextView.setText("speed: " + Double.toString(goal.getAverageSpeed()));
        formattedString = String.format("%.02f", goal.getAverageSpeed());
        nameTextView.setText("швидкість: " + formattedString + "м/c");

        return convertView;
    }
}
