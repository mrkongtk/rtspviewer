package com.mrkongtk.rstpviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mrkongtk.rstpviewer.ui.screen.NavigationScreen
import com.mrkongtk.rstpviewer.ui.theme.RSTPViewerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RSTPViewerTheme {
                NavigationScreen()
            }
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun GreetingPreview() {
    RSTPViewerTheme {
        NavigationScreen()
    }
}