package com.smarttechnologies.app.blackoverlay;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.annotation.NonNull;

public class ServiceLifecycleOwner implements LifecycleOwner {
	private LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(this);

	@NonNull
	@Override
	public Lifecycle getLifecycle() {
		return lifecycleRegistry;
	}

	public void onServiceCreated() {
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
	}

	public void onServiceStarted() {
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
	}

	public void onServiceDestroyed() {
		lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
	}
}
