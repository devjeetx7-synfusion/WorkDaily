package com.dailywork.attedance.billing

import com.android.billingclient.api.Purchase

class SubscriptionVerifier {
    fun isPurchaseValid(purchase: Purchase): Boolean {
        // In a production app, you should verify the purchase token on your server
        // For this implementation, we will trust the BillingClient's purchase state
        return purchase.purchaseState == Purchase.PurchaseState.PURCHASED
    }
}
