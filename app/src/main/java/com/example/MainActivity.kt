package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.MainScreenContainer
import com.example.ui.WorldCupViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    
    // Instantiate our unified state ViewModel
    private val viewModel: WorldCupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Dynamic live score updates (triggers check for user entered API key in local secrets)
        val apiKey = try {
            BuildConfig.FOOTBALL_API_KEY
        } catch (e: Exception) {
            ""
        }
        viewModel.triggerApiRefresh(apiKey)

        setContent {
            MyApplicationTheme {
                MainScreenContainer(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
