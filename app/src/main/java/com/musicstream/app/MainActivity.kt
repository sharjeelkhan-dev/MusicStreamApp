package com.musicstream.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.musicstream.app.presentation.MainApp
import com.musicstream.app.ui.theme.MusicStreamTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicStreamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.musicstream.app.ui.theme.DarkBackground
                ) {
                    MainApp()
                }
            }
        }
    }
}
