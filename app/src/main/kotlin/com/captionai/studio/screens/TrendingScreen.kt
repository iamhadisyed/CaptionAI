package com.captionai.studio.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.captionai.studio.ai.GeminiClient
import com.captionai.studio.storage.DatabaseHelper
import com.captionai.studio.storage.PrefsManager
import com.captionai.studio.storage.TrendingCaption
import kotlinx.coroutines.launch

@Composable
fun TrendingScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = PrefsManager.getInstance()
    val db = DatabaseHelper.getInstance(context)
    val coroutineScope = rememberCoroutineScope()

    var captions by remember { mutableStateOf<List<TrendingCaption>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        captions = db.getTrendingCaptions()
    }

    val canSync = prefs.canSync()
    val nextSyncHours = prefs.nextSyncInHours()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                Text(
                    "Trending",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Get viral captions synced from AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Sync Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(0.05f)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).background(Color(0xFF7D5FFF).copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Text("⚡", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cloud Sync", fontWeight = FontWeight.Bold)
                            Text(
                                if (canSync) "Ready to fetch new trends" else "Next sync in ${nextSyncHours}h",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                if (canSync && !isSyncing) {
                                    coroutineScope.launch {
                                        isSyncing = true
                                        val res = GeminiClient.syncTrendingCaptions(prefs.getLanguage())
                                        res.onSuccess { newCaptions ->
                                            db.saveTrendingCaptions(newCaptions)
                                            prefs.setLastSyncTime(System.currentTimeMillis())
                                            captions = db.getTrendingCaptions()
                                            Toast.makeText(context, "Trends Updated!", Toast.LENGTH_SHORT).show()
                                        }.onFailure { error ->
                                            Toast.makeText(context, "Sync Failed: ${error.message}", Toast.LENGTH_LONG).show()
                                        }
                                        isSyncing = false
                                    }
                                }
                            },
                            enabled = canSync && !isSyncing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D5FFF))
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("Sync")
                            }
                        }
                    }
                    
                    // Added Explanation Section
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.2f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Trending data is a collection of viral-potential phrases and topics fetched directly from AI. Use these to stay ahead of social media algorithms.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (captions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No trends synced yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(captions.size) { index ->
                        val caption = captions[index]
                        TrendingItem(index + 1, caption.caption)
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingItem(number: Int, text: String) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(5.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(0.02f)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#$number", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Black, 
                color = Color(0xFF7D5FFF).copy(0.2f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            
            IconButton(
                onClick = { 
                    copyToClipboard(context, text)
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp), tint = Color(0xFF7D5FFF))
            }
        }
    }
}
