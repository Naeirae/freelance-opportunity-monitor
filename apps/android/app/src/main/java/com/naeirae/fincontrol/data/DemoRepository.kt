package com.naeirae.fincontrol.data

import com.naeirae.fincontrol.domain.*
import java.time.LocalDate

class DemoRepository {
    fun snapshot(): DashboardSnapshot {
        val available = MoneyAmount(50_000.0, CurrencyCode.RUB)
        val protected = MoneyAmount(45_000.0, CurrencyCode.RUB)
        val allocatable = MoneyAmount(5_000.0, CurrencyCode.RUB)

        val obligations = listOf(
            Obligation(
                name = "Credit card",
                kind = ObligationKind.CREDIT_CARD,
                balance = MoneyAmount(39_900.0, CurrencyCode.RUB),
                nextPayment = MoneyAmount(39_900.0, CurrencyCode.RUB),
                nextDueDate = LocalDate.now().plusDays(30),
                graceUntil = LocalDate.now().plusDays(45),
            ),
            Obligation(
                name = "Monthly housing",
                kind = ObligationKind.RENT,
                balance = MoneyAmount(40_000.0, CurrencyCode.RUB),
                nextPayment = MoneyAmount(40_000.0, CurrencyCode.RUB),
                nextDueDate = LocalDate.now().plusDays(10),
            ),
        )

        val income = listOf(
            ExpectedIncome(
                name = "Salary",
                kind = IncomeKind.SALARY,
                amount = MoneyAmount(65_000.0, CurrencyCode.RUB),
                expectedDate = LocalDate.now().plusDays(12),
            ),
            ExpectedIncome(
                name = "Freelance job",
                kind = IncomeKind.FREELANCE,
                amount = MoneyAmount(8_000.0, CurrencyCode.RUB),
                expectedDate = LocalDate.now().plusDays(5),
                probabilityPercent = 70,
            ),
        )

        return DashboardSnapshot(
            available = available,
            protected = protected,
            allocatable = allocatable,
            obligations = obligations,
            expectedIncome = income,
            attentionItems = listOf(
                "One obligation is only partially covered",
                "Keep protected liquidity untouched",
            ),
        )
    }
}
