package com.smarttechnologies.app.blackoverlay;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements PermissionManager.PermissionCallback {

	private ExtendedFloatingActionButton mainStartButton;
	private PermissionManager permissionManager;
	private AppPreferencesManager prefsManager;
	private SharedViewModel sharedViewModel;
	private Handler handler;
	private Runnable updateTimeRunnable;
	private BroadcastReceiver serviceStateReceiver;
	ViewPager2 viewPager;
	//declare time and date textviews
	private TextView dateDayTextView;
	private TextView timeTextView;

	// New member variable to track the service's state
	private boolean isServiceRunning = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		prefsManager = AppPreferencesManager.getInstance(this);
		// Initialize the PermissionManager with this activity and callback
		permissionManager = new PermissionManager(this, this);
		// Get the shared ViewModel instance
		sharedViewModel = new ViewModelProvider(this).get(SharedViewModel.class);

		// Initialize the handler for the clock
		handler = new Handler(Looper.getMainLooper());

		setupUI();
		setupReceiver();

		// Check and request permissions as the first order of business
		permissionManager.checkAndRequestPermissions();
	}

	private void setupUI() {
		// Find the ViewPager2 and TabLayout from the layout file
		viewPager = findViewById(R.id.view_pager);
		TabLayout tabLayout = findViewById(R.id.tab_layout);
		mainStartButton = findViewById(R.id.fab_start);
		// Create an instance of our custom ViewPagerAdapter
		ViewPagerAdapter adapter = new ViewPagerAdapter(this);
		//initialize date and time text views
		dateDayTextView = findViewById(R.id.activity_main_date);
		timeTextView = findViewById(R.id.activity_main_time);

		// Set the adapter on the ViewPager2
		viewPager.setAdapter(adapter);

		// Connect the TabLayout to the ViewPager2 and set tab titles
		new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
			if (position == 0) {
				tab.setText("Look & Feel");
			} else {
				tab.setText("Settings");
			}
		}).attach();
		//Start updating time
		//clockUtils.startUpdatingTime(timeTextView,dateDayTextView);

		// Set the FAB click listener
		mainStartButton.setOnClickListener(v -> {
			// Check the current state of the service to decide whether to start or stop it
			if (isServiceRunning) {
				// If the service is running, stop it
				stopFloatingService();

			} else {
				// If the service is not running, start it
				// Check if we have at least overlay permission before starting service
				if (permissionManager.hasOverlayPermission()) {
					startFloatingService();
				} else {
					Toast.makeText(this, "Please grant overlay permission first", Toast.LENGTH_SHORT).show();
					permissionManager.checkAndRequestPermissions();
				}
			}
		});
	}

	private void setupReceiver() {
		serviceStateReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (FloatingButtonService.ACTION_SERVICE_STATE_CHANGED.equals(intent.getAction())) {
					boolean isRunning = intent.getBooleanExtra("is_running", false);
					isServiceRunning = isRunning; // Update the state variable
					if (isRunning) {
						// Access the member variable here
						mainStartButton.setText(R.string.stop);
					} else {
						// Access the member variable here
						mainStartButton.setText(R.string.start);
					}
				}
			}
		};
	}

	private void startUpdatingTime() {
		updateTimeRunnable = new Runnable() {
			@Override
			public void run() {
				// Get current time and date strings
				SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
				String currentTime = timeFormat.format(new Date());

				SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
				String currentDate = dateFormat.format(new Date());

				// Update the LiveData for Fragments
				sharedViewModel.setCurrentTime(currentTime);

				// Update the Activity's TextViews directly
				if (timeTextView != null) {
					timeTextView.setText(currentTime);
				}
				if (dateDayTextView != null) {
					dateDayTextView.setText(currentDate);
				}

				// Schedule the next update
				handler.postDelayed(this, 1000);
			}
		};
		handler.post(updateTimeRunnable);
	}

	private void stopUpdatingTime() {
		if (handler != null && updateTimeRunnable != null) {
			handler.removeCallbacks(updateTimeRunnable);
		}
	}

	//--- PermissionCallback Methods ---//
	@Override
	public void onAllPermissionsGranted() {
		// Both permissions are granted!
		Toast.makeText(this, "All permissions granted. Full functionality enabled.", Toast.LENGTH_SHORT).show();
		// You can automatically start the service or enable UI elements
		startFloatingService();
	}

	@Override
	public void onEssentialPermissionGranted() {
		// Only overlay permission is granted, but that's enough for basic functionality
		Toast.makeText(this, "Essential permissions granted. Starting with basic features.", Toast.LENGTH_SHORT).show();
		startFloatingService();
	}

	@Override
	public void onPermissionsDenied() {
		// User denied essential overlay permission
		Toast.makeText(this, "Cannot function without overlay permission.", Toast.LENGTH_LONG).show();
		// You might want to finish the activity or show a message
	}
	//--- End PermissionCallback ---//

	private void startFloatingService() {
		Intent serviceIntent = new Intent(this, FloatingButtonService.class);
		startService(serviceIntent);
		Toast.makeText(this, "Service starting...", Toast.LENGTH_SHORT).show();
		// finish(); // Optional: close the activity
	}

	private void stopFloatingService() {
		Intent serviceIntent = new Intent(this, FloatingButtonService.class);
		stopService(serviceIntent);
		Toast.makeText(this, "Service stopping...", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onResume() {
		super.onResume();
		//Start updating time
		startUpdatingTime();
		// Register the receiver
		LocalBroadcastManager.getInstance(this).registerReceiver(serviceStateReceiver,
				new IntentFilter(FloatingButtonService.ACTION_SERVICE_STATE_CHANGED));
	}

	@Override
	protected void onPause() {
		super.onPause();
		//Start updating time
		stopUpdatingTime();
		// Unregister the receiver
		LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceStateReceiver);
	}

}