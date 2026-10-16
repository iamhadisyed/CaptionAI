package com.captionai.studio.utils

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.captionai.studio.storage.PrefsManager

class BillingHelper(private val context: Context) {

    private val PRO_PRODUCT_ID = "caption_ai_pro_monthly"

    private val billingClient = BillingClient.newBuilder(context)
        .setListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                purchases.forEach { handlePurchase(it) }
            }
        }
        .enablePendingPurchases()
        .build()

    fun startConnection(onReady: () -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) onReady()
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    fun restorePurchases(onResult: (Boolean) -> Unit) {
        startConnection {
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { _, purchases ->
                val isPro = purchases.any {
                    it.products.contains(PRO_PRODUCT_ID) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                PrefsManager.getInstance().setPro(isPro)
                onResult(isPro)
            }
        }
    }

    fun launchPurchase(activity: Activity) {
        startConnection {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRO_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            billingClient.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder().setProductList(productList).build()
            ) { _, productDetailsList ->
                if (productDetailsList.isNotEmpty()) {
                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetailsList[0])
                                .build()
                        )).build()
                    billingClient.launchBillingFlow(activity, flowParams)
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                ) { PrefsManager.getInstance().setPro(true) }
            } else {
                PrefsManager.getInstance().setPro(true)
            }
        }
    }
}
