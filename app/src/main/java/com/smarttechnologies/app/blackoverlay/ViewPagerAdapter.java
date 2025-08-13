package com.smarttechnologies.app.blackoverlay;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

	public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
		super(fragmentActivity);
	}

	@NonNull
	@Override
	public Fragment createFragment(int position) {
		switch (position) {
		case 0:
			return new LookFeelFragment();
		case 1:
			return new SettingsFragment();
		default:
			return new LookFeelFragment();
		}
	}

	@Override
	public int getItemCount() {
		return 2; // We have two fragments: Look & Feel and Settings
	}
}