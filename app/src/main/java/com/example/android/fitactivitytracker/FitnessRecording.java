package com.example.android.fitactivitytracker;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.fitness.result.ListSubscriptionsResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SawS on 6/12/16.
 */
public class FitnessRecording {
    public static final String TAG = "FitActivityTracker";
    private static List<DataType> mDataTypeList = new ArrayList<DataType>();

    public static void subscribe(GoogleApiClient mClient, final DataType dataType) {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        // [START subscribe_to_datatype]
        Fitness.RecordingApi.subscribe(mClient, dataType)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {

                            mDataTypeList.add(dataType);

                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                //Log.i(TAG, "Existing subscription for activity detected.");
                            } else {
                                //Log.i(TAG, "Successfully subscribed! " + dataType.getName());
                            }
                        } else {
                            //Log.i(TAG, "There was a problem subscribing... " + dataType.getName());
                        }
                    }
                });
        // [END subscribe_to_datatype]
    }

    /**
     * Fetch a list of all active subscriptions and log it. Since the logger for this sample
     * also prints to the screen, we can see what is happening in this way.
     */
    public static void dumpSubscriptionsList(GoogleApiClient mClient) {
        // [START list_current_subscriptions]
        for (DataType dataType: mDataTypeList) {
            Fitness.RecordingApi.listSubscriptions(mClient, dataType)
                    // Create the callback to retrieve the list of subscriptions asynchronously.
                    .setResultCallback(new ResultCallback<ListSubscriptionsResult>() {
                        @Override
                        public void onResult(ListSubscriptionsResult listSubscriptionsResult) {
                            for (Subscription sc : listSubscriptionsResult.getSubscriptions()) {
                                DataType dt = sc.getDataType();
                                Log.i(TAG, "Active subscription for data type: " + dt.getName());
                            }
                        }
                    });
        }

        // [END list_current_subscriptions]
    }

    public static void cancelSubscription(GoogleApiClient mClient) {

        for (DataType dataType: mDataTypeList) {
            final String dataTypeStr = dataType.toString();
            // Invoke the Recording API to unsubscribe from the data type and specify a callback that
            // will check the result.
            // [START unsubscribe_from_datatype]
            Fitness.RecordingApi.unsubscribe(mClient, dataType)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "Successfully unsubscribed for data type: " + dataTypeStr);
                            } else {
                                // Subscription not removed
                                Log.i(TAG, "Failed to unsubscribe for data type: " + dataTypeStr);
                            }
                        }
                    });
        }
        mDataTypeList.clear();
        // [END unsubscribe_from_datatype]
    }
}
