package com.mrkongtk.rtspviewer.shared.data

import android.content.Context

class DeviceInfoAndroidImpl(private val context: Context) : DeviceInfo {
    override val bundleId: String
        get() = context.packageName
}