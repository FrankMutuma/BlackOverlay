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
			// Check if we have at least overlay permission before starting service
			if (permissionManager.hasOverlayPermission()) {
				startFloatingService();
			} else {
				Toast.makeText(this, "Please grant overlay permission first", Toast.LENGTH_SHORT).show();
				permissionManager.checkAndRequestPermissions();
			}
		});
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
		Intent intent = new Intent(this, FloatingButtonService.class);
		startService(intent);
		Toast.makeText(this, "Service starting...", Toast.LENGTH_SHORT).show();
		// finish(); // Optional: close the activity
	}
}