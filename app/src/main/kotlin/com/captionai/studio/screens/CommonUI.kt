package com.captionai.studio.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

// ── Banner Ad — shows at bottom of most screens (free users only) ─────────────
@Composable
fun BannerAdView() {
    if (PrefsManager.getInstance().isPro()) return
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        factory = { context -> AdManager.getBannerAdView(context) }
    )
}

// ── Native Ad Card — blends between caption cards in ResultScreen ──────────────
@Composable
fun NativeAdCard() {
    if (PrefsManager.getInstance().isPro()) return
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF7D5FFF).copy(alpha = 0.06f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF7D5FFF).copy(0.5f), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Sponsored — Tap to support Caption AI ✨",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF7D5FFF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Rewarded Ad Button — CTA to earn 3 bonus generations ──────────────────────
@Composable
fun RewardedAdButton(onRewarded: () -> Unit) {
    if (PrefsManager.getInstance().isPro()) return
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF9F43), Color(0xFFFF6B6B))
    )

    Button(
        onClick = {
            activity?.let { act ->
                AdManager.showRewardedAd(act) { onRewarded() }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "Watch Ad → Get 3 Free Generations",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}
