package com.smarttechnologies.app.blackoverlay;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
	// A LiveData object to hold the current time
	private final MutableLiveData<String> currentTime = new MutableLiveData<>();

	public void setCurrentTime(String time) {
		currentTime.setValue(time);
	}

	public MutableLiveData<String> getCurrentTime() {
		return currentTime;
	}
}