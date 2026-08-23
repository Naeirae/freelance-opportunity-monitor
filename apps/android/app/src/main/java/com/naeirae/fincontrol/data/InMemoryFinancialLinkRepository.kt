package com.naeirae.fincontrol.data

import com.naeirae.fincontrol.domain.FinancialLink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryFinancialLinkRepository(
    initial: List<FinancialLink> = emptyList(),
) : FinancialLinkRepository {
    private val items = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<FinancialLink>> = items.asStateFlow()

    override suspend fun upsert(link: FinancialLink) {
        items.value = items.value.filterNot { it.id == link.id } + link
    }

    override suspend fun delete(id: String) {
        items.value = items.value.filterNot { it.id == id }
    }
}
