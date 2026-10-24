package com.captionai.studio

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.captionai.studio.storage.PrefsManager
import com.captionai.studio.utils.AdManager
import com.google.android.gms.ads.AdActivity

class MyApp : Application(), Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var currentActivity: Activity? = null

    override fun onCreate() {
        super<Application>.onCreate()
        PrefsManager.init(this)
        // Ads are initialised by ConsentManager once the user has given (or doesn't need to give) consent.
        registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    // App returned to the foreground -> show the app open ad (skipped for Pro users).
    override fun onStart(owner: LifecycleOwner) {
        currentActivity?.let { AdManager.showAppOpenIfAvailable(it) }
    }

    override fun onActivityStarted(activity: Activity) {
        // Don't track the ad's own activity, or we'd try to show an ad on top of an ad.
        if (activity !is AdActivity) currentActivity = activity
    }
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
