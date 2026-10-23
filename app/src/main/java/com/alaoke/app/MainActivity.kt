package com.alaoke.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alaoke.app.ui.AlaokeViewModel
import com.alaoke.app.ui.AlaokeViewModelFactory
import com.alaoke.app.ui.navigation.AlaokeNavHost
import com.alaoke.app.ui.theme.AlaokeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AlaokeApp

        setContent {
            AlaokeRoot(app)
        }
    }
}

@Composable
private fun AlaokeRoot(app: AlaokeApp) {
    val viewModel: AlaokeViewModel = viewModel(factory = AlaokeViewModelFactory(app.repository))

    AlaokeTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AlaokeNavHost(viewModel = viewModel)
        }
    }
}
