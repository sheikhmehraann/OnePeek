# Architecture

OnePeek is a single-purpose libxposed API 102 module built strictly for `com.google.android.apps.subscriptions.red` (Google One), verified against shipped Google One `1.335.1018429345` and `1.336.1024043136` builds.

## Process Identity and Hook Surface

Play Billing scopes subscription catalogs by the calling package UID passed over Android Binder to `com.android.vending` (`IInAppBillingService`). By executing inside Google One's process, OnePeek queries Google One subscription offers through Google's standard Billing Library without depending on any R8-obfuscated Google One classes.

Three protective framework hooks are installed:

1. `onepeek.activity.resume` (`Activity.onResume`, after): attaches the floating `Offers` pill to `decorView`, skipping `com.android.billingclient.*` (`ProxyBillingActivity`, `ProxyBillingActivityV2`) and six non-UI wrapper/gateway activities verified in Google One `1.336`'s manifest (`PlayCoreDialogWrapperActivity`, `TransparentGoogleOneSdkActivity`, `GatewayActivity`, `AnimatedGatewayActivity`, `GoogleApiActivity`, `SystemTrayActivity`).
2. `onepeek.activity.destroy` (`Activity.onDestroy`, before): closes the per-Activity `BillingSession` and ends its `BillingClient` connection.
3. `onepeek.bundle.skus` (`Bundle.putStringArrayList`, before): watches exclusively for key `ITEM_ID_LIST`. Static analysis of Google One `1.336` confirmed that Google One hardcodes only `g1.100gb` locally and receives all other subscription SKUs dynamically from Google servers (`SubscriptionsManagementService.GetStorageUpsellFlowUrl` and `SubscriptionsApiMobileService.GetDynamicStorefrontInfo`) before passing them to Play Billing `9.2.0` via `Bundle.putStringArrayList("ITEM_ID_LIST", ...)`. Recording those strings passively keeps OnePeek's scan list current when Google adds new regional or promotional SKUs.

## Play Billing 5.2.1 Compatibility Against Google One's Billing 9.2.0

OnePeek pins `com.android.billingclient:billing:5.2.1` and disables code shrinking (`isMinifyEnabled = false`):

- Google One `1.335` and `1.336` ship heavily R8-minified Play Billing `9.2.0` (`com.google.android.play.billingclient.version = 9.2.0`), yet their manifest still declares both `com.android.billingclient.api.ProxyBillingActivity` and `ProxyBillingActivityV2`, and their minified client still uses `IInAppBillingService` Binder transactions `901` (`PRODUCT_DETAILS`) and `8` (`getBuyIntentExtraParams`).
- Leaving class names unminified guarantees `ProxyBillingActivity.class` resolves directly to Google One's manifest entry.
- Play Billing `5.2.1`'s `BillingClientImpl` internally chunks `ITEM_ID_LIST` in steps of 20. `SkuCatalog` splits queries into independent 18-SKU batches so a failure on one batch never discards offers returned by other batches.

## Trial Discovery, Ranking, and Checkout Gate

`OfferPricing` parses `P(\d+)([DWMY])` ISO-8601 periods into day equivalents, sums consecutive leading zero-cost phases, and formats concise trial-to-renewal labels (`Free 1Y -> $99.99/P1M`). Returned offers are deduplicated by `(productId, basePlanId, offerId, offerToken)` and sorted by:

1. Free introductory phase present (descending)
2. Total leading free duration in days (descending)
3. Product ID (ascending)
4. Offer ID or base plan ID (ascending)

The Offer List opens in free-trial mode when trials are present, with buttons to toggle all returned offers or query custom SKUs on demand. Only offers with a free introductory phase expose `Open Play Checkout`, which requires a second confirmation dialog and checks `billing.isReady` before invoking `BillingClient.launchBillingFlow`.

## Explicit Non-Goals

OnePeek does not perform account switching, credential entry, payment card handling, response tampering, automated checkout clicking, or purchase acknowledgment. All eligibility enforcement and billing terms remain under Google Play's control.
