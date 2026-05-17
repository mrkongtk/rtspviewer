package com.mrkongtk.rtspviewer.shared.util

import android.util.Log

actual object AppLog {
    actual fun d(tag: String, msg: String, throwable: Throwable?) {
        Log.d(tag, msg, throwable)
    }

    actual fun e(tag: String, msg: String, throwable: Throwable?) {
        Log.e(tag, msg, throwable)
    }
}