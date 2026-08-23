package com.naeirae.fincontrol.data

import com.naeirae.fincontrol.domain.FinancialObject
import com.naeirae.fincontrol.domain.ObjectStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryFinancialObjectRepository(
    seed: List<FinancialObject> = emptyList(),
) : FinancialObjectRepository {
    private val items = MutableStateFlow(seed)

    override fun observeAll(): Flow<List<FinancialObject>> = items.asStateFlow()

    override suspend fun get(id: String): FinancialObject? = items.value.firstOrNull { it.id == id }

    override suspend fun upsert(item: FinancialObject) {
        items.update { current ->
            val index = current.indexOfFirst { it.id == item.id }
            if (index < 0) current + item
            else current.toMutableList().apply { set(index, item) }
        }
    }

    override suspend fun archive(id: String) {
        items.update { current ->
            current.map { item ->
                if (item.id == id) item.copy(status = ObjectStatus.ARCHIVED) else item
            }
        }
    }
}
