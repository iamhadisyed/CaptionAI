package com.captionai.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.captionai.studio.screens.CaptionAIApp
import com.captionai.studio.utils.BillingHelper
import com.captionai.studio.ui.CaptionAITheme

class MainActivity : ComponentActivity() {

    private lateinit var billingHelper: BillingHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        billingHelper = BillingHelper(this)
        billingHelper.restorePurchases { }

        setContent {
            CaptionAITheme {
                CaptionAIApp(billingHelper = billingHelper)
            }
        }
    }
}
