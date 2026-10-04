package com.rds.questlog.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.rds.questlog.domain.model.PurchaseQuery
import com.rds.questlog.domain.model.PurchaseResult
import com.rds.questlog.domain.repository.BillingService
import kotlin.coroutines.resume
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Build `release`: Google Play Billing resmi untuk produk sekali-bayar [PRODUCT_ID]. BillingClient hanya ada di data
 * layer; domain hanya mengenal [BillingService]. Pembelian wajib di-acknowledge (Play me-refund otomatis setelah 3 hari
 * bila tidak), dilakukan di sini saat pembelian ditemukan.
 */
class GooglePlayBillingService(
    context: Context,
    private val activities: CurrentActivityHolder,
) : BillingService, PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectLock = Mutex()
    private var pendingPurchase: CompletableDeferred<PurchaseResult>? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    override suspend fun queryUnlimitedPurchase(): PurchaseQuery {
        if (!connect()) return PurchaseQuery.Unavailable("Google Play tidak dapat dihubungi")
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        val owned = result.purchasesList.firstOrNull { it.isUnlimited() && it.isPurchased() }
        return when {
            result.billingResult.responseCode != BillingResponseCode.OK ->
                PurchaseQuery.Unavailable(result.billingResult.debugMessage)
            owned == null -> PurchaseQuery.NotOwned
            else -> acknowledgeIfNeeded(owned)?.let { PurchaseQuery.Owned(it.purchaseToken) }
                ?: PurchaseQuery.Unavailable("Gagal mengonfirmasi pembelian")
        }
    }

    override suspend fun purchaseUnlimited(): PurchaseResult {
        val activity = activities.current
        val details = if (activity != null && connect()) loadUnlimitedDetails() else null
        return if (activity == null || details == null) {
            PurchaseResult.Failure("Google Play tidak dapat dihubungi atau produk tidak ditemukan")
        } else {
            launchFlow(activity, details)
        }
    }

    private suspend fun loadUnlimitedDetails(): ProductDetails? = client.queryProductDetails(
        QueryProductDetailsParams.newBuilder().setProductList(listOf(unlimitedProduct())).build(),
    ).productDetailsList?.firstOrNull()

    private suspend fun launchFlow(activity: Activity, details: ProductDetails): PurchaseResult {
        val waiting = CompletableDeferred<PurchaseResult>()
        pendingPurchase = waiting
        val launch = client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()),
                )
                .build(),
        )
        if (launch.responseCode == BillingResponseCode.OK) return waiting.await()
        pendingPurchase = null
        return launch.toFailure()
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        val waiting = pendingPurchase ?: return
        pendingPurchase = null
        val bought = purchases.orEmpty().firstOrNull { it.isUnlimited() }
        when {
            result.responseCode == BillingResponseCode.USER_CANCELED -> waiting.complete(PurchaseResult.Cancelled)
            result.responseCode != BillingResponseCode.OK -> waiting.complete(result.toFailure())
            bought == null -> waiting.complete(PurchaseResult.Failure("Pembelian tidak ditemukan"))
            !bought.isPurchased() -> waiting.complete(PurchaseResult.Failure("Pembayaran masih tertunda"))
            else -> scope.launch {
                val confirmed = acknowledgeIfNeeded(bought)
                waiting.complete(
                    confirmed?.let { PurchaseResult.Success(it.purchaseToken) }
                        ?: PurchaseResult.Failure("Gagal mengonfirmasi pembelian"),
                )
            }
        }
    }

    private suspend fun connect(): Boolean = connectLock.withLock {
        client.isReady || suspendCancellableCoroutine { continuation ->
            client.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (continuation.isActive) continuation.resume(result.responseCode == BillingResponseCode.OK)
                    }

                    override fun onBillingServiceDisconnected() = Unit
                },
            )
        }
    }

    /** Mengonfirmasi (acknowledge) pembelian bila belum; null bila konfirmasi gagal. */
    private suspend fun acknowledgeIfNeeded(purchase: Purchase): Purchase? {
        if (purchase.isAcknowledged) return purchase
        val result = client.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
        )
        return purchase.takeIf { result.responseCode == BillingResponseCode.OK }
    }

    private fun Purchase.isUnlimited() = PRODUCT_ID in products

    private fun Purchase.isPurchased() = purchaseState == Purchase.PurchaseState.PURCHASED

    private fun unlimitedProduct() = QueryProductDetailsParams.Product.newBuilder()
        .setProductId(PRODUCT_ID)
        .setProductType(BillingClient.ProductType.INAPP)
        .build()

    private fun BillingResult.toFailure(): PurchaseResult = when (responseCode) {
        BillingResponseCode.USER_CANCELED -> PurchaseResult.Cancelled
        BillingResponseCode.ITEM_ALREADY_OWNED ->
            PurchaseResult.Failure("Sudah dimiliki; gunakan Restore Purchase")
        else -> PurchaseResult.Failure(debugMessage.ifBlank { "Kode Google Play $responseCode" })
    }

    companion object {
        /** ID produk sekali-bayar di Play Console. */
        const val PRODUCT_ID = "questlog_unlimited"
    }
}
