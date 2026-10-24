package com.captionai.studio.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
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

    private const val BANNER_AD_UNIT_ID     = "ca-app-pub-4894264775611031/1876381885"
    private const val NATIVE_AD_UNIT_ID     = "ca-app-pub-1237562221952085/3584354536"
    private const val INTERSTITIAL_AD_UNIT  = "ca-app-pub-4894264775611031/8058646850"
    
    private const val REWARDED_AD_UNIT      = "ca-app-pub-4894264775611031/4011590630"

    private const val APP_OPEN_AD_UNIT      = "ca-app-pub-4894264775611031/3577098480"
    private const val APP_OPEN_MAX_AGE_MS   = 4 * 60 * 60 * 1000L // Google: app open ads expire after 4 hours

    private var appOpenAd: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var isLoadingAppOpen = false
    private var isShowingAppOpen = false

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var generationCount = 0

    /** True once consent allows ad requests and the SDK has been initialised. */
    val adsReady = androidx.compose.runtime.mutableStateOf(false)
    private var initialized = false

    /** Call only after [ConsentManager] says ads may be requested. Safe to call repeatedly. */
    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        MobileAds.initialize(context)
        adsReady.value = true
        preloadInterstitial(context)
        preloadRewarded(context)
        preloadAppOpen(context)
    }

    // ── App Open Ad ───────────────────────────────────────────────────────────
    // Shown when the user brings the app back to the foreground (see MyApp).

    private fun appOpenAvailable(): Boolean =
        appOpenAd != null && System.currentTimeMillis() - appOpenLoadedAt < APP_OPEN_MAX_AGE_MS

    fun preloadAppOpen(context: Context) {
        if (!initialized || isLoadingAppOpen || appOpenAvailable()) return
        isLoadingAppOpen = true
        AppOpenAd.load(
            context,
            APP_OPEN_AD_UNIT,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    appOpenLoadedAt = System.currentTimeMillis()
                    isLoadingAppOpen = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    isLoadingAppOpen = false
                }
            }
        )
    }

    /** Shows the app open ad if one is ready. Pro users never see it. */
    fun showAppOpenIfAvailable(activity: Activity) {
        if (isShowingAppOpen || PrefsManager.getInstance().isPro()) return
        val ad = appOpenAd
        if (ad == null || !appOpenAvailable()) {
            appOpenAd = null
            preloadAppOpen(activity)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowingAppOpen = false
                appOpenAd = null
                preloadAppOpen(activity)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                isShowingAppOpen = false
                appOpenAd = null
                preloadAppOpen(activity)
            }
        }
        isShowingAppOpen = true
        ad.show(activity)
    }

    // ── Banner Ad ─────────────────────────────────────────────────────────────

    fun getBannerAdView(context: Context): AdView {
        check(initialized) { "Ads are not initialised yet (no consent)" }
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    }

    // ── Native Ad ─────────────────────────────────────────────────────────────
    
    fun loadNativeAd(context: Context, onLoaded: (NativeAd) -> Unit) {
        if (!initialized) return
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
        if (!initialized) return
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
        if (!initialized) return
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
