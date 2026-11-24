package io.github.sheikhmehraann.onepeek

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import java.lang.ref.WeakReference

internal class BillingSession(activity: Activity) {
    private val activityRef = WeakReference(activity)
    private var client: BillingClient? = null

    @Volatile
    private var requestId = 0

    @Volatile
    private var closed = false

    private data class OfferEntry(
        val product: ProductDetails,
        val details: ProductDetails.SubscriptionOfferDetails,
        val phases: List<OfferPhase>,
    ) {
        val isFreeIntro: Boolean
            get() = OfferPricing.hasFreeIntro(phases)

        val freeDays: Int
            get() = OfferPricing.freeIntroDays(phases)

        fun rowLabel(activity: Activity): String {
            val badge = ModuleResources.text(
                activity,
                if (isFreeIntro) R.string.tag_free_trial else R.string.tag_paid,
            )
            val id = details.offerId ?: details.basePlanId
            return ModuleResources.text(
                activity,
                R.string.offer_row,
                product.productId,
                id,
                badge,
            )
        }
    }

    fun query() {
        if (closed) return
        val activity = activityRef.get() ?: return
        requestId += 1
        val currentId = requestId
        client?.endConnection()
        client = null
        toastShort(R.string.status_querying)

        try {
            val billing = BillingClient.newBuilder(activity.applicationContext)
                .setListener(
                    PurchasesUpdatedListener { result, _ ->
                        if (isCurrent(currentId)) {
                            toastShort(R.string.status_purchase_callback, result.responseCode)
                        }
                    },
                )
                .enablePendingPurchases()
                .build()
            client = billing
            billing.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (!isCurrent(currentId)) return
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            fetchOffers(billing, currentId)
                        } else {
                            fail(currentId, R.string.error_connect_failed, result.responseCode, result.debugMessage)
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        if (isCurrent(currentId)) {
                            fail(currentId, R.string.error_disconnected)
                        }
                    }
                },
            )
        } catch (t: Throwable) {
            Log.e(TAG, "BillingClient init failed", t)
            fail(currentId, R.string.error_init_failed, t.javaClass.simpleName)
        }
    }

    private fun fetchOffers(billing: BillingClient, id: Int) {
        try {
            val productList = TARGET_SKUS.map { sku ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(sku)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billing.queryProductDetailsAsync(
                params,
                ProductDetailsResponseListener { result, details ->
                    if (!isCurrent(id)) return@ProductDetailsResponseListener
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        fail(id, R.string.error_query_failed, result.responseCode, result.debugMessage)
                        return@ProductDetailsResponseListener
                    }

                    val offers = details.orEmpty().flatMap { product ->
                        product.subscriptionOfferDetails.orEmpty().map { offer ->
                            val phases = offer.pricingPhases.pricingPhaseList.map { phase ->
                                OfferPhase(
                                    amountMicros = phase.priceAmountMicros,
                                    formattedPrice = phase.formattedPrice,
                                    billingPeriod = phase.billingPeriod,
                                    billingCycles = phase.billingCycleCount,
                                    recurrenceMode = phase.recurrenceMode,
                                )
                            }
                            OfferEntry(product, offer, phases)
                        }
                    }.sortedWith(
                        compareByDescending<OfferEntry> { it.isFreeIntro }
                            .thenByDescending { it.freeDays }
                            .thenBy { it.product.productId },
                    )

                    onUi(id) { showOfferList(it, id, offers) }
                },
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Product details query threw", t)
            fail(id, R.string.error_query_exception, t.javaClass.simpleName)
        }
    }

    private fun showOfferList(activity: Activity, id: Int, offers: List<OfferEntry>) {
        if (offers.isEmpty()) {
            AlertDialog.Builder(activity)
                .setTitle(text(activity, R.string.dialog_offers_title))
                .setMessage(text(activity, R.string.dialog_offers_empty))
                .setPositiveButton(text(activity, R.string.action_ok), null)
                .show()
            return
        }

        AlertDialog.Builder(activity)
            .setTitle(text(activity, R.string.dialog_offers_count, offers.size))
            .setItems(offers.map { it.rowLabel(activity) }.toTypedArray()) { _, index ->
                if (isCurrent(id)) {
                    showOfferDetail(activity, id, offers[index])
                }
            }
            .setNegativeButton(text(activity, R.string.action_close), null)
            .show()
    }

    private fun showOfferDetail(activity: Activity, id: Int, offer: OfferEntry) {
        val summary = formatOfferSummary(activity, offer)
        val message = summary + text(activity, R.string.detail_disclaimer)

        val dialog = AlertDialog.Builder(activity)
            .setTitle(text(activity, R.string.dialog_details_title))
            .setMessage(message)
            .setNegativeButton(text(activity, R.string.action_back), null)
            .setNeutralButton(text(activity, R.string.action_copy)) { _, _ ->
                copyToClipboard(activity, summary)
            }

        if (offer.isFreeIntro) {
            dialog.setPositiveButton(text(activity, R.string.action_checkout)) { _, _ ->
                AlertDialog.Builder(activity)
                    .setTitle(text(activity, R.string.confirm_title))
                    .setMessage(text(activity, R.string.confirm_message))
                    .setNegativeButton(text(activity, R.string.action_cancel), null)
                    .setPositiveButton(text(activity, R.string.action_continue)) { _, _ ->
                        launchCheckout(activity, id, offer)
                    }
                    .show()
            }
        }
        dialog.show()
    }

    private fun launchCheckout(activity: Activity, id: Int, offer: OfferEntry) {
        if (!isCurrent(id) || activity.isFinishing || activity.isDestroyed) return
        val billing = client
        if (billing == null || !billing.isReady) {
            fail(id, R.string.error_disconnected)
            return
        }

        try {
            val item = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(offer.product)
                .setOfferToken(offer.details.offerToken)
                .build()
            val params = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(item))
                .build()
            val result = billing.launchBillingFlow(activity, params)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                fail(id, R.string.error_checkout_failed, result.responseCode, result.debugMessage)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "launchBillingFlow threw", t)
            fail(id, R.string.error_checkout_exception, t.javaClass.simpleName)
        }
    }

    private fun formatOfferSummary(activity: Activity, offer: OfferEntry): String = buildString {
        append(text(activity, R.string.detail_product, offer.product.productId))
        if (offer.product.title.isNotBlank()) {
            append(text(activity, R.string.detail_title, offer.product.title))
        }
        val offerId = offer.details.offerId ?: text(activity, R.string.detail_no_offer_id)
        append(text(activity, R.string.detail_offer_id, offerId))
        append(text(activity, R.string.detail_base_plan, offer.details.basePlanId))
        if (offer.details.offerTags.isNotEmpty()) {
            append(text(activity, R.string.detail_tags, offer.details.offerTags.joinToString(", ")))
        }
        append(text(activity, R.string.detail_phases_header))
        append(formatPhases(activity, offer.phases))
    }

    private fun formatPhases(activity: Activity, phases: List<OfferPhase>): String {
        if (phases.isEmpty()) return text(activity, R.string.detail_phases_empty)
        return phases.mapIndexed { index, phase ->
            val price = if (phase.amountMicros == 0L) {
                text(activity, R.string.price_free)
            } else {
                phase.formattedPrice
            }
            val cadence = when {
                phase.recurrenceMode == OfferPhase.RECURRENCE_INFINITE || phase.billingCycles == 0 ->
                    text(activity, R.string.phase_ongoing)
                phase.recurrenceMode == OfferPhase.RECURRENCE_NON_RECURRING && phase.billingCycles <= 1 ->
                    text(activity, R.string.phase_once)
                else -> text(activity, R.string.phase_cycles, phase.billingCycles)
            }
            text(activity, R.string.phase_line, index + 1, price, phase.billingPeriod, cadence)
        }.joinToString("\n")
    }

    private fun copyToClipboard(activity: Activity, content: String) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("OnePeek Offer", content))
        toastShort(R.string.status_copied)
    }

    private fun isCurrent(id: Int): Boolean = !closed && id == requestId

    private fun onUi(id: Int, block: (Activity) -> Unit) {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread {
            if (isCurrent(id) && !activity.isFinishing && !activity.isDestroyed) {
                try {
                    block(activity)
                } catch (t: Throwable) {
                    Log.e(TAG, "UI action failed", t)
                }
            }
        }
    }

    private fun text(activity: Activity, resId: Int, vararg args: Any): String =
        ModuleResources.text(activity, resId, *args)

    private fun fail(id: Int, resId: Int, vararg args: Any) {
        onUi(id) { activity ->
            val message = text(activity, resId, *args)
            Log.w(TAG, message)
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun toastShort(resId: Int, vararg args: Any) {
        val activity = activityRef.get() ?: return
        activity.runOnUiThread {
            if (!closed && !activity.isFinishing && !activity.isDestroyed) {
                Toast.makeText(activity, text(activity, resId, *args), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun close() {
        closed = true
        requestId += 1
        client?.endConnection()
        client = null
        activityRef.clear()
    }

    private companion object {
        const val TAG = "OnePeek"
        val TARGET_SKUS = listOf(
            "g1.play.20tb",
            "g1.play.ai.pro",
            "g1.30tb",
            "g1.2tb.ai.subs_variant",
            "g1.ai.pro",
            "g1.youtube_premium_individual_add_on",
        )
    }
}
