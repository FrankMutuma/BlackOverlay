package com.smarttechnologies.app.blackOverlay;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.util.Log;
import android.widget.TextView; // Specific import for TextView

public class MainActivity extends Activity
{

    private static final String TAG = "BrightnessControl"; // Tag for Logcat messages
    private static final int PERMISSION_REQUEST_CODE = 123;

    private int originalSystemBrightnessValue = -1;
    private int originalSystemBrightnessMode = -1;
    private float originalWindowBrightness = -2.0f;
	private TextView txttext; // Declared here

    private boolean isSystemBrightnessControlled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity created.");

        // Existing full-screen UI setup (moved general window settings up)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        // --- CRITICAL FIX: setContentView MUST be called BEFORE findViewById ---
        setContentView(R.layout.activity_main); // Layout is inflated here

		// find the display text view - NOW this is the correct place
		txttext = findViewById(R.id.txtoutput);
        // Optional: Set initial text for clarity if TextView is found
        if (txttext != null)
		{
            txttext.setText("Brightness Info: Loading...");
        }
		else
		{
            Log.e(TAG, "onCreate: txtoutput TextView not found! Check activity_main.xml for @id/txtoutput");
        }
        // --- END CRITICAL FIX ---


        // --- Add FLAG_KEEP_SCREEN_ON here ---
        // This keeps the screen from turning off/locking while your app is active.
        // It does NOT control brightness, allowing system auto-brightness to work
        // or for your app to set it manually.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "onCreate: FLAG_KEEP_SCREEN_ON added.");
        // --- End FLAG_KEEP_SCREEN_ON ---

        // --- Brightness Control Logic ---
        checkAndApplyBrightness();
        // Removed displayCurrentBrightness() here, as it's called inside applySystemBrightness()
    }

    @Override
    protected void onResume()
	{
        super.onResume();
        // --- ADDED DEBUG TOAST: Crucial for testing ---
        Toast.makeText(this, "onResume CALLED", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onResume: Activity resumed.");
        // Re-apply brightness settings when the activity resumes
        checkAndApplyBrightness();
    }

    @Override
    protected void onPause()
	{
        super.onPause();
        // --- ADDED DEBUG TOAST: Crucial for testing ---
        Toast.makeText(this, "onPause CALLED", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onPause: Activity paused. Restoring brightness.");
        // Restore brightness when the activity pauses
        restoreBrightness(); // Calling the unified restore method
    }

    private void checkAndApplyBrightness()
    {
        Log.d(TAG, "checkAndApplyBrightness: Checking permission.");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (!Settings.System.canWrite(this))
            {
                Log.d(TAG, "checkAndApplyBrightness: WRITE_SETTINGS permission not granted. Requesting...");
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                           Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_REQUEST_CODE);
                Toast.makeText(this, "Please grant 'Modify system settings' permission for full control.", Toast.LENGTH_LONG).show();
                applyInAppWindowBrightness(); // Fallback immediately
            }
            else
            {
                Log.d(TAG, "checkAndApplyBrightness: WRITE_SETTINGS permission granted.");
                applySystemBrightness(); // Apply system brightness
            }
        }
        else
        {
            Log.d(TAG, "checkAndApplyBrightness: API < 23, permission assumed granted.");
            applySystemBrightness(); // Apply system brightness for older APIs
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PERMISSION_REQUEST_CODE)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.System.canWrite(this))
            {
                Log.d(TAG, "onActivityResult: Permission granted by user.");
                applySystemBrightness();
                Toast.makeText(this, "Permission granted. Using full system brightness control.", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Log.d(TAG, "onActivityResult: Permission denied by user. Falling back to in-app.");
                Toast.makeText(this, "Permission denied. Using in-app brightness control only.", Toast.LENGTH_LONG).show();
                applyInAppWindowBrightness(); // Ensure fallback is active
            }
        }
    }

    private void applySystemBrightness()
	{
		Log.d(TAG, "applySystemBrightness: Attempting to set system brightness to 0.");
		ContentResolver cResolver = getContentResolver();
		try
		{
			originalSystemBrightnessValue = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
			originalSystemBrightnessMode = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE);

			// Added more detailed logging for the original mode
			String originalModeName = (originalSystemBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) ? "AUTO" : "MANUAL";
			Log.d(TAG, "applySystemBrightness: Original system brightness: " + originalSystemBrightnessValue + ", Mode: " + originalModeName + " (" + originalSystemBrightnessMode + ")");

			// Temporarily set brightness mode to manual if it's automatic
			if (originalSystemBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
			{
				Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
									   Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
				Log.d(TAG, "applySystemBrightness: Switched system brightness mode to MANUAL for app control.");
			}
			else
			{
				Log.d(TAG, "applySystemBrightness: Original mode was already MANUAL, no mode change needed.");
			}


			// Set system brightness to 0 (lowest)
			Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 0);
			Log.d(TAG, "applySystemBrightness: System brightness set to 0.");

			// Set window brightness to follow system (i.e., not override it)
			WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
			layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE; // Corrected to BRIGHTNESS_OVERRIDE_NONE
			getWindow().setAttributes(layoutParams);
			Log.d(TAG, "applySystemBrightness: Window brightness set to follow system (BRIGHTNESS_OVERRIDE_NONE).");

			isSystemBrightnessControlled = true;
			Toast.makeText(this, "System brightness set to 0 (full control).", Toast.LENGTH_SHORT).show();
			displayCurrentBrightness(); // Call to display after applying brightness

		}
		catch (Settings.SettingNotFoundException e)
		{
			Log.e(TAG, "applySystemBrightness: Settings.SettingNotFoundException", e);
			Toast.makeText(this, "Error accessing system brightness settings.", Toast.LENGTH_SHORT).show();
			applyInAppWindowBrightness(); // Fallback if system settings are not found
		}
		catch (SecurityException e)
		{
			Log.e(TAG, "applySystemBrightness: SecurityException, WRITE_SETTINGS permission issue?", e);
			Toast.makeText(this, "Permission issue. Cannot set system brightness.", Toast.LENGTH_SHORT).show();
			applyInAppWindowBrightness(); // Fallback
		}
	}


    private void applyInAppWindowBrightness()
    {
        // Only apply if we haven't already successfully applied system brightness
        if (!isSystemBrightnessControlled)
        {
            Log.d(TAG, "applyInAppWindowBrightness: Attempting to set in-app window brightness to 0.");
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            originalWindowBrightness = layoutParams.screenBrightness; // Save current window brightness level

            layoutParams.screenBrightness = 0.0f; // Set window brightness to 0 (darkest)
            getWindow().setAttributes(layoutParams); // Apply the new brightness setting
            Toast.makeText(this, "Using in-app window brightness control.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Log.d(TAG, "applyInAppWindowBrightness: Not applying, system brightness already controlled.");
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
        {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Activity destroyed.");
        // Restore original brightness based on which method was used
        // This is a fallback if onPause didn't get called, or if the app is fully killed.
        restoreBrightness(); // Calling the unified restore method

        // --- Clear FLAG_KEEP_SCREEN_ON here ---
        // This removes the flag when the activity is destroyed, allowing the screen
        // to turn off normally afterwards (e.g., via idle timeout or power button).
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG, "onDestroy: FLAG_KEEP_SCREEN_ON cleared.");
        // --- End Clear FLAG_KEEP_SCREEN_ON ---
    }

    // Unified brightness restoration method
    private void restoreBrightness()
	{
        Log.d(TAG, "restoreBrightness: Restoring brightness based on control type.");
        if (isSystemBrightnessControlled)
		{
            restoreSystemBrightness();
        }
		else
		{
            restoreInAppWindowBrightness();
        }
    }

    private void restoreSystemBrightness()
	{
		ContentResolver cResolver = getContentResolver();
		try
		{
			// Only restore if we successfully saved original values
			if (originalSystemBrightnessValue != -1 && originalSystemBrightnessMode != -1)
			{
				Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, originalSystemBrightnessValue);
				Log.d(TAG, "restoreSystemBrightness: System brightness restored to: " + originalSystemBrightnessValue);

				// Added more detailed logging for the restored mode
				String restoredModeName = (originalSystemBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) ? "AUTO" : "MANUAL";
				Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, originalSystemBrightnessMode);
				Log.d(TAG, "restoreSystemBrightness: System brightness mode restored to: " + restoredModeName + " (" + originalSystemBrightnessMode + ")");

				Toast.makeText(this, "System brightness restored.", Toast.LENGTH_SHORT).show();
			}
			else
			{
				Log.d(TAG, "restoreSystemBrightness: No original system brightness values to restore.");
			}
		}
		catch (Exception e) // Catching generic Exception to log any issues during restore
		{
			Log.e(TAG, "restoreSystemBrightness: Error restoring system brightness.", e);
			Toast.makeText(this, "Error restoring system brightness.", Toast.LENGTH_SHORT).show();
		}
	}


    private void restoreInAppWindowBrightness()
    {
        // Only restore if we had a valid original brightness saved for the window
        if (originalWindowBrightness != -2.0f)
        {
            Log.d(TAG, "restoreInAppWindowBrightness: Restoring in-app window brightness to: " + originalWindowBrightness);
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.screenBrightness = originalWindowBrightness;
            getWindow().setAttributes(layoutParams);
            Toast.makeText(this, "In-app brightness restored.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Log.d(TAG, "restoreInAppWindowBrightness: No original in-app window brightness to restore.");
        }
    }

	private void displayCurrentBrightness()
	{
		ContentResolver cResolver = getContentResolver();
		// Removed redundant Toast: Toast.makeText(this, "apply brightnesd", Toast.LENGTH_SHORT).show();
		try
		{
			int currentBrightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);
			int currentBrightnessMode = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS_MODE);

			String modeText;
			if (currentBrightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
			{
				modeText = "Automatic";
			}
			else
			{
				modeText = "Manual";
			}

			String message = "Current Brightness: " + currentBrightness + " (out of 255)\nMode: " + modeText;
			Toast.makeText(this, message, Toast.LENGTH_LONG).show();
			Log.d(TAG, "Current Brightness: " + currentBrightness + ", Mode: " + modeText);
			// Ensure txttext is not null before setting text
			if (txttext != null)
			{
			    txttext.setText(message);
			}
			else
			{
			    Log.e(TAG, "displayCurrentBrightness: txttext is null. findViewById might have failed.");
			}
		}
		catch (Settings.SettingNotFoundException e)
		{
			Log.e(TAG, "displayCurrentBrightness: Settings.SettingNotFoundException", e);
			Toast.makeText(this, "Could not read system brightness settings.", Toast.LENGTH_SHORT).show();
		}
		catch (SecurityException e)
		{
			Log.e(TAG, "displayCurrentBrightness: SecurityException, READ_SETTINGS permission issue?", e);
			Toast.makeText(this, "Permission to read settings denied.", Toast.LENGTH_SHORT).show();
		}
	}
}

