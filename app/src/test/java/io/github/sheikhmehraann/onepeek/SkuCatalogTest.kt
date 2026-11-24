package io.github.sheikhmehraann.onepeek

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkuCatalogTest {
    @After
    fun tearDown() {
        SkuCatalog.clearDynamicForTest()
    }

    @Test
    fun defaultCatalogHasUniqueEntriesAndChunksUnderBillingLimit() {
        assertEquals(SkuCatalog.defaultSkus.size, SkuCatalog.defaultSkus.toSet().size)
        assertTrue(SkuCatalog.ultraSkus.contains("g1.play.20tb"))
        assertTrue(SkuCatalog.ultraSkus.contains("g1.play.ai.pro"))
        assertTrue(SkuCatalog.ultraSkus.contains("g1.30tb"))
        assertTrue(SkuCatalog.ultraSkus.contains("g1.30tb.annual"))
        assertTrue(SkuCatalog.aiProPlusSkus.contains("g1.2tb.ai.subs_variant"))
        assertTrue(SkuCatalog.aiProPlusSkus.contains("g1.2tb.ai.annual"))
        assertTrue(SkuCatalog.storageSkus.contains("g1.100gb"))
        assertTrue(SkuCatalog.storageSkus.contains("g1.100gb.annual"))
        val chunks = SkuCatalog.chunkedSkus()
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.all { it.size in 1..SkuCatalog.BATCH_SIZE })
        assertTrue(SkuCatalog.BATCH_SIZE <= 20)
    }

    @Test
    fun classifiesUltra5xUltra20xAiStorageAddonAndLiveSkus() {
        assertEquals(UltraTier.ULTRA_5X_20TB, SkuCatalog.ultraTierOf("g1.play.20tb"))
        assertEquals(UltraTier.ULTRA_20X_30TB, SkuCatalog.ultraTierOf("g1.play.ai.pro"))
        assertEquals(UltraTier.ULTRA_20X_30TB, SkuCatalog.ultraTierOf("g1.30tb"))
        assertEquals(UltraTier.ULTRA_20X_30TB, SkuCatalog.ultraTierOf("g1.future.plan", "Google AI Ultra (30 TB)"))
        assertEquals(SkuCategory.ULTRA, SkuCatalog.categoryOf("g1.play.20tb"))
        assertEquals(SkuCategory.ULTRA, SkuCatalog.categoryOf("g1.play.ai.pro"))
        assertEquals(SkuCategory.AI_PRO_PLUS, SkuCatalog.categoryOf("g1.2tb.ai.subs_variant"))
        assertEquals(SkuCategory.STORAGE, SkuCatalog.categoryOf("g1.100gb"))
        assertEquals(SkuCategory.ADDON, SkuCatalog.categoryOf("g1.youtube_premium_individual_add_on"))
        assertEquals(SkuCategory.DYNAMIC, SkuCatalog.categoryOf("g1.unknown.regional"))
    }

    @Test
    fun capturesLiveOutboundDocIdsAndInboundDetailsJsonWithoutDuplicatingDefaults() {
        val outAdded = SkuCatalog.recordObserved(
            listOf(
                "g1.100gb",
                "subs:com.google.android.apps.subscriptions.red:g1.black_friday.2026",
                "bad sku!",
                "",
            ),
        )
        assertEquals(1, outAdded)

        val inAdded = SkuCatalog.recordObservedDetailsJson(
            listOf(
                "{\"productId\":\"g1.early_access.ultra\",\"type\":\"subs\",\"transitionPlanDetails\":{\"productId\":\"g1.ultra.transition\"}}",
                "{\"productId\":\"g1.play.20tb\",\"type\":\"subs\"}",
            ),
        )
        assertEquals(2, inAdded)
        assertEquals(3, SkuCatalog.observedCount())
        assertTrue(
            SkuCatalog.allSkus().take(3).containsAll(
                listOf("g1.black_friday.2026", "g1.early_access.ultra", "g1.ultra.transition"),
            ),
        )
    }

    @Test
    fun parsesCustomSkuListIncludingSerializedDocIds() {
        val parsed = SkuCatalog.addCustom(
            "g1.custom.tier1, subs:com.google.android.apps.subscriptions.red:g1.custom.tier2\ng1.play.20tb",
        )
        assertEquals(listOf("g1.custom.tier1", "g1.custom.tier2", "g1.play.20tb"), parsed)
        assertTrue(SkuCatalog.allSkus().contains("g1.custom.tier1"))
        assertTrue(SkuCatalog.allSkus().contains("g1.custom.tier2"))
    }
}
