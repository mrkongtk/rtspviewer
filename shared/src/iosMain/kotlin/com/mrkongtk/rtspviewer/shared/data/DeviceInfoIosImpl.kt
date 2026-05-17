package com.mrkongtk.rtspviewer.shared.data

import platform.Foundation.NSBundle

class DeviceInfoIosImpl : DeviceInfo {
    /**
     * Retrieves the bundle identifier from the main application bundle.
     * Returns "unknown" if the identifier cannot be resolved.
     */
    override val bundleId: String
        get() = NSBundle.mainBundle.bundleIdentifier ?: "unknown"
}
