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
import com.naeirae.fincontrol.domain.CurrencyCode
import com.naeirae.fincontrol.domain.FinancialObject
import com.naeirae.fincontrol.domain.FinancialObjectType
import com.naeirae.fincontrol.domain.MoneyAmount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialObjectsScreen(
    viewModel: FinancialObjectsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<FinancialObject?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Финансовые объекты") }) },
    ) { padding ->
        if (creating || editing != null) {
            FinancialObjectEditor(
                initial = editing,
                modifier = Modifier.padding(padding),
                onCancel = {
                    creating = false
                    editing = null
                },
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
                item {
                    Text(
                        "Крупные позиции, которые действительно меняют финансовое состояние.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.selectedType == null,
                            onClick = { viewModel.setFilter(null) },
                            label = { Text("Все") },
                        )
                        FilterChip(
                            selected = state.selectedType == FinancialObjectType.OBLIGATION,
                            onClick = { viewModel.setFilter(FinancialObjectType.OBLIGATION) },
                            label = { Text("Обязательства") },
                        )
                        FilterChip(
                            selected = state.selectedType == FinancialObjectType.EXPECTED_INFLOW,
                            onClick = { viewModel.setFilter(FinancialObjectType.EXPECTED_INFLOW) },
                            label = { Text("Ожидается") },
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.selectedType == FinancialObjectType.OPPORTUNITY,
                            onClick = { viewModel.setFilter(FinancialObjectType.OPPORTUNITY) },
                            label = { Text("Возможности") },
                        )
                        FilterChip(
                            selected = state.selectedType == FinancialObjectType.CLAIM,
                            onClick = { viewModel.setFilter(FinancialObjectType.CLAIM) },
                            label = { Text("Требования") },
                        )
                        FilterChip(
                            selected = state.selectedType == FinancialObjectType.ASSET,
                            onClick = { viewModel.setFilter(FinancialObjectType.ASSET) },
                            label = { Text("Активы") },
                        )
                    }
                }
                items(state.visible, key = { it.id }) { item ->
                    FinancialObjectCard(
                        item = item,
                        onEdit = { editing = item },
                        onArchive = { viewModel.archive(item.id) },
                    )
                }
                item {
                    Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Добавить крупный объект")
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialObjectCard(
    item: FinancialObject,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(item.title, fontWeight = FontWeight.SemiBold)
            Text(typeLabel(item.type), style = MaterialTheme.typography.labelMedium)
            item.amount?.let { Text(formatMoney(it), style = MaterialTheme.typography.titleMedium) }
            item.probabilityPercent?.let { Text("Вероятность: $it%") }
            item.dueDate?.let { Text("Дедлайн: $it") }
            item.nextAction?.let { Text("Следующее действие: $it") }
            if (item.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.tags.take(3).forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                }
            }
            Row {
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
    var nextAction by remember(initial?.id) { mutableStateOf(initial?.nextAction.orEmpty()) }
    var type by remember(initial?.id) { mutableStateOf(initial?.type ?: FinancialObjectType.OBLIGATION) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(if (initial == null) "Новый финансовый объект" else "Редактирование", style = MaterialTheme.typography.titleLarge) }
        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название") },
            )
        }
        item {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Сумма") },
            )
        }
        item {
            Text("Тип", fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FinancialObjectType.entries
                    .filter { it != FinancialObjectType.EVENT }
                    .forEach { candidate ->
                        FilterChip(
                            selected = type == candidate,
                            onClick = { type = candidate },
                            label = { Text(typeLabel(candidate)) },
                        )
                    }
            }
        }
        item {
            OutlinedTextField(
                value = nextAction,
                onValueChange = { nextAction = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Следующее действие") },
                minLines = 2,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        val parsed = amount.replace(',', '.').toDoubleOrNull()
                        onSave(
                            (initial ?: FinancialObject(type = type, title = title)).copy(
                                type = type,
                                title = title.trim(),
                                amount = parsed?.let { MoneyAmount(it, initial?.amount?.currency ?: CurrencyCode.RUB) },
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
