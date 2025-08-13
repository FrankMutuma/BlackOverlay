package com.smarttechnologies.app.blackoverlay;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Find the ViewPager2 and TabLayout from the layout file
		ViewPager2 viewPager = findViewById(R.id.view_pager);
		TabLayout tabLayout = findViewById(R.id.tab_layout);

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
	}
}