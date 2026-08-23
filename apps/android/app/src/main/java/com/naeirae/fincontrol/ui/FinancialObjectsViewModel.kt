package com.naeirae.fincontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naeirae.fincontrol.data.AppContainer
import com.naeirae.fincontrol.data.FinancialObjectRepository
import com.naeirae.fincontrol.domain.FinancialObject
import com.naeirae.fincontrol.domain.FinancialObjectType
import com.naeirae.fincontrol.domain.ObjectStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FinancialObjectsUiState(
    val all: List<FinancialObject> = emptyList(),
    val selectedType: FinancialObjectType? = null,
) {
    val visible: List<FinancialObject>
        get() = all.filter { it.status != ObjectStatus.ARCHIVED }
            .filter { selectedType == null || it.type == selectedType }
}

class FinancialObjectsViewModel(
    private val repository: FinancialObjectRepository = AppContainer.financialObjects,
) : ViewModel() {
    private val selectedType = kotlinx.coroutines.flow.MutableStateFlow<FinancialObjectType?>(null)

    val state: StateFlow<FinancialObjectsUiState> = kotlinx.coroutines.flow.combine(
        repository.observeAll(), selectedType,
    ) { items, filter ->
        FinancialObjectsUiState(items, filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinancialObjectsUiState())

    fun setFilter(type: FinancialObjectType?) {
        selectedType.value = type
    }

    fun save(item: FinancialObject) {
        viewModelScope.launch { repository.upsert(item) }
    }

    fun archive(id: String) {
        viewModelScope.launch { repository.archive(id) }
    }
}
