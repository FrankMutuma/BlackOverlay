package com.smarttechnologies.app.blackoverlay;
import androidx.lifecycle.LiveData;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;

public class SharedViewModel {
	private static final String TAG = "SharedPreferences";

	private static final MutableLiveData<Boolean> isOverlayServiceRunning = new MutableLiveData<>();
	//initialize default values
	static {
		isOverlayServiceRunning.setValue(false);

	}

	public static void setOverlayServiceRunningStatus(Boolean state) {
		Log.i(TAG, "OverlayServiceState set to :" + state);
		isOverlayServiceRunning.setValue(state);
	}

	public static LiveData<Boolean> getOverlayServiceRunningStatus() {
		return isOverlayServiceRunning;
	}

}