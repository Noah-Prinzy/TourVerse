package com.tourverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tourverse.ui.screens.HomeScreen
import com.tourverse.ui.theme.TourismTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TourismTheme {
                HomeScreen()
            }
        }
    }
}
