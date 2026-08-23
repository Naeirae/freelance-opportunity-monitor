package com.naeirae.fincontrol.domain

import kotlin.math.max

object AllocationCalculator {
    fun allocatable(
        available: MoneyAmount,
        protectedBlocks: List<ProtectedLiquidityBlock>,
    ): MoneyAmount {
        require(protectedBlocks.all { it.amount.currency == available.currency }) {
            "MVP calculator requires one currency per calculation"
        }
        val protectedTotal = protectedBlocks.sumOf { it.amount.amount }
        return MoneyAmount(
            amount = max(0.0, available.amount - protectedTotal),
            currency = available.currency,
        )
    }

    fun uncoveredAmount(
        obligation: Obligation,
        coverage: List<CoverageLink>,
    ): MoneyAmount {
        val target = obligation.nextPayment ?: obligation.balance
        val covered = coverage
            .filter { it.obligationId == obligation.id && it.amount.currency == target.currency }
            .sumOf { it.amount.amount }
        return MoneyAmount(max(0.0, target.amount - covered), target.currency)
    }

    fun canAllocate(
        requested: MoneyAmount,
        allocatable: MoneyAmount,
    ): Boolean = requested.currency == allocatable.currency && requested.amount <= allocatable.amount
}
