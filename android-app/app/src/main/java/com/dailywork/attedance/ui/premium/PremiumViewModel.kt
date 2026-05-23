package com.dailywork.attedance.ui.premium

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.dailywork.attedance.billing.BillingManager
import com.dailywork.attedance.billing.BillingRepository
import com.dailywork.attedance.data.UserPreferencesRepository
import com.dailywork.attedance.premium.PremiumManager
import com.dailywork.attedance.BuildConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FallbackProduct(
    val productId: String,
    val title: String,
    val price: String,
    val description: String,
    val tag: String? = null
)

val FALLBACK_PRODUCTS = listOf(
    FallbackProduct("workdaily_premium_monthly", "Monthly Premium", "₹99", "Billed Monthly"),
    FallbackProduct("workdaily_premium_yearly", "Yearly Premium", "₹799", "Billed Annually", "Best Value"),
    FallbackProduct("workdaily_premium_lifetime", "Lifetime Premium", "₹1999", "Pay Once", "One-Time")
)

data class PremiumUiState(
    val products: List<ProductDetails> = emptyList(),
    val fallbackProducts: List<FallbackProduct> = emptyList(),
    val isPremium: Boolean = false,
    val premiumType: String? = null,
    val isLoading: Boolean = true,
    val isFallbackMode: Boolean = false,
    val error: String? = null,
    val isBillingReady: Boolean = false
)

class PremiumViewModel(
    private val repository: UserPreferencesRepository,
    private val billingRepository: BillingRepository,
    private val billingManager: BillingManager,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _isFallbackMode = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState = _uiState.asStateFlow()

    private var timeoutJob: Job? = null

    init {
        viewModelScope.launch {
            val userStateFlow = combine(
                repository.isPremiumFlow,
                repository.premiumTypeFlow
            ) { isPremium, premiumType -> Pair(isPremium, premiumType) }

            val billingStateFlow = combine(
                billingRepository.products,
                billingRepository.isBillingReady
            ) { products, isBillingReady -> Pair(products, isBillingReady) }

            val localStateFlow = combine(
                _isLoading,
                _isFallbackMode,
                _error
            ) { isLoading, isFallbackMode, error -> Triple(isLoading, isFallbackMode, error) }

            combine(
                userStateFlow,
                billingStateFlow,
                localStateFlow
            ) { userState, billingState, localState ->
                val (isPremium, premiumType) = userState
                val (products, isBillingReady) = billingState
                val (isLoading, isFallbackMode, error) = localState

                PremiumUiState(
                    isPremium = isPremium,
                    premiumType = premiumType,
                    products = products,
                    fallbackProducts = if (isFallbackMode) FALLBACK_PRODUCTS else emptyList(),
                    isBillingReady = isBillingReady,
                    isLoading = isLoading,
                    isFallbackMode = isFallbackMode,
                    error = error
                )
            }.collect {
                _uiState.value = it
            }
        }

        viewModelScope.launch {
            billingRepository.products.collect { products ->
                if (products.isNotEmpty()) {
                    _isLoading.value = false
                    _error.value = null
                    timeoutJob?.cancel()
                }
            }
        }

        viewModelScope.launch {
            billingRepository.isBillingReady.collect { isReady ->
                if (isReady) {
                    refreshProducts()
                } else if (_isLoading.value && timeoutJob?.isActive != true) {
                    startLoadingTimeout()
                }
            }
        }
    }

    private fun startLoadingTimeout() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(5000) // 5 seconds timeout
            if (billingRepository.products.value.isEmpty()) {
                _isLoading.value = false
                if (BuildConfig.DEBUG) {
                    _isFallbackMode.value = true
                } else {
                    _error.value = "Failed to load products. Please try again."
                }
            }
        }
    }

    fun refreshProducts() {
        _isLoading.value = true
        _error.value = null
        _isFallbackMode.value = false
        startLoadingTimeout()
        viewModelScope.launch {
            if (billingRepository.isBillingReady.value) {
                billingRepository.fetchProductDetails()
            }
        }
    }

    fun buyProduct(activity: Activity, productDetails: ProductDetails) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        billingManager.launchBillingFlow(activity, productDetails, offerToken)
    }

    fun restorePurchases() {
        premiumManager.restorePurchases()
    }

    fun manageSubscription(context: Context) {
        val packageName = context.packageName
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/account/subscriptions?package=$packageName")
        }
        context.startActivity(intent)
    }
}
