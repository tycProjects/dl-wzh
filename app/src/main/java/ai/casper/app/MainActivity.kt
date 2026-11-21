package ai.casper.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.casper.app.ui.CasperTheme
import ai.casper.app.ui.ChatApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CasperTheme {
                val vm: ChatViewModel = viewModel()
                ChatApp(vm)
            }
        }
    }
}
