package com.smarttechnologies.app.blackoverlay;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.snackbar.Snackbar;

public class DarkActivity extends AppCompatActivity {

	private static final String TAG = "BrightnessControl";
	private static final int SYSTEM_BRIGHTNESS_MIN = 1;
	private static final float WINDOW_BRIGHTNESS_ABSOLUTE_MIN = 0.00f; // This is the target for full dimming
	private static final float WINDOW_BRIGHTNESS_READABLE_MIN = 0.10f; // Adjusted for better visibility when prompting

	private int originalSystemBrightnessValue = -1;
	private int originalSystemBrightnessMode = -1;
	private float originalWindowBrightness = -2.0f; // Use a value outside 0.0f-1.0f range for initial check

	private boolean isSystemBrightnessControlled = false;

	private float brightnessTrackValue = 1.0f; // Tracks current effective brightness for display

	// Session-based counter for permission prompts within the current activity lifecycle
	// Resets to 0 in onCreate. Max 3 times per session.
	private int sessionPermissionPromptCount = 0;
	private static final int MAX_SESSION_PERMISSION_PROMPTS = 3;

	// Flag to indicate if a permission-related Snackbar is currently being displayed.
	private boolean isSnackbarShowing = false;

	// NEW FLAG: True when we are actively in a phase where we are prompting for permission
	// or navigating to system settings for it. During this phase, screen must be readable.
	private boolean isPermissionPromptPhaseActive = false;

	private TextView txttext;
	private ActivityResultLauncher<Intent> writeSettingsLauncher;

	private AppPreferencesManager prefsManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate: Activity created. API Level: " + Build.VERSION.SDK_INT);

		prefsManager = AppPreferencesManager.getInstance(this);
		// Reset session-specific counters and flags on every new app launch/creation
		sessionPermissionPromptCount = 0;
		isSnackbarShowing = false;
		isPermissionPromptPhaseActive = false; // Ensure it's false at the start of a new creation cycle

		// --- Logging current preference states on onCreate ---
		Log.d(TAG,
				"onCreate: Initial prefs state -> " + "InitialLaunchPromptCount: "
						+ prefsManager.getInitialLaunchPromptCount() + " (Max: "
						+ AppPreferencesManager.MAX_INITIAL_LAUNCH_PROMPTS + "), " + "TotalDenials: "
						+ prefsManager.getTotalDenials() + " (Max: " + AppPreferencesManager.MAX_TOTAL_DENIALS + ")");
		// --- End Logging ---

		writeSettingsLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
				result -> {
					// This block executes when returning from Settings.ACTION_MANAGE_WRITE_SETTINGS
					Log.d(TAG, "onActivityResult: Returned from settings. Checking permission.");

					// IMPORTANT: Reset this flag based on the *outcome* of the permission check
					// The phase ends if permission is granted, or if we hit limits and stop prompting.
					// It continues if we are re-prompting.

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(this)) {
						Log.d(TAG, "onActivityResult: Permission granted by user. Applying combined brightness.");
						isPermissionPromptPhaseActive = false; // Prompt phase successfully concluded
						applyCombinedBrightness();
						Toast.makeText(this, "Permission granted. Using full brightness control.", Toast.LENGTH_SHORT)
								.show();

						// Reset all relevant counters on successful grant
						sessionPermissionPromptCount = 0; // Reset session count for current session
						prefsManager.resetTotalDenials(); // Reset global total denials
						prefsManager.resetInitialLaunchPromptCount(); // Reset initial launch count
						Log.d(TAG,
								"Counters reset on GRANT: session=" + sessionPermissionPromptCount
										+ ", total_persistent=" + prefsManager.getTotalDenials() + ", initial_launch="
										+ prefsManager.getInitialLaunchPromptCount());
					} else {
						Log.d(TAG,
								"onActivityResult: Permission denied by user. Handling fallback and potential re-prompt.");

						// Increment global total denials as this counts as one denied attempt
						prefsManager.incrementTotalDenials();
						Log.d(TAG, "onActivityResult: TotalDenials incremented to " + prefsManager.getTotalDenials());

						// Increment session prompt count after a denial in settings (if not already at max)
						if (sessionPermissionPromptCount < MAX_SESSION_PERMISSION_PROMPTS) {
							sessionPermissionPromptCount++;
							Log.d(TAG, "onActivityResult: sessionPermissionPromptCount incremented to "
									+ sessionPermissionPromptCount);
						}

						boolean shouldShowRepromptSnackbar = sessionPermissionPromptCount < MAX_SESSION_PERMISSION_PROMPTS
								&& prefsManager.getTotalDenials() < AppPreferencesManager.MAX_TOTAL_DENIALS;

						if (shouldShowRepromptSnackbar) {
							Log.d(TAG, "onActivityResult: Re-displaying permission explanation Snackbar.");
							isPermissionPromptPhaseActive = true; // Still in prompt phase
							// Ensure screen is readable before showing the re-prompt Snackbar
							setWindowBrightness(WINDOW_BRIGHTNESS_READABLE_MIN);
							showInitialPermissionExplanationInternal(true); // Call internal method for re-prompt
						} else {
							Log.d(TAG,
									"onActivityResult: Max permission prompts reached (session or persistent). No more Snackbar reminders. Applying fallback directly.");
							isPermissionPromptPhaseActive = false; // Prompt phase concluded due to limits
							Toast.makeText(this, "Permission denied. Limited brightness control available.",
									Toast.LENGTH_LONG).show();
							// Apply full dimming immediately if no more prompts
							applyInAppWindowBrightness();
						}
					}
				});

		// Configure system bars for immersive mode
		WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			final WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(),
					getWindow().getDecorView());
			if (controller != null) {
				controller.hide(WindowInsetsCompat.Type.systemBars());
				controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
				Log.d(TAG, "onCreate: Modern system bars hidden.");
			}
		} else {
			getWindow().getDecorView().setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
							| View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
			Log.d(TAG, "onCreate: Legacy system bars hidden.");
		}

		// Handle display cutout for notch devices
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			getWindow()
					.getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
			Log.d(TAG, "onCreate: Display cutout mode set for short edges.");
		}

		setContentView(R.layout.activity_main_2);

		txttext = findViewById(R.id.txtoutput);
		if (txttext == null) {
			Log.e(TAG, "onCreate: txtoutput TextView not found! Check activity_main.xml for @id/txtoutput");
		}

		// Capture the original window brightness BEFORE any modifications by our app.
		// This value is used for restoring when the app is no longer actively dimming.
		originalWindowBrightness = getWindow().getAttributes().screenBrightness;
		// If system reports -1.0f, it means no override, which is fine for restoring later.
		if (originalWindowBrightness < 0) {
			originalWindowBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE; // Often -1.0f
		}
		Log.d(TAG, "onCreate: Initial originalWindowBrightness captured as: " + originalWindowBrightness
				+ " (BRIGHTNESS_OVERRIDE_NONE is -1.0f).");

		// Keep the screen on while the app is active
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Log.d(TAG, "onCreate: FLAG_KEEP_SCREEN_ON added.");

		// --- ONCREATE PERMISSION HANDLING ---
		// This block decides what happens when the app is first created/launched.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean canWriteSettings = Settings.System.canWrite(this);
			Log.d(TAG, "onCreate: Settings.System.canWrite(this) = " + canWriteSettings);

			if (!canWriteSettings) {
				boolean shouldShowInitialPrompt = prefsManager
						.getInitialLaunchPromptCount() < AppPreferencesManager.MAX_INITIAL_LAUNCH_PROMPTS
						&& prefsManager.getTotalDenials() < AppPreferencesManager.MAX_TOTAL_DENIALS;

				if (shouldShowInitialPrompt) {
					Log.d(TAG, "onCreate: Permission NOT granted. Displaying initial explanation prompt.");
					isPermissionPromptPhaseActive = true; // Activate prompt phase
					// Ensure readable brightness immediately before showing the Snackbar
					setWindowBrightness(WINDOW_BRIGHTNESS_READABLE_MIN);
					showInitialPermissionExplanationInternal(false); // Initial prompt

					// Increment counters when the prompt is SHOWN
					prefsManager.incrementInitialLaunchPromptCount();
					sessionPermissionPromptCount++;
					Log.d(TAG,
							"onCreate: Counters incremented after showing initial prompt: session="
									+ sessionPermissionPromptCount + ", initial_launch="
									+ prefsManager.getInitialLaunchPromptCount());

				} else {
					Log.d(TAG,
							"onCreate: Permission NOT granted. Initial launch prompt limit OR total denials reached. Applying fallback directly.");
					isPermissionPromptPhaseActive = false; // No prompt, so not in prompt phase
					// If limits are reached on onCreate, no more prompts, so dimming should happen immediately.
					applyInAppWindowBrightness();
				}
			} else {
				// Permission IS granted - apply full brightness control immediately.
				Log.d(TAG, "onCreate: Permission already granted. Applying combined brightness immediately.");
				isPermissionPromptPhaseActive = false; // Permission granted, no prompt needed
				applyCombinedBrightness();
				// Reset counters on a successful grant (even if granted in a previous session)
				prefsManager.resetInitialLaunchPromptCount();
				prefsManager.resetTotalDenials();
			}
		} else {
			// API < 23, WRITE_SETTINGS permission not required - apply full brightness control immediately.
			Log.d(TAG, "onCreate: API Level < 23. Permission not required. Applying combined brightness immediately.");
			isPermissionPromptPhaseActive = false; // Not applicable or granted by default
			applyCombinedBrightness();
		}
		// --- END ONCREATE PERMISSION HANDLING ---
	}

	@Override
	protected void onResume() {
		super.onResume();
		Toast.makeText(this, "onResume CALLED", Toast.LENGTH_SHORT).show();
		Log.d(TAG, "onResume: Activity resumed. isPermissionPromptPhaseActive: " + isPermissionPromptPhaseActive);

		// --- ONRESUME PERMISSION HANDLING ---
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			boolean canWriteSettings = Settings.System.canWrite(this);
			Log.d(TAG, "onResume: Settings.System.canWrite(this) = " + canWriteSettings);

			if (!canWriteSettings) {
				boolean sessionLimitReached = sessionPermissionPromptCount >= MAX_SESSION_PERMISSION_PROMPTS;
				boolean totalDenialLimitReached = prefsManager
						.getTotalDenials() >= AppPreferencesManager.MAX_TOTAL_DENIALS;

				if (isPermissionPromptPhaseActive) {
					// If we are currently in an active permission prompting phase (e.g., just returned from settings
					// or a Snackbar was already active from onCreate/onActivityResult), ensure readability.
					Log.d(TAG, "onResume: Permission prompt phase still active. Ensuring readable brightness ("
							+ WINDOW_BRIGHTNESS_READABLE_MIN + ").");
					setWindowBrightness(WINDOW_BRIGHTNESS_READABLE_MIN);
					// No need to show Snackbar again if it's already showing or will be handled by onActivityResult
				} else if (sessionLimitReached || totalDenialLimitReached) {
					// If limits are reached, always apply in-app brightness directly on resume,
					// as no more prompts should be shown.
					Log.d(TAG,
							"onResume: Permission NOT granted, session prompt limit OR total denials reached. Applying fallback directly.");
					isPermissionPromptPhaseActive = false; // Not in prompt phase anymore
					applyInAppWindowBrightness();
				} else if (!isSnackbarShowing) {
					// This block handles other resume scenarios (e.g., app coming back from background,
					// and it's not the immediate onResume after onCreate), and no Snackbar is currently active.
					Log.d(TAG,
							"onResume: Permission NOT granted and no active Snackbar/prompt phase. Displaying resume explanation prompt.");
					isPermissionPromptPhaseActive = true; // Activate prompt phase
					// Ensure readable brightness before showing a new prompt
					setWindowBrightness(WINDOW_BRIGHTNESS_READABLE_MIN);
					showInitialPermissionExplanationInternal(false);
					sessionPermissionPromptCount++;
				} else {
					// This branch should ideally not be hit if logic is perfect, but acts as a safeguard.
					// If a Snackbar is somehow active but isPermissionPromptPhaseActive is false,
					// we ensure brightness is readable for the active Snackbar.
					Log.d(TAG,
							"onResume: Permission NOT granted, but a Snackbar is active (isPermissionPromptPhaseActive was false). Ensuring readable brightness ("
									+ WINDOW_BRIGHTNESS_READABLE_MIN + ").");
					setWindowBrightness(WINDOW_BRIGHTNESS_READABLE_MIN);
				}
			} else {
				// Permission IS granted - immediate dimming is intended and desired here.
				Log.d(TAG, "onResume: Permission granted. Applying combined brightness immediately.");
				isPermissionPromptPhaseActive = false; // Permission granted, no prompt needed
				applyCombinedBrightness();
				// Reset counters on successful grant, even if it happens later in lifecycle
				prefsManager.resetInitialLaunchPromptCount();
				prefsManager.resetTotalDenials();
				sessionPermissionPromptCount = 0;
			}
		} else {
			// API < 23, permission not required - immediate dimming is OK here.
			Log.d(TAG, "onResume: API Level < 23. Permission not required. Applying combined brightness immediately.");
			isPermissionPromptPhaseActive = false; // Not applicable or granted by default
			applyCombinedBrightness();
		}
		// --- END ONRESUME PERMISSION HANDLING ---
	}

	@Override
	protected void onPause() {
		super.onPause();
		Toast.makeText(this, "onPause CALLED", Toast.LENGTH_SHORT).show();
		Log.d(TAG, "onPause: Activity paused. isPermissionPromptPhaseActive: " + isPermissionPromptPhaseActive);

		// CRITICAL: Only restore brightness if we are NOT in an active permission prompting phase.
		// If we ARE in a prompt phase (e.g., user is going to settings), keep the screen readable.
		if (!isPermissionPromptPhaseActive) {
			Log.d(TAG, "onPause: Not in permission prompt phase. Restoring brightness.");
			restoreBrightness();
		} else {
			Log.d(TAG, "onPause: Permission prompt phase active. KEEPING screen at READABLE brightness ("
					+ WINDOW_BRIGHTNESS_READABLE_MIN + ") for user to navigate settings.");
			// Explicitly set to readable brightness to ensure it doesn't revert to original (potentially 0)
			setWindowBrightness(WINDOW_BRIGHTNESS_READABLE_MIN);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy: Activity destroyed.");
		// Always restore on destroy, as the app is exiting.
		// This ensures system brightness is returned to normal if we had controlled it.
		restoreBrightness();

		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Log.d(TAG, "onDestroy: FLAG_KEEP_SCREEN_ON cleared.");
	}

	/**
	* Launches the system settings for WRITE_SETTINGS permission.
	*/
	private void requestWriteSettingsPermission() {
		Log.d(TAG, "requestWriteSettingsPermission: Launching WRITE_SETTINGS permission intent.");
		isPermissionPromptPhaseActive = true; // Set true as we are going to settings for permission
		Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
		writeSettingsLauncher.launch(intent);
	}

	/**
	* Internal method to show a Snackbar explaining why WRITE_SETTINGS permission is needed.
	* This method is called by onCreate, onResume, and onActivityResult.
	* The brightness is set by the *caller* of this method BEFORE it's invoked.
	*
	* @param isReprompt True if this is a re-prompt after a denial, false for initial prompt.
	*/
	private void showInitialPermissionExplanationInternal(boolean isReprompt) {
		String message = isReprompt ? "Permission denied. Please grant 'Modify system settings' for full dimming."
				: "For optimal dark mode, please enable 'Modify system settings'. This allows full screen dimming.";

		Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE // User must dismiss or click action
		);
		snackbar.setAction("GRANT ACCESS", v -> {
			Log.d(TAG, "Snackbar: 'Grant Access' clicked. Requesting permission.");
			isSnackbarShowing = false; // Snackbar will dismiss as we navigate away
			requestWriteSettingsPermission(); // This will set isPermissionPromptPhaseActive to true
		});
		snackbar.addCallback(new Snackbar.Callback() {
			@Override
			public void onShown(Snackbar sb) {
				super.onShown(sb);
				isSnackbarShowing = true;
				Log.d(TAG, "Snackbar shown. isSnackbarShowing = true. Current window brightness: "
						+ getWindow().getAttributes().screenBrightness);
				// Brightness should already be set to WINDOW_BRIGHTNESS_READABLE_MIN by the caller (onCreate/onResume/onActivityResult)
			}

			@Override
			public void onDismissed(Snackbar transientBottomBar, int event) {
				super.onDismissed(transientBottomBar, event);
				isSnackbarShowing = false; // Reset flag when Snackbar is dismissed
				Log.d(TAG, "Snackbar dismissed. isSnackbarShowing = false. Event: " + event);

				// If dismissed without the "GRANT ACCESS" action, AND permission is still denied,
				// then we transition out of the prompt phase and apply full dimming.
				if (event != DISMISS_EVENT_ACTION) {
					Log.d(TAG, "Snackbar dismissed without action. Checking permission state.");
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
							&& !Settings.System.canWrite(DarkActivity.this)) {
						Log.d(TAG,
								"Permission still denied after Snackbar dismissal. Applying in-app window brightness.");
						isPermissionPromptPhaseActive = false; // Prompt phase ends if dismissed without action
						applyInAppWindowBrightness(); // This will set it to 0.0f
					} else {
						// This else block handles a scenario where the Snackbar was dismissed NOT via action,
						// BUT permission *is* now granted (e.g., granted in the background, or a system issue).
						// In this specific case, permission is granted, so we should apply full control.
						Log.d(TAG,
								"Snackbar dismissed without action, but permission is now granted. Applying combined brightness.");
						isPermissionPromptPhaseActive = false; // Prompt phase concluded successfully
						applyCombinedBrightness();
					}
				} else {
					// User clicked 'Grant Access'. The next brightness change will be handled by onActivityResult
					// or subsequent onResume after they return from settings.
					// isPermissionPromptPha

					Log.d(TAG,

							"Snackbar dismissed via 'Grant Access' action. Expecting result from permission launcher. isPermissionPromptPhaseActive remains true.");

				}

			}

		});

		snackbar.show();

	}

	/**
	
	* Public wrapper for initial permission explanation.
	
	* This is what onCreate/onResume should call to start the permission flow.
	
	*/

	private void showInitialPermissionExplanation() {

		showInitialPermissionExplanationInternal(false);

	}

	/**
	
	* Helper method to set window brightness.
	
	* @param brightnessValue The brightness level (0.0f to 1.0f or -1.0f for no override).
	
	*/

	private void setWindowBrightness(float brightnessValue) {

		WindowManager.LayoutParams layoutParams = getWindow().getAttributes();

		layoutParams.screenBrightness = brightnessValue;

		getWindow().setAttributes(layoutParams);

		brightnessTrackValue = brightnessValue; // Update tracked value

		Log.d(TAG, "setWindowBrightness: Window brightness set to " + brightnessValue);

		displayCurrentBrightness();

	}

	/**
	
	* Applies combined system and in-app window brightness for full dimming.
	
	* Requires WRITE_SETTINGS permission.
	
	*/

	private void applyCombinedBrightness() {

		Log.d(TAG, "applyCombinedBrightness: Attempting to set system brightness to " + SYSTEM_BRIGHTNESS_MIN

				+ " and window brightness to " + WINDOW_BRIGHTNESS_ABSOLUTE_MIN + ".");

		Log.d(TAG, "CALL STACK for applyCombinedBrightness: " + Log.getStackTraceString(new Throwable()));

		applySystemBrightnessOnly(); // Try to set system brightness first

		if (isSystemBrightnessControlled) {

			// If system brightness was successfully controlled, also set window brightness to minimum

			setWindowBrightness(WINDOW_BRIGHTNESS_ABSOLUTE_MIN); // Use the helper

			Log.d(TAG, "applyCombinedBrightness: Window brightness explicitly set to " + WINDOW_BRIGHTNESS_ABSOLUTE_MIN

					+ ".");

			Toast.makeText(this, "System brightness dimmed, window forced to " + WINDOW_BRIGHTNESS_ABSOLUTE_MIN + ".",

					Toast.LENGTH_SHORT).show();

		} else {

			Log.d(TAG,

					"applyCombinedBrightness: System brightness control failed unexpectedly, falling back to in-app only.");

			applyInAppWindowBrightness(); // Fallback if system control fails

		}

		displayCurrentBrightness();

	}

	/**
	
	* Attempts to set the device's system brightness to a minimum value.
	
	* Requires WRITE_SETTINGS permission.
	
	*/

	private void applySystemBrightnessOnly() {

		Log.d(TAG, "applySystemBrightnessOnly: Attempting to set system brightness to " + SYSTEM_BRIGHTNESS_MIN + ".");

		ContentResolver cResolver = getContentResolver();

		try {

			// Save original brightness value and mode

			originalSystemBrightnessValue = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);

			originalSystemBrightnessMode = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE);

			String originalModeName = (originalSystemBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)

					? "AUTO"

					: "MANUAL";

			Log.d(TAG, "applySystemBrightnessOnly: Original system brightness saved: " + originalSystemBrightnessValue

					+ ", Mode: " + originalModeName + " (" + originalSystemBrightnessMode + ")");

			// If auto brightness is on, switch to manual to allow control

			if (originalSystemBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {

				Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,

						Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

				Log.d(TAG, "applySystemBrightnessOnly: Switched system brightness mode to MANUAL for app control.");

			} else {

				Log.d(TAG, "applySystemBrightnessOnly: Original mode was already MANUAL, no mode change needed.");

			}

			// Set system brightness to the minimum

			Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, SYSTEM_BRIGHTNESS_MIN);

			Log.d(TAG, "applySystemBrightnessOnly: System brightness set to " + SYSTEM_BRIGHTNESS_MIN + ".");

			isSystemBrightnessControlled = true; // Mark that system brightness is now under app control

		} catch (Settings.SettingNotFoundException e) {

			Log.e(TAG, "applySystemBrightnessOnly: Settings.SettingNotFoundException: " + e.getMessage(), e);

			isSystemBrightnessControlled = false;

		} catch (SecurityException e) {

			Log.e(TAG, "applySystemBrightnessOnly: SecurityException (WRITE_SETTINGS permission issue?): "

					+ e.getMessage(), e);

			isSystemBrightnessControlled = false;

		}

	}

	/**
	
	* Applies in-app window brightness, which works even without WRITE_SETTINGS permission.
	
	*/

	private void applyInAppWindowBrightness() {

		Log.d(TAG, "applyInAppWindowBrightness: Attempting to set in-app window brightness to "

				+ WINDOW_BRIGHTNESS_ABSOLUTE_MIN + ".");

		Log.d(TAG, "CALL STACK for applyInAppWindowBrightness: " + Log.getStackTraceString(new Throwable()));

		setWindowBrightness(WINDOW_BRIGHTNESS_ABSOLUTE_MIN); // Use the helper

		Toast.makeText(this, "Using in-app window brightness control.", Toast.LENGTH_SHORT).show();

	}

	/**
	
	* Restores both in-app window brightness and, if applicable, system brightness to original values.
	
	* This method should only be called when the app explicitly wishes to stop controlling brightness
	
	* (e.g., app going to background, app exiting), and not during an active permission prompt phase.
	
	*/

	private void restoreBrightness() {

		Log.d(TAG, "restoreBrightness: Restoring brightness based on control type.");

		restoreInAppWindowBrightness(); // Always restore in-app window brightness first

		if (isSystemBrightnessControlled) {

			Log.d(TAG, "restoreBrightness: Restoring system brightness and mode.");

			ContentResolver cResolver = getContentResolver();

			try {

				if (originalSystemBrightnessMode != -1) {

					Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,

							originalSystemBrightnessMode);

					Log.d(TAG, "restoreBrightness: System brightness mode restored to " + originalSystemBrightnessMode

							+ ".");

				}

				if (originalSystemBrightnessValue != -1) {

					Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, originalSystemBrightnessValue);

					Log.d(TAG, "restoreBrightness: System brightness value restored to " + originalSystemBrightnessValue

							+ ".");

				}

				isSystemBrightnessControlled = false; // Reset control flag

			} catch (SecurityException e) {

				Log.e(TAG, "restoreBrightness: SecurityException restoring system brightness: " + e.getMessage(), e);

			}

		}

		brightnessTrackValue = 1.0f; // Reset tracker to default full brightness

		Log.d(TAG, "restoreBrightness: brightnessTrackValue reset to: " + brightnessTrackValue);

		displayCurrentBrightness();

	}

	/**
	
	* Restores the in-app window brightness to its original value captured on onCreate.
	
	* Note: When `isPermissionPromptPhaseActive` is true, calls to `setWindowBrightness`
	
	* will override this behavior temporarily.
	
	*/

	private void restoreInAppWindowBrightness() {

		Log.d(TAG, "restoreInAppWindowBrightness: Restoring in-app window brightness to: " + originalWindowBrightness);

		setWindowBrightness(originalWindowBrightness); // Use the helper

	}

	/**
	
	* Updates the TextView with current brightness information.
	
	*/

	private void displayCurrentBrightness() {

		if (txttext == null) {

			Log.e(TAG, "displayCurrentBrightness: txtoutput TextView is null.");

			return;

		}

		String mode = "N/A";

		int systemBrightness = -1;

		try {

			systemBrightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);

			int systemMode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);

			mode = (systemMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) ? "Auto" : "Manual";

		} catch (Settings.SettingNotFoundException e) {

			Log.e(TAG, "displayCurrentBrightness: Could not read system brightness settings: " + e.getMessage());

		}

		float windowBrightness = getWindow().getAttributes().screenBrightness;

		String display = String.format(

				"System Brightness: %d (Mode: %s)\nWindow Brightness: %.2f\nIn-App Tracked: %.2f\nSystem Control Used: %b\nisSnackbarShowing: %b\nisPromptPhaseActive: %b",

				systemBrightness, mode, windowBrightness, brightnessTrackValue, isSystemBrightnessControlled,

				isSnackbarShowing, isPermissionPromptPhaseActive);

		txttext.setText(display);

	}

}