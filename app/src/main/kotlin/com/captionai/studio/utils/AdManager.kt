package com.captionai.studio.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.captionai.studio.storage.PrefsManager
import com.google.android.gms.ads.nativead.NativeAd

// ─────────────────────────────────────────────────────────────────────────────
// Real AdMob IDs applied for Production
// ─────────────────────────────────────────────────────────────────────────────

object AdManager {

    private const val BANNER_AD_UNIT_ID     = "ca-app-pub-1237562221952085/2394112086"
    private const val NATIVE_AD_UNIT_ID     = "ca-app-pub-1237562221952085/3584354536"
    private const val INTERSTITIAL_AD_UNIT  = "ca-app-pub-1237562221952085/9958191194"
    
    // TODO: replace with your real rewarded ad unit ID from AdMob before release.
    // This is Google's official test ID, so no revenue is earned until it is replaced.
    private const val REWARDED_AD_UNIT      = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var generationCount = 0

    fun initialize(context: Context) {
        MobileAds.initialize(context)
        preloadInterstitial(context)
        preloadRewarded(context)
    }

    // ── Banner Ad ─────────────────────────────────────────────────────────────

    fun getBannerAdView(context: Context): AdView {
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    // ── Native Ad ─────────────────────────────────────────────────────────────
    
    fun loadNativeAd(context: Context, onLoaded: (NativeAd) -> Unit) {
        val adLoader = AdLoader.Builder(context, NATIVE_AD_UNIT_ID)
            .forNativeAd { nativeAd -> onLoaded(nativeAd) }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    // Handle failure
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    // ── Interstitial Ad ───────────────────────────────────────────────────────
    // Shows after every 3rd generation for free users

    fun onCaptionGenerated(activity: Activity) {
        if (PrefsManager.getInstance().isPro()) return
        generationCount++
        if (generationCount % 3 == 0) {
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
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    private fun showInterstitial(activity: Activity) {
        try {
            if (interstitialAd != null) {
                interstitialAd?.show(activity)
                interstitialAd = null
                preloadInterstitial(activity)
            } else {
                preloadInterstitial(activity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Rewarded Ad (Bonus) ───────────────────────────────────────────────────

    fun isRewardedReady(): Boolean = rewardedAd != null

    /**
     * Shows a rewarded ad. [onRewarded] fires only when the user earns the reward.
     * Returns false (and starts loading another ad) if none is ready yet.
     */
    fun showRewardedAd(activity: Activity, onRewarded: () -> Unit): Boolean {
        val ad = rewardedAd
        if (ad == null) {
            preloadRewarded(activity)
            return false
        }
        rewardedAd = null
        ad.show(activity) { _ -> onRewarded() }
        preloadRewarded(activity)
        return true
    }

    private fun preloadRewarded(context: Context) {
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }
}
