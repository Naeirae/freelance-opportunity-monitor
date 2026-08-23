package com.naeirae.fincontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naeirae.fincontrol.domain.CapitalEfficiency
import com.naeirae.fincontrol.domain.CapitalRole
import com.naeirae.fincontrol.domain.CurrencyCode
import com.naeirae.fincontrol.domain.FinancialObject
import com.naeirae.fincontrol.domain.FinancialObjectType
import com.naeirae.fincontrol.domain.MoneyAmount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialObjectsScreen(
    viewModel: FinancialObjectsViewModel = viewModel(),
    onOpenObligation: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<FinancialObject?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Финансовые объекты") }) }) { padding ->
        if (creating || editing != null) {
            FinancialObjectEditor(
                initial = editing,
                modifier = Modifier.padding(padding),
                onCancel = { creating = false; editing = null },
                onSave = {
                    viewModel.save(it)
                    creating = false
                    editing = null
                },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Text("Крупные позиции, которые действительно меняют финансовое состояние.") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(state.selectedType == null, { viewModel.setFilter(null) }, label = { Text("Все") })
                        FilterChip(state.selectedType == FinancialObjectType.OBLIGATION, { viewModel.setFilter(FinancialObjectType.OBLIGATION) }, label = { Text("Обязательства") })
                        FilterChip(state.selectedType == FinancialObjectType.EXPECTED_INFLOW, { viewModel.setFilter(FinancialObjectType.EXPECTED_INFLOW) }, label = { Text("Ожидается") })
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(state.selectedType == FinancialObjectType.OPPORTUNITY, { viewModel.setFilter(FinancialObjectType.OPPORTUNITY) }, label = { Text("Возможности") })
                        FilterChip(state.selectedType == FinancialObjectType.CLAIM, { viewModel.setFilter(FinancialObjectType.CLAIM) }, label = { Text("Требования") })
                        FilterChip(state.selectedType == FinancialObjectType.ASSET, { viewModel.setFilter(FinancialObjectType.ASSET) }, label = { Text("Активы") })
                    }
                }
                items(state.visible, key = { it.id }) { item ->
                    FinancialObjectCard(
                        item = item,
                        onOpen = if (item.type == FinancialObjectType.OBLIGATION) ({ onOpenObligation(item.id) }) else null,
                        onEdit = { editing = item },
                        onArchive = { viewModel.archive(item.id) },
                    )
                }
                item {
                    Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) { Text("Добавить крупный объект") }
                }
            }
        }
    }
}

@Composable
private fun FinancialObjectCard(
    item: FinancialObject,
    onOpen: (() -> Unit)?,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title, fontWeight = FontWeight.SemiBold)
            Text(typeLabel(item.type), style = MaterialTheme.typography.labelMedium)
            item.amount?.let { Text(formatMoney(it), style = MaterialTheme.typography.titleMedium) }
            item.capitalRequired?.let { Text("Нужно вложить: ${formatMoney(it)}") }
            item.expectedGain?.let { Text("Ожидаемый возврат: ${formatMoney(it)}") }
            item.probabilityPercent?.let { Text("Вероятность: $it%") }
            CapitalEfficiency.evaluate(item)?.let { metrics ->
                val weightedRoi = metrics.probabilityWeightedRoiPercent
                if (weightedRoi != null) Text("Ожидаемый ROI: ${"%.1f".format(weightedRoi)}%")
            }
            item.dueDate?.let { Text("Дедлайн: $it") }
            item.nextAction?.let { Text("Следующее действие: $it") }
            if (item.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.tags.take(3).forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                }
            }
            Row {
                onOpen?.let { TextButton(onClick = it) { Text("Покрытие") } }
                TextButton(onClick = onEdit) { Text("Изменить") }
                TextButton(onClick = onArchive) { Text("В архив") }
            }
        }
    }
}

@Composable
private fun FinancialObjectEditor(
    initial: FinancialObject?,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit,
    onSave: (FinancialObject) -> Unit,
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var amount by remember(initial?.id) { mutableStateOf(initial?.amount?.amount?.toString().orEmpty()) }
    var capitalRequired by remember(initial?.id) { mutableStateOf(initial?.capitalRequired?.amount?.toString().orEmpty()) }
    var expectedGain by remember(initial?.id) { mutableStateOf(initial?.expectedGain?.amount?.toString().orEmpty()) }
    var probability by remember(initial?.id) { mutableStateOf(initial?.probabilityPercent?.toString().orEmpty()) }
    var lockDays by remember(initial?.id) { mutableStateOf(initial?.liquidityLockDays?.toString().orEmpty()) }
    var nextAction by remember(initial?.id) { mutableStateOf(initial?.nextAction.orEmpty()) }
    var type by remember(initial?.id) { mutableStateOf(initial?.type ?: FinancialObjectType.OBLIGATION) }
    var capitalRole by remember(initial?.id) { mutableStateOf(initial?.capitalRole ?: CapitalRole.WORKING) }
    var scalable by remember(initial?.id) { mutableStateOf(initial?.scalable ?: false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(if (initial == null) "Новый финансовый объект" else "Редактирование", style = MaterialTheme.typography.titleLarge) }
        item { OutlinedTextField(title, { title = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Название") }) }
        item { OutlinedTextField(amount, { amount = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Сумма / потенциальный возврат") }) }
        item {
            Text("Тип", fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FinancialObjectType.entries.filter { it != FinancialObjectType.EVENT }.forEach { candidate ->
                    FilterChip(type == candidate, { type = candidate }, label = { Text(typeLabel(candidate)) })
                }
            }
        }

        if (type in setOf(FinancialObjectType.OPPORTUNITY, FinancialObjectType.ASSET, FinancialObjectType.INCOME_SOURCE, FinancialObjectType.CLAIM)) {
            item { Text("Экономика вложения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item { OutlinedTextField(capitalRequired, { capitalRequired = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Сколько капитала нужно") }) }
            item { OutlinedTextField(expectedGain, { expectedGain = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Сколько ожидается вернуть всего") }) }
            item { OutlinedTextField(probability, { probability = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Вероятность результата, %") }) }
            item { OutlinedTextField(lockDays, { lockDays = it }, modifier = Modifier.fillMaxWidth(), label = { Text("На сколько дней деньги станут менее ликвидными") }) }
            item {
                Text("Роль капитала", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(capitalRole == CapitalRole.WORKING, { capitalRole = CapitalRole.WORKING }, label = { Text("Рабочий") })
                    FilterChip(capitalRole == CapitalRole.INVESTMENT, { capitalRole = CapitalRole.INVESTMENT }, label = { Text("Инвестиционный") })
                }
            }
            item {
                Row { Checkbox(checked = scalable, onCheckedChange = { scalable = it }); Text("Можно масштабировать") }
            }
        }

        item { OutlinedTextField(nextAction, { nextAction = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Следующее действие") }, minLines = 2) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        val currency = initial?.amount?.currency ?: CurrencyCode.RUB
                        fun number(text: String) = text.replace(',', '.').toDoubleOrNull()
                        onSave(
                            (initial ?: FinancialObject(type = type, title = title)).copy(
                                type = type,
                                title = title.trim(),
                                amount = number(amount)?.let { MoneyAmount(it, currency) },
                                capitalRequired = number(capitalRequired)?.let { MoneyAmount(it, currency) },
                                expectedGain = number(expectedGain)?.let { MoneyAmount(it, currency) },
                                probabilityPercent = number(probability)?.toInt()?.coerceIn(0, 100),
                                liquidityLockDays = number(lockDays)?.toInt()?.coerceAtLeast(0),
                                capitalRole = if (type in setOf(FinancialObjectType.OPPORTUNITY, FinancialObjectType.ASSET, FinancialObjectType.INCOME_SOURCE, FinancialObjectType.CLAIM)) capitalRole else initial?.capitalRole,
                                scalable = if (type in setOf(FinancialObjectType.OPPORTUNITY, FinancialObjectType.ASSET, FinancialObjectType.INCOME_SOURCE, FinancialObjectType.CLAIM)) scalable else initial?.scalable,
                                nextAction = nextAction.trim().ifBlank { null },
                            ),
                        )
                    },
                ) { Text("Сохранить") }
                TextButton(onClick = onCancel) { Text("Отмена") }
            }
        }
    }
}

private fun typeLabel(type: FinancialObjectType): String = when (type) {
    FinancialObjectType.MONEY -> "Деньги"
    FinancialObjectType.OBLIGATION -> "Обязательство"
    FinancialObjectType.EXPECTED_INFLOW -> "Ожидаемые деньги"
    FinancialObjectType.OPPORTUNITY -> "Возможность"
    FinancialObjectType.CLAIM -> "Требование / возврат"
    FinancialObjectType.ASSET -> "Актив"
    FinancialObjectType.INCOME_SOURCE -> "Источник дохода"
    FinancialObjectType.EVENT -> "Событие"
}

private fun formatMoney(value: MoneyAmount): String = "${"%,.0f".format(value.amount)} ${when (value.currency) {
    CurrencyCode.RUB -> "₽"
    CurrencyCode.KZT -> "₸"
    CurrencyCode.USD -> "$"
    CurrencyCode.EUR -> "€"
}}"
