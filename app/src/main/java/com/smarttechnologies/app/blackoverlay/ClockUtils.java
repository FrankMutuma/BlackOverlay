package com.smarttechnologies.app.blackoverlay;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockUtils {
	private static final String TAG = "ClockUtils";

	// 1. LiveData is now owned by the Singleton
	private static final MutableLiveData<String> sTimeLiveData = new MutableLiveData<>();
	private static final MutableLiveData<String> sDateDayLiveData = new MutableLiveData<>();

	// 2. The SharedViewModel reference is no longer needed
	// private static SharedViewModel sSharedViewModel;

	private static final Handler sHandler = new Handler(Looper.getMainLooper());
	private static boolean sIsTimerRunning = false;
	private static int sObserverCount = 0;

	// Formatting constants (moved from SharedViewModel)
	private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm", Locale.getDefault());
	private static final SimpleDateFormat DATE_DAY_FORMAT = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());

	// Runnable to update time every second
	private static final Runnable sTimeUpdaterRunnable = new Runnable() {
		@Override
		public void run() {
			Date now = new Date();
			// 3. Update the static LiveData directly
			sTimeLiveData.setValue(TIME_FORMAT.format(now));
			sDateDayLiveData.setValue(DATE_DAY_FORMAT.format(now));

			sHandler.postDelayed(this, 1000); // Repeat every second
		}
	};

	// No longer takes SharedViewModel, only checks if LiveData is available.
	public static void initialize() {
		Log.i(TAG, "ClockUtils instance created.");

		// This check is mainly for robust logging/debugging, LiveData is static.
		if (sTimeLiveData.getValue() == null) {
			Log.i(TAG, "Singleton successfully initialized. Initializing time.");
			// Set initial time immediately
			Date now = new Date();
			sTimeLiveData.setValue(TIME_FORMAT.format(now));
			sDateDayLiveData.setValue(DATE_DAY_FORMAT.format(now));
		} else {
			Log.w(TAG, "Attempted to re-initialize ClockUtils. Skipping time initialization.");
		}
	}

	public static LiveData<String> getTimeLiveData() {
		return sTimeLiveData;
	}

	public static LiveData<String> getDateDayLiveData() {
		return sDateDayLiveData;
	}

	public static void registerObserver() {
		sObserverCount++;
		Log.d(TAG, "Observer registered. Count: " + sObserverCount);

		if (!sIsTimerRunning && sObserverCount > 0) {
			startTimer();
		}
	}

	public static void unregisterObserver() {
		if (sObserverCount > 0) {
			sObserverCount--;
		}
		Log.d(TAG, "Observer unregistered. Count: " + sObserverCount);

		if (sIsTimerRunning && sObserverCount == 0) {
			stopTimer();
		}
	}

	private static void startTimer() {
		sIsTimerRunning = true;
		sHandler.post(sTimeUpdaterRunnable);
		Log.i(TAG, "Timer running (REF-COUNTED)");
	}

	private static void stopTimer() {
		sHandler.removeCallbacks(sTimeUpdaterRunnable);
		sIsTimerRunning = false;
		Log.i(TAG, "Timer stopped (REF-COUNTED)");
	}

	// Helper to log the current timer status
	public static boolean isTimerActive() {
		return sIsTimerRunning;
	}
}