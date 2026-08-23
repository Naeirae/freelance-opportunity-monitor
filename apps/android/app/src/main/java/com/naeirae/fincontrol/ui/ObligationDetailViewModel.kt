package com.naeirae.fincontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naeirae.fincontrol.data.FinancialLinkRepository
import com.naeirae.fincontrol.data.FinancialObjectRepository
import com.naeirae.fincontrol.data.InMemoryFinancialLinkRepository
import com.naeirae.fincontrol.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ObligationDetailViewModel(
    private val objects: FinancialObjectRepository,
    private val links: FinancialLinkRepository = InMemoryFinancialLinkRepository(),
) : ViewModel() {

    fun observe(obligationId: String): StateFlow<ObligationDetailState> = combine(
        objects.observeAll(),
        links.observeAll(),
    ) { allObjects, allLinks ->
        val obligation = allObjects.firstOrNull { it.id == obligationId && it.type == FinancialObjectType.OBLIGATION }
        if (obligation == null || obligation.amount == null) {
            ObligationDetailState()
        } else {
            val summary = FinancialCoverageEngine.summarize(obligation, allObjects, allLinks)
            val candidates = allObjects.filter {
                it.id != obligation.id &&
                    it.status != ObjectStatus.ARCHIVED &&
                    it.amount?.currency == obligation.amount.currency &&
                    it.type in setOf(
                        FinancialObjectType.MONEY,
                        FinancialObjectType.EXPECTED_INFLOW,
                        FinancialObjectType.OPPORTUNITY,
                        FinancialObjectType.CLAIM,
                        FinancialObjectType.INCOME_SOURCE,
                    )
            }
            ObligationDetailState(obligation, summary, candidates)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ObligationDetailState())

    fun assignCoverage(obligationId: String, source: FinancialObject, amount: Double, probability: Int) {
        val money = source.amount ?: return
        if (amount <= 0.0) return
        viewModelScope.launch {
            links.upsert(
                FinancialLink(
                    fromObjectId = source.id,
                    toObjectId = obligationId,
                    type = FinancialLinkType.COVERS,
                    amount = MoneyAmount(amount, money.currency),
                    probabilityPercent = probability.coerceIn(0, 100),
                ),
            )
        }
    }

    fun removeCoverage(linkId: String) {
        viewModelScope.launch { links.delete(linkId) }
    }
}

data class ObligationDetailState(
    val obligation: FinancialObject? = null,
    val coverage: CoverageSummary? = null,
    val candidateSources: List<FinancialObject> = emptyList(),
)
