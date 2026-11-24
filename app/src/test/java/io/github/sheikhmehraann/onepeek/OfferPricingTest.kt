package io.github.sheikhmehraann.onepeek

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferPricingTest {
    @Test
    fun parsesStandardIsoPeriods() {
        assertEquals(7, OfferPricing.parsePeriodDays("P7D"))
        assertEquals(14, OfferPricing.parsePeriodDays("P2W"))
        assertEquals(30, OfferPricing.parsePeriodDays("P1M"))
        assertEquals(90, OfferPricing.parsePeriodDays("P3M"))
        assertEquals(180, OfferPricing.parsePeriodDays("P6M"))
        assertEquals(360, OfferPricing.parsePeriodDays("P12M"))
        assertEquals(365, OfferPricing.parsePeriodDays("P1Y"))
    }

    @Test
    fun rejectsMalformedOrZeroPeriods() {
        assertEquals(0, OfferPricing.parsePeriodDays(""))
        assertEquals(0, OfferPricing.parsePeriodDays("P0M"))
        assertEquals(0, OfferPricing.parsePeriodDays("P1H"))
        assertEquals(0, OfferPricing.parsePeriodDays("invalid"))
    }

    @Test
    fun detectsFreeIntroOnlyOnFirstPhase() {
        val freeIntro = listOf(
            OfferPhase(0L, "$0.00", "P1Y", 1),
            OfferPhase(99_990_000L, "$99.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE),
        )
        val paidOnly = listOf(
            OfferPhase(19_990_000L, "$19.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE),
        )
        val delayedFree = listOf(
            OfferPhase(1_990_000L, "$1.99", "P1M", 1),
            OfferPhase(0L, "$0.00", "P1M", 1),
        )

        assertTrue(OfferPricing.hasFreeIntro(freeIntro))
        assertFalse(OfferPricing.hasFreeIntro(paidOnly))
        assertFalse(OfferPricing.hasFreeIntro(delayedFree))
        assertFalse(OfferPricing.hasFreeIntro(emptyList()))
    }

    @Test
    fun sumsConsecutiveLeadingFreePhasesAndMultiCycles() {
        val singleYear = listOf(
            OfferPhase(0L, "$0.00", "P1Y", 1),
            OfferPhase(99_990_000L, "$99.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE),
        )
        val threeMonthlyFree = listOf(
            OfferPhase(0L, "$0.00", "P1M", 3),
            OfferPhase(19_990_000L, "$19.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE),
        )
        val chainedFreeIntro = listOf(
            OfferPhase(0L, "$0.00", "P14D", 1, OfferPhase.RECURRENCE_NON_RECURRING),
            OfferPhase(0L, "$0.00", "P1M", 2, OfferPhase.RECURRENCE_FINITE),
            OfferPhase(19_990_000L, "$19.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE),
        )

        assertEquals(365, OfferPricing.freeIntroDays(singleYear))
        assertEquals(90, OfferPricing.freeIntroDays(threeMonthlyFree))
        assertEquals(74, OfferPricing.freeIntroDays(chainedFreeIntro))
        assertTrue(OfferPricing.freeIntroDays(singleYear) > OfferPricing.freeIntroDays(threeMonthlyFree))
    }
}
