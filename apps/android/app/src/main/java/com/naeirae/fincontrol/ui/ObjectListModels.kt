package com.naeirae.fincontrol.ui

import com.naeirae.fincontrol.domain.FinancialObject
import com.naeirae.fincontrol.domain.FinancialObjectType

data class ObjectSection(
    val title: String,
    val items: List<FinancialObject>,
)

object ObjectSections {
    fun from(items: List<FinancialObject>): List<ObjectSection> {
        val order = listOf(
            FinancialObjectType.OBLIGATION to "Обязательства",
            FinancialObjectType.EXPECTED_INFLOW to "Ожидаемые деньги",
            FinancialObjectType.OPPORTUNITY to "Возможности",
            FinancialObjectType.CLAIM to "Требования и возвраты",
            FinancialObjectType.ASSET to "Активы",
            FinancialObjectType.INCOME_SOURCE to "Источники дохода",
            FinancialObjectType.MONEY to "Деньги",
            FinancialObjectType.EVENT to "События",
        )

        return order.mapNotNull { (type, title) ->
            items.filter { it.type == type }
                .takeIf { it.isNotEmpty() }
                ?.let { ObjectSection(title, it) }
        }
    }
}
