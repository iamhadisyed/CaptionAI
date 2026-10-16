package com.captionai.studio.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.captionai.studio.ai.GeminiClient
import com.captionai.studio.storage.PrefsManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.viewinterop.AndroidView
import com.captionai.studio.utils.AdManager
import com.google.android.gms.ads.AdView
import java.io.InputStream
import java.io.ByteArrayOutputStream

enum class Platform(val label: String, val color: Color, val icon: ImageVector) {
    INSTAGRAM("Instagram", Color(0xFFFF0069), Icons.Default.CameraAlt),
    FACEBOOK("Facebook", Color(0xFF1877F2), Icons.Default.Facebook),
    LINKEDIN("LinkedIn", Color(0xFF0077B5), Icons.Default.Work),
    WHATSAPP("WhatsApp", Color(0xFF25D366), Icons.Default.Chat),
    TIKTOK("TikTok", Color(0xFF010101), Icons.Default.MusicNote)
}

val CONTENT_TYPES = listOf("✨ Selfie", "✈️ Travel", "❤️ Love", "😂 Funny", "🔥 Attitude", "💪 Motivation", "😔 Sad")
val TONES = listOf("Cool", "Romantic", "Savage", "Deep", "Professional")
val LENGTHS = listOf("Short", "Standard", "Long")

@Composable
fun HomeScreen(navController: NavController) {
    val prefs = PrefsManager.getInstance()
    var selectedPlatform by remember { mutableStateOf(Platform.INSTAGRAM) }
    var selectedContent by remember { mutableStateOf("✨ Selfie") }
    var selectedTone by remember { mutableStateOf("Cool") }
    var selectedLength by remember { mutableStateOf("Standard") }
    var userContext by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val dailyCount = remember(navBackStackEntry) { prefs.getDailyCount() }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFF7D5FFF).copy(alpha = 0.05f),
                radius = 350.dp.toPx(),
                center = Offset(size.width * 0.9f, size.height * 0.1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Modern App Bar Section
            HomeAppBar(isPro = prefs.isPro(), navController = navController)

            // Daily Limit Indicator (Modern UI)
            if (!prefs.isPro()) {
                Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                    Surface(
                        color = Color(0xFF7D5FFF).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF7D5FFF).copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF7D5FFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "FREE GENERATIONS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF7D5FFF),
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${3 - dailyCount} of 3 generations left today",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
// Removed Demo Mode indicator for live build

            // Feature Card (Analyzing Photo)
            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFFF9F43).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "Top Feature",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF9F43),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF7D5FFF), modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("AI Visual Insights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Let the AI see your photo to write better", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Image Picker Slot
                        ImageUploadSlot(
                            uri = selectedImageUri,
                            onPick = { imagePickerLauncher.launch("image/*") },
                            onRemove = { selectedImageUri = null }
                        )
                    }
                }
            }

            // Platform Grid
            SectionLabel("Target Platform")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(Platform.entries.toTypedArray()) { platform ->
                    PlatformIconButton(
                        platform = platform,
                        isSelected = selectedPlatform == platform,
                        onSelect = { selectedPlatform = platform },
                        modifier = Modifier.width(72.dp)
                    )
                }
            }

            // Tabs / Chips
            SectionLabel("Choose Vibe")
            PillSelectionRow(items = CONTENT_TYPES, selected = selectedContent, onSelect = { selectedContent = it })
            
            Spacer(modifier = Modifier.height(12.dp))
            PillSelectionRow(items = TONES, selected = selectedTone, onSelect = { selectedTone = it })

            SectionLabel("Caption Length")
            PillSelectionRow(items = LENGTHS, selected = selectedLength, onSelect = { selectedLength = it })

            // Context Input
            SectionLabel("Extra Context")
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = userContext,
                        onValueChange = { userContext = it },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        placeholder = { Text("What's happening in this photo?", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }

            // Action Button
            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Column {
                    if (GeminiClient.lastResult != null) {
                        OutlinedButton(
                            onClick = {
                                val last = GeminiClient.lastRequest
                                if (last != null) {
                                    navController.navigate(
                                        Screen.Result.route + "?platform=${last.platform}" +
                                                "&content=${Uri.encode(last.contentType)}&tone=${Uri.encode(last.tone)}" +
                                                "&length=${Uri.encode(last.length)}&context=${Uri.encode(last.userContext)}"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF7D5FFF).copy(0.3f))
                        ) {
                            Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp), tint = Color(0xFF7D5FFF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Last Result", color = Color(0xFF7D5FFF), fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    val primaryGradient = Brush.linearGradient(
                        colors = listOf(Color(0xFF7D5FFF), Color(0xFF6C5CE7))
                    )

                    Button(
                        onClick = {
                            if (userContext.isBlank() && selectedImageUri == null) {
                                Toast.makeText(context, "Please add context or a photo!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!isLoading) {
                                if (!prefs.consumeGeneration()) {
                                Toast.makeText(context, "Daily limit reached! Upgrade to Pro for more.", Toast.LENGTH_LONG).show()
                                navController.navigate(Screen.Settings.route)
                                return@Button
                            }
                                isLoading = true
                                if (selectedImageUri != null) {
                                    try {
                                        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                        context.contentResolver.openInputStream(selectedImageUri!!).use { stream ->
                                            BitmapFactory.decodeStream(stream, null, options)
                                        }

                                        var scale = 1
                                        while (options.outWidth / scale > 1024 || options.outHeight / scale > 1024) {
                                            scale *= 2
                                        }

                                        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
                                        val bitmap = context.contentResolver.openInputStream(selectedImageUri!!).use { stream ->
                                            BitmapFactory.decodeStream(stream, null, decodeOptions)
                                        }

                                        bitmap?.let {
                                            val outputStream = ByteArrayOutputStream()
                                            it.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                                            GeminiClient.pendingImageBase64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                                            it.recycle()
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("HomeScreen", "Image processing failed", e)
                                    }
                                }
                                navController.navigate(
                                    Screen.Result.route + "?platform=${selectedPlatform.name}" +
                                            "&content=${Uri.encode(selectedContent)}&tone=${Uri.encode(selectedTone)}" +
                                            "&length=${Uri.encode(selectedLength)}&context=${Uri.encode(userContext)}"
                                )
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .shadow(12.dp, RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(primaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("MAGIC GENERATE", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp, color = Color.White)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(20.dp), tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Rewarded Ad button — shown only when limit reached or as bonus option
            if (!prefs.isPro() && prefs.getDailyCount() >= 3) {
                RewardedAdButton {
                    prefs.addBonusGenerations(3)
                    Toast.makeText(context, "🎉 3 bonus generations added!", Toast.LENGTH_SHORT).show()
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Banner Ad at the bottom — earns every session
            BannerAdView()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HomeAppBar(isPro: Boolean, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp).clickable { navController.navigate(Screen.History.route) },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.GridView, null, modifier = Modifier.size(24.dp), tint = Color(0xFF7D5FFF))
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Caption AI", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("Smart Creative Lab", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        
        Surface(
            modifier = Modifier.size(48.dp).clickable { navController.navigate(Screen.Settings.route) },
            shape = RoundedCornerShape(16.dp),
            color = if (isPro) Color(0xFF7D5FFF) else MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPro) Icons.Default.WorkspacePremium else Icons.Default.Person,
                    null,
                    tint = if (isPro) Color.White else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ImageUploadSlot(uri: Uri?, onPick: () -> Unit, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { if (uri == null) onPick() },
        contentAlignment = Alignment.Center
    ) {
        if (uri == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFF7D5FFF), modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tap to add photo", color = Color(0xFF7D5FFF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        } else {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF25D366).copy(0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, null, tint = Color(0xFF25D366))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Image Uploaded", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.DeleteOutline, null, tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun PlatformIconButton(platform: Platform, isSelected: Boolean, onSelect: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onSelect() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = if (isSelected) platform.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            shadowElevation = if (isSelected) 12.dp else 0.dp,
            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    platform.icon,
                    null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            platform.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun PillSelectionRow(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items) { item ->
            val isSelected = item == selected
            Surface(
                onClick = { onSelect(item) },
                shape = CircleShape,
                color = if (isSelected) Color(0xFF7D5FFF) else MaterialTheme.colorScheme.surface,
                shadowElevation = if (isSelected) 4.dp else 1.dp
            ) {
                Text(
                    text = item,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) Color.White else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                )
            }
        }
    }
}


// Removed SectionLabel definition to CommonUI.kt
