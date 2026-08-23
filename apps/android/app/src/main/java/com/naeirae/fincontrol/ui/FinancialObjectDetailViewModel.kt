package com.naeirae.fincontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naeirae.fincontrol.data.AppContainer
import com.naeirae.fincontrol.data.FinancialObjectRepository
import com.naeirae.fincontrol.domain.FinancialObject
import com.naeirae.fincontrol.domain.ObjectStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinancialObjectDetailViewModel(
    private val repository: FinancialObjectRepository = AppContainer.financialObjects,
) : ViewModel() {
    fun observe(id: String): StateFlow<FinancialObject?> = repository.observeAll()
        .map { items -> items.firstOrNull { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(item: FinancialObject) {
        viewModelScope.launch { repository.upsert(item) }
    }

    fun complete(item: FinancialObject) {
        save(item.copy(status = ObjectStatus.COMPLETED))
    }
}
