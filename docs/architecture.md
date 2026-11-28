# Architecture

OnePeek is a single-purpose libxposed API 102 module built strictly for `com.google.android.apps.subscriptions.red` (Google One), verified longitudinally against shipped Google One `1.191.571481160` (2025, Billing `6.0.1`), `1.335.1018429345` (`680972`), and `1.336.1024043136` (`682232`, Billing `9.2.0`).

## Process Identity and Hook Surface

Play Billing scopes subscription catalogs by the calling package UID passed over Android Binder to `com.android.vending` (`IInAppBillingService`). By executing inside Google One's process, OnePeek queries Google One subscription offers through Google's standard Billing Library without depending on any R8-obfuscated Google One classes.

Four protective framework hooks are installed:

1. `onepeek.activity.resume` (`Activity.onResume`, after): attaches the floating `Offers` pill to `decorView`, skipping `com.android.billingclient.*` (`ProxyBillingActivity`, `ProxyBillingActivityV2`) and six non-UI wrapper/gateway activities verified in Google One `1.336`'s manifest (`PlayCoreDialogWrapperActivity`, `TransparentGoogleOneSdkActivity`, `GatewayActivity`, `AnimatedGatewayActivity`, `GoogleApiActivity`, `SystemTrayActivity`).
2. `onepeek.activity.destroy` (`Activity.onDestroy`, before): closes the per-Activity `BillingSession` and ends its `BillingClient` connection.
3. `onepeek.bundle.skus` (`Bundle.putStringArrayList`, before): watches exclusively for keys `ITEM_ID_LIST` and `SKU_SERIALIZED_DOCID_LIST`. Static analysis of Google One `1.191` through `1.336` confirmed that Google One hardcodes at most `g1.100gb` locally and receives all other subscription SKUs dynamically from Google servers (`SubscriptionsManagementService.GetStorageUpsellFlowUrl`, `SubscriptionsApiMobileService.ListEligiblePlans`, `GetDynamicStorefrontInfo`, and `1.336`'s `PurchaseWebViewInterfaceImpl` JS bridge) before passing them to Play Billing `9.2.0`.
4. `onepeek.bundle.details` (`Bundle.getStringArrayList`, after): passively reads `DETAILS_LIST` and `UNFETCHED_PRODUCT_LIST` returned from Play Store to Google One's Billing `9.2.0` client (`Lhbu;.C`), extracting all `"productId"` values (including nested `transitionPlanDetails` target SKUs) without mutating responses.

## Play Billing 5.2.1 Compatibility Against Google One's Billing 9.2.0

OnePeek pins `com.android.billingclient:billing:5.2.1` and disables code shrinking (`isMinifyEnabled = false`):

- Google One `1.335` and `1.336` ship heavily R8-minified Play Billing `9.2.0` (`com.google.android.play.billingclient.version = 9.2.0`), yet their manifest still declares both `com.android.billingclient.api.ProxyBillingActivity` and `ProxyBillingActivityV2`, and their minified client still uses `IInAppBillingService` Binder transactions `901` (`PRODUCT_DETAILS`) and `8` (`getBuyIntentExtraParams`).
- Leaving class names unminified guarantees `ProxyBillingActivity.class` resolves directly to Google One's manifest entry.
- Play Billing `5.2.1`'s `BillingClientImpl` internally chunks `ITEM_ID_LIST` in steps of 20. `SkuCatalog` splits its 146 categorized SKUs (56 Ultra 5x/20x, 37 AI Pro/Plus/Student, 40 Core Storage/EFT, 13 Add-On/Bundle, plus live/custom SKUs) into independent 18-SKU batches so a failure on one batch never discards offers returned by other batches.
- Although Play Billing `5.2.1` does not expose public Java getters for `installmentPlanDetails` or `serializedDocid`, `ProductDetails.toString()` embeds the raw `jsonString` payload returned by Google Play. `OfferPricing.parseRawOfferMeta` slices the matching `offerIdToken` object to surface `commitmentPaymentsCount`, `subsequentCommitmentPaymentsCount`, and `serializedDocid` cleanly without reflection.

## Offer Classification, Ultra 5x/20x Filtering, and Checkout Gate

Mirroring Google One `1.336`'s three-bucket promotion taxonomy (`FREE_TRIAL_PROMOTION_OFFER`, `INTRO_PRICING_PROMOTION_OFFER`, `IMMEDIATE_PURCHASE`), its `.+eft.*` Extended Free Trial detector, and Google One's live `pricing_2026_11_16.json` tier matrix (`PRICE_GEN_AI_ULTRA_100_MONTHLY` 20 TB vs `PRICE_GEN_AI_ULTRA_200_MONTHLY` 30 TB):

- `SkuCatalog.ultraTierOf` and `SkuCatalog.categoryOf` classify every SKU (including live-captured and custom SKUs, using both SKU ID and Play Billing product title) into `ULTRA_20X_30TB` (`[ULTRA 20x]`), `ULTRA_5X_20TB` (`[ULTRA 5x]`), `ULTRA_GENERIC` (`[ULTRA]`), `AI_PRO_PLUS`, `STORAGE`, `ADDON`, or `DYNAMIC`.
- `OfferPricing` parses `P(\d+)([DWMY])` ISO-8601 periods into day equivalents, detects the active Play Store billing currency (`priceCurrencyCode`) across Google One's 63 regional currencies, classifies each offer into `FREE_TRIAL`, `INTRO_PROMO` (computing normalized daily-rate discount percentages between intro and recurring phases), or `BASE_PLAN`, flags EFT matches across `offerId`, `basePlanId`, and `offerTags`, and formats concise row summaries (`Free 1Y (EFT) -> $99.99/P1M`, `Intro -50% 3M ($124.99) -> $249.99/P1M`).
- Returned offers are deduplicated by `(productId, basePlanId, offerId, offerToken)` and sorted by:

1. Offer kind rank (`FREE_TRIAL` > `INTRO_PROMO` > `BASE_PLAN`)
2. Ultra sub-tier priority (`ULTRA_20X_30TB` > `ULTRA_5X_20TB` > `ULTRA_GENERIC` > `NONE`)
3. Total leading free duration in days (descending)
4. Introductory discount percentage (descending)
5. Product ID (ascending)
6. Offer ID or base plan ID (ascending)

The Offer List opens directly in `Ultra Free Trials` when Google Play returns `$0` Ultra trial offers (falling back to `All Free Trials`, then `All Trials & Intro Promos`, then `All Ultra Plans & Promos`, then `All Returned Offers`), and provides a `Filter` picker for switching between all 5 views or copying a full diagnostic scan report without re-querying Play Billing. Both `FREE_TRIAL` and `INTRO_PROMO` offers expose `Open Play Checkout`, which requires a second confirmation dialog and checks `billing.isReady` before invoking `BillingClient.launchBillingFlow`.

## Explicit Non-Goals

OnePeek does not perform account switching, credential entry, payment card handling, response tampering, automated checkout clicking, or purchase acknowledgment. All eligibility enforcement and billing terms remain under Google Play's control.
