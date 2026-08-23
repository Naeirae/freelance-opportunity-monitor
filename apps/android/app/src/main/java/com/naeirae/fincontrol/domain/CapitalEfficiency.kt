package com.naeirae.fincontrol.domain

data class CapitalEfficiencyMetrics(
    val capitalRequired: MoneyAmount,
    val grossExpectedReturn: MoneyAmount,
    val expectedProfit: MoneyAmount,
    val probabilityWeightedProfit: MoneyAmount,
    val roiPercent: Double?,
    val probabilityWeightedRoiPercent: Double?,
    val paybackDays: Double?,
    val profitPerLockedDay: Double?,
)

object CapitalEfficiency {
    fun evaluate(item: FinancialObject): CapitalEfficiencyMetrics? {
        val capital = item.capitalRequired ?: return null
        if (capital.amount <= 0.0) return null

        val gross = item.expectedGain?.takeIf { it.currency == capital.currency }
            ?: item.amount?.takeIf {
                it.currency == capital.currency &&
                    item.type in setOf(
                        FinancialObjectType.OPPORTUNITY,
                        FinancialObjectType.INCOME_SOURCE,
                        FinancialObjectType.ASSET,
                        FinancialObjectType.CLAIM,
                    )
            }
            ?: return null

        val probability = (item.probabilityPercent ?: 100).coerceIn(0, 100) / 100.0
        val expectedProfitAmount = gross.amount - capital.amount
        val weightedProfitAmount = gross.amount * probability - capital.amount
        val roi = expectedProfitAmount / capital.amount * 100.0
        val weightedRoi = weightedProfitAmount / capital.amount * 100.0

        val lockDays = item.liquidityLockDays?.takeIf { it > 0 }
        val paybackDays = lockDays?.let { days ->
            if (gross.amount <= 0.0) null else days * (capital.amount / gross.amount)
        }
        val profitPerLockedDay = lockDays?.let { days -> weightedProfitAmount / days }

        return CapitalEfficiencyMetrics(
            capitalRequired = capital,
            grossExpectedReturn = gross,
            expectedProfit = MoneyAmount(expectedProfitAmount, capital.currency),
            probabilityWeightedProfit = MoneyAmount(weightedProfitAmount, capital.currency),
            roiPercent = roi,
            probabilityWeightedRoiPercent = weightedRoi,
            paybackDays = paybackDays,
            profitPerLockedDay = profitPerLockedDay,
        )
    }
}
