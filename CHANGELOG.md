# Changelog

All notable changes to OnePeek are documented in this file.

## [0.5.0-beta.1] - 2026-11-24

Ultra-first inspection defaults, multi-phase pricing chain display, transition-plan SKU capture, and one-tap scan report export.

### Added

- `Copy Report` action inside the `Filter` picker exporting a structured summary of scanned SKU totals, live-captured count, all discovered Ultra 5x/20x plans and trials, and all free trials across every tier.
- Unit tests covering 3-phase `Free -> Intro -> Recurring` compact formatting and multi-`productId` (`transitionPlanDetails`) extraction from Play Billing `DETAILS_LIST` JSON.

### Changed

- Updated default offer view to open directly on `Ultra Free Trials` whenever Google Play returns `$0` Ultra trial offers on the active account, falling back smoothly to `All Free Trials`, `All Trials & Intro Promos`, `All Ultra Plans & Promos`, and `All Returned Offers`.
- Updated `SkuCatalog.recordObservedDetailsJson` to extract all `"productId"` occurrences in each Play Billing JSON object (capturing nested `transitionPlanDetails` target SKUs as well as top-level products).
- Updated `OfferPricing.formatCompactPrice` to display both the intermediate discounted phase and final recurring price on 3-phase `Free -> Intro -> Recurring` offers (`Free 1M -> $49.99/P1M -> $99.99/P1M`).

## [0.4.0-beta.1] - 2026-11-24

Deep Ultra 5x/20x differentiation, dual-direction Play Billing live SKU capture, and raw `ProductDetails` metadata extraction informed by deep DEX diffing of Google One `1.335` -> `1.336`, Billing `5.2.1` `ProductDetails` bytecode inspection, and live `one.google.com` pricing feed (`pricing_2026_11_16.json`) verification.

### Added

- Expanded `SkuCatalog` to 146 unique categorized SKUs (56 Ultra 5x/20x, 37 AI Pro/Plus/Student including 400 GB, 40 Core Storage & EFT, and 13 Add-On/Bundle including `g1.home_premium_advanced`), incorporating `.annual` subscription SKUs verified on `one.google.com/about/plans` (`g1.30gb.annual`, `g1.100gb.annual`, `g1.2tb.ai.annual`, plus high-tier `.annual` equivalents) and expanded Ultra 5x/20x duration and EFT families.
- Added `UltraTier` classification distinguishing **Google AI Ultra 20x (30 TB)** (`[ULTRA 20x]`, `g1.play.ai.pro`, `g1.30tb`) from **Google AI Ultra 5x (20 TB)** (`[ULTRA 5x]`, `g1.play.20tb`, `g1.20tb`) and generic/dynamic Ultra SKUs (`[ULTRA]`) across row labels, sorting, and Offer Breakdown.
- Added dual-direction passive SKU capture in `MainHook`: hooks `Bundle.putStringArrayList` for both `ITEM_ID_LIST` and `SKU_SERIALIZED_DOCID_LIST` (stripping `subs:com.google.android.apps.subscriptions.red:`) and adds protective hook `onepeek.bundle.details` on `Bundle.getStringArrayList` for `DETAILS_LIST` and `UNFETCHED_PRODUCT_LIST` returned to Google One's Billing `9.2.0` client.
- Added pure-Kotlin `OfferPricing.parseRawOfferMeta` to extract `serializedDocid` and `installmentPlanDetails` (`commitmentPaymentsCount`, `subsequentCommitmentPaymentsCount`) from Billing `5.2.1` `ProductDetails` raw JSON payloads without reflection.
- Upgraded the `Filter` picker to 5 instant in-memory views: `Ultra Free Trials`, `All Ultra Plans & Promos`, `All Free Trials`, `All Trials & Intro Promos`, and `All Returned Offers`.
- Added unit tests covering `UltraTier`, dual-direction `SKU_SERIALIZED_DOCID_LIST` / `DETAILS_LIST` capture, and per-offer `RawOfferMeta` JSON isolation.

## [0.3.0-beta.1] - 2026-11-24

Deep Google AI Ultra and all-tier trial expansion informed by longitudinal analysis across official Google One `1.191.571481160` (2025, Billing `6.0.1`), `1.335.1018429345` (`680972`), and `1.336.1024043136` (`682232`, Billing `9.2.0`).

### Added

- Expanded categorized 110-SKU Google One catalog (`SkuCatalog`) partitioned across 42 Ultra and high-tier trial/promo SKUs (`g1.play.20tb`, `g1.play.ai.pro`, `g1.30tb`, `g1.20tb`, `g1.ai.ultra`, `.subs_variant`, duration-scoped `g1.play.*`, EFT, and acquisition promo variants), 27 AI Pro/Plus/Student SKUs, 29 Core Storage and EFT SKUs (`30GB` through `10TB` and `helium`), and 12 Add-On/Bundle SKUs.
- Offer classification (`OfferKind`: `FREE_TRIAL`, `INTRO_PROMO`, `BASE_PLAN`), EFT detection matching Google One `1.336`'s `.*eft.*` pattern across `offerId`, `basePlanId`, and `offerTags`, and normalized daily-rate introductory discount percentage computation in `OfferPricing`.
- Four instant in-memory filter views accessible via the `Filter` picker (`All Free Trials`, `Ultra Trials & Promos`, `All Trials & Intro Promos`, and `All Returned Offers`) with live per-bucket counts.
- Inline `[ULTRA]` row tags, `(EFT)` trial labels (`Free 1Y (EFT) -> $99.99/P1M`), intro promo summaries (`Intro -50% 3M ($124.99) -> $249.99/P1M`), and SKU Category plus Offer Type fields in the Offer Breakdown dialog.
- Expanded JUnit 4 tests in `OfferPricingTest` and `SkuCatalogTest` covering offer classification, EFT detection, daily-rate intro discount math, and SKU categorization.

### Changed

- Updated offer ranking to prioritize free trials, then intro promos, then Ultra tier, then longest free duration, then highest intro discount percentage.
- Enabled the two-step manual `Open Play Checkout` flow for both `FREE_TRIAL` and `INTRO_PROMO` offers (including Ultra discounted intro promotions).

## [0.2.0-beta.1] - 2026-11-24

Verified against official Google One `1.335.1018429345` (`680972`) and `1.336.1024043136` (`682232`) APKM builds (Play Billing `9.2.0`, `targetSdk 37`).

### Added

- Full 59-SKU Google One subscription catalog (`SkuCatalog`) covering storage tiers (`30GB` through `30TB`, `helium`), AI Plus/Pro/Ultra and `g1.play.*` trial variants, acquisition/EFT promo SKUs, and add-ons (YouTube Premium, Home/Nest, Health/Fitbit, Play Pass), queried across resilient 18-SKU Play Billing batches with per-batch fault isolation and offer deduplication.
- Passive live SKU discovery via a protective hook on `android.os.Bundle.putStringArrayList` (`ITEM_ID_LIST`), capturing new server-delivered SKUs directly from Google One's own Play Billing `9.2.0` queries with zero obfuscated-symbol coupling.
- Trial-first offer list defaulting to all discovered free-trial subscriptions with inline trial duration and renewal price (`Free 1Y -> $99.99/P1M`), a one-tap toggle between free trials and all offers, and an inline `Add SKU` dialog for arbitrary SKU lookups.
- Unit test suite expanded with `SkuCatalogTest` and compact price formatting assertions in `OfferPricingTest`.

### Changed

- Expanded non-UI Activity exclusion list in `MainHook` to skip `PlayCoreDialogWrapperActivity`, `TransparentGoogleOneSdkActivity`, `GatewayActivity`, `AnimatedGatewayActivity`, `GoogleApiActivity`, and `SystemTrayActivity` in addition to `ProxyBillingActivity` and `ProxyBillingActivityV2`.

## [0.1.0-beta.1] - 2026-11-24

Initial public pre-release of OnePeek for Google One (`com.google.android.apps.subscriptions.red`).

### Added

- Standalone libxposed API 102 module statically scoped solely to `com.google.android.apps.subscriptions.red`.
- Protective `Activity.onResume` and `Activity.onDestroy` hooks that skip `com.android.billingclient.*` activities so the floating button never covers `ProxyBillingActivity`.
- Google Play Billing Library `5.2.1` offer inspector with ISO-8601 duration parsing (`P(\d+)([DWMY])`) and consecutive leading free-phase scoring.
- Offer breakdown dialog showing SKU, title, offer ID, base plan ID, offer tags, full pricing phases, clipboard copy, and two-step manual confirmation before opening Play checkout.
- Thread-safe cross-process module string resolution via `ApplicationInfo` and `BillingClient.isReady` verification before checkout launch.
- Split GitHub Actions `CI` and `Release` workflows with APK signature validation, Xposed metadata checks, and `SHA256SUMS.txt` generation.
