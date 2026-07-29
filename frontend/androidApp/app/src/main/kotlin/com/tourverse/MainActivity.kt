package com.tourverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tourverse.ui.navigation.TourVerseApp
import com.tourverse.ui.theme.TourismTheme

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = AppContainer(applicationContext)

        setContent {
            TourismTheme {
                TourVerseApp(container)
            }
        }
    }
}
