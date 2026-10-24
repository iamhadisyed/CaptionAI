package com.captionai.studio.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.captionai.studio.storage.PrefsManager
import com.captionai.studio.utils.AdManager

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 24.dp, top = 28.dp, bottom = 8.dp)
    )
}

fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Caption AI", text))
}

@Composable
fun BannerAdView() {
    val isPro by PrefsManager.getInstance().proState
    if (isPro) return
    
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        factory = { context ->
            AdManager.getBannerAdView(context)
        }
    )
}
