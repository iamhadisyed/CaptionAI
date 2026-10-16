package com.captionai.studio.screens

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.captionai.studio.storage.PrefsManager
import androidx.compose.ui.platform.LocalContext
import com.captionai.studio.storage.DatabaseHelper
import com.captionai.studio.utils.BillingHelper
import android.app.Activity

@Composable
fun SettingsScreen(navController: NavController, billingHelper: BillingHelper) {
    val context = LocalContext.current
    val prefs = PrefsManager.getInstance()

    var selectedLanguage by remember { mutableStateOf(prefs.getLanguage()) }
    var apiKey by remember { mutableStateOf(prefs.getApiKey() ?: "") }
    var apiKeyVisible by remember { mutableStateOf(false) }
    val darkModeEnabled by prefs.darkModeState
    var showClearDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
            )

            // Pro upgrade banner
            ProUpgradeBanner(isPro = prefs.isPro()) {
                (context as? Activity)?.let { activity ->
                    billingHelper.launchPurchase(activity)
                }
            }

            SectionLabel("Language")
            LanguageSelector(
                selected = selectedLanguage,
                onSelect = {
                    selectedLanguage = it
                    prefs.setLanguage(it)
                }
            )

            SectionLabel("Preferences")
            // Settings rows
            SettingsSection {
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = if (darkModeEnabled) "Currently enabled" else "Currently disabled",
                    trailing = {
                        Switch(
                            checked = darkModeEnabled,
                            onCheckedChange = {
                                prefs.setDarkMode(it)
                            }
                        )
                    }
                )

                SettingsRow(
                    icon = Icons.Default.Delete,
                    title = "Clear History",
                    subtitle = "Remove all saved captions",
                    trailing = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null,
                            tint = Color.Gray)
                    },
                    onClick = { showClearDialog = true }
                )
            }

            var showAdvanced by remember { mutableStateOf(false) }
            TextButton(
                onClick = { showAdvanced = !showAdvanced },
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(if (showAdvanced) "Hide Advanced Settings" else "Show Advanced Settings", 
                    style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }

            if (showAdvanced) {
                SectionLabel("Custom AI Key (Advanced)")
                Text(
                    "Optional: Use your own AI key for higher rate limits.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                ApiKeyInput(
                    value = apiKey,
                    visible = apiKeyVisible,
                    onValueChange = { apiKey = it },
                    onToggleVisibility = { apiKeyVisible = !apiKeyVisible },
                    onSave = { 
                        prefs.setApiKey(apiKey)
                        Toast.makeText(context, "Key Saved!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear History") },
            text = { Text("This will remove all saved captions and history. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    DatabaseHelper.getInstance(context).clearHistory()
                    showClearDialog = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProUpgradeBanner(isPro: Boolean, onUpgrade: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(0.05f)),
        shape = RoundedCornerShape(24.dp),
        color = if (isPro) Color(0xFF7D5FFF) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(20.dp).clickable { if (!isPro) onUpgrade() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp).background(if (isPro) Color.White.copy(0.2f) else Color(0xFF7D5FFF).copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Text("⭐", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isPro) "Pro Active" else "Upgrade to Pro ($5/mo)",
                    style = MaterialTheme.typography.titleSmall, 
                    fontWeight = FontWeight.Bold,
                    color = if (isPro) Color.White else Color.Black
                )
                Text(
                    if (isPro) "Enjoy unlimited power" else "Get unlimited generations and high-priority AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPro) Color.White.copy(0.8f) else Color.Gray
                )
            }
            if (!isPro) {
                Icon(Icons.Default.ArrowForwardIos, null, modifier = Modifier.size(14.dp), tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun LanguageSelector(selected: String, onSelect: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("English", "اردو", "Roman Urdu").forEach { lang ->
            item {
                val isSelected = selected == lang
                Surface(
                    onClick = { onSelect(lang) },
                    shape = CircleShape,
                    color = if (isSelected) Color(0xFF7D5FFF) else MaterialTheme.colorScheme.surface,
                    shadowElevation = if (isSelected) 4.dp else 1.dp
                ) {
                    Text(
                        text = lang,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ApiKeyInput(
    value: String,
    visible: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("AIzaSy...", style = MaterialTheme.typography.bodySmall, color = Color.LightGray) },
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                trailingIcon = {
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray
                        )
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D5FFF))
        ) {
            Text("Save Key", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsSection(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .shadow(5.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(0.02f)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null)
        Modifier.clickable(onClick = onClick)
    else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = Color.Gray)
        }
        trailing()
    }
}
