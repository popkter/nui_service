package com.senseauto.nui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.senseauto.nui.ui.theme.MyApplicationTheme
import com.senseauto.nui_service.dialogue.ChatContent
import com.senseauto.nui_service.dialogue.ChatData
import com.senseauto.nui_service.dialogue.Role
import com.senseauto.nui_service.nlu.GeminiNlu
import com.senseauto.nui_service.platform
import kotlinx.coroutines.launch
import kotlin.String

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {

                var response by remember { mutableStateOf("sss") }

                LaunchedEffect(Unit) {

                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = response,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    Text(
        text = "Hello $name!",
        modifier = modifier.clickable(onClick = {
            coroutineScope.launch {
                val intent = GeminiNlu.recognize(ChatData(
                    chatContent = ChatContent(
                        role = Role.User,
                        content = "上海明天的天气怎么样"
                    ),
                    userId = "123",
                    userName = "小明",
                ))
                println("PopKter: ${intent.intents.toList()}")
            }
        })
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}