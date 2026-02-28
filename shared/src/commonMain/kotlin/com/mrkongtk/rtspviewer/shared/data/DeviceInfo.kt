package com.mrkongtk.rtspviewer.shared.data

/**
 * Provides access to platform-specific device and application metadata.
 */
interface DeviceInfo {
    /**
     * The unique identifier of the application.
     * Maps to the 'Bundle Identifier' on iOS and the 'Application ID' (Package Name) on Android.
     */
    val bundleId: String
}
