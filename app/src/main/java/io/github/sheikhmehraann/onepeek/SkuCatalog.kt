package io.github.sheikhmehraann.onepeek

import java.util.concurrent.ConcurrentHashMap

internal enum class SkuCategory {
    ULTRA,
    AI_PRO_PLUS,
    STORAGE,
    ADDON,
    DYNAMIC,
}

internal enum class UltraTier {
    NONE,
    ULTRA_5X_20TB,
    ULTRA_20X_30TB,
    ULTRA_GENERIC,
}

internal object SkuCatalog {
    const val BATCH_SIZE = 18

    private val skuPattern = Regex("^[a-zA-Z0-9][a-zA-Z0-9._-]{2,79}$")
    private val docIdPrefix = "subs:com.google.android.apps.subscriptions.red:"
    private val jsonProductIdRegex = Regex("\"productId\"\\s*:\\s*\"([a-zA-Z0-9][a-zA-Z0-9._-]{2,79})\"")
    private val observed = ConcurrentHashMap.newKeySet<String>()
    private val custom = ConcurrentHashMap.newKeySet<String>()

    val ultraSkus: List<String> = listOf(
        "g1.play.20tb",
        "g1.play.ai.pro",
        "g1.30tb",
        "g1.20tb",
        "g1.ai.ultra",
        "g1.play.ai.ultra",
        "g1.ultra",
        "g1.ultra.20tb",
        "g1.ultra.30tb",
        "g1.20tb.ai",
        "g1.30tb.ai",
        "g1.20tb.annual",
        "g1.30tb.annual",
        "g1.ai.ultra.annual",
        "g1.play.30tb",
        "g1.play.20tb.12month",
        "g1.play.20tb.6month",
        "g1.play.20tb.3month",
        "g1.play.20tb.1month",
        "g1.play.30tb.12month",
        "g1.play.30tb.6month",
        "g1.play.30tb.3month",
        "g1.play.30tb.1month",
        "g1.play.ai.pro.12month",
        "g1.play.ai.pro.6month",
        "g1.play.ai.pro.3month",
        "g1.play.ai.pro.1month",
        "g1.play.ai.pro.subs_variant",
        "g1.play.ai.ultra.12month",
        "g1.play.ai.ultra.6month",
        "g1.play.ai.ultra.3month",
        "g1.play.ai.ultra.1month",
        "g1.play.ultra",
        "g1.play.ultra.20tb",
        "g1.play.ultra.30tb",
        "g1.ai.ultra.20tb",
        "g1.ai.ultra.30tb",
        "g1.20tb.12months_eft",
        "g1.20tb.6months_eft",
        "g1.20tb.3months_eft",
        "g1.20tb.1month_eft",
        "g1.30tb.12months_eft",
        "g1.30tb.6months_eft",
        "g1.30tb.3months_eft",
        "g1.30tb.1month_eft",
        "g1.20tb.ai.subs_variant",
        "g1.30tb.ai.subs_variant",
        "g1.ai.ultra.subs_variant",
        "g1.play.20tb.subs_variant",
        "g1.play.30tb.subs_variant",
        "g1.play.ai.ultra.subs_variant",
        "g1.20tb.subs_variant",
        "g1.30tb.subs_variant",
        "g1.30tb.3months_ip_50p.acq",
        "g1.20tb.3months_ip_50p.acq",
        "g1.ai.ultra.3months_ip_50p.acq",
    )

    val aiProPlusSkus: List<String> = listOf(
        "g1.2tb.ai.subs_variant",
        "g1.2tb.ai",
        "g1.2tb.ai.annual",
        "g1.5tb.ai",
        "g1.5tb.ai.annual",
        "g1.10tb.ai",
        "g1.ai.pro",
        "g1.ai.pro.annual",
        "g1.ai.plus",
        "g1.ai.plus.annual",
        "g1.ai",
        "g1.5tb.ai.subs_variant",
        "g1.10tb.ai.subs_variant",
        "g1.ai.pro.subs_variant",
        "g1.ai.plus.subs_variant",
        "g1.play.ai",
        "g1.play.ai.plus",
        "g1.play.ai.plus.subs_variant",
        "g1.play.pro",
        "g1.play.plus",
        "g1.pro",
        "g1.plus",
        "g1.premium",
        "g1.play",
        "g1.2tb.ai.12months_eft",
        "g1.2tb.ai.6months_eft",
        "g1.2tb.ai.3months_eft",
        "g1.2tb.ai.1month_eft",
        "g1.5tb.ai.12months_eft",
        "g1.400gb",
        "g1.400gb.annual",
        "g1.400gb.12months_eft",
        "g1.400gb.1month_eft",
        "g1.play.400gb",
        "g1.ai.student",
        "g1.play.ai.student",
        "g1.2tb.ai.student",
    )

    val storageSkus: List<String> = listOf(
        "g1.30gb",
        "g1.30gb.annual",
        "g1.30gb.1month_eft",
        "g1.100gb",
        "g1.100gb.annual",
        "g1.100gb.12months_eft",
        "g1.100gb.1month_eft",
        "g1.100gb.3months_ip_83p.acq",
        "g1.200gb",
        "g1.200gb.annual",
        "g1.200gb.12months_eft",
        "g1.200gb.1month_eft",
        "g1.200gb.3months_ip_83p.acq",
        "g1.helium",
        "g1.helium.annual",
        "g1.helium.12months_eft",
        "g1.helium.1month_eft",
        "g1.helium.3months_ip_83p.acq",
        "g1.2tb",
        "g1.2tb.annual",
        "g1.2tb.subs_variant",
        "g1.2tb.3months_ip_85p.acq",
        "g1.2tb.12months_eft",
        "g1.2tb.1month_eft",
        "g1.5tb",
        "g1.5tb.annual",
        "g1.5tb.subs_variant",
        "g1.5tb.3months_ip_75p.acq",
        "g1.5tb.12months_eft",
        "g1.5tb.1month_eft",
        "g1.10tb",
        "g1.10tb.annual",
        "g1.10tb.1month_eft",
        "g1.play.30gb",
        "g1.play.100gb",
        "g1.play.200gb",
        "g1.play.2tb",
        "g1.play.5tb",
        "g1.play.10tb",
        "g1.play.helium",
    )

    val addonSkus: List<String> = listOf(
        "g1.youtube_premium_individual_add_on",
        "g1.youtube_premium_family_add_on",
        "g1.home_premium",
        "g1.home_premium_advanced",
        "g1.health_premium",
        "g1.play.home",
        "g1.play.health",
        "g1.nest_aware",
        "g1.nest_aware_plus",
        "g1.fitbit_premium",
        "g1.play_pass",
        "g1.play.pass",
        "g1.2tb.play_pass",
    )

    val defaultSkus: List<String> = ultraSkus + aiProPlusSkus + storageSkus + addonSkus

    private val ultraSet: Set<String> = ultraSkus.toSet()
    private val aiSet: Set<String> = aiProPlusSkus.toSet()
    private val storageSet: Set<String> = storageSkus.toSet()
    private val addonSet: Set<String> = addonSkus.toSet()
    private val defaultSkuSet: Set<String> = defaultSkus.toSet()

    fun ultraTierOf(sku: String, title: String = ""): UltraTier {
        val lowSku = sku.lowercase()
        val lowTitle = title.lowercase()
        val is30TbPool = "30tb" in lowSku || "30 tb" in lowTitle || "20x" in lowTitle || lowSku.startsWith("g1.play.ai.pro")
        if (is30TbPool) return UltraTier.ULTRA_20X_30TB
        val is20TbPool = "20tb" in lowSku || "20 tb" in lowTitle || "5x" in lowTitle
        if (is20TbPool) return UltraTier.ULTRA_5X_20TB
        if (sku in ultraSet || "ultra" in lowSku || "ultra" in lowTitle) {
            return UltraTier.ULTRA_GENERIC
        }
        return UltraTier.NONE
    }

    fun categoryOf(sku: String, title: String = ""): SkuCategory {
        if (ultraTierOf(sku, title) != UltraTier.NONE) {
            return SkuCategory.ULTRA
        }
        val lowSku = sku.lowercase()
        val lowTitle = title.lowercase()
        if (sku in aiSet || ".ai" in lowSku || "ai_" in lowSku || "ai pro" in lowTitle || "ai plus" in lowTitle) {
            return SkuCategory.AI_PRO_PLUS
        }
        if (sku in addonSet || "youtube" in lowSku || "nest" in lowSku || "fitbit" in lowSku || "play_pass" in lowSku || "home" in lowSku || "health" in lowSku) {
            return SkuCategory.ADDON
        }
        if (sku in storageSet) {
            return SkuCategory.STORAGE
        }
        return SkuCategory.DYNAMIC
    }

    fun recordObserved(candidates: Collection<*>?): Int {
        if (candidates.isNullOrEmpty()) return 0
        var added = 0
        for (item in candidates) {
            val raw = (item as? String)?.trim() ?: continue
            val sku = normalizeCandidate(raw) ?: continue
            if (sku !in defaultSkuSet && observed.add(sku)) {
                added += 1
            }
        }
        return added
    }

    fun recordObservedDetailsJson(jsonItems: Collection<*>?): Int {
        if (jsonItems.isNullOrEmpty()) return 0
        var added = 0
        for (item in jsonItems) {
            val json = item as? String ?: continue
            for (match in jsonProductIdRegex.findAll(json)) {
                val sku = match.groupValues[1]
                if (sku !in defaultSkuSet && observed.add(sku)) {
                    added += 1
                }
            }
        }
        return added
    }

    fun addCustom(rawInput: String): List<String> {
        val added = mutableListOf<String>()
        for (token in rawInput.split(',', ';', ' ', '\n', '\t')) {
            val sku = normalizeCandidate(token.trim()) ?: continue
            if (sku !in defaultSkuSet) {
                custom.add(sku)
            }
            added.add(sku)
        }
        return added
    }

    fun observedCount(): Int = observed.size

    fun allSkus(): List<String> {
        val merged = LinkedHashSet<String>(defaultSkus.size + observed.size + custom.size)
        merged.addAll(observed.sorted())
        merged.addAll(custom.sorted())
        merged.addAll(defaultSkus)
        return merged.toList()
    }

    fun chunkedSkus(batchSize: Int = BATCH_SIZE): List<List<String>> = allSkus().chunked(batchSize.coerceAtLeast(1))

    private fun normalizeCandidate(raw: String): String? {
        if (raw.isEmpty()) return null
        val stripped = if (raw.startsWith(docIdPrefix)) raw.removePrefix(docIdPrefix) else raw
        return stripped.takeIf { skuPattern.matches(it) }
    }

    internal fun clearDynamicForTest() {
        observed.clear()
        custom.clear()
    }
}
