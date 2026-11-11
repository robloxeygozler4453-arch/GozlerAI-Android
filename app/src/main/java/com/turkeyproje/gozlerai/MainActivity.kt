package com.turkeyproje.gozlerai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF10131C)
private val Panel = Color(0xFF1A2030)
private val Accent = Color(0xFF7C8CFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GozlerAIApp() }
    }
}

private data class ChatMessage(val text: String, val fromUser: Boolean)
private data class ModelInfo(val name: String, val version: String, val size: String, val repo: String, val file: String, val description: String)

@Composable
private fun GozlerAIApp() {
    var tab by remember { mutableIntStateOf(0) }
    var prompt by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("GözlerAI v2") }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Ink, surface = Panel)) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.Chat, null) }, label = { Text("Sohbet") })
                    NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.Download, null) }, label = { Text("Modeller") })
                    NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Ayarlar") })
                }
            }
        ) { padding ->
            when (tab) {
                0 -> ChatScreen(Modifier.padding(padding), selectedModel, messages, prompt, { prompt = it }) {
                    val text = prompt.trim()
                    if (text.isNotEmpty()) {
                        messages.add(ChatMessage(text, true))
                        messages.add(ChatMessage("Yerel model motoru henüz entegre edilmedi. Bu arayüz prototipidir.", false))
                        prompt = ""
                    }
                }
                1 -> ModelScreen(Modifier.padding(padding), selectedModel) { selectedModel = it }
                else -> SettingsScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun ChatScreen(modifier: Modifier, modelName: String, messages: List<ChatMessage>, prompt: String, onPromptChange: (String) -> Unit, onSend: () -> Unit) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("GözlerAI", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Yerel yapay zekâ asistanın", color = Color.LightGray)
        Spacer(Modifier.height(12.dp))
        Surface(color = Panel, shape = RoundedCornerShape(14.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Aktif model", color = Color.LightGray, fontSize = 12.sp)
                    Text(modelName, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                AssistChip(onClick = {}, label = { Text("Motor bekleniyor") })
            }
        }
        Spacer(Modifier.height(12.dp))
        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✦", color = Accent, fontSize = 42.sp)
                    Text("Nasıl yardımcı olabilirim?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Sohbet motoru henüz etkin değil.", color = Color.LightGray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(messages) { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                        Surface(color = if (message.fromUser) Color(0xFF303B68) else Panel, shape = RoundedCornerShape(16.dp)) {
                            Text(message.text, Modifier.widthIn(max = 310.dp).padding(12.dp), color = Color.White)
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = prompt, onValueChange = onPromptChange, modifier = Modifier.weight(1f), placeholder = { Text("Mesajını yaz...") }, maxLines = 4, shape = RoundedCornerShape(18.dp))
            Spacer(Modifier.width(8.dp))
            Button(onClick = onSend, enabled = prompt.isNotBlank()) { Text("Gönder") }
        }
    }
}

@Composable
private fun ModelScreen(modifier: Modifier, selectedModel: String, onSelect: (String) -> Unit) {
    val models = listOf(
        ModelInfo("GözlerAI v2", "2.0", "~808 MB", "TurkeyProje/GozlerAI-1B-v2-GGUF", "GozlerAI-v2-Q4_K_M.gguf", "Güncel Türkçe LoRA sürümü · GGUF Q4_K_M"),
        ModelInfo("GözlerAI v1", "1.0", "~808 MB", "TurkeyProje/GozlerAI-1B-Merged-GGUF", "GozlerAI-1B-Q4_K_M.gguf", "İlk yayımlanan GGUF sürümü · Q4_K_M")
    )
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Model kütüphanesi", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Yalnızca resmî GözlerAI modelleri", color = Color.LightGray)
        Spacer(Modifier.height(16.dp))
        models.forEach { model ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(model.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Sürüm ${model.version} · ${model.size}", color = Color.LightGray, fontSize = 12.sp)
                        }
                        if (selectedModel == model.name) AssistChip(onClick = {}, label = { Text("Seçili") })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(model.description, color = Color.White, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onSelect(model.name) }, modifier = Modifier.fillMaxWidth()) { Text(if (selectedModel == model.name) "Seçili model" else "Bu modeli seç") }
                    Text("HF: ${model.repo}/blob/main/${model.file}", color = Color(0xFFB9C3FF), fontSize = 10.sp)
                }
            }
        }
        Text("Model indirme ve cihazda çalıştırma henüz bağlanmadı.", color = Color.LightGray, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Ayarlar", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("GözlerAI Android", fontWeight = FontWeight.Bold, color = Color.White)
                Text("Sürüm 1.0.0 · İlk arayüz prototipi", color = Color.LightGray)
                Spacer(Modifier.height(8.dp))
                Text("Android 8.0+ · ARM64 ve x86_64", color = Color.White)
            }
        }
    }
}
