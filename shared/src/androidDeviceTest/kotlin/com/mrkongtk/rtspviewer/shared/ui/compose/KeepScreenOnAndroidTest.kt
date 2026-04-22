package com.mrkongtk.rtspviewer.shared.ui.compose

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import kotlin.test.AfterTest

class KeepScreenOnAndroidTest: KeepScreenOnTest() {

    var view: View? = null

    @AfterTest
    fun clearView() {
        view = null
    }

    @Composable
    override fun WhenContentSet() {
        view = LocalView.current
    }

    override fun isScreenKeepOn(): Boolean? {
        return view?.keepScreenOn
    }
}
