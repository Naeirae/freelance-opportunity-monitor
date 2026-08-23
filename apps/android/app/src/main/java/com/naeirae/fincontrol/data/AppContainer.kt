package com.naeirae.fincontrol.data

import com.naeirae.fincontrol.domain.CurrencyCode
import com.naeirae.fincontrol.domain.FinancialObject
import com.naeirae.fincontrol.domain.FinancialObjectType
import com.naeirae.fincontrol.domain.MoneyAmount
import com.naeirae.fincontrol.domain.ObjectStatus

object AppContainer {
    private val seedObjects = listOf(
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

    val financialObjects: FinancialObjectRepository = InMemoryFinancialObjectRepository(seedObjects)
    val financialLinks: FinancialLinkRepository = InMemoryFinancialLinkRepository()
}
