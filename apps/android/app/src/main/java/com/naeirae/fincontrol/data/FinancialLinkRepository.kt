package com.naeirae.fincontrol.data

import com.naeirae.fincontrol.domain.FinancialLink
import kotlinx.coroutines.flow.Flow

interface FinancialLinkRepository {
    fun observeAll(): Flow<List<FinancialLink>>
    suspend fun upsert(link: FinancialLink)
    suspend fun delete(id: String)
}
