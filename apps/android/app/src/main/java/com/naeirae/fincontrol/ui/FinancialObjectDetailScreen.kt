package com.naeirae.fincontrol.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naeirae.fincontrol.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialObjectDetailScreen(
    item: FinancialObject?,
    onBack: () -> Unit,
    onSave: (FinancialObject) -> Unit,
    onComplete: (FinancialObject) -> Unit,
) {
    if (item == null) {
        Scaffold(topBar = { TopAppBar(title = { Text("Финансовый объект") }) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).padding(16.dp)) { Text("Объект не найден") }
        }
        return
    }

    var capitalText by remember(item.id, item.capitalRequired) { mutableStateOf(item.capitalRequired?.amount?.toString().orEmpty()) }
    var returnText by remember(item.id, item.expectedGain) { mutableStateOf(item.expectedGain?.amount?.toString().orEmpty()) }
    var probabilityText by remember(item.id, item.probabilityPercent) { mutableStateOf((item.probabilityPercent ?: 100).toString()) }
    var lockDaysText by remember(item.id, item.liquidityLockDays) { mutableStateOf(item.liquidityLockDays?.toString().orEmpty()) }

    val currency = item.amount?.currency ?: item.capitalRequired?.currency ?: CurrencyCode.RUB
    val capital = capitalText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0)
    val grossReturn = returnText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0)
    val probability = probabilityText.toIntOrNull()?.coerceIn(0, 100) ?: 100
    val lockDays = lockDaysText.toIntOrNull()?.coerceAtLeast(0)

    val draft = item.copy(
        capitalRequired = capital?.let { MoneyAmount(it, currency) },
        expectedGain = grossReturn?.let { MoneyAmount(it, currency) },
        probabilityPercent = probability,
        liquidityLockDays = lockDays,
        capitalRole = item.capitalRole ?: CapitalRole.WORKING,
    )
    val metrics = CapitalEfficiency.evaluate(draft)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item.title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(detailTypeLabel(item.type), style = MaterialTheme.typography.labelLarge)
                item.amount?.let { Text(detailMoney(it), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                item.nextAction?.let { Text("Следующее действие: $it") }
            }

            if (item.type in setOf(FinancialObjectType.OPPORTUNITY, FinancialObjectType.ASSET, FinancialObjectType.INCOME_SOURCE, FinancialObjectType.CLAIM)) {
                item { Text("Экономика вложения", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                item {
                    OutlinedTextField(
                        value = capitalText,
                        onValueChange = { capitalText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Нужно вложить") },
                    )
                }
                item {
                    OutlinedTextField(
                        value = returnText,
                        onValueChange = { returnText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Вернётся при успехе") },
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = probabilityText,
                            onValueChange = { probabilityText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Вероятность, %") },
                        )
                        OutlinedTextField(
                            value = lockDaysText,
                            onValueChange = { lockDaysText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("До денег, дней") },
                        )
                    }
                }

                metrics?.let { value ->
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                DetailMetric("Прибыль при успехе", detailMoney(value.expectedProfit))
                                DetailMetric("Ожидаемая прибыль", detailMoney(value.probabilityWeightedProfit))
                                value.roiPercent?.let { DetailMetric("ROI при успехе", "${"%.1f".format(it)}%") }
                                value.probabilityWeightedRoiPercent?.let { DetailMetric("Ожидаемый ROI", "${"%.1f".format(it)}%") }
                                value.profitPerLockedDay?.let { DetailMetric("Ожидаемая прибыль в день", "${"%,.0f".format(it)}") }
                                value.paybackDays?.let { DetailMetric("Расчётная окупаемость", "${"%.1f".format(it)} дн.") }
                            }
                        }
                    }
                }

                item {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = capital != null && grossReturn != null,
                        onClick = { onSave(draft) },
                    ) { Text("Сохранить экономику") }
                }
            }

            item {
                Text("Статус: ${item.status}")
                Text("Источник: ${item.source.kind}${item.source.label?.let { " — $it" } ?: ""}")
                item.notes?.let { Text(it) }
            }

            if (item.status != ObjectStatus.COMPLETED) {
                item {
                    OutlinedButton(onClick = { onComplete(item) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Отметить завершённым")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetric(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun detailTypeLabel(type: FinancialObjectType): String = when (type) {
    FinancialObjectType.MONEY -> "Деньги"
    FinancialObjectType.OBLIGATION -> "Обязательство"
    FinancialObjectType.EXPECTED_INFLOW -> "Ожидаемые деньги"
    FinancialObjectType.OPPORTUNITY -> "Возможность"
    FinancialObjectType.CLAIM -> "Требование / возврат"
    FinancialObjectType.ASSET -> "Актив"
    FinancialObjectType.INCOME_SOURCE -> "Источник дохода"
    FinancialObjectType.EVENT -> "Событие"
}

private fun detailMoney(value: MoneyAmount): String = "${"%,.0f".format(value.amount)} ${when (value.currency) {
    CurrencyCode.RUB -> "₽"
    CurrencyCode.KZT -> "₸"
    CurrencyCode.USD -> "$"
    CurrencyCode.EUR -> "€"
}}"
