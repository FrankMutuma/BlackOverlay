package com.smarttechnologies.app.blackoverlay;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {

	private final MutableLiveData<String> currentTime = new MutableLiveData<>();
	private final MutableLiveData<String> currentDate = new MutableLiveData<>();

	public LiveData<String> getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(String time) {
		currentTime.setValue(time);
	}

	public LiveData<String> getCurrentDate() {
		return currentDate;
	}

	public void setCurrentDate(String date) {
		currentDate.setValue(date);
	}

}
