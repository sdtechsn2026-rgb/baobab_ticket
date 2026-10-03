package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ai.GeminiService
import com.example.data.repository.BaobabRepository
import com.example.ui.screens.MainAppScreen
import com.example.ui.theme.GraphiteBlack
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BaobabViewModel

class MainActivity : ComponentActivity() {

    private lateinit var repository: BaobabRepository
    private lateinit var viewModel: BaobabViewModel
    private lateinit var geminiService: GeminiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = BaobabRepository(applicationContext)
        viewModel = BaobabViewModel(repository)
        geminiService = GeminiService()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GraphiteBlack
                ) {
                    MainAppScreen(
                        viewModel = viewModel,
                        geminiService = geminiService
                    )
                }
            }
        }
    }
}
