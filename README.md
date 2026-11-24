# OnePeek

A minimal libxposed API 102 module scoped strictly to Google One (`com.google.android.apps.subscriptions.red`).

OnePeek adds a floating `Offers` button inside Google One that scans Google Play Billing for all available Google One subscription offers and free trials returned to the active Play account, displays every pricing phase verbatim, and lets you manually open Google Play's official checkout sheet after confirmation.

## Requirements

- Android 8.0+ (`minSdk 26`)
- An LSPosed / libxposed API 102 framework
- Google One (`com.google.android.apps.subscriptions.red`, verified against `1.191.571481160`, `1.335.1018429345`, and `1.336.1024043136`) and Google Play Store (`com.android.vending`)

## Repository Layout

```text
.
├── .github/workflows/
│   ├── ci.yml                  # Spotless, unit tests, APK assembly, signature verification
│   └── release.yml             # Tag-driven pre-release and stable release publisher with SHA256SUMS.txt
├── app/src/
│   ├── main/
│   │   ├── java/io/github/sheikhmehraann/onepeek/
│   │   │   ├── MainHook.kt         # libxposed API 102 hooks, Activity filtering, live SKU capture
│   │   │   ├── BillingSession.kt   # Multi-batch Billing 5.2.1 scan, 4-mode filter, dialogs, checkout
│   │   │   ├── SkuCatalog.kt       # 110-SKU categorized catalog + live observed & custom SKUs
│   │   │   ├── OfferPricing.kt     # OfferKind classifier, EFT matcher, ISO-8601 parser, promo math
│   │   │   └── ModuleResources.kt  # Cross-process module string resolver via ApplicationInfo
│   │   ├── res/                    # Vector launcher icon and English strings
│   │   └── resources/META-INF/xposed/
│   └── test/                   # JUnit 4 tests for OfferPricing and SkuCatalog
├── docs/architecture.md        # Hook surface, longitudinal Google One analysis, Billing 5.2.1 notes
├── CHANGELOG.md
└── LICENSE
```

## How Subscription and Trial Discovery Works

Google Play Billing has no "list all SKUs" RPC; Google One fetches plan SKUs dynamically from Google's servers and passes them to Play Billing via `ITEM_ID_LIST`. OnePeek covers known, high-tier Ultra, and newly introduced subscriptions through three layers:

1. **Built-in 110-SKU categorized catalog:** Queries 42 Google AI Ultra and high-tier SKUs (`g1.play.20tb`, `g1.play.ai.pro`, `g1.30tb`, `g1.20tb`, `g1.ai.ultra`, `.subs_variant`, duration-scoped `g1.play.*`, EFT, and 50% acquisition promos), 27 AI Pro/Plus/Student SKUs, 29 Core Storage and EFT SKUs (`30GB` through `10TB`, `helium`), and 12 Add-On/Bundle SKUs (YouTube Premium, Home/Nest, Health/Fitbit, Play Pass) across independent 18-SKU Play Billing batches.
2. **Passive live SKU capture:** Hooks `android.os.Bundle.putStringArrayList` for `ITEM_ID_LIST` inside Google One to capture any new server-pushed SKUs (such as seasonal, Black Friday, or regional plans) as you browse Google One's own plans pages, prioritizing them at the front of subsequent scans without touching obfuscated app classes.
3. **Custom SKU lookup:** Includes an inline `Add SKU` action inside the offer dialog for querying arbitrary SKUs on demand.

Every returned offer is classified into `FREE_TRIAL` (with `EFT` tagging), `INTRO_PROMO` (with normalized daily-rate discount percentage), or `BASE_PLAN`, tagged with `[ULTRA]` when applicable (`Free 1Y (EFT) -> $99.99/P1M`, `Intro -50% 3M ($124.99) -> $249.99/P1M`). The `Filter` picker switches instantly between `All Free Trials`, `Ultra Trials & Promos`, `All Trials & Intro Promos`, and `All Returned Offers`.

## Installation

1. Download `OnePeek-<version>-release.apk` from [Releases](https://github.com/sheikhmehraann/OnePeek/releases) and verify its digest against `SHA256SUMS.txt`.
2. Install the APK and enable OnePeek in your Xposed manager with scope limited to Google One (`com.google.android.apps.subscriptions.red`).
3. Force-stop and reopen Google One.
4. Tap the **Offers** pill in the bottom-right corner.

Seeing an offer in the list does not guarantee account eligibility. Always verify the active Google Play account, trial length, renewal price, and first billing date on Google Play's checkout sheet.

## Building from Source

Requires JDK 21 and the Android SDK (`compileSdk 36`):

```bash
./gradlew spotlessCheck :app:testDebugUnitTest :app:assembleRelease
```

Optional environment variables for release signing:

- `ONEPEEK_KEYSTORE_PATH`
- `ONEPEEK_STORE_PASSWORD`
- `ONEPEEK_KEY_ALIAS`
- `ONEPEEK_KEY_PASSWORD`

## Release Process

- Pushing to `main` or opening a pull request triggers **CI** (Spotless check, unit tests, debug/release APK build, `apksigner` validation).
- Pushing a pre-release tag such as `v0.3.0-beta.1` triggers **Release** and publishes a GitHub Pre-Release with signed release and debug APKs plus `SHA256SUMS.txt`.
- Pushing a clean semver tag such as `v1.0.0` publishes a stable GitHub Release.
- Set repository secrets `SIGNING_KEYSTORE_BASE64`, `ONEPEEK_STORE_PASSWORD`, `ONEPEEK_KEY_ALIAS`, and `ONEPEEK_KEY_PASSWORD` to keep a fixed release signing key across builds; otherwise CI generates an ephemeral RSA-2048 keystore.

## License

MIT. See [LICENSE](LICENSE).
