# Changelog

All notable changes to OnePeek are documented in this file.

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
