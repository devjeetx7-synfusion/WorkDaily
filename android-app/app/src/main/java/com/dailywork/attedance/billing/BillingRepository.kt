package com.dailywork.attedance.billing

import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingRepository(private val billingManager: BillingManager) {

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products = _products.asStateFlow()

    private val _isBillingReady = billingManager.isReady
    val isBillingReady = _isBillingReady

    val premiumProductIds = listOf(
        "workdaily_premium_monthly" to "subs",
        "workdaily_premium_yearly" to "subs",
        "workdaily_premium_lifetime" to "inapp"
    )

    suspend fun fetchProductDetails() {
        val details = billingManager.queryProductDetails(premiumProductIds)
        _products.value = details
    }

    fun refreshPurchases() {
        billingManager.queryPurchases()
    }
}
