package com.naeirae.fincontrol.domain

import java.util.UUID

enum class FinancialLinkType {
    COVERS,
    GENERATED_BY,
    CONVERTS_TO,
    EVIDENCE_FOR,
    REFINANCES,
    DEPENDS_ON,
}

data class FinancialLink(
    val id: String = UUID.randomUUID().toString(),
    val fromObjectId: String,
    val toObjectId: String,
    val type: FinancialLinkType,
    val amount: MoneyAmount? = null,
    val probabilityPercent: Int = 100,
    val note: String? = null,
)

data class CoverageSummary(
    val obligation: FinancialObject,
    val sources: List<Pair<FinancialObject, FinancialLink>>,
    val target: MoneyAmount,
    val nominalCovered: MoneyAmount,
    val probabilityWeightedCovered: MoneyAmount,
    val deficit: MoneyAmount,
)

object FinancialCoverageEngine {
    fun summarize(
        obligation: FinancialObject,
        objects: List<FinancialObject>,
        links: List<FinancialLink>,
    ): CoverageSummary {
        require(obligation.type == FinancialObjectType.OBLIGATION)
        val target = requireNotNull(obligation.amount) { "Obligation must have an amount" }
        val objectById = objects.associateBy { it.id }

        val sources = links
            .filter { it.type == FinancialLinkType.COVERS && it.toObjectId == obligation.id }
            .mapNotNull { link -> objectById[link.fromObjectId]?.let { it to link } }
            .filter { (_, link) -> link.amount?.currency == target.currency }

        val nominal = sources.sumOf { (_, link) -> link.amount?.amount ?: 0.0 }.coerceAtMost(target.amount)
        val weighted = sources.sumOf { (source, link) ->
            val probability = minOf(source.probabilityPercent ?: 100, link.probabilityPercent).coerceIn(0, 100)
            (link.amount?.amount ?: 0.0) * probability / 100.0
        }.coerceAtMost(target.amount)

        return CoverageSummary(
            obligation = obligation,
            sources = sources,
            target = target,
            nominalCovered = MoneyAmount(nominal, target.currency),
            probabilityWeightedCovered = MoneyAmount(weighted, target.currency),
            deficit = MoneyAmount((target.amount - nominal).coerceAtLeast(0.0), target.currency),
        )
    }
}
