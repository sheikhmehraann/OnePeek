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
    private val periodRegex = Regex("^P(\\d+)([DWMY])$")

    fun hasFreeIntro(phases: List<OfferPhase>): Boolean = phases.firstOrNull()?.amountMicros == 0L

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

    fun formatFreeIntroLabel(phases: List<OfferPhase>): String {
        if (!hasFreeIntro(phases)) return ""
        val parts = mutableListOf<String>()
        for (phase in phases) {
            if (phase.amountMicros != 0L) break
            parts.add(formatScaledPeriod(phase.billingPeriod, phase.billingCycles))
        }
        return if (parts.isEmpty()) "Free" else "Free ${parts.joinToString("+")}"
    }

    fun formatCompactPrice(phases: List<OfferPhase>): String {
        if (phases.isEmpty()) return "Unknown"
        if (!hasFreeIntro(phases)) {
            val first = phases.first()
            return "${first.formattedPrice}/${first.billingPeriod}"
        }
        val intro = formatFreeIntroLabel(phases)
        val renewal = phases.firstOrNull { it.amountMicros > 0L }
        return if (renewal != null) {
            "$intro -> ${renewal.formattedPrice}/${renewal.billingPeriod}"
        } else {
            intro
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
