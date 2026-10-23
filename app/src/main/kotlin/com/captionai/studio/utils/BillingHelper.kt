package com.captionai.studio.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import com.captionai.studio.storage.PrefsManager

class BillingHelper(private val context: Context) {

    private companion object {
        const val TAG = "BillingHelper"
        const val PRO_PRODUCT_ID = "caption_ai_pro_monthly"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val billingClient = BillingClient.newBuilder(context)
        .setListener { billingResult, purchases ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> purchases?.forEach { handlePurchase(it) }
                BillingClient.BillingResponseCode.USER_CANCELED -> Unit
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases { }
                else -> toast("Purchase failed: ${billingResult.debugMessage}")
            }
        }
        .enablePendingPurchases()
        .build()

    /** Runs [onReady] once connected, reusing an existing connection when there is one. */
    fun startConnection(onReady: () -> Unit, onError: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            onReady()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) onReady()
                else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                    onError?.invoke()
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    fun restorePurchases(onResult: (Boolean) -> Unit) {
        startConnection(onReady = {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { result, purchases ->
                // Never downgrade Pro because of a transient billing error (offline, service down).
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "queryPurchases failed: ${result.debugMessage}")
                    onResult(PrefsManager.getInstance().isPro())
                    return@queryPurchasesAsync
                }
                val owned = purchases.filter {
                    it.products.contains(PRO_PRODUCT_ID) &&
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                owned.filter { !it.isAcknowledged }.forEach { handlePurchase(it) }
                val isPro = owned.isNotEmpty()
                PrefsManager.getInstance().setPro(isPro)
                onResult(isPro)
            }
        }, onError = { onResult(PrefsManager.getInstance().isPro()) })
    }

    fun launchPurchase(activity: Activity) {
        startConnection(onReady = {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder().setProductList(productList).build()
            ) { result, productDetailsList ->
                val details = productDetailsList.firstOrNull()
                // Subscriptions require an offer token (base plan / offer) in the billing flow.
                val offerToken = details?.subscriptionOfferDetails?.firstOrNull()?.offerToken
                if (result.responseCode != BillingClient.BillingResponseCode.OK || details == null || offerToken == null) {
                    toast("Subscription is not available right now. Please try again later.")
                    return@queryProductDetailsAsync
                }
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .setOfferToken(offerToken)
                            .build()
                    )).build()
                val launch = billingClient.launchBillingFlow(activity, flowParams)
                if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
                    toast("Could not start purchase: ${launch.debugMessage}")
                }
            }
        }, onError = { toast("Google Play Billing is unavailable on this device.") })
    }

    /** Localised price string (e.g. "Rs 6,999.00") for the paywall, or null if unavailable. */
    fun queryFormattedPrice(onResult: (String?) -> Unit) {
        startConnection(onReady = {
            val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )).build()
            billingClient.queryProductDetailsAsync(params) { _, list ->
                val price = list.firstOrNull()?.subscriptionOfferDetails?.firstOrNull()
                    ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                mainHandler.post { onResult(price) }
            }
        }, onError = { onResult(null) })
    }

    private fun handlePurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                // Grant access right away; acknowledge in the background (Play refunds unacknowledged purchases after 3 days).
                mainHandler.post { PrefsManager.getInstance().setPro(true) }
                if (!purchase.isAcknowledged) {
                    billingClient.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    ) { r ->
                        if (r.responseCode != BillingClient.BillingResponseCode.OK) {
                            Log.w(TAG, "Acknowledge failed: ${r.debugMessage}")
                        }
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> toast("Your purchase is pending. Pro unlocks once payment completes.")
            else -> Unit
        }
    }

    private fun toast(msg: String) {
        mainHandler.post { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    }
}
