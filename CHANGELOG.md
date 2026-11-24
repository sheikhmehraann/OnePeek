# Changelog

All notable changes to OnePeek are documented in this file.

## [0.1.0-beta.1] - 2026-11-24

Initial public pre-release of OnePeek for Google One (`com.google.android.apps.subscriptions.red`).

### Added

- Standalone libxposed API 102 module statically scoped solely to `com.google.android.apps.subscriptions.red`.
- Protective `Activity.onResume` and `Activity.onDestroy` hooks that skip `com.android.billingclient.*` activities so the floating button never covers `ProxyBillingActivity`.
- Google Play Billing Library `5.2.1` offer inspector querying `g1.play.20tb`, `g1.play.ai.pro`, `g1.30tb`, `g1.2tb.ai.subs_variant`, `g1.ai.pro`, and `g1.youtube_premium_individual_add_on`.
- ISO-8601 duration parser (`P(\d+)([DWMY])`) and consecutive leading free-phase scorer with JUnit 4 coverage.
- Offer breakdown dialog showing SKU, title, offer ID, base plan ID, offer tags, full pricing phases (intro and recurring), clipboard copy, and two-step manual confirmation before opening Play checkout.
- Thread-safe cross-process module string resolution via `ApplicationInfo` and `BillingClient.isReady` verification before checkout launch.
- Split GitHub Actions `CI` and `Release` workflows with APK signature validation, Xposed metadata checks, and `SHA256SUMS.txt` generation.
