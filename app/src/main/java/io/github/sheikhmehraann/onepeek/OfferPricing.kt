package io.github.sheikhmehraann.onepeek

import kotlin.math.roundToInt

internal enum class OfferKind {
    FREE_TRIAL,
    INTRO_PROMO,
    BASE_PLAN,
}

internal data class OfferPhase(
    val amountMicros: Long,
    val formattedPrice: String,
    val billingPeriod: String,
    val billingCycles: Int,
    val recurrenceMode: Int = RECURRENCE_FINITE,
) {
    companion object {
        const val RECURRENCE_INFINITE = 1
        const val RECURRENCE_FINITE = 2
        const val RECURRENCE_NON_RECURRING = 3
    }
}

internal object OfferPricing {
    private val periodRegex = Regex("^P(\\d+)([DWMY])$")
    private val eftRegex = Regex(".*eft.*", RegexOption.IGNORE_CASE)

    fun hasFreeIntro(phases: List<OfferPhase>): Boolean = phases.firstOrNull()?.amountMicros == 0L

    fun classify(phases: List<OfferPhase>): OfferKind {
        if (phases.isEmpty()) return OfferKind.BASE_PLAN
        if (hasFreeIntro(phases)) return OfferKind.FREE_TRIAL
        if (phases.size >= 2 && introDiscountPercent(phases) > 0) return OfferKind.INTRO_PROMO
        return OfferKind.BASE_PLAN
    }

    fun isEft(offerId: String?, basePlanId: String, offerTags: List<String> = emptyList()): Boolean {
        if (offerId != null && eftRegex.matches(offerId)) return true
        if (eftRegex.matches(basePlanId)) return true
        return offerTags.any { eftRegex.matches(it) }
    }

    fun introDiscountPercent(phases: List<OfferPhase>): Int {
        if (phases.size < 2) return 0
        val intro = phases.first()
        val renewal = phases.last()
        if (intro.amountMicros <= 0L || renewal.amountMicros <= 0L) return 0
        val introDays = parsePeriodDays(intro.billingPeriod)
        val renewalDays = parsePeriodDays(renewal.billingPeriod)
        if (introDays <= 0 || renewalDays <= 0) return 0
        val introDaily = intro.amountMicros.toDouble() / introDays
        val renewalDaily = renewal.amountMicros.toDouble() / renewalDays
        if (introDaily >= renewalDaily) return 0
        return (((renewalDaily - introDaily) / renewalDaily) * 100.0).roundToInt().coerceIn(1, 99)
    }

    fun freeIntroDays(phases: List<OfferPhase>): Int {
        var totalDays = 0
        for (phase in phases) {
            if (phase.amountMicros != 0L) break
            val unitDays = parsePeriodDays(phase.billingPeriod)
            if (unitDays <= 0) continue
            val cycles = if (phase.billingCycles > 0) phase.billingCycles else 1
            totalDays += unitDays * cycles
        }
        return totalDays
    }

    fun parsePeriodDays(period: String): Int {
        val match = periodRegex.matchEntire(period.trim()) ?: return 0
        val count = match.groupValues[1].toIntOrNull() ?: return 0
        if (count <= 0) return 0
        val factor = when (match.groupValues[2]) {
            "D" -> 1
            "W" -> 7
            "M" -> 30
            "Y" -> 365
            else -> 0
        }
        return count * factor
    }

    fun formatFreeIntroLabel(phases: List<OfferPhase>, eft: Boolean = false): String {
        if (!hasFreeIntro(phases)) return ""
        val parts = mutableListOf<String>()
        for (phase in phases) {
            if (phase.amountMicros != 0L) break
            parts.add(formatScaledPeriod(phase.billingPeriod, phase.billingCycles))
        }
        val base = if (parts.isEmpty()) "Free" else "Free ${parts.joinToString("+")}"
        return if (eft) "$base (EFT)" else base
    }

    fun formatCompactPrice(phases: List<OfferPhase>, eft: Boolean = false): String {
        if (phases.isEmpty()) return "Unknown"
        return when (classify(phases)) {
            OfferKind.FREE_TRIAL -> {
                val intro = formatFreeIntroLabel(phases, eft)
                val renewal = phases.firstOrNull { it.amountMicros > 0L }
                if (renewal != null) "$intro -> ${renewal.formattedPrice}/${renewal.billingPeriod}" else intro
            }

            OfferKind.INTRO_PROMO -> {
                val first = phases.first()
                val renewal = phases.last()
                val pct = introDiscountPercent(phases)
                val span = formatScaledPeriod(first.billingPeriod, first.billingCycles)
                "Intro -$pct% $span (${first.formattedPrice}) -> ${renewal.formattedPrice}/${renewal.billingPeriod}"
            }

            OfferKind.BASE_PLAN -> {
                val first = phases.first()
                "${first.formattedPrice}/${first.billingPeriod}"
            }
        }
    }

    private fun formatScaledPeriod(period: String, cycles: Int): String {
        val match = periodRegex.matchEntire(period.trim()) ?: return period
        val count = match.groupValues[1].toIntOrNull() ?: return period
        val unit = match.groupValues[2]
        val total = count * (if (cycles > 0) cycles else 1)
        return "$total$unit"
    }
}
