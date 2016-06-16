package com.example.android.fitactivitytracker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
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

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.SessionReadResult;
import com.google.android.gms.fitness.result.SessionStopResult;

import java.text.DateFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getTimeInstance;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "FitActivityTracker";
    public static final String SAMPLE_SESSION_NAME = "Test Session";
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private static StringBuilder emptyEditText = new StringBuilder("");
    private static int emptyEditTextCount = 0;

    private static Button checkGoalButton;
    private static Button startWorkoutButton;
    private static Button endWorkoutButton;

    public static FitApiClient mClient = null;

    private Session mSession;

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

    public void executeTask(View view) {
        String[] goalStr = valAllEditText();
        if (emptyEditTextCount > 0) {
            TextView textView = (TextView) findViewById(R.id.return_msg_text_view);
            textView.setText(emptyEditText.toString());
            emptyEditText = new StringBuilder("");
            emptyEditTextCount = 0;
        }
        else {
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
            mClient = new FitApiClient(this, checkGoalButton, startWorkoutButton);
            mClient.connect();
        }
    }


    private void findFitnessDataSources() {
        FitnessSensor.findFitnessDataSources(mClient.getClient());
    }

    public void startWorkout(View v) {
        findFitnessDataSources();

        startSession();
    }

    public void endWorkout(View v) {
        stopSession();
        FitnessRecording.cancelSubscription(mClient.getClient());
    }

    public void startSession() {
        if (mSession == null){
            mSession = new Session.Builder()
                    .setName(SAMPLE_SESSION_NAME)
                    .setIdentifier(getString(R.string.app_name) + " " + System.currentTimeMillis())
                    .setDescription("Test Session")
                    .setStartTime(System.currentTimeMillis()-TimeUnit.SECONDS.toMillis(1), TimeUnit.MILLISECONDS)
                    // optional - if your app knows what activity:
                    //.setActivity(FitnessActivities.ON_FOOT)
                    .build();
            PendingResult<Status> pendingResult =
                    Fitness.SessionsApi.startSession(mClient.getClient(),mSession);

            pendingResult.setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "Successfully started session");
                                endWorkoutButton.setEnabled(true);
                                startWorkoutButton.setEnabled(false);
                            } else {
                                Log.i(TAG, "Failed to start session: " + status.getStatusMessage());
                            }
                        }
                    }
            );
        }
    }

    public void stopSession() {
        PendingResult<SessionStopResult> pendingResult =
                Fitness.SessionsApi.stopSession(mClient.getClient(), mSession.getIdentifier());

        pendingResult.setResultCallback(new ResultCallback<SessionStopResult>() {
            @Override
            public void onResult(SessionStopResult sessionStopResult) {
                if( sessionStopResult.getStatus().isSuccess() ) {
                    Log.i(TAG, "Successfully stopped session");
                    mSession = sessionStopResult.getSessions().get(0);
                    DateFormat dateFormat = getTimeInstance();
                    if( sessionStopResult.getSessions() != null && !sessionStopResult.getSessions().isEmpty() ) {
                        Log.i(TAG, "Session name: " + mSession.getName());
                        Log.i(TAG, "Session start: " + dateFormat.format(mSession.getStartTime(TimeUnit.MILLISECONDS)));
                        Log.i(TAG, "Session end: " + dateFormat.format(mSession.getEndTime(TimeUnit.MILLISECONDS)));
                    }
                    startWorkoutButton.setEnabled(true);
                    endWorkoutButton.setEnabled(false);
                    //Add any task that needs to be called
                    FitnessSensor.unregisterFitnessDataListener();
                    new TempAsyncTask().execute();
                } else {
                    Log.i(TAG, "Failed to stop session: " + sessionStopResult.getStatus().getStatusMessage());
                }
            }

        });
    }

    private class TempAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            //Log.i(TAG, "Size of mDataSourceList (MainActivity): " + FitnessSensor.mDataSourceList.size());
            //Log.i(TAG, "FitnessSensor Datasource: "+ FitnessSensor.mDataSourceList.get(0).toString());

            //for (DataSource dataSource: FitnessSensor.mDataSourceList) {
            //}
            /*
            DataSource fSDataSource = FitnessSensor.mDataSourceList.get(0);
            DataSource stepDataSource = new DataSource.Builder()
                    .setAppPackageName(fSDataSource.getAppPackageName())
                    .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                    .setName(SAMPLE_SESSION_NAME + "- step counts")
                    .setType(DataSource.TYPE_DERIVED)
                    .build();

            DataSet stepDataSet = DataSet.create(stepDataSource);
            DataPoint stepDataPoint = stepDataSet.createDataPoint().
                    setTimeInterval(mSession.getStartTime(TimeUnit.MILLISECONDS), mSession.getEndTime(TimeUnit.MILLISECONDS),
                            TimeUnit.MILLISECONDS);
            stepDataPoint.getValue(Field.FIELD_STEPS).setInt(Integer.parseInt(FitnessSensor.stepCountsStr));
            stepDataSet.add(stepDataPoint);
            FitnessDataHandler.dumpDataSet(stepDataSet);


            Session tsession = new Session.Builder()
                    .setName(SAMPLE_SESSION_NAME)
                    .setDescription("blah blah blah")
                    .setIdentifier(mSession.getIdentifier())
                    .setActivity(FitnessActivities.ON_FOOT)
                    .setStartTime(mSession.getStartTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                    .setEndTime(mSession.getEndTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
                    .build();
            dumpSession(tsession);
            */
            SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                    .setSession(mSession)
                    //.addDataSet(stepDataSet)
                    .build();

            Log.i(TAG, "Inserting the session in the History API");
            com.google.android.gms.common.api.Status insertStatus =
                    Fitness.SessionsApi.insertSession(mClient.getClient(), insertRequest)
                            .await(1, TimeUnit.MINUTES);

            // Before querying the session, check to see if the insertion succeeded.
            if (!insertStatus.isSuccess()) {
                Log.i(TAG, "There was a problem inserting the session: " +
                        insertStatus.getStatusMessage());
                return null;
            }

            // At this point, the session has been inserted and can be read.
            Log.i(TAG, "Session insert was successful!");

            // Begin by creating the query.
            SessionReadRequest readRequest = readFitnessSession();

            // [START read_session]
            // Invoke the Sessions API to fetch the session with the query and wait for the result
            // of the read request. Note: Fitness.SessionsApi.readSession() requires the
            // ACCESS_FINE_LOCATION permission.
            SessionReadResult sessionReadResult =
                    Fitness.SessionsApi.readSession(mClient.getClient(), readRequest)
                            .await(1, TimeUnit.MINUTES);

            // Get a list of the sessions that match the criteria to check the result.
            Log.i(TAG, "Session read was successful. Number of returned sessions is: "
                    + sessionReadResult.getSessions().size());
            for (Session session : sessionReadResult.getSessions()) {
                // Process the session
                dumpSession(session);

                // Process the data sets for this session
                List<DataSet> dataSets = sessionReadResult.getDataSet(session);
                for (DataSet dataSet : dataSets) {
                    FitnessDataHandler.dumpDataSet(dataSet);
                }
            }

            // [END read_session]
            return null;
        }

        @Override
        protected void onPostExecute(Void v){
            Log.i(TAG,"mSession is set to null ...");
            mSession = null;
        }
    }

    private SessionReadRequest readFitnessSession() {
        Log.i(TAG, "Reading History API results for session: " + SAMPLE_SESSION_NAME);
        // [START build_read_session_request]
        long startTime = mSession.getStartTime(TimeUnit.MILLISECONDS);
        long endTime = mSession.getEndTime(TimeUnit.MILLISECONDS);

        // Build a session read request
        SessionReadRequest readRequest = new SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                //.read(DataType.TYPE_ACTIVITY_SAMPLE)
                .read(DataType.TYPE_STEP_COUNT_DELTA)
                //.read(DataType.TYPE_CALORIES_EXPENDED)
                //.read(DataType.TYPE_DISTANCE_DELTA)
                .setSessionName(SAMPLE_SESSION_NAME)
                .build();
        // [END build_read_session_request]

        return readRequest;
    }

    private void dumpSession(Session session) {
        DateFormat dateFormat = getTimeInstance();
        Log.i(TAG, "Data returned for Session: " + session.getName()
                + "\n\tDescription: " + session.getDescription()
                + "\n\tStart: " + dateFormat.format(session.getStartTime(TimeUnit.MILLISECONDS))
                + "\n\tEnd: " + dateFormat.format(session.getEndTime(TimeUnit.MILLISECONDS)));
    }


    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        //mClient.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "onActivityResult..." + requestCode);
        mClient.getClient().connect();
        /*
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            Log.i(TAG, "resultCode: " + resultCode);
            Log.i(TAG, "resultCode == Activity.RESULT_OK: "+ (resultCode == Activity.RESULT_OK));
            if (resultCode == Activity.RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                buildFitnessClient();
            }
        }*/

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
            FitnessDataHandler.deleteData(mClient.getClient());
            return true;
        }
        else if (id == R.id.action_unregister_fitness_datalisteners){
            FitnessSensor.unregisterFitnessDataListener();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
