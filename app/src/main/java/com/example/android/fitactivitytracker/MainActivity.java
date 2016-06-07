package com.example.android.fitactivitytracker;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "FitActivityTracker";
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    public static final String SAMPLE_SESSION_NAME = "Test Session";

    private static final String AUTH_PENDING = "auth_state_pending";
    private static boolean authInProgress = false;

    private static StringBuilder activityInfo = new StringBuilder("Your Today Activity Summary: \n\n");
    private static StringBuilder todayFitInfo = new StringBuilder("Your Today Fitness Result: \n");
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

        if (!checkPermissions()) {
            requestPermissions();
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

    /*
    private void executeTask2() {
        ((EditText)findViewById(R.id.activity_duration_edit_text)).setOnEditorActionListener(
                new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                                actionId == EditorInfo.IME_ACTION_DONE ||
                                actionId == EditorInfo.IME_ACTION_GO ||
                                actionId == EditorInfo.IME_ACTION_NEXT ||
                                event.getAction() == KeyEvent.ACTION_DOWN &&
                                        event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                            //if (!event.isShiftPressed()) {
                            // the user is done typing.

                            //}
                            String[] goalStr = valAllEditText();
                            new ReadDataAndCheckGoalTask().execute(goalStr);
                            return true; // consume.
                        }
                        return false; // pass on to other listeners.
                    }
                });
    }
    */


    public void executeTask(View view) {
        String[] goalStr = valAllEditText();
        if (emptyEditTextCount > 0) {
            TextView textView = (TextView) findViewById(R.id.return_msg_text_view);
            textView.setText(emptyEditText.toString());
            emptyEditText = new StringBuilder("");
            emptyEditTextCount = 0;
        }
        else {
            new ReadDataAndCheckGoalTask().execute(goalStr);
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
        if (mClient == null && checkPermissions()){
            mClient = new GoogleApiClient.Builder(this)
                    .addApi(Fitness.RECORDING_API)
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Fitness.SESSIONS_API)
                    .addScope(new Scope(Scopes.FITNESS_LOCATION_READ_WRITE))
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                    .addConnectionCallbacks(
                            new ConnectionCallbacks() {
                                @Override
                                public void onConnected(Bundle bundle) {
                                    Log.i(TAG, "Connected!!!");
                                    Toast.makeText(getApplicationContext(),
                                            "GoogleApiClient Connected!!!",Toast.LENGTH_SHORT).show();
                                    Button checkGoalButton = (Button) findViewById(R.id.check_your_goal_button);
                                    checkGoalButton.setEnabled(true);
                                }

                                @Override
                                public void onConnectionSuspended(int i) {
                                    // If your connection to the sensor gets lost at some point,
                                    // you'll be able to determine the reason and react to it here.
                                    if (i == ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                        Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                    } else if (i == ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                        Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                    }
                                }
                            }
                    )
                    .enableAutoManage(this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.i(TAG, "Google Play services connection failed. Cause: " +
                                    result.toString());
                        }
                    })
                    .build();
        }
    }

    private class ReadDataAndCheckGoalTask extends AsyncTask<String[], Void, String[]> {

        @Override
        protected String[] doInBackground(String[]... params) {

            // Begin by creating the query.
            DataReadRequest readRequest = queryFitnessData();

            // [START read_dataset]
            // Invoke the History API to fetch the data with the query and await the result of
            // the read request.
            DataReadResult dataReadResult =
                    Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);
            // [END read_dataset]
            printData(dataReadResult);
            return checkGoal(dataReadResult, params[0]);
        }

        @Override
        protected void onPostExecute(String[] result) {
            //indices 0 - for step counts, 1 - for calories expended, 2 - for distance covered
            //currently only supports first two, [will fix it]
            String text1, text2, text3, text4;

            int numStepsDiff = Integer.valueOf(result[0]);
            float caloriesDiff = Float.valueOf(result[1]);
            int distanceDiff = Integer.valueOf(result[2]);
            long durationDiff = Long.valueOf(result[3]);
            Log.i(TAG,"numStepsDiff: "+ numStepsDiff);
            Log.i(TAG,"caloriesDiff: "+ caloriesDiff);
            Log.i(TAG,"distanceDiff: "+ distanceDiff);
            Log.i(TAG,"durationDiff: "+ distanceDiff);
            if (numStepsDiff <= 0) {
                text1 = "Yay, you've completed your goal for number of steps.\n";
            } else {
                text1 = "Your number of steps is " + numStepsDiff + " short of your goal.\n";
            }
            if (Float.compare(caloriesDiff,0) <= 0) {
                text2 = "Yay, you've completed your goal for calories expended.\n";
            } else {
                text2 = "Your number of calories expended is " + caloriesDiff + " short of your goal.\n";
            }
            if (distanceDiff <= 0) {
                text3 = "Yay, you've completed your goal for distance covered.\n";
            } else {
                text3 = "Your distance covered is " + distanceDiff + " short of your goal.\n";
            }
            if (durationDiff <= 0) {
                text4 = "Yay, you've completed your goal for activity duration.\n";
            } else {
                text4 = "Your activity duration is " + durationDiff + " short of your goal.\n";
            }
            TextView textView = (TextView) findViewById(R.id.return_msg_text_view);
            String returnMsg = text1 + text2 + text3 + text4 +
                    "\n" + todayFitInfo.toString() + "\n" + activityInfo.toString();
            textView.setText(returnMsg);
            clearAllGlobalVar();
        }
    }

    private void clearAllGlobalVar() {
        activityInfo = new StringBuilder("Your Today Activity Summary: \n\n");
        todayFitInfo = new StringBuilder("Your Today Fitness Result: \n");
    }

    public static DataReadRequest queryFitnessData() {
        // [START build_read_data_request]
        // Setting a start and end date using a range of 1 day before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        long startTime = cal.getTimeInMillis();

        java.text.DateFormat dateFormat = getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest = new DataReadRequest.Builder()

                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        // [END build_read_data_request]

        return readRequest;
    }

    public static void printData(DataReadResult dataReadResult) {
        // [START parse_read_data_result]
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: "
                    + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: "
                    + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpDataSet(dataSet);
            }
        }
        // [END parse_read_data_result]
    }

    public static String[] checkGoal(DataReadResult dataReadResult, String[] goal){
        List<String> strL = new ArrayList<String>(4);
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: "
                    + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    String str = checkGoalHelper(dataSet, goal);
                    strL.add(str);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: "
                    + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                String str = checkGoalHelper(dataSet, goal);
                strL.add(str);
            }
        }
        return strL.toArray(new String[4]);
    }

    private static String checkGoalHelper(DataSet dataSet, String[] goal){
        DataType dataType = dataSet.getDataType();
        List<DataPoint> datatPointsList = dataSet.getDataPoints();
        int dataPointsListSize = datatPointsList.size();

        if (dataPointsListSize > 0){

            if (dataPointsListSize == 1) {
                DataPoint dp = datatPointsList.get(0);
                Field field = dp.getDataType().getFields().get(0);
                Value value = dp.getValue(field);
                if (dataType.equals(DataType.TYPE_STEP_COUNT_DELTA)){
                    int val = Integer.valueOf(""+value);
                    int goalVal = Integer.valueOf(""+goal[0]);
                    todayFitInfo.append("Your total step counts: " + value + "\n");
                    return Integer.toString(goalVal-val);
                }
                else if (dataType.equals(DataType.TYPE_CALORIES_EXPENDED)) {
                    float val = Float.valueOf(""+value);
                    float goalVal = Float.valueOf(""+goal[1]);
                    todayFitInfo.append("Your total calories expended: " + value + "\n");
                    return Float.toString(goalVal-val);
                }
                else if (dataType.equals(DataType.TYPE_DISTANCE_DELTA)) {
                    int val = Integer.valueOf(""+value);
                    int goalVal = Integer.valueOf(""+goal[2]);
                    todayFitInfo.append("Your total distance covered: " + value + "\n");
                    return Integer.toString(goalVal-val);
                }
            }

            if (dataPointsListSize > 1) {
                DateFormat dateFormat = getTimeInstance();
                long activeMinutes = 0;
                for (DataPoint dp : datatPointsList) {
                    activityInfo.append("Start: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + "\n")
                    .append("End: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + "\n");

                    for (Field field: dp.getDataType().getFields()) {

                        if (field.getName().equals("duration")) {
                            long duration = TimeUnit.MINUTES.convert(Long.valueOf(""+dp.getValue(field))
                                    , TimeUnit.MILLISECONDS);
                            activeMinutes += duration;
                            activityInfo.append("Duration (Minutes): " + duration + "\n\n");
                        }
                        if (field.getName().equals("activity")) {
                            activityInfo.append("Activity: " +
                                    FitnessActivities.getName(Integer.valueOf(""+dp.getValue(field))) +"\n");
                        }
                    }
                }
                Long goalVal = Long.valueOf(""+goal[3]);
                todayFitInfo.append("Your total activity duration (minutes): " + activeMinutes + "\n");
                return Long.toString(goalVal-activeMinutes);
            }

        }
        else if (datatPointsList.size() == 0){
            if (dataType.equals(DataType.TYPE_STEP_COUNT_DELTA)){
                todayFitInfo.append("Your total step counts: 0\n");
                return goal[0];
            }
            else if (dataType.equals(DataType.TYPE_CALORIES_EXPENDED)) {
                todayFitInfo.append("Your total calories expended: 0.0\n");
                return goal[1];
            }
            else if (dataType.equals(DataType.TYPE_DISTANCE_DELTA)) {
                todayFitInfo.append("Your total distance covered: 0\n");
                return goal[2];
            }
            else {
                todayFitInfo.append("Your total activity duration (minutes): 0\n");
                return goal[3];
            }
        }
        return "error";
    }

    // [START parse_dataset]
    private static void dumpDataSet(DataSet dataSet) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = getTimeInstance();

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for(Field field : dp.getDataType().getFields()) {
                Log.i(TAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
            }
        }
    }

    private void deleteData() {
        Log.i(TAG, "Deleting today's step count data.");

        // [START delete_dataset]
        // Set a start and end time for our data, using a start time of 1 day before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        //cal.add(field, -1);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, 1);
        long startTime = cal.getTimeInMillis();

        //  Create a delete request object, providing a data type and a time interval
        DataDeleteRequest request = new DataDeleteRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED)
                .addDataType(DataType.TYPE_DISTANCE_DELTA)
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT)
                .build();

        // Invoke the History API with the Google API client object and delete request, and then
        // specify a callback that will check the result.
        Fitness.HistoryApi.deleteData(mClient, request)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Successfully deleted today's step count data.");
                        } else {
                            // The deletion will fail if the requesting app tries to delete data
                            // that it did not insert.
                            Log.i(TAG, "Failed to delete today's step count data.");
                        }
                    }
                });
        // [END delete_dataset]
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.main_activity_view),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
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
            deleteData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
