package io.github.sheikhmehraann.onepeek

import java.util.concurrent.ConcurrentHashMap

internal object SkuCatalog {
    const val BATCH_SIZE = 18

    private val skuPattern = Regex("^[a-zA-Z0-9][a-zA-Z0-9._-]{2,79}$")
    private val observed = ConcurrentHashMap.newKeySet<String>()
    private val custom = ConcurrentHashMap.newKeySet<String>()

    val defaultSkus: List<String> = listOf(
        // Storage tiers (g1.100gb verified in Google One 1.336 base DEX)
        "g1.30gb",
        "g1.30gb.1month_eft",
        "g1.100gb",
        "g1.100gb.3months_ip_83p.acq",
        "g1.200gb",
        "g1.200gb.3months_ip_83p.acq",
        "g1.helium",
        "g1.helium.3months_ip_83p.acq",
        "g1.2tb",
        "g1.2tb.3months_ip_85p.acq",
        "g1.2tb.12months_eft",
        "g1.2tb.1month_eft",
        "g1.5tb",
        "g1.5tb.3months_ip_75p.acq",
        "g1.10tb",
        "g1.20tb",
        "g1.20tb.12months_eft",
        "g1.30tb",
        "g1.30tb.12months_eft",
        "g1.30tb.1month_eft",
        // AI tiers and Play trial catalog variants
        "g1.play.20tb",
        "g1.play.20tb.12month",
        "g1.play.30tb",
        "g1.play.ai.pro",
        "g1.play.ai.pro.12month",
        "g1.play.ai.plus",
        "g1.play.ai.ultra",
        "g1.play.ai",
        "g1.play.2tb",
        "g1.play.5tb",
        "g1.play.helium",
        "g1.play.pro",
        "g1.play.plus",
        "g1.play",
        "g1.2tb.ai.subs_variant",
        "g1.2tb.ai",
        "g1.5tb.ai",
        "g1.20tb.ai",
        "g1.30tb.ai",
        "g1.ai",
        "g1.ai.pro",
        "g1.ai.plus",
        "g1.ai.ultra",
        "g1.ultra",
        "g1.ultra.20tb",
        "g1.ultra.30tb",
        "g1.pro",
        "g1.plus",
        "g1.premium",
        // Bundled benefits and add-ons
        "g1.youtube_premium_individual_add_on",
        "g1.youtube_premium_family_add_on",
        "g1.home_premium",
        "g1.health_premium",
        "g1.play.home",
        "g1.play.health",
        "g1.nest_aware",
        "g1.nest_aware_plus",
        "g1.fitbit_premium",
        "g1.play_pass",
    )

    private val defaultSkuSet: Set<String> = defaultSkus.toSet()

    fun recordObserved(candidates: Collection<*>?): Int {
        if (candidates.isNullOrEmpty()) return 0
        var added = 0
        for (item in candidates) {
            val sku = (item as? String)?.trim() ?: continue
            if (!skuPattern.matches(sku)) continue
            if (sku !in defaultSkuSet && observed.add(sku)) {
                added += 1
            }
        }
        return added
    }

    fun addCustom(rawInput: String): List<String> {
        val added = mutableListOf<String>()
        for (token in rawInput.split(',', ';', ' ', '\n', '\t')) {
            val sku = token.trim()
            if (sku.isEmpty() || !skuPattern.matches(sku)) continue
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

    internal fun clearDynamicForTest() {
        observed.clear()
        custom.clear()
    }
}
