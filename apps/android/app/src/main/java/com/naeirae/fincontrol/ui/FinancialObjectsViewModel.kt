package com.naeirae.fincontrol.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.naeirae.fincontrol.data.FinancialObjectRepository
import com.naeirae.fincontrol.data.InMemoryFinancialObjectRepository
import com.naeirae.fincontrol.domain.CurrencyCode
import com.naeirae.fincontrol.domain.FinancialObject
import com.naeirae.fincontrol.domain.FinancialObjectType
import com.naeirae.fincontrol.domain.MoneyAmount
import com.naeirae.fincontrol.domain.ObjectStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val demoObjects = listOf(
    FinancialObject(
        type = FinancialObjectType.OBLIGATION,
        title = "Кредитная карта",
        amount = MoneyAmount(39_900.0, CurrencyCode.RUB),
        status = ObjectStatus.ACTIVE,
        nextAction = "Не потерять льготный период",
        tags = setOf("кредит", "грейс"),
    ),
    FinancialObject(
        type = FinancialObjectType.EXPECTED_INFLOW,
        title = "Ожидаемое поступление",
        amount = MoneyAmount(12_000.0, CurrencyCode.RUB),
        status = ObjectStatus.EXPECTED,
        probabilityPercent = 80,
        tags = setOf("доход"),
    ),
    FinancialObject(
        type = FinancialObjectType.OPPORTUNITY,
        title = "Короткий заказ",
        amount = MoneyAmount(8_000.0, CurrencyCode.RUB),
        status = ObjectStatus.NEEDS_ACTION,
        probabilityPercent = 35,
        nextAction = "Оценить трудозатраты и откликнуться",
        tags = setOf("работа", "заказ"),
    ),
)

data class FinancialObjectsUiState(
    val all: List<FinancialObject> = emptyList(),
    val selectedType: FinancialObjectType? = null,
) {
    val visible: List<FinancialObject>
        get() = all.filter { it.status != ObjectStatus.ARCHIVED }
            .filter { selectedType == null || it.type == selectedType }
}

class FinancialObjectsViewModel(
    private val repository: FinancialObjectRepository = InMemoryFinancialObjectRepository(demoObjects),
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
