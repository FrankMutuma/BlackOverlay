package com.smarttechnologies.app.blackoverlay;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class MainActivity extends AppCompatActivity implements PermissionManager.PermissionCallback {
	private ClockUtils clockUtils;
	private TextView timeTextView;
	private TextView dateDayTextView;
	private PermissionManager permissionManager;
	private AppPreferencesManager prefsManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		clockUtils = new ClockUtils();
		prefsManager = AppPreferencesManager.getInstance(this);
		// Initialize the PermissionManager with this activity and callback
		permissionManager = new PermissionManager(this, this);

		setupUI();

		// Check and request permissions as the first order of business
		permissionManager.checkAndRequestPermissions();
	}

	private void setupUI() {
		// Find the ViewPager2 and TabLayout from the layout file
		ViewPager2 viewPager = findViewById(R.id.view_pager);
		TabLayout tabLayout = findViewById(R.id.tab_layout);
		FloatingActionButton mainStartButton = findViewById(R.id.fab_start);
		timeTextView = findViewById(R.id.activity_main_time);
		dateDayTextView = findViewById(R.id.activity_main_date);
		// Create an instance of our custom ViewPagerAdapter
		ViewPagerAdapter adapter = new ViewPagerAdapter(this);

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

		// Set the FAB click listener
		mainStartButton.setOnClickListener(v -> {
			// The FAB now simply starts the service.
			// Permission checks are already handled on app startup.
			startFloatingService();
		});
	}

	//--- PermissionCallback Methods ---//
	@Override
	public void onOverlayPermissionGranted() {
		// Overlay permission is granted. Now check the next one (Write Settings).
		Toast.makeText(this, "Overlay permission granted!", Toast.LENGTH_SHORT).show();
		permissionManager.checkAndRequestPermissions(); // This will now check Write Settings
	}

	@Override
	public void onWriteSettingsPermissionGranted() {
		// Both critical permissions are granted!
		Toast.makeText(this, "All permissions granted. Full functionality enabled.", Toast.LENGTH_SHORT).show();
		// You can now enable any pro features or UI elements that require full access
	}

	@Override
	public void onPermissionDenied() {
		// This is called if the user denies WRITE_SETTINGS.
		// The app can still function with overlay permission alone.
		Toast.makeText(this, "Starting with standard features.", Toast.LENGTH_SHORT).show();
		// The overlay service can still be started, but BrightnessManager will use fallback.
	}
	//--- End PermissionCallback ---//

	private void startFloatingService() {
		// This method is now simple. Just start the service.
		// The Service's onCreate will handle the foreground notification and overlay.
		Intent intent = new Intent(this, FloatingButtonService.class);
		startService(intent);
		Toast.makeText(this, "Service starting...", Toast.LENGTH_SHORT).show();
		// finish(); // Optional: close the activity
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (permissionManager != null) {
			permissionManager.clearCallback(); // Prevent memory leaks
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		clockUtils.startUpdatingTime(timeTextView, dateDayTextView);
	}

	@Override
	protected void onPause() {
		super.onPause();
		clockUtils.stopUpdatingTime();
	}

	// Register a launcher for the permission request
	private ActivityResultLauncher<Intent> permissionLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(), result -> {
				if (Settings.canDrawOverlays(this)) {
					startFloatingService();
				} else {
					Toast.makeText(this, "Overlay permission is required to display the floating button.",
							Toast.LENGTH_LONG).show();
				}
			});

	private void requestOverlayPermission() {
		if (!Settings.canDrawOverlays(this)) {
			Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
					Uri.parse("package:" + getPackageName()));
			permissionLauncher.launch(intent);
		} else {
			startFloatingService();
		}
	}

}