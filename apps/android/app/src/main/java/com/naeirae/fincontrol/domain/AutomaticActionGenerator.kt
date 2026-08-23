package com.naeirae.fincontrol.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ActionGenerationContext(
    val currency: CurrencyCode,
    val today: LocalDate = LocalDate.now(),
)

object AutomaticActionGenerator {
    fun generate(
        objects: List<FinancialObject>,
        context: ActionGenerationContext,
    ): List<ActionCandidate> {
        val active = objects.filter { it.status != ObjectStatus.ARCHIVED && it.status != ObjectStatus.COMPLETED }
        val result = mutableListOf<ActionCandidate>()

        result += ActionCandidate(
            id = "system-keep-liquid-${context.currency}",
            title = "Оставить часть денег ликвидной",
            required = MoneyAmount(0.0, context.currency),
            liquidityCost = MoneyAmount(0.0, context.currency),
            expectedBenefit = MoneyAmount(0.0, context.currency),
            urgencyBonus = 10.0,
            notes = "Базовый сценарий: не направлять деньги автоматически в долг или инвестицию, если впереди обязательные расходы или высокая неопределённость.",
        )

        active.forEach { item ->
            when (item.type) {
                FinancialObjectType.OBLIGATION -> obligationCandidate(item, context)?.let(result::add)
                FinancialObjectType.OPPORTUNITY,
                FinancialObjectType.ASSET,
                FinancialObjectType.INCOME_SOURCE -> growthCandidate(item, context)?.let(result::add)
                FinancialObjectType.CLAIM -> claimCandidate(item, context)?.let(result::add)
                else -> Unit
            }
        }

        return result.distinctBy { it.id }
    }

    private fun obligationCandidate(item: FinancialObject, context: ActionGenerationContext): ActionCandidate? {
        val balance = item.amount?.takeIf { it.currency == context.currency } ?: return null
        val minimum = item.capitalRequired?.takeIf { it.currency == context.currency }?.amount ?: balance.amount
        val required = minimum.coerceAtMost(balance.amount).coerceAtLeast(0.0)
        if (required <= 0.0) return null

        val rateSaving = item.annualRatePercent?.takeIf { it > 0.0 }?.let { rate ->
            required * (rate / 100.0) / 12.0
        }
        val guaranteed = item.guaranteedSaving
            ?.takeIf { it.currency == context.currency }
            ?: rateSaving?.let { MoneyAmount(it, context.currency) }

        return ActionCandidate(
            id = "pay-${item.id}",
            title = "Уменьшить обязательство: ${item.title}",
            required = MoneyAmount(required, context.currency),
            guaranteedBenefit = guaranteed,
            expectedBenefit = null,
            liquidityCost = MoneyAmount(required, context.currency),
            urgencyBonus = urgency(item, context.today),
            notes = item.nextAction ?: "Сравнить экономию на стоимости долга с потерей ликвидности.",
        )
    }

    private fun growthCandidate(item: FinancialObject, context: ActionGenerationContext): ActionCandidate? {
        val capital = item.capitalRequired?.takeIf { it.currency == context.currency } ?: return null
        if (capital.amount <= 0.0) return null

        val metrics = CapitalEfficiency.evaluate(item)
        val expectedNetBenefit = metrics
            ?.probabilityWeightedProfit
            ?.takeIf { it.currency == context.currency }

        val risk = (item.riskScore ?: defaultRisk(item.type)).coerceAtLeast(0.0)
        val lockPenalty = (item.liquidityLockDays ?: 0).coerceAtLeast(0) / 30.0 * 3.0
        val scalabilityBonus = if (item.scalable == true) 12.0 else 0.0

        return ActionCandidate(
            id = "grow-${item.id}",
            title = when (item.type) {
                FinancialObjectType.OPPORTUNITY -> "Профинансировать возможность: ${item.title}"
                FinancialObjectType.ASSET -> "Увеличить доходный актив: ${item.title}"
                FinancialObjectType.INCOME_SOURCE -> "Усилить источник дохода: ${item.title}"
                else -> item.title
            },
            required = capital,
            guaranteedBenefit = item.guaranteedSaving?.takeIf { it.currency == context.currency },
            expectedBenefit = expectedNetBenefit,
            liquidityCost = capital,
            riskPenalty = risk + lockPenalty,
            urgencyBonus = urgency(item, context.today) + scalabilityBonus,
            notes = buildString {
                append(item.nextAction ?: "Сравнить ожидаемую отдачу, срок до денег и риск потери капитала.")
                metrics?.probabilityWeightedRoiPercent?.let { append(" Ожидаемый ROI: ${"%.1f".format(it)}%.") }
                item.liquidityLockDays?.let { append(" Срок снижения ликвидности: $it дн.") }
            },
        )
    }

    private fun claimCandidate(item: FinancialObject, context: ActionGenerationContext): ActionCandidate? {
        val cost = item.capitalRequired?.takeIf { it.currency == context.currency } ?: return null
        val claim = item.amount?.takeIf { it.currency == context.currency }
        val probability = (item.probabilityPercent ?: 50).coerceIn(0, 100) / 100.0
        val expectedGross = item.expectedGain?.takeIf { it.currency == context.currency } ?: claim
        val expectedNet = expectedGross?.let {
            MoneyAmount(it.amount * probability - cost.amount, context.currency)
        }

        return ActionCandidate(
            id = "claim-${item.id}",
            title = "Потратить на получение денег: ${item.title}",
            required = cost,
            guaranteedBenefit = item.guaranteedSaving?.takeIf { it.currency == context.currency },
            expectedBenefit = expectedNet,
            liquidityCost = cost,
            riskPenalty = item.riskScore ?: 20.0,
            urgencyBonus = urgency(item, context.today),
            notes = item.nextAction ?: "Учитывать вероятность взыскания/получения и стоимость процедуры.",
        )
    }

    private fun urgency(item: FinancialObject, today: LocalDate): Double {
        val due = item.dueDate ?: return if (item.status == ObjectStatus.NEEDS_ACTION) 8.0 else 0.0
        val days = ChronoUnit.DAYS.between(today, due)
        return when {
            days < 0 -> 35.0
            days <= 3 -> 30.0
            days <= 7 -> 22.0
            days <= 30 -> 10.0
            else -> 0.0
        }
    }

    private fun defaultRisk(type: FinancialObjectType): Double = when (type) {
        FinancialObjectType.OPPORTUNITY -> 20.0
        FinancialObjectType.ASSET -> 25.0
        FinancialObjectType.INCOME_SOURCE -> 15.0
        else -> 0.0
    }
}
