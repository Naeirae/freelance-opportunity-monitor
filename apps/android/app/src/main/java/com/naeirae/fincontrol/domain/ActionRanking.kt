package com.naeirae.fincontrol.domain

data class ActionCandidate(
    val id: String,
    val title: String,
    val required: MoneyAmount,
    val guaranteedBenefit: MoneyAmount? = null,
    val expectedBenefit: MoneyAmount? = null,
    val liquidityCost: MoneyAmount,
    val riskPenalty: Double = 0.0,
    val urgencyBonus: Double = 0.0,
    val notes: String? = null,
)

data class RankedAction(
    val candidate: ActionCandidate,
    val score: Double,
    val feasible: Boolean,
    val reason: String,
)

object ActionRanking {
    fun rank(
        allocatable: MoneyAmount,
        candidates: List<ActionCandidate>,
    ): List<RankedAction> = candidates.map { candidate ->
        require(candidate.required.currency == allocatable.currency) {
            "Action and allocatable money must use the same currency"
        }

        val feasible = candidate.required.amount <= allocatable.amount
        val guaranteed = candidate.guaranteedBenefit
            ?.takeIf { it.currency == allocatable.currency }
            ?.amount ?: 0.0
        val expected = candidate.expectedBenefit
            ?.takeIf { it.currency == allocatable.currency }
            ?.amount ?: 0.0

        val rawReturn = guaranteed + expected
        val denominator = candidate.required.amount.coerceAtLeast(1.0)
        val score = if (feasible) {
            (rawReturn / denominator) * 100.0 + candidate.urgencyBonus - candidate.riskPenalty
        } else {
            Double.NEGATIVE_INFINITY
        }

        RankedAction(
            candidate = candidate,
            score = score,
            feasible = feasible,
            reason = if (feasible) {
                "Вписывается в свободную сумму; сравниваем выгоду, срочность и риск"
            } else {
                "Требует больше свободных денег, чем доступно к распределению"
            },
        )
    }.sortedByDescending { it.score }
}
