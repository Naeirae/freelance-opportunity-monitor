package com.naeirae.fincontrol.domain

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

enum class FinancialObjectType {
    MONEY,
    OBLIGATION,
    EXPECTED_INFLOW,
    OPPORTUNITY,
    CLAIM,
    ASSET,
    INCOME_SOURCE,
    EVENT,
}

enum class ObjectStatus {
    ACTIVE,
    EXPECTED,
    NEEDS_ACTION,
    WAITING,
    COMPLETED,
    REJECTED,
    ARCHIVED,
}

enum class SourceKind {
    MANUAL,
    DOCUMENT,
    SCREENSHOT,
    TELEGRAM,
    CONNECTOR,
    IMPORT,
}

enum class CapitalRole {
    LIQUIDITY,
    PROTECTED,
    WORKING,
    INVESTMENT,
}

data class SourceReference(
    val kind: SourceKind,
    val label: String? = null,
    val uri: String? = null,
    val capturedAt: OffsetDateTime = OffsetDateTime.now(),
)

data class FinancialObject(
    val id: String = UUID.randomUUID().toString(),
    val type: FinancialObjectType,
    val title: String,
    val amount: MoneyAmount? = null,
    val status: ObjectStatus = ObjectStatus.ACTIVE,
    val dueDate: LocalDate? = null,
    val probabilityPercent: Int? = null,
    val protectedAmount: MoneyAmount? = null,
    val capitalRole: CapitalRole? = null,
    val capitalRequired: MoneyAmount? = null,
    val guaranteedSaving: MoneyAmount? = null,
    val expectedGain: MoneyAmount? = null,
    val annualRatePercent: Double? = null,
    val riskScore: Double? = null,
    val liquidityLockDays: Int? = null,
    val scalable: Boolean? = null,
    val tags: Set<String> = emptySet(),
    val nextAction: String? = null,
    val notes: String? = null,
    val source: SourceReference = SourceReference(SourceKind.MANUAL),
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
