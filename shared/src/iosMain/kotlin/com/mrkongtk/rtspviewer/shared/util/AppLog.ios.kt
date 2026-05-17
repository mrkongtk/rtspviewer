package com.mrkongtk.rtspviewer.shared.util

import platform.Foundation.NSLog

actual object AppLog {
    actual fun d(tag: String, msg: String, throwable: Throwable?) {
        NSLog("[$tag] $msg${throwable?.let { " $it" } ?: ""}")
    }

    actual fun e(tag: String, msg: String, throwable: Throwable?) {
        NSLog("[$tag] $msg${throwable?.let { " $it" } ?: ""}")
    }
}