package com.mrkongtk.rtspviewer.shared.util

expect object AppLog {
    fun d(tag: String, msg: String, throwable: Throwable? = null)
    fun e(tag: String, msg: String, throwable: Throwable? = null)
}
