package com.kyawgyi.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0A0A10)) {
                    Column(
                        Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🕷️ Kyaw Gyi Scanner", color = Color(0xFFFF44CC),
                            fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Wi-Fi Portal Detector — Safe Demo",
                            color = Color(0xFF66CCFF), textAlign = TextAlign.Center)
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E))) {
                            Column(Modifier.padding(16.dp)) {
                                Text("ဒီ ZIP သည် စတင်တည်ဆောက်နိုင်သော Android project နမူနာဖြစ်သည်။",
                                    color = Color.White)
                                Spacer(Modifier.height(8.dp))
                                Text("Voucher ခန့်မှန်းစမ်းသပ်ခြင်း မပါဝင်ပါ။ ကိုယ်ပိုင်/ခွင့်ပြုထားသော network အတွက် portal detection ကို သီးခြားစမ်းသပ်ပါ။",
                                    color = Color(0xFFFFCC33))
                            }
                        }
                        var message by remember { mutableStateOf("Ready") }
                        Button(onClick = { message = "Demo UI အလုပ်လုပ်နေပါသည်" }) {
                            Text("TEST UI")
                        }
                        Text(message, color = Color(0xFF22CC77))
                    }
                }
            }
        }
    }
}
