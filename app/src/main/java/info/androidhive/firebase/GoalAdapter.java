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

        TextView speedTextView = (TextView) convertView.findViewById(R.id.speedTextView);
        TextView distanceTextView = (TextView) convertView.findViewById(R.id.distanceTextView);
        TextView durationTextView = (TextView) convertView.findViewById(R.id.durationTextView);
        TextView nameTextView = (TextView) convertView.findViewById(R.id.nameTextView);

        Goal goal = getItem(position);

        speedTextView.setText(goal.getKey());
        distanceTextView.setText(Float.toString(goal.getDistance()));
        durationTextView.setText(Long.toString(goal.getDuration()));
        nameTextView.setText(goal.getName());

        return convertView;
    }
}
