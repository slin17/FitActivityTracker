package com.example.android.fitactivitytracker;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.fitness.result.SessionStopResult;

import java.text.DateFormat;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getTimeInstance;

/**
 * Created by SawS on 6/21/16.
 */
public class FitnessSession {
    public static final String TAG = "FitActivityTracker";
    public static Session mSession;
    //public final String SAMPLE_SESSION_NAME = "Test Session";
    private static FitApiClient mRClient = null;
    private static Activity mActivity;

    public static void refreshSession() {
        mSession = null;
    }

    public static void startSession(final Activity activity, FitApiClient mClient) {
        mRClient = mClient;
        mActivity = activity;
        mSession = new Session.Builder()
                //.setName(SAMPLE_SESSION_NAME)
                .setIdentifier(activity.getString(R.string.app_name) + " " + System.currentTimeMillis())
                .setStartTime(System.currentTimeMillis()- TimeUnit.SECONDS.toMillis(1), TimeUnit.MILLISECONDS)
                .build();
        PendingResult<Status> pendingResult =
                Fitness.SessionsApi.startSession(mClient.getClient(),mSession);

        pendingResult.setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Successfully started session");
                            setButtonsStartS(activity);
                        } else {
                            Log.i(TAG, "Failed to start session: " + status.getStatusMessage());
                        }
                    }
                }
        );
    }

    private static void setButtonsStartS(final Activity activity) {
        Button endWorkOutButton = (Button) activity.findViewById(R.id.end_workout_button);
        Button startWorkOutButton = (Button) activity.findViewById(R.id.start_workout_button);
        endWorkOutButton.setEnabled(true);
        startWorkOutButton.setEnabled(false);
    }

    public static void stopSession() {
        PendingResult<SessionStopResult> pendingResult =
                Fitness.SessionsApi.stopSession(mRClient.getClient(), mSession.getIdentifier());

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
                    setButtonsEndS();
                    new InsertSessionTask().execute();
                } else {
                    Log.i(TAG, "Failed to stop session: " + sessionStopResult.getStatus().getStatusMessage());
                }
            }

        });
    }

    private static void setButtonsEndS() {
        Button endWorkOutButton = (Button) mActivity.findViewById(R.id.end_workout_button);
        Button startWorkOutButton = (Button) mActivity.findViewById(R.id.start_workout_button);
        endWorkOutButton.setEnabled(false);
        startWorkOutButton.setEnabled(true);
    }

    private static class InsertSessionTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                    .setSession(mSession)
                    .build();

            Log.i(TAG, "Inserting the session in the History API");
            com.google.android.gms.common.api.Status insertStatus =
                    Fitness.SessionsApi.insertSession(mRClient.getClient(), insertRequest)
                            .await(1, TimeUnit.MINUTES);

            // Before querying the session, check to see if the insertion succeeded.
            if (!insertStatus.isSuccess()) {
                Log.i(TAG, "There was a problem inserting the session: " +
                        insertStatus.getStatusMessage());
                return null;
            }

            // At this point, the session has been inserted and can be read.
            Log.i(TAG, "Session insert was successful!");

            DataReadRequest readRequest = FitnessDataHandler.queryFitnessData(mSession);
            DataReadResult dataReadResult =
                    Fitness.HistoryApi.readData(mRClient.getClient(), readRequest).await(1, TimeUnit.MINUTES);

            FitnessDataHandler.printData(dataReadResult);
            return null;
        }

        @Override
        protected void onPostExecute(Void v){
            //mSession = null;
        }
    }

    private static SessionReadRequest readFitnessSession() {
        //Log.i(TAG, "Reading History API results for session: " + SAMPLE_SESSION_NAME);
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
                //.setSessionName(SAMPLE_SESSION_NAME)aa
                .build();
        // [END build_read_session_request]

        return readRequest;
    }

    private static void dumpSession(Session session) {
        DateFormat dateFormat = getTimeInstance();
        Log.i(TAG, "Data returned for Session: " + session.getName()
                + "\n\tDescription: " + session.getDescription()
                + "\n\tStart: " + dateFormat.format(session.getStartTime(TimeUnit.MILLISECONDS))
                + "\n\tEnd: " + dateFormat.format(session.getEndTime(TimeUnit.MILLISECONDS)));
    }
}
