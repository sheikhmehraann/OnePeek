# OnePeek

A minimal libxposed API 102 module scoped strictly to Google One (`com.google.android.apps.subscriptions.red`).

OnePeek adds a small floating button inside Google One that queries Google Play Billing Library `5.2.1` for the subscription offers returned to the active Play account, lists every pricing phase verbatim, and lets you manually open Google Play's official checkout sheet after confirmation.

## Requirements

- Android 8.0+ (`minSdk 26`)
- An LSPosed / libxposed API 102 framework
- Google One (`com.google.android.apps.subscriptions.red`) and Google Play Store (`com.android.vending`)

## Repository Layout

```text
.
├── .github/workflows/
│   ├── ci.yml                  # Unit tests, Spotless check, APK assembly, signature verification
│   └── release.yml             # Tag-driven pre-release and stable release publisher with SHA256SUMS.txt
├── app/src/
│   ├── main/
│   │   ├── java/io/github/sheikhmehraann/onepeek/
│   │   │   ├── MainHook.kt         # libxposed API 102 entrypoint and floating button lifecycle
│   │   │   ├── BillingSession.kt   # Play Billing 5.2.1 connection, query, dialogs, checkout launch
│   │   │   ├── OfferPricing.kt     # ISO-8601 duration parser and leading free-phase scorer
│   │   │   └── ModuleResources.kt  # Cross-process module string resolver via ApplicationInfo
│   │   ├── res/                    # Vector launcher icon and English strings
│   │   └── resources/META-INF/xposed/
│   └── test/                   # JUnit 4 tests for OfferPricing
├── docs/architecture.md        # Hook surface, Binder IPC notes, and Billing 5.2.1 rationale
├── CHANGELOG.md
└── LICENSE
```

## Tracked SKUs

`BillingSession` queries these subscription SKUs through `BillingClient.queryProductDetailsAsync`:

- `g1.play.20tb`
- `g1.play.ai.pro`
- `g1.30tb`
- `g1.2tb.ai.subs_variant`
- `g1.ai.pro`
- `g1.youtube_premium_individual_add_on`

Offers with a free introductory phase sort first by total leading free duration, then by SKU. Tapping any row displays the SKU, title, offer ID, base plan ID, tags, and all introductory and recurring phases, with an option to copy the breakdown.

## Installation

1. Download `OnePeek-<version>-release.apk` from [Releases](https://github.com/sheikhmehraann/OnePeek/releases) and verify its digest against `SHA256SUMS.txt`.
2. Install the APK and enable OnePeek in your Xposed manager with scope limited to Google One (`com.google.android.apps.subscriptions.red`).
3. Force-stop and reopen Google One.
4. Tap the **Offers** pill in the bottom-right corner.

Seeing an offer in the list does not guarantee account eligibility. Always verify the active Google Play account, trial length, renewal price, and first billing date on Google Play's checkout sheet.

## Building from Source

Requires JDK 21 and the Android SDK (`compileSdk 36`):

```bash
./gradlew :app:testDebugUnitTest :app:assembleRelease
```

Optional environment variables for release signing:

- `ONEPEEK_KEYSTORE_PATH`
- `ONEPEEK_STORE_PASSWORD`
- `ONEPEEK_KEY_ALIAS`
- `ONEPEEK_KEY_PASSWORD`

## Release Process

- Pushing to `main` or opening a pull request triggers **CI** (tests, Spotless check, debug/release APK build, `apksigner` validation).
- Pushing a pre-release tag such as `v0.1.0-beta.1` triggers **Release** and publishes a GitHub Pre-Release with signed release and debug APKs plus `SHA256SUMS.txt`.
- Pushing a clean semver tag such as `v1.0.0` publishes a stable GitHub Release.
- Set repository secrets `SIGNING_KEYSTORE_BASE64`, `ONEPEEK_STORE_PASSWORD`, `ONEPEEK_KEY_ALIAS`, and `ONEPEEK_KEY_PASSWORD` to keep a fixed release signing key across builds; otherwise CI generates an ephemeral RSA-2048 keystore.

## License

MIT. See [LICENSE](LICENSE).
