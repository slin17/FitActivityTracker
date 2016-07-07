package com.example.android.fitactivitytracker;

import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getTimeInstance;

/**
 * Created by SawS on 6/7/16.
 */
public class FitnessGoalChecker {
    public static String[] checkGoal(DataReadResult dataReadResult, String[] goal, StringBuilder activityInfo,
                                     StringBuilder todayFitInfo){

        List<String> strL = new ArrayList<String>(4);
        if (dataReadResult.getBuckets().size() > 0) {
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    String str = checkGoalHelper(dataSet, goal, activityInfo, todayFitInfo);
                    strL.add(str);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                String str = checkGoalHelper(dataSet, goal, activityInfo, todayFitInfo);
                strL.add(str);
            }
        }
        return strL.toArray(new String[4]);
    }

    private static String checkGoalHelper(DataSet dataSet, String[] goal,
                                          StringBuilder activityInfo, StringBuilder todayFitInfo){
        DataType dataType = dataSet.getDataType();
        List<DataPoint> datatPointsList = dataSet.getDataPoints();
        int dataPointsListSize = datatPointsList.size();

        if (dataPointsListSize > 0){

            if (dataPointsListSize == 1) {
                DataPoint dp = datatPointsList.get(0);
                Field field = dp.getDataType().getFields().get(0);
                Value value = dp.getValue(field);
                if (dataType.equals(DataType.TYPE_STEP_COUNT_DELTA)){
                    int val = Integer.parseInt(""+value);
                    int goalVal = Integer.parseInt(""+goal[0]);
                    todayFitInfo.append("Your total step counts: " + value + "\n");
                    return Integer.toString(goalVal-val);
                }
                else if (dataType.equals(DataType.TYPE_CALORIES_EXPENDED)) {
                    float val = Float.parseFloat(""+value);
                    float goalVal = Float.parseFloat(""+goal[1]);
                    todayFitInfo.append("Your total calories expended: " + value + "\n");
                    return Float.toString(goalVal-val);
                }
                else if (dataType.equals(DataType.TYPE_DISTANCE_DELTA)) {
                    float val = Float.parseFloat(""+value);
                    float goalVal = Float.parseFloat(""+goal[2]);
                    todayFitInfo.append("Your total distance covered: " + value + "\n");
                    return Float.toString(goalVal-val);
                }
            }

            if (dataType.equals(DataType.AGGREGATE_ACTIVITY_SUMMARY)) {
                DateFormat dateFormat = getTimeInstance();
                long activeMinutes = 0;
                for (int i = 1; i < dataPointsListSize; i++) {
                    DataPoint dp = datatPointsList.get(i);
                    activityInfo.append("Start: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + "\n")
                            .append("End: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + "\n");

                    for (Field field: dp.getDataType().getFields()) {

                        if (field.getName().equals("duration")) {
                            long duration = TimeUnit.MINUTES.convert(Long.parseLong(""+dp.getValue(field))
                                    , TimeUnit.MILLISECONDS);
                            activeMinutes += duration;
                            activityInfo.append("Duration (Minutes): " + duration + "\n\n");
                        }
                        if (field.getName().equals("activity")) {
                            activityInfo.append("Activity: " +
                                    FitnessActivities.getName(Integer.parseInt(""+dp.getValue(field))) +"\n");
                        }
                    }
                }
                Long goalVal = Long.parseLong(""+goal[3]);
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
}
