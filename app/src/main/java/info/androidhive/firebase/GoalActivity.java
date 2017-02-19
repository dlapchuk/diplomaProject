package info.androidhive.firebase;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class GoalActivity extends AppCompatActivity {

    private TextView name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal);

        name = (TextView) findViewById(R.id.nametxt);

        Goal goal = (Goal)getIntent().getSerializableExtra("goal");
        name.setText(name.getText().toString() + " " + goal.getName());

    }
}
