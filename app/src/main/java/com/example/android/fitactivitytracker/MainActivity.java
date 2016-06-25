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
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "FitActivityTracker";

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private static StringBuilder emptyEditText = new StringBuilder("");
    private static int emptyEditTextCount = 0;

    private static Button checkGoalButton;
    private static Button startWorkoutButton;
    private static Button endWorkoutButton;

    public static FitApiClient mClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!PermissionsManager.checkPermissions(this)) {
            PermissionsManager.requestPermissions(this);
        }
        checkGoalButton = (Button) findViewById(R.id.check_your_goal_button);
        checkGoalButton.setEnabled(false);
        startWorkoutButton = (Button) findViewById(R.id.start_workout_button);
        startWorkoutButton.setEnabled(false);
        endWorkoutButton = (Button) findViewById(R.id.end_workout_button);
        endWorkoutButton.setEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        buildFitnessClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // This ensures that if the user denies the permissions then uses Settings to re-enable
        // them, the app will start working.
        buildFitnessClient();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
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
            if (FitnessSession.mSession == null) {
                Toast.makeText(this, "Checking Goal for Today Results ...", Toast.LENGTH_LONG).show();
            }
            else {
                Toast.makeText(this, "Checking Goal for Your Workout Session ...", Toast.LENGTH_LONG).show();
            }
            ReadDataAndCheckGoal.ReadDataAndCheckGoalExecute(this, mClient.getClient(), goalStr);
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
            mClient = new FitApiClient(this);
            mClient.connect();
            findFitnessDataSources();
        }
        else if (mClient != null && !mClient.getClient().isConnecting() && !mClient.getClient().isConnected()) {
            mClient.connect();
        }
    }


    private void findFitnessDataSources() {
        FitnessSensor.findFitnessDataSources(mClient.getClient());
    }

    public void startWorkout(View v) {
        FitnessSession.startSession(this, mClient);
    }

    public void endWorkout(View v) {
        FitnessSession.stopSession();
        //FitnessRecording.cancelSubscription(mClient.getClient(),FitnessSensor.mDataSourceList.get(0));
    }


    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        //mClient.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult...");
        //Log.i(TAG, "onActivityResult...resultCode: " + resultCode);
        //mClient.getClient().connect();
        buildFitnessClient();
        /*
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            Log.i(TAG, "resultCode: " + resultCode);
            Log.i(TAG, "resultCode == Activity.RESULT_OK: "+ (resultCode == Activity.RESULT_OK));
            if (resultCode == Activity.RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                buildFitnessClient();
            }
        }
        */

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult and requestCode :" + requestCode);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                Log.i(TAG, "permission granted: " + grantResults[0]);
                buildFitnessClient();
            } else {
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
            FitnessDataHandler.deleteData(mClient.getClient());
            return true;
        }
        else if (id == R.id.action_unregister_fitness_datalisteners){
            FitnessSensor.unregisterFitnessDataListener();
            return true;
        }
        else if (id == R.id.action_dump_subs) {
            FitnessRecording.dumpSubscriptionsList(mClient.getClient());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
