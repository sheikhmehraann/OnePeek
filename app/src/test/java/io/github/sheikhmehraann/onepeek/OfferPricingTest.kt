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
    fun classifiesFreeTrialsIntroPromosAndBasePlans() {
        val ultraFree = listOf(
            OfferPhase(0L, "$0.00", "P1Y", 1),
            OfferPhase(99_990_000L, "$99.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE),
        )
        val ultraHalfOff = listOf(
            OfferPhase(124_990_000L, "$124.99", "P1M", 3),
            OfferPhase(249_990_000L, "$249.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE),
        )
        val paidOnly = listOf(
            OfferPhase(19_990_000L, "$19.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE),
        )

        assertEquals(OfferKind.FREE_TRIAL, OfferPricing.classify(ultraFree))
        assertEquals(OfferKind.INTRO_PROMO, OfferPricing.classify(ultraHalfOff))
        assertEquals(OfferKind.BASE_PLAN, OfferPricing.classify(paidOnly))
        assertEquals(50, OfferPricing.introDiscountPercent(ultraHalfOff))
        assertEquals(0, OfferPricing.introDiscountPercent(paidOnly))
    }

    @Test
    fun detectsEftAcrossOfferIdBasePlanAndTags() {
        assertTrue(OfferPricing.isEft("eft-12month", "monthly"))
        assertTrue(OfferPricing.isEft("12months-eft", "monthly"))
        assertTrue(OfferPricing.isEft(null, "annual-eft-plan"))
        assertTrue(OfferPricing.isEft("promo-2026", "monthly", listOf("g1-eft-eligible")))
        assertFalse(OfferPricing.isEft("intro-50p", "monthly", listOf("standard")))
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

    @Test
    fun formatsCompactRowSummaryForTrialsPromosAndPaidOffers() {
        val ultraEft = listOf(
            OfferPhase(0L, "$0.00", "P1Y", 1),
            OfferPhase(99_990_000L, "$99.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE),
        )
        val ultraPromo = listOf(
            OfferPhase(124_990_000L, "$124.99", "P1M", 3),
            OfferPhase(249_990_000L, "$249.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE),
        )
        val chainedFreeToIntroToBase = listOf(
            OfferPhase(0L, "$0.00", "P1M", 1),
            OfferPhase(49_990_000L, "$49.99", "P1M", 3),
            OfferPhase(99_990_000L, "$99.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE),
        )
        val paidOnly = listOf(
            OfferPhase(9_990_000L, "$9.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE),
        )

        assertEquals("Free 1Y (EFT) -> $99.99/P1M", OfferPricing.formatCompactPrice(ultraEft, eft = true))
        assertEquals("Free 1Y -> $99.99/P1M", OfferPricing.formatCompactPrice(ultraEft, eft = false))
        assertEquals("Free 1M -> $49.99/P1M -> $99.99/P1M", OfferPricing.formatCompactPrice(chainedFreeToIntroToBase))
        assertEquals("Intro -50% 3M ($124.99) -> $249.99/P1M", OfferPricing.formatCompactPrice(ultraPromo))
        assertEquals("$9.99/P1M", OfferPricing.formatCompactPrice(paidOnly))
    }

    @Test
    fun parsesRawDocIdAndInstallmentPlanFromProductDetailsString() {
        val fakeToString = "ProductDetails{jsonString='{\"productId\":\"g1.play.20tb\",\"serializedDocid\":\"subs:com.google.android.apps.subscriptions.red:g1.play.20tb\",\"subscriptionOfferDetails\":[{\"offerIdToken\":\"tok/base\",\"basePlanId\":\"monthly\"},{\"offerIdToken\":\"tok/ultra/12m\",\"offerId\":\"eft-12month\",\"installmentPlanDetails\":{\"commitmentPaymentsCount\":12,\"subsequentCommitmentPaymentsCount\":1}}]}', parsedJson={}}"

        val baseMeta = OfferPricing.parseRawOfferMeta(fakeToString, "tok/base")
        assertEquals("subs:com.google.android.apps.subscriptions.red:g1.play.20tb", baseMeta.serializedDocId)
        assertFalse(baseMeta.hasInstallmentPlan)

        val ultraMeta = OfferPricing.parseRawOfferMeta(fakeToString, "tok/ultra/12m")
        assertEquals("subs:com.google.android.apps.subscriptions.red:g1.play.20tb", ultraMeta.serializedDocId)
        assertTrue(ultraMeta.hasInstallmentPlan)
        assertEquals(12, ultraMeta.commitmentPayments)
        assertEquals(1, ultraMeta.subsequentCommitmentPayments)
    }

    @Test
    fun detectsBillingCurrencyFromPhases() {
        val phases = listOf(
            OfferPhase(0L, "$0.00", "P1Y", 1, currencyCode = ""),
            OfferPhase(99_990_000L, "$99.99", "P1M", 0, OfferPhase.RECURRENCE_INFINITE, currencyCode = "USD"),
        )
        assertEquals("USD", OfferPricing.detectCurrency(phases))
        assertEquals("", OfferPricing.detectCurrency(emptyList()))
    }
}
