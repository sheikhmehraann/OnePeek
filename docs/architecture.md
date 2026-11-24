# Architecture

OnePeek is a single-purpose libxposed API 102 module built strictly for `com.google.android.apps.subscriptions.red` (Google One), verified longitudinally against shipped Google One `1.191.571481160` (2025, Billing `6.0.1`), `1.335.1018429345` (`680972`), and `1.336.1024043136` (`682232`, Billing `9.2.0`).

## Process Identity and Hook Surface

Play Billing scopes subscription catalogs by the calling package UID passed over Android Binder to `com.android.vending` (`IInAppBillingService`). By executing inside Google One's process, OnePeek queries Google One subscription offers through Google's standard Billing Library without depending on any R8-obfuscated Google One classes.

Three protective framework hooks are installed:

1. `onepeek.activity.resume` (`Activity.onResume`, after): attaches the floating `Offers` pill to `decorView`, skipping `com.android.billingclient.*` (`ProxyBillingActivity`, `ProxyBillingActivityV2`) and six non-UI wrapper/gateway activities verified in Google One `1.336`'s manifest (`PlayCoreDialogWrapperActivity`, `TransparentGoogleOneSdkActivity`, `GatewayActivity`, `AnimatedGatewayActivity`, `GoogleApiActivity`, `SystemTrayActivity`).
2. `onepeek.activity.destroy` (`Activity.onDestroy`, before): closes the per-Activity `BillingSession` and ends its `BillingClient` connection.
3. `onepeek.bundle.skus` (`Bundle.putStringArrayList`, before): watches exclusively for key `ITEM_ID_LIST`. Static analysis of Google One `1.191` through `1.336` confirmed that Google One hardcodes at most `g1.100gb` locally and receives all other subscription SKUs dynamically from Google servers (`SubscriptionsManagementService.GetStorageUpsellFlowUrl`, `SubscriptionsApiMobileService.ListEligiblePlans`, and `GetDynamicStorefrontInfo`) before passing them to Play Billing `9.2.0` via `Bundle.putStringArrayList("ITEM_ID_LIST", ...)`. Recording those strings passively keeps OnePeek's scan list current when Google rolls out seasonal, Black Friday, early-access, or regional SKUs.

## Play Billing 5.2.1 Compatibility Against Google One's Billing 9.2.0

OnePeek pins `com.android.billingclient:billing:5.2.1` and disables code shrinking (`isMinifyEnabled = false`):

- Google One `1.335` and `1.336` ship heavily R8-minified Play Billing `9.2.0` (`com.google.android.play.billingclient.version = 9.2.0`), yet their manifest still declares both `com.android.billingclient.api.ProxyBillingActivity` and `ProxyBillingActivityV2`, and their minified client still uses `IInAppBillingService` Binder transactions `901` (`PRODUCT_DETAILS`) and `8` (`getBuyIntentExtraParams`).
- Leaving class names unminified guarantees `ProxyBillingActivity.class` resolves directly to Google One's manifest entry.
- Play Billing `5.2.1`'s `BillingClientImpl` internally chunks `ITEM_ID_LIST` in steps of 20. `SkuCatalog` splits its 110 categorized SKUs (42 Ultra/high-tier, 27 AI Pro/Plus/Student, 29 Core Storage/EFT, 12 Add-On/Bundle, plus live/custom SKUs) into independent 18-SKU batches so a failure on one batch never discards offers returned by other batches.

## Offer Classification, Ultra Filtering, and Checkout Gate

Mirroring Google One `1.336`'s three-bucket promotion taxonomy (`FREE_TRIAL_PROMOTION_OFFER`, `INTRO_PRICING_PROMOTION_OFFER`, `IMMEDIATE_PURCHASE`) and its `.+eft.*` Extended Free Trial detector:

- `SkuCatalog.categoryOf` classifies every SKU (including live-captured and custom SKUs, using both SKU ID and Play Billing product title) into `ULTRA`, `AI_PRO_PLUS`, `STORAGE`, `ADDON`, or `DYNAMIC`.
- `OfferPricing` parses `P(\d+)([DWMY])` ISO-8601 periods into day equivalents, classifies each offer into `FREE_TRIAL`, `INTRO_PROMO` (computing normalized daily-rate discount percentages between intro and recurring phases), or `BASE_PLAN`, flags EFT matches across `offerId`, `basePlanId`, and `offerTags`, and formats concise row summaries (`Free 1Y (EFT) -> $99.99/P1M`, `Intro -50% 3M ($124.99) -> $249.99/P1M`).
- Returned offers are deduplicated by `(productId, basePlanId, offerId, offerToken)` and sorted by:

1. Offer kind rank (`FREE_TRIAL` > `INTRO_PROMO` > `BASE_PLAN`)
2. Ultra category priority (descending)
3. Total leading free duration in days (descending)
4. Introductory discount percentage (descending)
5. Product ID (ascending)
6. Offer ID or base plan ID (ascending)

The Offer List opens in `All Free Trials` when free trials exist (or `All Trials & Intro Promos` if only intro promos exist), and provides a `Filter` picker for switching between `All Free Trials`, `Ultra Trials & Promos`, `All Trials & Intro Promos`, and `All Returned Offers` without re-querying Play Billing. Both `FREE_TRIAL` and `INTRO_PROMO` offers expose `Open Play Checkout`, which requires a second confirmation dialog and checks `billing.isReady` before invoking `BillingClient.launchBillingFlow`.

## Explicit Non-Goals

OnePeek does not perform account switching, credential entry, payment card handling, response tampering, automated checkout clicking, or purchase acknowledgment. All eligibility enforcement and billing terms remain under Google Play's control.
