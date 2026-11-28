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
│   │   │   ├── MainHook.kt         # libxposed API 102 hooks, Activity filtering, dual-direction SKU capture
│   │   │   ├── BillingSession.kt   # Multi-batch Billing 5.2.1 scan, 5-mode filter, dialogs, checkout
│   │   │   ├── SkuCatalog.kt       # 146-SKU categorized catalog, Ultra 5x/20x tiers, live & custom SKUs
│   │   │   ├── OfferPricing.kt     # OfferKind classifier, EFT matcher, promo math, raw JSON meta parser
│   │   │   └── ModuleResources.kt  # Cross-process module string resolver via ApplicationInfo
│   │   ├── res/                    # Vector launcher icon and English strings
│   │   └── resources/META-INF/xposed/
│   └── test/                   # JUnit 4 tests for OfferPricing and SkuCatalog
├── docs/architecture.md        # Hook surface, longitudinal Google One analysis, Billing 5.2.1 notes
├── CHANGELOG.md
└── LICENSE
```

## How Subscription and Trial Discovery Works

Google Play Billing has no "list all SKUs" RPC; Google One fetches plan SKUs dynamically from Google's servers and exchanges them with Google Play through `Bundle` lists. OnePeek covers known, Ultra 5x/20x, annual, and newly introduced subscriptions through three layers:

1. **Built-in 146-SKU categorized catalog:** Queries 56 Google AI Ultra 5x (20 TB) and Ultra 20x (30 TB) SKUs (`g1.play.20tb`, `g1.play.ai.pro`, `g1.30tb`, `g1.20tb`, `g1.ai.ultra`, `.annual`, `.subs_variant`, duration-scoped `g1.play.*`, EFT, and 50% acquisition promos), 37 AI Pro/Plus/Student SKUs (including `400gb` and `.annual`), 40 Core Storage and EFT SKUs (`30GB` through `10TB`, `helium`, `.annual`), and 13 Add-On/Bundle SKUs (YouTube Premium, Home/Nest including `g1.home_premium_advanced`, Health/Fitbit, Play Pass) across independent 18-SKU Play Billing batches.
2. **Dual-direction passive live SKU capture:** Hooks `android.os.Bundle.putStringArrayList` (`ITEM_ID_LIST` and `SKU_SERIALIZED_DOCID_LIST`) and `android.os.Bundle.getStringArrayList` (`DETAILS_LIST` and `UNFETCHED_PRODUCT_LIST`) inside Google One to passively record any new server-pushed SKUs (such as Black Friday, early-access, or regional plans) as you browse Google One's own plans pages, prioritizing them at the front of subsequent scans without touching obfuscated app classes.
3. **Custom SKU lookup:** Includes an inline `Add SKU` action inside the offer dialog accepting raw SKUs or full `subs:com.google.android.apps.subscriptions.red:<sku>` doc IDs on demand.

Every returned offer is classified into `FREE_TRIAL` (with `EFT` tagging and multi-phase `Free -> Intro -> Recurring` display), `INTRO_PROMO` (with normalized daily-rate discount percentage), or `BASE_PLAN`, badged by Ultra sub-tier (`[ULTRA 20x]`, `[ULTRA 5x]`, `[ULTRA]`), and enriched in Offer Breakdown with billing currency (`priceCurrencyCode`), `installmentPlanDetails`, and `serializedDocid` parsed from Play Billing's raw JSON response. The offer list shows the active Play Store billing currency in its title, opens directly into `Ultra Free Trials` when Google Play returns `$0` Ultra trials to the account (falling back cleanly to `All Free Trials`, `All Trials & Intro Promos`, `All Ultra Plans & Promos`, or `All Returned Offers`), and includes a one-tap `Copy Report` export in `Filter`.

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
- Pushing a pre-release tag such as `v0.5.1-beta.1` triggers **Release** and publishes a GitHub Pre-Release with signed release and debug APKs plus `SHA256SUMS.txt`.
- Pushing a clean semver tag such as `v1.0.0` publishes a stable GitHub Release.
- Set repository secrets `SIGNING_KEYSTORE_BASE64`, `ONEPEEK_STORE_PASSWORD`, `ONEPEEK_KEY_ALIAS`, and `ONEPEEK_KEY_PASSWORD` to keep a fixed release signing key across builds; otherwise CI generates an ephemeral RSA-2048 keystore.

## License

MIT. See [LICENSE](LICENSE).
