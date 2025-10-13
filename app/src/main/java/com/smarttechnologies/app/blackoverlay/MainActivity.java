package com.smarttechnologies.app.blackoverlay;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayoutMediator;

// Implementing the callback for permission results
public class MainActivity extends AppCompatActivity implements PermissionManager.PermissionCallback {

	private static final String TAG = "MainActivity";

	// Architecture Components (ClockUtils is NOT an instance here)
	private ExtendedFloatingActionButton mainStartButton;
	private PermissionManager permissionManager;
	private AppPreferencesManager prefsManager;

	// UI Elements
	private TextView timeTextView;
	private TextView dateDayTextView;
	private TextView debugTextView;
	private ViewPager2 viewPager;

	// State Tracking
	private boolean isServiceRunning;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 1. Initialize Architecture Components (ClockUtils is static, so no instance)
		prefsManager = AppPreferencesManager.getInstance(this);
		permissionManager = new PermissionManager(this, this);
		// 2. Setup UI, Listeners, and Navigation
		setupUI();

		// 3. Setup Observers for LiveData (using static ClockUtils methods)
		observeLiveData();

		// 4. Initial Permission Check (Starts the flow)
		permissionManager.checkAndRequestPermissions();
	}

	private void setupUI() {
		viewPager = findViewById(R.id.view_pager);
		TabLayout tabLayout = findViewById(R.id.tab_layout);
		mainStartButton = findViewById(R.id.fab_start);
		dateDayTextView = findViewById(R.id.main_date_and_day);
		timeTextView = findViewById(R.id.main_time);
		debugTextView = findViewById(R.id.txtdebug);

		// --- ViewPager2 and TabLayout Setup ---
		ViewPagerAdapter adapter = new ViewPagerAdapter(this); // Assuming this class exists
		viewPager.setAdapter(adapter);

		// Connect the TabLayout to the ViewPager2 and set tab titles
		new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
			if (position == 0) {
				tab.setText(getString(R.string.tab_look_feel));
			} else {
				tab.setText(getString(R.string.tab_settings));
			}
		}).attach();

		// Set the FAB click listener
		if (mainStartButton != null) {
			updateMainFabUI();

			mainStartButton.setOnClickListener(v -> {
				if (isServiceRunning) {
					stopFloatingService();
				} else {
					if (permissionManager.hasOverlayPermission()) {
						startFloatingService();
					} else {
						Toast.makeText(this, getString(R.string.permission_required_overlay), Toast.LENGTH_SHORT)
								.show();
						permissionManager.checkAndRequestPermissions();
					}
				}
			});
		}
	}

	/**
	 * Links the UI TextViews to the static LiveData in ClockUtils.
	 */
	private void observeLiveData() {
		Log.i(TAG, "Observe overlay activity state");
		SharedViewModel.getOverlayServiceRunningStatus().observe(this, status -> {
			isServiceRunning = status;
			updateMainFabUI();
		});

		Log.i(TAG, "Observe static clock LiveData called");

		// Observe the static time LiveData from ClockUtils
		ClockUtils.getTimeLiveData().observe(this, newTime -> {
			if (timeTextView != null) {
				timeTextView.setText(newTime);
				timeTextView.setContentDescription("Current time is " + newTime);
			}
		});

		// Observe the static date LiveData from ClockUtils
		ClockUtils.getDateDayLiveData().observe(this, newDate -> {
			if (dateDayTextView != null) {
				dateDayTextView.setText(newDate);
				dateDayTextView.setContentDescription("Today's date is " + newDate);
			}
		});
	}

	/**
	 * Updates the FAB text and icon based on the actual service running state.
	 */
	private void updateMainFabUI() {
		if (mainStartButton == null)
			return;

		if (isServiceRunning) {
			mainStartButton.setText(getString(R.string.stop));
			mainStartButton.setIconResource(R.drawable.ic_stop_white_24dp);
			mainStartButton.setContentDescription(getString(R.string.stop));
		} else {
			mainStartButton.setText(getString(R.string.start));
			mainStartButton.setIconResource(R.drawable.ic_play_arrow_white_24dp);
			mainStartButton.setContentDescription(getString(R.string.start));
		}
	}

	//--- Service Control Methods ---//

	private void startFloatingService() {
		Intent serviceIntent = new Intent(this, FloatingButtonService.class);
		try {
			startForegroundService(serviceIntent);
			Toast.makeText(this, getString(R.string.service_started_toast), Toast.LENGTH_SHORT).show();

		} catch (Exception e) {
			Log.e(TAG, "Error Occured While Starting the Floating Button " + e);
		}
	}

	private void stopFloatingService() {
		Intent serviceIntent = new Intent(this, FloatingButtonService.class);

		try {
			stopService(serviceIntent);
			Toast.makeText(this, getString(R.string.service_stopped_toast), Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Log.e(TAG, "Error Occured While Hidding the Floating Button " + e);
		}
	}

	//--- PermissionCallback Methods ---//

	@Override
	public void onAllPermissionsGranted() {
		Toast.makeText(this, getString(R.string.permissions_all_granted), Toast.LENGTH_SHORT).show();
		startFloatingService();
	}

	@Override
	public void onEssentialPermissionGranted() {
		if (debugTextView != null)
			debugTextView.setText(R.string.permissions_essential_granted);// + "\n click here to grant the WriteSettings  permission");

		Toast.makeText(this, getString(R.string.permissions_essential_granted), Toast.LENGTH_SHORT).show();
		startFloatingService();
	}

	@Override
	public void onPermissionsDenied() {
		if (debugTextView != null)
			debugTextView.setText(R.string.permissions_denied_overlay);

		Toast.makeText(this, getString(R.string.permissions_denied_overlay), Toast.LENGTH_LONG).show();
	}

	//--- Activity Lifecycle Methods ---//

	@Override
	protected void onResume() {
		super.onResume();

		// RESTORED: Using the static observer registration for ClockUtils
		ClockUtils.registerObserver();

		// check service status
		updateMainFabUI();
	}

	@Override
	protected void onPause() {
		// RESTORED: Using the static observer unregistration for ClockUtils
		ClockUtils.unregisterObserver();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		permissionManager.clearCallback();
	}
}