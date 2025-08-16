package com.smarttechnologies.app.blackoverlay;

import android.os.Bundle;
import android.view.View;
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

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Find the ViewPager2 and TabLayout from the layout file
		ViewPager2 viewPager = findViewById(R.id.view_pager);
		TabLayout tabLayout = findViewById(R.id.tab_layout);
		FloatingActionButton mainStartButton = findViewById(R.id.fab_start);

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

		mainStartButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				requestOverlayPermission();
			}

		});

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

	private void startFloatingService() {
		Intent intent = new Intent(this, FloatingButtonService.class);
		Toast.makeText(this,"float started",Toast.LENGTH_LONG).show();
		startForegroundService(intent);
		// You may also want to finish the MainActivity to go to the background
		// finish();
	}

}