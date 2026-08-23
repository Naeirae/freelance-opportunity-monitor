package com.naeirae.fincontrol.data

import com.naeirae.fincontrol.domain.FinancialObject
import kotlinx.coroutines.flow.Flow

interface FinancialObjectRepository {
    fun observeAll(): Flow<List<FinancialObject>>
    suspend fun get(id: String): FinancialObject?
    suspend fun upsert(item: FinancialObject)
    suspend fun archive(id: String)
}
