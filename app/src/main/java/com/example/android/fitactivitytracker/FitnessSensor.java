package com.example.android.fitactivitytracker;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by SawS on 6/10/16.
 */
public class FitnessSensor {
    public static final String TAG = "FitActivityTracker";

    private static GoogleApiClient fSClient = null;
    private static OnDataPointListener fSListener;
    public static List<DataSource> mDataSourceList = new ArrayList<DataSource>();

    public static void findFitnessDataSources(final GoogleApiClient mClient) {
        fSClient = mClient;

        // [START find_data_sources]
        // Note: Fitness.SensorsApi.findDataSources() requires the ACCESS_FINE_LOCATION permission.

        Fitness.SensorsApi.findDataSources(mClient, new DataSourcesRequest.Builder()
                // At least one datatype must be specified.

                .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA, DataType.TYPE_CALORIES_EXPENDED, DataType.TYPE_DISTANCE_DELTA,
                        DataType.TYPE_ACTIVITY_SEGMENT)

                // Can specify whether data type is raw or derived.
                .setDataSourceTypes(DataSource.TYPE_RAW, DataSource.TYPE_DERIVED)
                .build())
                .setResultCallback(new ResultCallback<DataSourcesResult>() {
                    @Override
                    public void onResult(DataSourcesResult dataSourcesResult) {
                        Log.i(TAG, "Result: " + dataSourcesResult.getStatus().toString());

                        mDataSourceList = dataSourcesResult.getDataSources();

                        Log.i(TAG, "Size of mDataSourceList (FitnessSensor): " + mDataSourceList.size());
                        for (DataSource dataSource : dataSourcesResult.getDataSources()) {
                            Log.i(TAG, "Data source found: " + dataSource.toString());
                            Log.i(TAG, "Data Source type: " + dataSource.getDataType().getName());

                            FitnessRecording.subscribe(mClient, dataSource.getDataType());

                            Log.i(TAG, "Data source for "+ dataSource.getDataType().getName()+ " found!  Registering.");

                            if (fSListener == null && dataSource.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA)) {
                                registerFitnessDataListener(dataSource);
                            }
                        }
                    }
                });

        // [END find_data_sources]
    }

    private static void registerFitnessDataListener(final DataSource dataSource) {
        // [START register_data_listener]

        final DataType dataType = dataSource.getDataType();

        fSListener = new OnDataPointListener() {
            @Override
            public void onDataPoint(DataPoint dataPoint) {
                for (Field field : dataPoint.getDataType().getFields()) {
                    Value val = dataPoint.getValue(field);
                    Log.i(TAG, "Detected DataPoint field: " + field.getName());
                    Log.i(TAG, "Detected DataPoint value: " + val);
                }
            }
        };

        Fitness.SensorsApi.add(
                fSClient,
                new SensorRequest.Builder()
                        .setDataSource(dataSource)
                        .setDataType(dataType)
                        .setSamplingRate(5, TimeUnit.SECONDS)
                        .build(),
                fSListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Listener registered! for: " + dataType.getName());

                        } else {
                            Log.i(TAG, "Listener not registered. " + dataType. getName());
                        }
                    }
                });
        // [END register_data_listener]
    }

    public static void unregisterFitnessDataListener() {
        if (fSListener == null) {
            // This code only activates one listener at a time.  If there's no listener, there's
            // nothing to unregister.
            return;
        }

        // [START unregister_data_listener]
        // Waiting isn't actually necessary as the unregister call will complete regardless,
        // even if called from within onStop, but a callback can still be added in order to
        // inspect the results.

        Fitness.SensorsApi.remove(
                fSClient,
                fSListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Listener was removed!");
                        } else {
                            Log.i(TAG, "Listener was not removed.");
                        }
                    }
                });
        // [END unregister_data_listener]
    }
}
