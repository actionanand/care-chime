package com.actionanand.carechime.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.actionanand.carechime.app.ui.navigation.CareChimeApp
import com.actionanand.carechime.app.ui.theme.CareChimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CareChimeTheme {
                CareChimeApp()
            }
        }
    }
}
