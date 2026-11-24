# Architecture

OnePeek is a single-purpose libxposed API 102 module built strictly for `com.google.android.apps.subscriptions.red` (Google One).

## Process Identity and Hook Surface

Play Billing scopes subscription catalogs by the calling package UID passed across Android Binder to `com.android.vending` (`IInAppBillingService`). By running inside Google One's process, OnePeek queries Google One's subscription SKUs using Google's standard Billing Library without touching any private or obfuscated Google One internals.

Two protective hooks are installed on `android.app.Activity`:

- `onepeek.activity.resume` (after): attaches a floating `Offers` pill to the Activity `decorView` unless the Activity class lives under `com.android.billingclient.` (avoiding `ProxyBillingActivity` overlays).
- `onepeek.activity.destroy` (before): tears down the per-Activity `BillingSession` and ends its `BillingClient` connection.

## Play Billing 5.2.1 Protocol

OnePeek pins `com.android.billingclient:billing:5.2.1` and leaves code shrinking disabled (`isMinifyEnabled = false`) for two reasons:

1. Billing `5.2.1` uses API v17 `PRODUCT_DETAILS` over `IInAppBillingService` (`tx 901` for `queryProductDetailsAsync`, `tx 8` for `launchBillingFlow`) and starts `com.android.billingclient.api.ProxyBillingActivity` against the host application's manifest.
2. Leaving class names intact ensures `ProxyBillingActivity.class` resolves to the exact component name declared by Google One.

## Offer Ranking and Checkout Gate

`OfferPricing` parses `P(\d+)([DWMY])` ISO-8601 periods into day equivalents and sums consecutive leading phases where `priceAmountMicros == 0L`. Returned offers sort by:

1. Free introductory phase present (descending)
2. Total leading free duration in days (descending)
3. Product ID (ascending)

Only offers with a free introductory phase expose the `Open Play Checkout` action, which requires a second confirmation dialog warning about Play account mismatches and one-time eligibility consumption before calling `BillingClient.launchBillingFlow`. Before launching, `BillingSession` verifies `billing.isReady` to avoid failing silently on a stale connection.

## Explicit Non-Goals

OnePeek does not perform account switching, credential entry, payment card handling, response tampering, automated checkout clicking, or purchase acknowledgment. All eligibility enforcement and billing terms remain under Google Play's control.
