package com.dailywork.attedance.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class AdConsentManager(private val context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    interface ConsentGatheringListener {
        fun onConsentGatheringComplete(error: Exception?)
    }

    fun gatherConsent(
        activity: Activity,
        onConsentGatheringCompleteListener: ConsentGatheringListener
    ) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    activity
                ) { formError ->
                    if (formError != null) {
                        onConsentGatheringCompleteListener.onConsentGatheringComplete(
                            Exception(formError.message)
                        )
                    } else {
                        onConsentGatheringCompleteListener.onConsentGatheringComplete(null)
                    }
                }
            },
            { requestConsentError ->
                onConsentGatheringCompleteListener.onConsentGatheringComplete(
                    Exception(requestConsentError.message)
                )
            }
        )
    }

    fun isPrivacyOptionsRequired(): Boolean {
        return consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: UserMessagingPlatform.OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    }

    fun canRequestAds(): Boolean {
        return consentInformation.canRequestAds()
    }
}
