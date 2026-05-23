package com.dailywork.attedance.premium

import android.util.Log
import com.android.billingclient.api.Purchase
import com.dailywork.attedance.billing.BillingManager
import com.dailywork.attedance.billing.PurchaseHandler
import com.dailywork.attedance.billing.SubscriptionVerifier
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class PremiumManager(
    private val repository: UserPreferencesRepository,
    private val subscriptionVerifier: SubscriptionVerifier,
    private val billingManager: BillingManager
) : PurchaseHandler {

    private val TAG = "PremiumManager"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    init {
        // Initial sync/check can be triggered here or from MainActivity
    }

    override fun processPurchases(purchases: List<Purchase>?) {
        if (purchases.isNullOrEmpty()) {
            handleNoActivePurchases()
            return
        }

        scope.launch {
            var premiumFound = false
            for (purchase in purchases) {
                if (subscriptionVerifier.isPurchaseValid(purchase)) {
                    acknowledgeAndUnlock(purchase)
                    premiumFound = true
                    break // Found at least one valid premium purchase
                }
            }

            if (!premiumFound) {
                handleNoActivePurchases()
            }
        }
    }

    private fun handleNoActivePurchases() {
        scope.launch {
            repository.savePremiumStatus(false)
            syncWithFirebase(false)
        }
    }

    private suspend fun acknowledgeAndUnlock(purchase: Purchase) {
        billingManager.acknowledgePurchase(purchase) { success ->
            if (success) {
                scope.launch {
                    val type = when (purchase.products.firstOrNull()) {
                        "workdaily_premium_monthly" -> "monthly"
                        "workdaily_premium_yearly" -> "yearly"
                        "workdaily_premium_lifetime" -> "lifetime"
                        else -> "unknown"
                    }

                    repository.savePremiumStatus(
                        isPremium = true,
                        type = type,
                        token = purchase.purchaseToken,
                        lastVerified = System.currentTimeMillis()
                    )

                    syncWithFirebase(true, type, purchase.purchaseToken)
                }
            }
        }
    }

    private suspend fun syncWithFirebase(
        isPremium: Boolean,
        type: String? = null,
        token: String? = null
    ) {
        val user = auth.currentUser ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val updatedAt = sdf.format(Date())

        val subscriptionData = hashMapOf(
            "isPremium" to isPremium,
            "type" to type,
            "purchaseToken" to token,
            "updatedAt" to updatedAt
        )

        try {
            db.collection("users").document(user.uid)
                .update("subscription", subscriptionData, "isPremium", isPremium)
                .addOnFailureListener {
                    // If document doesn't exist or field update fails, try set with merge
                    db.collection("users").document(user.uid)
                        .set(hashMapOf("subscription" to subscriptionData, "isPremium" to isPremium), SetOptions.merge())
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing with Firebase", e)
        }
    }

    fun restorePurchases() {
        billingManager.queryPurchases()
    }
}
