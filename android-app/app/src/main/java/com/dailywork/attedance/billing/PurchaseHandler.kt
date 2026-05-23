package com.dailywork.attedance.billing

import com.android.billingclient.api.Purchase

interface PurchaseHandler {
    fun processPurchases(purchases: List<Purchase>?)
}
