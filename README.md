# FitActivityTracker (Android App)

Description:
A Fitness Activity Tracker App, using Google Fit API
Documentation Home Page: https://developers.google.com/fit/

App UI: https://drive.google.com/file/d/0B1LupCigXvHGZHZCZnhxOE1TbnM/view?pref=2&pli=1

"Check Your Goal" Button: If no workout session is created, it will check your fitness goals against "today" fitness data

"Start WorkOut" Button: Starts a new workout session.

"End WorkOut" Button: Stops an existing workout session.

[Jun 25, 2016] (Sat)
- Finished Adding SensorApi to find dataSources to subscribe to so that the user's fitness data
are tracked
- Using RecordingApi to subscribe to the desired dataTypes 
- Incorporated SessionApi so that the user can start and end workout sessions 
- Now supports checking goals against the "today" data or against workout sessions 

[Jun 7, 2016] (Tue)
- Using Google Fit HistoryApi, implemented functions to read today's fitness data 
  from Google Fitness Store, show the data to the user in a properly formatted fashion
- Currently only supports reading Fitness Data and checking if the user has reached 
  today's goals


Known Issues (To be Fixed Later):

- onActivityResult in MainActivity is being called with the wrong request code
- Better Activity Lifecycle Management (Somehow Resolved)
	- Currently, it's not guaranteed that there would be no bug with GoogleApiCient and ending a workout session
		if the user decides to move the app to the background stack, after a workout session is started 
	- Known error: The "Stop Workout" button is enabled before the GoogleApiClient is re-connected again