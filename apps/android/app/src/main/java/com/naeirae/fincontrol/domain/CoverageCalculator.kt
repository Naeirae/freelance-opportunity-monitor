package com.naeirae.fincontrol.domain

data class CoverageSource(
    val sourceId: String,
    val title: String,
    val amount: MoneyAmount,
    val probabilityPercent: Int = 100,
    val hardReserved: Boolean = false,
)

data class CoverageResult(
    val obligationId: String,
    val target: MoneyAmount,
    val covered: MoneyAmount,
    val expectedCovered: MoneyAmount,
    val deficit: MoneyAmount,
    val links: List<CoverageLink>,
)

object CoverageCalculator {
    fun calculate(
        obligation: Obligation,
        sources: List<CoverageSource>,
    ): CoverageResult {
        val target = obligation.nextPayment ?: obligation.balance
        require(sources.all { it.amount.currency == target.currency }) {
            "Coverage sources must use the same currency as the target"
        }

        var remaining = target.amount
        val links = mutableListOf<CoverageLink>()
        var covered = 0.0
        var expectedCovered = 0.0

        sources
            .filterNot { it.hardReserved }
            .sortedByDescending { it.probabilityPercent }
            .forEach { source ->
                if (remaining <= 0.0) return@forEach
                val allocated = minOf(source.amount.amount, remaining)
                if (allocated <= 0.0) return@forEach

                links += CoverageLink(
                    obligationId = obligation.id,
                    sourceId = source.sourceId,
                    amount = MoneyAmount(allocated, target.currency),
                )
                covered += allocated
                expectedCovered += allocated * (source.probabilityPercent.coerceIn(0, 100) / 100.0)
                remaining -= allocated
            }

        return CoverageResult(
            obligationId = obligation.id,
            target = target,
            covered = MoneyAmount(covered, target.currency),
            expectedCovered = MoneyAmount(expectedCovered, target.currency),
            deficit = MoneyAmount(maxOf(0.0, target.amount - covered), target.currency),
            links = links,
        )
    }
}
