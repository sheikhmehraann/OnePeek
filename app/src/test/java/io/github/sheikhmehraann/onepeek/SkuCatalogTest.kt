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
        assertTrue(SkuCatalog.aiProPlusSkus.contains("g1.2tb.ai.subs_variant"))
        assertTrue(SkuCatalog.storageSkus.contains("g1.100gb"))
        val chunks = SkuCatalog.chunkedSkus()
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.all { it.size in 1..SkuCatalog.BATCH_SIZE })
        assertTrue(SkuCatalog.BATCH_SIZE <= 20)
    }

    @Test
    fun classifiesUltraAiStorageAddonAndLiveSkus() {
        assertEquals(SkuCategory.ULTRA, SkuCatalog.categoryOf("g1.play.20tb"))
        assertEquals(SkuCategory.ULTRA, SkuCatalog.categoryOf("g1.play.ai.pro"))
        assertEquals(SkuCategory.ULTRA, SkuCatalog.categoryOf("g1.future.plan", "Google AI Ultra (30 TB)"))
        assertEquals(SkuCategory.AI_PRO_PLUS, SkuCatalog.categoryOf("g1.2tb.ai.subs_variant"))
        assertEquals(SkuCategory.STORAGE, SkuCatalog.categoryOf("g1.100gb"))
        assertEquals(SkuCategory.ADDON, SkuCatalog.categoryOf("g1.youtube_premium_individual_add_on"))
        assertEquals(SkuCategory.DYNAMIC, SkuCatalog.categoryOf("g1.unknown.regional"))
    }

    @Test
    fun capturesLiveObservedSkusWithoutDuplicatingDefaults() {
        val added = SkuCatalog.recordObserved(listOf("g1.100gb", "g1.black_friday.2026", "bad sku!", ""))
        assertEquals(1, added)
        assertEquals(1, SkuCatalog.observedCount())
        assertEquals("g1.black_friday.2026", SkuCatalog.allSkus().first())
    }

    @Test
    fun parsesCustomSkuList() {
        val parsed = SkuCatalog.addCustom("g1.custom.tier1, g1.custom.tier2\ng1.play.20tb")
        assertEquals(listOf("g1.custom.tier1", "g1.custom.tier2", "g1.play.20tb"), parsed)
        assertTrue(SkuCatalog.allSkus().contains("g1.custom.tier1"))
        assertTrue(SkuCatalog.allSkus().contains("g1.custom.tier2"))
    }
}
