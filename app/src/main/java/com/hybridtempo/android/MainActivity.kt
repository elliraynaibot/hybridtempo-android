package com.hybridtempo.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hybridtempo.android.ui.HybridTempoApp
import com.hybridtempo.android.ui.theme.HybridTempoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HybridTempoTheme {
                HybridTempoApp()
            }
        }
    }
}
