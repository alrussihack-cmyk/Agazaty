package com.alrussihack.agazaty

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alrussihack.agazaty.ui.AgazatyApp
import com.alrussihack.agazaty.ui.theme.AgazatyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE)
        )
        super.onCreate(savedInstanceState)
        setContent {
            AgazatyTheme { AgazatyApp(viewModel()) }
        }
    }
}
