package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.di.SmartMoneyViewModelFactory
import com.example.ui.navigation.SmartMoneyAppShell
import com.example.ui.theme.SmartMoneyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val app = application as SmartMoneyApplication
        val factory = SmartMoneyViewModelFactory(app.container)

        setContent {
            SmartMoneyTheme {
                SmartMoneyAppShell(factory = factory)
            }
        }
    }
}
