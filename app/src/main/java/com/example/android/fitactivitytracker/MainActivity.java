package com.example.android.fitactivitytracker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.data.Session;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "FitActivityTracker";
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private static final String AUTH_PENDING = "auth_state_pending";
    private static boolean authInProgress = false;

    private static StringBuilder emptyEditText = new StringBuilder("");
    private static int emptyEditTextCount = 0;

    public static GoogleApiClient mClient = null;

    private Session mSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        if (!PermissionsManager.checkPermissions(this)) {
            PermissionsManager.requestPermissions(this);
        }
        Button checkGoalButton = (Button) findViewById(R.id.check_your_goal_button);
        checkGoalButton.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // This ensures that if the user denies the permissions then uses Settings to re-enable
        // them, the app will start working.
        buildFitnessClient();
    }

    public void executeTask(View view) {
        String[] goalStr = valAllEditText();
        if (emptyEditTextCount > 0) {
            TextView textView = (TextView) findViewById(R.id.return_msg_text_view);
            textView.setText(emptyEditText.toString());
            emptyEditText = new StringBuilder("");
            emptyEditTextCount = 0;
        }
        else {
            ReadDataAndCheckGoal.ReadDataAndCheckGoalExecute(this, mClient, goalStr);
        }
    }

    private String[] valAllEditText(){
        //to check if the user has entered values into the edittext fields for number of steps, calories expended
        //and distance traveled
        EditText numStepsET = (EditText) findViewById(R.id.num_steps_edit_text);
        String numStepsStr = numStepsET.getText().toString();
        if (numStepsStr.equals("")) {
            emptyEditText.append("Please enter the number of steps ...\n");
            emptyEditTextCount++;
        }

        EditText calExpended = (EditText) findViewById(R.id.calories_expended_edit_text);
        String caloriesStr = calExpended.getText().toString();
        if (caloriesStr.equals("")) {
            emptyEditText.append("Please enter the number of calories expended ...\n");
            emptyEditTextCount++;
        }

        EditText distanceET = (EditText) findViewById(R.id.distance_traveled_edit_text);
        String distanceStr = distanceET.getText().toString();
        if (distanceStr.equals("")) {
            emptyEditText.append("Please enter the distance covered ...\n");
            emptyEditTextCount++;
        }

        EditText activityDurationEditText = (EditText) findViewById(R.id.activity_duration_edit_text);
        String durationStr = activityDurationEditText.getText().toString();
        if (durationStr.equals("")) {
            emptyEditText.append("Please enter the activity duration ...\n\n");
            emptyEditTextCount++;
        }

        String[] retStr = {numStepsStr, caloriesStr, distanceStr, durationStr};
        return retStr;
    }


    private void buildFitnessClient() {
        // Create the Google API Client
        if (mClient == null && PermissionsManager.checkPermissions(this)){
            mClient = FitApiClient.buildFitnessClient(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                buildFitnessClient();
            } else {
                // Permission denied.

                // In this Activity we've chosen to notify the user that they
                // have rejected a core permission for the app since it makes the Activity useless.
                // We're communicating this message in a Snackbar since this is a sample app, but
                // core permissions would typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                Snackbar.make(
                        findViewById(R.id.main_activity_view),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete_data) {
            FitnessDataHandler.deleteData(mClient);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
