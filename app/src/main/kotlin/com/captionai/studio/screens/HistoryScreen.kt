package com.captionai.studio.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.captionai.studio.storage.DatabaseHelper
import java.text.SimpleDateFormat
import java.util.*

data class HistoryItem(
    val id: Long,
    val caption: String,
    val hashtags: String,
    val platform: String,
    val createdAt: Long
)

@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val db = DatabaseHelper.getInstance(context)
    var historyItems by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var showClearDialog by remember { mutableStateOf(false) }
    
    fun refreshHistory() {
        try {
            historyItems = db.getHistory().map {
                HistoryItem(it.id, it.caption, it.hashtags, it.platform, it.createdAt)
            }
        } catch (e: Exception) {
            android.util.Log.e("HistoryScreen", "Failed to load history", e)
        }
    }

    LaunchedEffect(Unit) {
        refreshHistory()
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "History",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Review your past creations",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                if (historyItems.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.background(Color.Red.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.DeleteSweep, null, tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (historyItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 80.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.History, 
                                    null, 
                                    tint = Color(0xFF7D5FFF).copy(alpha = 0.3f), 
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "No history found", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Your generated captions will\nautomatically appear here.", 
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 48.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { navController.navigate(Screen.Home.route) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D5FFF))
                        ) {
                            Text("Magic Generate", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(historyItems) { item ->
                        ModernHistoryItem(item, context)
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History?") },
            text = { Text("Are you sure you want to delete all history? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        db.clearHistory()
                        refreshHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear All", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ModernHistoryItem(item: HistoryItem, context: Context) {
    val accentColor = when(item.platform.uppercase()) {
        "INSTAGRAM" -> Color(0xFFE1306C)
        "TIKTOK" -> Color(0xFF000000)
        "FACEBOOK" -> Color(0xFF1877F2)
        "WHATSAPP" -> Color(0xFF25D366)
        "LINKEDIN" -> Color(0xFF0077B5)
        else -> Color(0xFF7D5FFF)
    }

    Surface(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(0.04f)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(item.platform, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = accentColor)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.createdAt)), style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(item.caption, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 4, lineHeight = 24.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(
                    onClick = { 
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Caption AI", item.caption))
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp), tint = Color(0xFF7D5FFF))
                }
            }
        }
    }
}
