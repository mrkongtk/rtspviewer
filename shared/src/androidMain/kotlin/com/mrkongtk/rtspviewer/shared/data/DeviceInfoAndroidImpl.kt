package com.mrkongtk.rtspviewer.shared.data

import android.content.Context

class DeviceInfoAndroidImpl(private val context: Context) : DeviceInfo {
    /**
     * Returns the Android application's package name as defined in the manifest.
     */
    override val bundleId: String
        get() = context.packageName
}
