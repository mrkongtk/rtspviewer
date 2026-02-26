package com.mrkongtk.rtspviewer.shared.data

import platform.Foundation.NSBundle

class DeviceInfoIosImpl : DeviceInfo {
    override val bundleId: String
        get() = NSBundle.mainBundle.bundleIdentifier ?: "unknown"

}