package com.captionai.studio.utils

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Google User Messaging Platform (UMP) consent flow.
 *
 * Ads are only initialised and requested once [ConsentInformation.canRequestAds] is true, which keeps the
 * app compliant with GDPR (EEA/UK) and US state privacy laws. Users in regions where consent is required
 * can change their choice later via [showPrivacyOptions].
 *
 * Configure the consent message in AdMob: Privacy & messaging -> European regulations / US states.
 */
object ConsentManager {

    private const val TAG = "ConsentManager"

    private var started = false

    private val _privacyOptionsRequired = mutableStateOf(false)
    /** True when the user must be able to reopen the consent form (show a "Privacy settings" row). */
    val privacyOptionsRequired: State<Boolean> = _privacyOptionsRequired

    /** Call from the main activity on every launch. Safe to call more than once. */
    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)

        // A previous session may already have consent; start ads right away in that case.
        if (info.canRequestAds()) startAds(activity)

        if (started) return
        started = true

        // Add ConsentDebugSettings here when testing EEA behaviour on a test device.
        val params = ConsentRequestParameters.Builder().build()

        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error ${formError.errorCode}: ${formError.message}")
                    }
                    refresh(activity, info)
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed ${requestError.errorCode}: ${requestError.message}")
                refresh(activity, info)
            }
        )
    }

    /** Lets the user revisit their choice (required in some regions). */
    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) Log.w(TAG, "Privacy options error: ${formError.message}")
            refresh(activity, UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    private fun refresh(activity: Activity, info: ConsentInformation) {
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (info.canRequestAds()) startAds(activity)
    }

    private fun startAds(activity: Activity) {
        AdManager.initialize(activity.applicationContext)
    }
}
