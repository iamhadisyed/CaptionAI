package com.captionai.studio.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.captionai.studio.ai.CaptionRequest
import com.captionai.studio.ai.CaptionResult
import com.captionai.studio.ai.GeminiClient
import com.captionai.studio.storage.DatabaseHelper
import com.captionai.studio.storage.PrefsManager
import com.captionai.studio.utils.AdManager
import com.captionai.studio.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    navController: NavController,
    platform: String,
    content: String,
    tone: String,
    length: String,
    contextArg: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = PrefsManager.getInstance()
    val db = DatabaseHelper.getInstance(context)

    val request = CaptionRequest(
        platform = platform,
        contentType = content,
        tone = tone,
        length = length,
        userContext = contextArg,
        language = prefs.getLanguage()
    )

    var result by remember { mutableStateOf<CaptionResult?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var favoritedItems by remember { mutableStateOf(setOf<String>()) }

    var attempt by remember { mutableStateOf(0) }
    // True when the last result was already delivered (rotation, back-navigation, "View Last Result"):
    // show it again without calling the AI or charging quota.
    val viewOnly = remember { GeminiClient.showLastResultOnly }

    LaunchedEffect(attempt) {
        isLoading = true
        error = null
        val cached = GeminiClient.lastResult
        if (viewOnly && attempt == 0 && cached != null) {
            result = cached
            isLoading = false
            return@LaunchedEffect
        }
        val imageBase64 = GeminiClient.pendingImageBase64
        val res = GeminiClient.generateCaptions(request, imageBase64, forceRefresh = attempt > 0)
        res.onSuccess {
            result = it
            GeminiClient.showLastResultOnly = true
            favoritedItems = withContext(Dispatchers.IO) {
                (it.captions + it.hashtags.joinToString(" ") + it.description).filter { t -> t.isNotBlank() && db.isFavorite(t) }.toSet()
            }
            // Charge quota only now that the user actually received a result.
            prefs.consumeGeneration()
            withContext(Dispatchers.IO) { db.saveCaptionToHistory(it) }
            (context as? Activity)?.let { act -> AdManager.onCaptionGenerated(act) }
        }.onFailure {
            error = friendlyError(it)
        }
        // Don't let this photo leak into later requests.
        GeminiClient.pendingImageBase64 = null
        isLoading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", modifier = Modifier.size(18.dp), tint = Color(0xFF7D5FFF))
                    }
                    
                    Text(
                        "MAGIC RESULTS", 
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Black, 
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            when {
                isLoading -> LoadingStateModern()
                error != null -> ErrorStateModern(error!!) { navController.popBackStack() }
                result != null -> ResultContentModern(
                    result = result!!,
                    canRegenerate = prefs.canGenerate(),
                    onRegenerate = { attempt++ },
                context = context,
                favoritedItems = favoritedItems,
                onSaveFavorite = { caption -> 
                    result?.let { 
                        db.saveFavorite(caption, it.platform)
                        favoritedItems = favoritedItems + caption
                    } 
                }
                )
            }
        }
    }
}

/** Turns low-level exceptions into messages a user can act on. */
private fun friendlyError(t: Throwable): String {
    val m = t.message.orEmpty()
    return when {
        t is GeminiClient.RateLimitException -> m
        t is java.net.UnknownHostException || t is java.net.SocketTimeoutException || t is java.net.ConnectException ->
            "No internet connection or the AI service timed out. Check your connection and try again."
        m.contains("429") || m.contains("quota", true) || m.contains("rate", true) ->
            "The AI service is busy right now. Please try again in a minute."
        m.contains("401") || m.contains("403") || m.contains("API key", true) ->
            "The AI key was rejected. If you set a custom key in Settings, check that it is valid."
        m.contains("404") -> "The AI model is temporarily unavailable. Please try again later."
        m.isBlank() -> "AI request failed. Please try again."
        else -> m
    }
}

@Composable
fun LoadingStateModern() {
    Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 6.dp, 
                    color = Color(0xFF7D5FFF)
                )
                Icon(
                    Icons.Default.AutoAwesome, 
                    contentDescription = null, 
                    modifier = Modifier.size(32.dp),
                    tint = Color(0xFF7D5FFF)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                "AI is crafting magic...", 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Mixing the perfect words for you", 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ErrorStateModern(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = Color.Red.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color.Red)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("AI Generation Failed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D5FFF))
        ) {
            Text("Try Again", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ResultContentModern(
    result: CaptionResult, 
    canRegenerate: Boolean,
    onRegenerate: () -> Unit,
    context: Context, 
    favoritedItems: Set<String>,
    onSaveFavorite: (String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 40.dp)) {
        // Tag list
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = Color(0xFF7D5FFF).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                Text(result.platform, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF7D5FFF))
            }
        }

        result.captions.forEachIndexed { index, caption ->
            ModernCaptionCard(
                index = index + 1,
                caption = caption,
                isFavorited = favoritedItems.contains(caption),
                onCopy = { 
                    copyToClipboard(context, caption)
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                },
                onSave = { 
                    onSaveFavorite(caption)
                    Toast.makeText(context, "Saved to Favorites", Toast.LENGTH_SHORT).show()
                },
                onShare = { shareCaption(context, caption) },
                onShareImage = { ImageUtils.shareCaptionCard(context, caption, result.platform) }
            )
        }

        SectionLabel("Hashtags")
        ModernHashtagCard(
            hashtags = result.hashtags,
            isFavorited = favoritedItems.contains(result.hashtags.joinToString(" ")),
            onCopy = { 
                copyToClipboard(context, result.hashtags.joinToString(" "))
                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
            },
            onSave = {
                onSaveFavorite(result.hashtags.joinToString(" "))
                Toast.makeText(context, "Hashtags saved!", Toast.LENGTH_SHORT).show()
            }
        )

        if (result.description.isNotBlank()) {
            SectionLabel("Post Description")
            ModernDescriptionCard(
                description = result.description,
                isFavorited = favoritedItems.contains(result.description),
                onCopy = { 
                    copyToClipboard(context, result.description)
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                },
                onSave = {
                    onSaveFavorite(result.description)
                    Toast.makeText(context, "Description saved!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (result.bio.isNotBlank()) {
            SectionLabel("Suggested Bio")
            ModernBioCard(
                bio = result.bio,
                onCopy = { 
                    copyToClipboard(context, result.bio)
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRegenerate,
            enabled = canRegenerate,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (canRegenerate) "Regenerate" else "Daily limit reached", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "AI might make mistakes. Verify before posting.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@Composable
fun ModernCaptionCard(index: Int, caption: String, isFavorited: Boolean, onCopy: () -> Unit, onSave: () -> Unit, onShare: () -> Unit, onShareImage: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp)
            .shadow(20.dp, RoundedCornerShape(32.dp), ambientColor = Color.Black.copy(0.08f), spotColor = Color.Black.copy(0.08f)),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFF7D5FFF).copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text("Option #$index", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF7D5FFF))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = onSave, modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                        Icon(
                            if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                            contentDescription = null, 
                            modifier = Modifier.size(18.dp), 
                            tint = if (isFavorited) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(caption, style = MaterialTheme.typography.bodyLarge, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val accentGradient = Brush.linearGradient(colors = listOf(Color(0xFF7D5FFF), Color(0xFF6C5CE7)))
                
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D5FFF).copy(alpha = 0.1f), contentColor = Color(0xFF7D5FFF)),
                    elevation = null
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share", fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
                
                Button(
                    onClick = onShareImage,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(accentGradient), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Image", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernHashtagCard(hashtags: List<String>, isFavorited: Boolean, onCopy: () -> Unit, onSave: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(0.03f), spotColor = Color.Black.copy(0.03f)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Relevant Tags", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                IconButton(onClick = onSave, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                        null, 
                        modifier = Modifier.size(16.dp), 
                        tint = if (isFavorited) Color.Red else Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(hashtags.joinToString(" "), color = Color(0xFF7D5FFF), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCopy, 
                modifier = Modifier.fillMaxWidth().height(44.dp), 
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = Color(0xFF7D5FFF))
            ) {
                Text("Copy all Tags", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ModernBioCard(bio: String, onCopy: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(0.03f), spotColor = Color.Black.copy(0.03f)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(bio, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onCopy, modifier = Modifier.align(Alignment.End)) {
                Text("Copy Bio", fontWeight = FontWeight.Bold, color = Color(0xFF7D5FFF))
            }
        }
    }
}

@Composable
fun ModernDescriptionCard(description: String, isFavorited: Boolean, onCopy: () -> Unit, onSave: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(0.03f), spotColor = Color.Black.copy(0.03f)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(description, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSave, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                        null, 
                        modifier = Modifier.size(18.dp), 
                        tint = if (isFavorited) Color.Red else Color(0xFF7D5FFF)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onCopy) {
                    Text("Copy Description", fontWeight = FontWeight.Bold, color = Color(0xFF7D5FFF))
                }
            }
        }
    }
}

// Utility functions removed, now using CommonUI.kt versions

fun shareCaption(context: Context, caption: String) {
    val shareText = "$caption\n\n— Generated with Caption AI"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Caption"))
}
