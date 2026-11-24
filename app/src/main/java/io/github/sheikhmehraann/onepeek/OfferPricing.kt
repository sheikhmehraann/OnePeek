package io.github.sheikhmehraann.onepeek

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
    private val PERIOD_REGEX = Regex("^P(\\d+)([DWMY])$")

    fun hasFreeIntro(phases: List<OfferPhase>): Boolean =
        phases.firstOrNull()?.amountMicros == 0L

    fun freeIntroDays(phases: List<OfferPhase>): Int {
        var totalDays = 0
        for (phase in phases) {
            if (phase.amountMicros != 0L) break
            val unitDays = parsePeriodDays(phase.billingPeriod)
            if (unitDays <= 0) continue
            val cycles = when {
                phase.billingCycles > 0 -> phase.billingCycles
                phase.recurrenceMode == OfferPhase.RECURRENCE_NON_RECURRING -> 1
                else -> 1
            }
            totalDays += unitDays * cycles
        }
        return totalDays
    }

    fun parsePeriodDays(period: String): Int {
        val match = PERIOD_REGEX.matchEntire(period.trim()) ?: return 0
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
}
