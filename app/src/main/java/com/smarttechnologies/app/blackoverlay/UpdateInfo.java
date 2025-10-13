package com.smarttechnologies.app.blackoverlay;

//import com.google.gson.annotations.SerializedName;

/**
 * Data class representing the JSON structure from the Firebase Remote Config
 * parameter 'app_update_info'.
 *
 * Gson will automatically map the JSON keys to these fields.
 */
public class UpdateInfo {
	/*

	@SerializedName("latest_version_code")
	private double latestVersionCode = 1.0; // Default to 1, same as our default JSON

	@SerializedName("update_type")
	private String updateType = "recommend"; // Default to NONE

	@SerializedName("apk_download_url")
	private String apkDownloadUrl = ""; // Default to empty string

	@SerializedName("release_notes")
	private String releaseNotes = "";
	// --- Getters ---

	public double getLatestVersionCode() {
		return latestVersionCode;
	}

	public String getUpdateType() {
		// Return uppercase for consistent comparison (FORCE or RECOMMEND)
		return updateType.toUpperCase();
	}

	public String getApkDownloadUrl() {
		return apkDownloadUrl;
	}

	public String getReleaseNotes() {
		return releaseNotes;
	}

	// You can omit setters if you only read data from the JSON
	*/
}