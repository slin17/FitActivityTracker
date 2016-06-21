package com.example.android.fitactivitytracker;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.util.concurrent.TimeUnit;

/**
 * Created by SawS on 6/7/16.
 */
public class ReadDataAndCheckGoal {
    public static final String TAG = "FitActivityTracker";
    private static Activity mActivity = null;
    private static GoogleApiClient apiClient = null;
    private static StringBuilder activityInfo = new StringBuilder("Your Today Activity Summary: \n\n");
    private static StringBuilder todayFitInfo = new StringBuilder("Your Today Fitness Result: \n");

    public static void ReadDataAndCheckGoalExecute
            (final Activity activity, GoogleApiClient mClient, String[] goalStr) {
        mActivity = activity;
        apiClient = mClient;
        new ReadDataAndCheckGoalTask().execute(goalStr);
    }

    private static class ReadDataAndCheckGoalTask extends AsyncTask<String[], Void, String[]> {

        @Override
        protected String[] doInBackground(String[]... params) {

            // Begin by creating the query.
            DataReadRequest readRequest = FitnessDataHandler.queryFitnessData(MainActivity.mSession);

            // [START read_dataset]
            // Invoke the History API to fetch the data with the query and await the result of
            // the read request.
            DataReadResult dataReadResult =
                    Fitness.HistoryApi.readData(apiClient, readRequest).await(1, TimeUnit.MINUTES);
            // [END read_dataset]
            FitnessDataHandler.printData(dataReadResult);
            return FitnessGoalChecker.checkGoal(dataReadResult, params[0], activityInfo, todayFitInfo);
        }

        @Override
        protected void onPostExecute(String[] result) {
            String text1, text2, text3, text4;

            int numStepsDiff = Integer.parseInt(result[0]);
            float caloriesDiff = Float.parseFloat(result[1]);
            float distanceDiff = Float.parseFloat(result[2]);
            long durationDiff = Long.parseLong(result[3]);
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
            if (Float.compare(distanceDiff,0) <= 0) {
                text3 = "Yay, you've completed your goal for distance covered.\n";
            } else {
                text3 = "Your distance covered is " + distanceDiff + " short of your goal.\n";
            }
            if (durationDiff <= 0) {
                text4 = "Yay, you've completed your goal for activity duration.\n";
            } else {
                text4 = "Your activity duration is " + durationDiff + " short of your goal.\n";
            }
            TextView textView = (TextView) mActivity.findViewById(R.id.return_msg_text_view);
            String returnMsg = text1 + text2 + text3 + text4 +
                    "\n" + todayFitInfo.toString() + "\n" + activityInfo.toString();
            textView.setText(returnMsg);
            clearAllGlobalVar();
        }
    }

    public static void clearAllGlobalVar() {
        activityInfo = new StringBuilder("Your Today Activity Summary: \n\n");
        todayFitInfo = new StringBuilder("Your Today Fitness Result: \n");
    }

}
