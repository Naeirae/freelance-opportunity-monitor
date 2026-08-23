package com.naeirae.fincontrol.domain

import java.time.LocalDate
import java.util.UUID

enum class CurrencyCode { RUB, KZT, USD, EUR }

enum class ObligationKind { CREDIT, CREDIT_CARD, RENT, EDUCATION, PRIVATE_DEBT, TAX, OTHER }

enum class IncomeKind { SALARY, FREELANCE, REFUND, TAX_REFUND, BENEFIT, OTHER }

enum class AttentionLevel { LOW, MEDIUM, HIGH, CRITICAL }

data class MoneyAmount(
    val amount: Double,
    val currency: CurrencyCode,
)

data class MoneyAccount(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val balance: MoneyAmount,
    val isLiquid: Boolean = true,
)

data class ProtectedLiquidityBlock(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val amount: MoneyAmount,
    val until: LocalDate? = null,
    val hardProtection: Boolean = true,
)

data class Obligation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: ObligationKind,
    val balance: MoneyAmount,
    val nextPayment: MoneyAmount? = null,
    val nextDueDate: LocalDate? = null,
    val annualRatePercent: Double? = null,
    val graceUntil: LocalDate? = null,
    val minimumEarlyPayment: MoneyAmount? = null,
    val penaltyNote: String? = null,
)

data class ExpectedIncome(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: IncomeKind,
    val amount: MoneyAmount,
    val expectedDate: LocalDate? = null,
    val probabilityPercent: Int = 100,
)

data class CoverageLink(
    val obligationId: String,
    val sourceId: String,
    val amount: MoneyAmount,
)

data class ActionOption(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val allocation: MoneyAmount,
    val expectedGain: MoneyAmount? = null,
    val guaranteedSaving: MoneyAmount? = null,
    val liquidityAfter: MoneyAmount,
    val attention: AttentionLevel,
    val rationale: String,
)

data class DashboardSnapshot(
    val available: MoneyAmount,
    val protected: MoneyAmount,
    val allocatable: MoneyAmount,
    val obligations: List<Obligation>,
    val expectedIncome: List<ExpectedIncome>,
    val attentionItems: List<String>,
)
