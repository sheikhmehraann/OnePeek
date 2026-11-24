# OnePeek

A minimal libxposed API 102 module scoped strictly to Google One (`com.google.android.apps.subscriptions.red`).

It adds a small floating button inside Google One that queries Google Play Billing Library `5.2.1` for the subscription offers returned to the active Play account, displays every pricing phase verbatim, and lets you manually open Google Play's official checkout sheet after a confirmation prompt.

## Scope and Design

- Targets `com.google.android.apps.subscriptions.red` only (`staticScope=true`).
- Hooks only `android.app.Activity.onResume` and `android.app.Activity.onDestroy` in protective mode, skipping Play Billing's own `ProxyBillingActivity` so the overlay button does not cover checkout screens.
- Touches zero private or obfuscated Google One classes, so it needs no bytecode scanners or reflection wrappers.
- Bundles only `com.android.billingclient:billing:5.2.1` (matching the API v17 `PRODUCT_DETAILS` and `ProxyBillingActivity` protocol) with no extra permissions or native libraries.
- Never automates account switching, card entry, price overrides, or checkout confirmation. All eligibility checks and purchase terms are enforced by Google Play.

## Tracked SKUs

Queries the following subscription SKUs through `BillingClient.queryProductDetailsAsync`:

- `g1.play.20tb`
- `g1.play.ai.pro`
- `g1.30tb`
- `g1.2tb.ai.subs_variant`
- `g1.ai.pro`
- `g1.youtube_premium_individual_add_on`

Offers are sorted with free introductory phases first, ordered by total leading free duration, then by SKU name.

## Usage

1. Install the APK on a device running an LSPosed / libxposed API 102 framework.
2. Enable OnePeek for Google One (`com.google.android.apps.subscriptions.red`) in your Xposed manager.
3. Force-stop and reopen Google One.
4. Tap the **Offers** pill in the bottom-right corner to inspect returned offers and pricing phases.

Seeing an offer in the query list does not guarantee account eligibility. Always verify the active Google Play account, trial length, and renewal price on the official Play checkout sheet.

## Build

Requires JDK 21 and the Android SDK (compileSdk 36):

```bash
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

Push to `main` or trigger the **Build APK** workflow in GitHub Actions to run unit tests, assemble signed release and debug APKs, verify signatures and `META-INF/xposed/*` entries, upload artifacts, and publish a release.

Optional repository secrets for stable release signing across builds:

- `SIGNING_KEYSTORE_BASE64`: base64-encoded PKCS12/JKS keystore
- `ONEPEEK_STORE_PASSWORD`
- `ONEPEEK_KEY_ALIAS`
- `ONEPEEK_KEY_PASSWORD`

If omitted, the workflow generates a fresh RSA-2048 keystore for signing.
