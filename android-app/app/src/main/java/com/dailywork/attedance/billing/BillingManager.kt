package com.dailywork.attedance.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(
    private val context: Context
) : PurchasesUpdatedListener {

    private var purchaseHandler: PurchaseHandler? = null

    fun setPurchaseHandler(handler: PurchaseHandler) {
        this.purchaseHandler = handler
    }

    private val TAG = "BillingManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    init {
        startConnection()
    }

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client setup finished")
                    _isReady.value = true
                    queryPurchases()
                } else {
                    Log.e(TAG, "Billing client setup failed: ${billingResult.debugMessage}")
                    reconnect()
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected")
                _isReady.value = false
                reconnect()
            }
        })
    }

    private fun reconnect() {
        scope.launch {
            delay(5000)
            startConnection()
        }
    }

    fun queryPurchases() {
        if (!billingClient.isReady) {
            Log.e(TAG, "queryPurchases: Billing client not ready")
            return
        }

        val allPurchases = mutableListOf<Purchase>()
        var queriesFinished = 0

        val checkFinished = {
            queriesFinished++
            if (queriesFinished == 2) {
                Log.d(TAG, "queryPurchases: Finished querying all purchases. Total found: ${allPurchases.size}")
                purchaseHandler?.processPurchases(allPurchases)
            }
        }

        // Query Subscriptions
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "queryPurchases (SUBS): Found ${purchases.size} purchases")
                allPurchases.addAll(purchases)
            } else {
                Log.e(TAG, "queryPurchases (SUBS) failed: ${result.debugMessage} (Code: ${result.responseCode})")
            }
            checkFinished()
        }

        // Query One-time purchases (Lifetime)
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "queryPurchases (INAPP): Found ${purchases.size} purchases")
                allPurchases.addAll(purchases)
            } else {
                Log.e(TAG, "queryPurchases (INAPP) failed: ${result.debugMessage} (Code: ${result.responseCode})")
            }
            checkFinished()
        }
    }

    suspend fun queryProductDetails(productIds: List<Pair<String, String>>): List<ProductDetails> {
        if (!billingClient.isReady) {
            Log.e(TAG, "queryProductDetails: Billing client not ready")
            return emptyList()
        }

        val allProductDetails = mutableListOf<ProductDetails>()
        val groupedProducts = productIds.groupBy { it.second }

        for ((productType, products) in groupedProducts) {
            val productList = products.map { (id, type) ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(type)
                    .build()
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            Log.d(TAG, "queryProductDetails: Starting query for ${products.size} $productType products")

            val (billingResult, productDetailsList) = billingClient.queryProductDetails(params)

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList != null) {
                    allProductDetails.addAll(productDetailsList)
                }
            } else {
                Log.e(TAG, "queryProductDetails: Error querying $productType products. Code: ${billingResult.responseCode}, Message: ${billingResult.debugMessage}")
            }
        }

        Log.d(TAG, "queryProductDetails: Successfully returned ${allProductDetails.size} total products")

        // Check for missing products
        val requestedIds = productIds.map { it.first }
        val returnedIds = allProductDetails.map { it.productId }
        val missingIds = requestedIds.filterNot { returnedIds.contains(it) }
        if (missingIds.isNotEmpty()) {
            Log.w(TAG, "queryProductDetails: Warning - Missing products from Play Console: $missingIds")
        }

        return allProductDetails
    }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails, offerToken: String? = null) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
                    if (offerToken != null) {
                        setOfferToken(offerToken)
                    }
                }
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        Log.d(TAG, "onPurchasesUpdated: ${billingResult.responseCode}, ${billingResult.debugMessage}")
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(TAG, "Purchase successful: $purchases")
                purchases?.let { purchaseHandler?.processPurchases(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled the purchase")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned")
                queryPurchases() // Refresh purchases if already owned
            }
            else -> {
                Log.e(TAG, "Error updating purchases: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * For testing purposes, allows manual injection of a purchase state.
     * Use only in debug builds or internal testing.
     */
    fun simulatePurchase(productId: String, productType: String) {
        Log.d(TAG, "Simulating purchase for $productId")
        // This is a placeholder for actual simulation logic if needed for internal testing apps
    }

    fun acknowledgePurchase(purchase: Purchase, callback: (Boolean) -> Unit) {
        Log.d(TAG, "Acknowledging purchase: ${purchase.purchaseToken}")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged successfully")
                    callback(true)
                } else {
                    Log.e(TAG, "Error acknowledging purchase: ${billingResult.debugMessage}")
                    callback(false)
                }
            }
        } else {
            callback(true)
        }
    }

    fun onDestroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
        scope.cancel()
    }
}
