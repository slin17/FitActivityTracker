package com.example.android.fitactivitytracker;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
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
    public static final String SAMPLE_SESSION_NAME = "Today Workout";

    private static final String AUTH_PENDING = "auth_state_pending";
    private static boolean authInProgress = false;

    private static int checkForFlag;

    private static int goalPeriod;

    public static GoogleApiClient mClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        ((EditText)findViewById(R.id.val_check_goal_edit_text)).setOnEditorActionListener(
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
                            buildFitnessClient();
                            return true; // consume.
                        }
                        return false; // pass on to other listeners.
                    }
                });
    }

    /*
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?

        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.days_radio_button:
                if (checked)
                    checkForFlag = 0;
                    break;
            case R.id.weeks_radio_button:
                if (checked)
                    checkForFlag = 1;
                    break;

            case R.id.months_radio_button:
                if (checked)
                    checkForFlag = 2;
                    break;

            case R.id.years_radio_button:
                if (checked)
                    checkForFlag = 3;
                    break;
        }
    }
    */

    private String[] valAllEditText(){
        //to check if the user has entered values into the edittext fields for number of steps, calories expended
        //and distance traveled
        EditText numStepsET = (EditText) findViewById(R.id.num_steps_edit_text);
        String numStepsStr = numStepsET.getText().toString();
        EditText calExpended = (EditText) findViewById(R.id.calories_expended_edit_text);
        String caloriesStr = calExpended.getText().toString();
        //EditText distanceET = (EditText) findViewById(R.id.distance_traveled_edit_text);
        //String distanceStr = distanceET.toString();
        //String[] retStr = {numStepsStr, caloriesStr, distanceStr};
        String[] retStr = {numStepsStr, caloriesStr};
        return retStr;
    }

    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
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
                                // Now you can make calls to the Fitness APIs.  What to do?
                                // Look at some data!!
                                EditText valCheckGoalET = (EditText) findViewById(R.id.val_check_goal_edit_text);
                                String valCGS = valCheckGoalET.getText().toString();
                                String[] goalStr = valAllEditText();
                                goalPeriod = Integer.valueOf(valCGS);
                                new ReadDataAndCompareTask().execute(goalStr);
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

    private class ReadDataAndCompareTask extends AsyncTask<String[], Void, String[]> {

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
            return hasCheckedGoal(dataReadResult, params[0]);
        }

        @Override
        protected void onPostExecute(String[] result) {
            //indices 0 - for step counts, 1 - for calories expended, 2 - for distance covered
            //currently only supports first two, [will fix it]
            String text1;
            String text2;
            int numStepsDiff = Integer.valueOf(result[0]);
            float caloriesDiff = Float.valueOf(result[1]);
            Log.i(TAG,"numStepsDiff: "+ numStepsDiff);
            Log.i(TAG,"caloriesDiff: "+ caloriesDiff);
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
            TextView textView = (TextView) findViewById(R.id.return_msg_text_view);
            String returnMsg = text1+text2;
            textView.setText(returnMsg);
        }
    }

    public static DataReadRequest queryFitnessData() {
        // [START build_read_data_request]
        // Setting a start and end date using a range of 1 day before this moment.
        /*int field;
        switch (checkForFlag) {
            case 0:
                field = Calendar.DAY_OF_YEAR;
                break;
            case 1:
                field = Calendar.WEEK_OF_YEAR;
                break;
            case 2:
                field = Calendar.MONTH;
                break;
            case 3:
                field = Calendar.YEAR;
                break;
            default:
                field = Calendar.DAY_OF_YEAR;
                break;
        }*/
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        //cal.add(field, -1);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -goalPeriod);
        long startTime = cal.getTimeInMillis();

        java.text.DateFormat dateFormat = getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest = new DataReadRequest.Builder()

                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                //.aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)

                .bucketByTime(goalPeriod, TimeUnit.DAYS)
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

    public static String[] hasCheckedGoal(DataReadResult dataReadResult, String[] goal){
        //int retVal = -1;
        //List<Integer> retVList = new ArrayList<Integer>();
        //int i = 0;
        List<String> strL = new ArrayList<String>(2);
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: "
                    + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    String str = tempCheckGoal(dataSet, goal);
                    strL.add(str);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: "
                    + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                String str = tempCheckGoal(dataSet, goal);
                strL.add(str);
            }
        }
        return strL.toArray(new String[2]);
    }

    //tempoaray checkGoal function - only check for total accumulated values for each dataSet
    //needs support for comparing goals with the dataSet bucketed by each day
    private static String tempCheckGoal(DataSet dataSet, String[] goal){
        //no need for "for" loop to loop through each data point
        //because we know there will only be one data point, the total of all, for e.g., step counts,
        // for the number of days, provided by the user
        DataType dataType = dataSet.getDataType();
        List<DataPoint> dpL = dataSet.getDataPoints();
        if (dpL.size() > 0){
            DataPoint dp = dpL.get(0);
            //also there is only one field, by assumption
            Field field = dp.getDataType().getFields().get(0);
            Value value = dp.getValue(field);
            if (dataType.equals(DataType.TYPE_STEP_COUNT_DELTA)){
                int val = Integer.valueOf(""+value);
                int goalVal = Integer.valueOf(""+goal[0]);
                return Integer.toString(goalVal-val);
            } else if (dataType.equals(DataType.TYPE_CALORIES_EXPENDED)) {
                float val = Float.valueOf(""+value);
                float goalVal = Float.valueOf(""+goal[1]);
                return Float.toString(goalVal-val);
            }
        } else if (dpL.size() == 0){
            if (dataType.equals(DataType.TYPE_STEP_COUNT_DELTA)){
                return goal[0];
            } else if (dataType.equals(DataType.TYPE_CALORIES_EXPENDED)) {
                return goal[1];
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
        cal.add(Calendar.DAY_OF_YEAR, -goalPeriod);
        long startTime = cal.getTimeInMillis();

        //  Create a delete request object, providing a data type and a time interval
        DataDeleteRequest request = new DataDeleteRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED)
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
