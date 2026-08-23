package com.naeirae.fincontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naeirae.fincontrol.data.AppContainer
import com.naeirae.fincontrol.data.FinancialObjectRepository
import com.naeirae.fincontrol.domain.ActionCandidate
import com.naeirae.fincontrol.domain.ActionGenerationContext
import com.naeirae.fincontrol.domain.AutomaticActionGenerator
import com.naeirae.fincontrol.domain.CurrencyCode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DecisionCandidatesViewModel(
    private val repository: FinancialObjectRepository = AppContainer.financialObjects,
) : ViewModel() {
    fun observe(currency: CurrencyCode): StateFlow<List<ActionCandidate>> = repository.observeAll()
        .map { objects ->
            AutomaticActionGenerator.generate(
                objects = objects,
                context = ActionGenerationContext(currency = currency),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
