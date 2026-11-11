package com.turkeyproje.gozlerai

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val Ink = Color(0xFF0D111B)
private val Panel = Color(0xFF171F2E)
private val Accent = Color(0xFF91A0FF)
private val Muted = Color(0xFFADB6C8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GozlerAIApp() }
    }
}

@Composable
private fun GozlerAIApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ModelStore(context) }
    val chatStore = remember { ChatStore(context) }
    val prefs = remember { context.getSharedPreferences("gozlerai_settings", Context.MODE_PRIVATE) }

    var tab by remember { mutableIntStateOf(0) }
    var prompt by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf(prefs.getString("selected_model", "v2") ?: "v2") }
    var loadedId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Bir model indirerek başlayabilirsin.") }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadLabel by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(chatStore.load()) }
    var engineReady by remember { mutableStateOf(false) }

    fun saveMessages() = chatStore.save(messages)

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent,
        secondary = Accent,
        background = Ink,
        surface = Panel,
        onBackground = Color.White,
        onSurface = Color.White
    )) {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    NavigationBarItem(
                        selected = tab == 0, onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                        label = { Text("Sohbet") }
                    )
                    NavigationBarItem(
                        selected = tab == 1, onClick = { tab = 1 },
                        icon = { Icon(Icons.Default.Download, contentDescription = null) },
                        label = { Text("Modeller") }
                    )
                    NavigationBarItem(
                        selected = tab == 2, onClick = { tab = 2 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Ayarlar") }
                    )
                }
            }
        ) { padding ->
            when (tab) {
                0 -> ChatScreen(
                    modifier = Modifier.padding(padding),
                    selectedName = ModelCatalog.official.first { it.id == selectedId }.title,
                    loaded = loadedId == selectedId,
                    messages = messages,
                    prompt = prompt,
                    busy = busy,
                    status = status,
                    onPromptChange = { prompt = it },
                    onSend = {
                        val clean = prompt.trim()
                        if (clean.isNotEmpty() && !busy) {
                            if (loadedId != selectedId) {
                                status = "Önce Modeller sekmesinden seçili modeli yükle."
                            } else {
                                prompt = ""
                                messages = messages + ChatLine(clean, true)
                                saveMessages()
                                busy = true
                                status = "GözlerAI düşünüyor…"
                                scope.launch {
                                    val transcript = buildString {
                                        append("<|start_header_id|>system<|end_header_id|>\n")
                                        append("Sen GözlerAI adlı, Türkçe konuşan yardımcı bir yapay zekâsın. Doğru, açık ve kısa cevap ver. Bilmediğin bilgiyi uydurma.\n")
                                        append("<|eot_id|>\n")
                                        messages.takeLast(12).forEach { line ->
                                            append(if (line.fromUser) "<|start_header_id|>user<|end_header_id|>\n" else "<|start_header_id|>assistant<|end_header_id|>\n")
                                            append(line.text)
                                            append("\n<|eot_id|>\n")
                                        }
                                        append("<|start_header_id|>assistant<|end_header_id|>\n")
                                    }
                                    try {
                                        val answer = withContext(Dispatchers.Default) {
                                            NativeBridge.nativeGenerate(transcript, 256)
                                        }.trim()
                                        val finalText = answer.ifBlank { "Yanıt üretilemedi. Daha kısa bir mesaj dener misin?" }
                                        messages = messages + ChatLine(finalText, false)
                                        saveMessages()
                                        status = "Çevrimdışı · ${ModelCatalog.official.first { it.id == selectedId }.title}"
                                    } catch (e: Exception) {
                                        status = "Üretim hatası: ${e.message ?: "bilinmeyen hata"}"
                                    } finally {
                                        busy = false
                                    }
                                }
                            }
                        }
                    },
                    onClear = {
                        messages = emptyList()
                        chatStore.clear()
                        status = "Sohbet geçmişi temizlendi."
                    }
                )
                1 -> ModelsScreen(
                    modifier = Modifier.padding(padding),
                    selectedId = selectedId,
                    loadedId = loadedId,
                    busy = busy,
                    progress = downloadProgress,
                    progressLabel = downloadLabel,
                    status = status,
                    store = store,
                    onSelect = { model ->
                        selectedId = model.id
                        prefs.edit().putString("selected_model", model.id).apply()
                        status = "${model.title} seçildi."
                    },
                    onDownload = { model ->
                        if (!busy) {
                            busy = true
                            downloadProgress = 0f
                            downloadLabel = "Bağlanıyor…"
                            status = "${model.title} indiriliyor."
                            scope.launch {
                                try {
                                    store.download(model) { current, total ->
                                        downloadProgress = if (total > 0) current.toFloat() / total.toFloat() else 0f
                                        downloadLabel = if (total > 0) {
                                            "${formatBytes(current)} / ${formatBytes(total)}"
                                        } else formatBytes(current)
                                    }
                                    status = "${model.title} indirildi."
                                } catch (e: Exception) {
                                    status = "İndirme başarısız: ${e.message ?: "bağlantı hatası"}"
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    },
                    onLoad = { model ->
                        if (!busy) {
                            busy = true
                            status = "${model.title} belleğe yükleniyor…"
                            scope.launch {
                                try {
                                    val loaded = withContext(Dispatchers.IO) {
                                        NativeBridge.nativeLoadModel(store.fileFor(model).absolutePath)
                                    }
                                    if (loaded) {
                                        loadedId = model.id
                                        engineReady = true
                                        status = "${model.title} hazır. İnternet bağlantısı olmadan sohbet edebilirsin."
                                    } else {
                                        loadedId = null
                                        status = "Model yüklenemedi. Dosya bozuk olabilir veya cihaz belleği yetersiz kalmış olabilir."
                                    }
                                } catch (e: UnsatisfiedLinkError) {
                                    status = "Yerel motor başlatılamadı: ${e.message}"
                                } catch (e: Exception) {
                                    status = "Model yükleme hatası: ${e.message}"
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    },
                    onDelete = { model ->
                        if (!busy) {
                            if (loadedId == model.id) {
                                try { NativeBridge.nativeUnload() } catch (_: Exception) {}
                                loadedId = null
                                engineReady = false
                            }
                            store.delete(model)
                            status = "${model.title} cihazdan silindi."
                        }
                    }
                )
                else -> SettingsScreen(
                    modifier = Modifier.padding(padding),
                    loadedName = loadedId?.let { id -> ModelCatalog.official.first { it.id == id }.title } ?: "Yüklü model yok",
                    onClear = {
                        messages = emptyList()
                        chatStore.clear()
                        status = "Sohbet geçmişi temizlendi."
                    },
                    onUnload = {
                        try { NativeBridge.nativeUnload() } catch (_: Exception) {}
                        loadedId = null
                        engineReady = false
                        status = "Model bellekten kaldırıldı."
                    },
                    status = status
                )
            }
        }
    }
}

@Composable
private fun ChatScreen(
    modifier: Modifier,
    selectedName: String,
    loaded: Boolean,
    messages: List<ChatLine>,
    prompt: String,
    busy: Boolean,
    status: String,
    onPromptChange: (String) -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit
) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("GözlerAI", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Kendi cihazında çalışan yapay zekâ", fontSize = 12.sp, color = Muted)
            }
            IconButton(onClick = onClear, enabled = messages.isNotEmpty() && !busy) {
                Icon(Icons.Default.Delete, contentDescription = "Sohbeti temizle", tint = Muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(color = Panel, shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = Accent)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(selectedName, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(if (loaded) "Model bellekte · çevrimdışı hazır" else "Model yüklenmedi", color = Muted, fontSize = 11.sp)
                }
                Text(if (loaded) "HAZIR" else "BEKLEMEDE", color = if (loaded) Color(0xFF7EE0B0) else Color(0xFFFFC66D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✦", color = Accent, fontSize = 52.sp)
                    Text("Nasıl yardımcı olabilirim?", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Text("Sohbet etmek için önce bir model indirip yükle.", color = Muted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(messages) { _, line ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = if (line.fromUser) Arrangement.End else Arrangement.Start
                    ) {
                        Surface(
                            color = if (line.fromUser) Color(0xFF303B68) else Panel,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.widthIn(max = 320.dp)
                        ) {
                            Text(line.text, Modifier.padding(13.dp), color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        if (busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent)
        }
        Text(status, color = Muted, fontSize = 10.sp, maxLines = 2)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mesajını yaz…") },
                enabled = !busy,
                maxLines = 4,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onSend, enabled = prompt.isNotBlank() && !busy) {
                Icon(Icons.Default.Send, contentDescription = "Gönder")
            }
        }
    }
}

@Composable
private fun ModelsScreen(
    modifier: Modifier,
    selectedId: String,
    loadedId: String?,
    busy: Boolean,
    progress: Float,
    progressLabel: String,
    status: String,
    store: ModelStore,
    onSelect: (ModelInfo) -> Unit,
    onDownload: (ModelInfo) -> Unit,
    onLoad: (ModelInfo) -> Unit,
    onDelete: (ModelInfo) -> Unit
) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Model kütüphanesi", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Yalnızca resmî GözlerAI modelleri", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(ModelCatalog.official) { _, model ->
                val downloaded = store.isDownloaded(model)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Panel),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(model.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Sürüm ${model.version} · ${model.sizeLabel}", color = Muted, fontSize = 11.sp)
                            }
                            if (loadedId == model.id) {
                                AssistChip(onClick = {}, label = { Text("Yüklü") })
                            } else if (selectedId == model.id) {
                                AssistChip(onClick = {}, label = { Text("Seçili") })
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                        Text(model.description, color = Color.White, fontSize = 12.sp)
                        Text("Hugging Face · ${model.repository}", color = Accent, fontSize = 10.sp)
                        Spacer(Modifier.height(12.dp))
                        if (busy && progress > 0f && selectedId == model.id && !downloaded) {
                            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = Accent)
                            Text(progressLabel, color = Muted, fontSize = 11.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { onSelect(model) },
                                enabled = !busy,
                                modifier = Modifier.weight(1f)
                            ) { Text(if (selectedId == model.id) "Seçili" else "Seç") }
                            if (!downloaded) {
                                Button(
                                    onClick = { onDownload(model) },
                                    enabled = !busy,
                                    modifier = Modifier.weight(1f)
                                ) { Text("İndir") }
                            } else {
                                Button(
                                    onClick = { onLoad(model) },
                                    enabled = !busy && loadedId != model.id,
                                    modifier = Modifier.weight(1f)
                                ) { Text(if (loadedId == model.id) "Çalışıyor" else "Yükle") }
                                IconButton(onClick = { onDelete(model) }, enabled = !busy) {
                                    Icon(Icons.Default.Delete, contentDescription = "Modeli sil", tint = Color(0xFFFF8D8D))
                                }
                            }
                        }
                        if (downloaded) Text("Dosya cihazda mevcut", color = Color(0xFF7EE0B0), fontSize = 10.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(status, color = Muted, fontSize = 10.sp, maxLines = 2)
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    loadedName: String,
    onClear: () -> Unit,
    onUnload: () -> Unit,
    status: String
) {
    val context = LocalContext.current
    val memoryMb = remember {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        info.totalMem / (1024L * 1024L)
    }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Ayarlar", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(14.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Cihaz bilgisi", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = Accent)
                    Spacer(Modifier.width(8.dp))
                    Text("Toplam RAM: ${memoryMb / 1024} GB", color = Color.White)
                }
                Text("Yerel 1B model için yaklaşık 2–3 GB boş bellek gerekebilir. Gerçek ihtiyaç cihaza göre değişir.", color = Muted, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Text("Bellekteki model: $loadedName", color = Color.White)
                OutlinedButton(onClick = onUnload, modifier = Modifier.fillMaxWidth()) { Text("Modeli bellekten kaldır") }
                OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text("Sohbet geçmişini temizle") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("GözlerAI Android · 1.0.0", color = Muted)
        Text("İnternet yalnızca model indirmek için kullanılır. Model yüklendikten sonra sohbet cihaz üzerinde çalışır.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text(status, color = Muted, fontSize = 10.sp)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt().coerceIn(0, 3)
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
