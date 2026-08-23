package com.naeirae.fincontrol.domain

data class DecisionScenarioInput(
    val freeNow: MoneyAmount,
    val keepLiquid: MoneyAmount,
    val candidates: List<ActionCandidate>,
)

data class DecisionScenarioResult(
    val distributableNow: MoneyAmount,
    val ranked: List<RankedAction>,
)

object DecisionScenarioEngine {
    fun evaluate(input: DecisionScenarioInput): DecisionScenarioResult {
        require(input.freeNow.currency == input.keepLiquid.currency)
        val distributable = MoneyAmount(
            amount = (input.freeNow.amount - input.keepLiquid.amount).coerceAtLeast(0.0),
            currency = input.freeNow.currency,
        )
        return DecisionScenarioResult(
            distributableNow = distributable,
            ranked = ActionRanking.rank(distributable, input.candidates),
        )
    }
}
