package com.captionai.studio.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.appopen.AppOpenAd
import com.captionai.studio.storage.PrefsManager
import com.google.android.gms.ads.nativead.NativeAd

// ─────────────────────────────────────────────────────────────────────────────
// AdManager — Maximum Monetization Setup
// ─────────────────────────────────────────────────────────────────────────────

object AdManager {

    // ── Real Production Ad Unit IDs ───────────────────────────────────────────
    private const val BANNER_AD_UNIT_ID    = "ca-app-pub-1237562221952085/2394112086"
    private const val NATIVE_AD_UNIT_ID    = "ca-app-pub-1237562221952085/3584354536"
    private const val INTERSTITIAL_AD_UNIT = "ca-app-pub-1237562221952085/9958191194"

    // ⚠️ REPLACE this with your real Rewarded Ad ID from admob.google.com
    // Steps: AdMob → Apps → Caption AI → Ad Units → Add Ad Unit → Rewarded → Copy ID
    private const val REWARDED_AD_UNIT     = "ca-app-pub-3940256099942544/5224354917"

    // ── Internal state ────────────────────────────────────────────────────────
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var generationCount = 0
    private var isInitialized = false

    // ─────────────────────────────────────────────────────────────────────────
    // Initialize — called once in MyApp.onCreate()
    // ─────────────────────────────────────────────────────────────────────────
    fun initialize(context: Context) {
        if (isInitialized) return
        MobileAds.initialize(context) {
            // Preload ads as soon as SDK is ready
            preloadInterstitial(context)
            preloadRewarded(context)
        }
        isInitialized = true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BANNER AD
    // Shown: Home, Trending, Favorites, History screens (always visible for free users)
    // ─────────────────────────────────────────────────────────────────────────
    fun getBannerAdView(context: Context): AdView {
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NATIVE AD
    // Shown: Between caption cards on ResultScreen (blends naturally into content)
    // ─────────────────────────────────────────────────────────────────────────
    fun loadNativeAd(context: Context, onLoaded: (NativeAd) -> Unit) {
        val adLoader = AdLoader.Builder(context, NATIVE_AD_UNIT_ID)
            .forNativeAd { nativeAd -> onLoaded(nativeAd) }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    android.util.Log.w("AdManager", "Native ad failed: ${adError.message}")
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERSTITIAL AD
    // Shown: After every 2nd generation for free users (was every 3rd → increased earnings)
    // ─────────────────────────────────────────────────────────────────────────
    fun onCaptionGenerated(activity: Activity) {
        if (PrefsManager.getInstance().isPro()) return  // Pro users never see interstitials
        generationCount++
        if (generationCount % 2 == 0) {  // Every 2nd generation (was 3rd)
            showInterstitial(activity)
        }
    }

    private fun preloadInterstitial(context: Context) {
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    android.util.Log.d("AdManager", "Interstitial preloaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    android.util.Log.w("AdManager", "Interstitial failed: ${error.message}")
                }
            }
        )
    }

    private fun showInterstitial(activity: Activity) {
        try {
            if (interstitialAd != null) {
                interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        preloadInterstitial(activity)  // Preload next one immediately
                    }
                }
                interstitialAd?.show(activity)
            } else {
                preloadInterstitial(activity)
            }
        } catch (e: Exception) {
            android.util.Log.e("AdManager", "Interstitial show error", e)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REWARDED AD
    // Shown: When user taps "Watch ad for 3 more generations" on Home Screen
    // Highest earning ad type — user voluntarily watches full video
    // ─────────────────────────────────────────────────────────────────────────
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.show(activity) { _ ->
                onRewarded()
                rewardedAd = null
                preloadRewarded(activity)  // Preload next immediately
            }
        } else {
            // Ad not ready yet — preload and inform user
            preloadRewarded(activity)
            android.widget.Toast.makeText(
                activity,
                "Ad loading, please try again in a moment",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun isRewardedAdReady(): Boolean = rewardedAd != null

    private fun preloadRewarded(context: Context) {
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    android.util.Log.d("AdManager", "Rewarded ad preloaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    android.util.Log.w("AdManager", "Rewarded failed: ${error.message}")
                }
            }
        )
    }
}
