package com.naeirae.fincontrol.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naeirae.fincontrol.domain.FinancialObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObligationDetailScreen(
    state: ObligationDetailState,
    onBack: () -> Unit,
    onAssignCoverage: (FinancialObject, Double, Int) -> Unit,
    onRemoveCoverage: (String) -> Unit,
) {
    var selectedSource by remember { mutableStateOf<FinancialObject?>(null) }
    var amountText by remember { mutableStateOf("") }
    var probabilityText by remember { mutableStateOf("100") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.obligation?.title ?: "Обязательство") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            state.coverage?.let { summary ->
                item { CoveragePlanner(summary) }
                item {
                    Text(
                        "Назначить источник покрытия",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Связь означает план покрытия. Она не списывает деньги и не делает весь баланс доступным для погашения.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                items(state.candidateSources, key = { it.id }) { source ->
                    FilterChip(
                        selected = selectedSource?.id == source.id,
                        onClick = {
                            selectedSource = source
                            amountText = source.amount?.amount?.toString().orEmpty()
                            probabilityText = (source.probabilityPercent ?: 100).toString()
                        },
                        label = { Text(source.title) },
                    )
                }
                if (selectedSource != null) {
                    item {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Сколько из источника направить на покрытие") },
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = probabilityText,
                            onValueChange = { probabilityText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Вероятность поступления, %") },
                        )
                    }
                    item {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val source = selectedSource ?: return@Button
                                val amount = amountText.replace(',', '.').toDoubleOrNull() ?: return@Button
                                val probability = probabilityText.toIntOrNull() ?: 100
                                onAssignCoverage(source, amount, probability)
                                selectedSource = null
                                amountText = ""
                                probabilityText = "100"
                            },
                        ) { Text("Назначить покрытие") }
                    }
                }
                if (summary.sources.isNotEmpty()) {
                    item {
                        Text("Текущие связи", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(summary.sources, key = { it.second.id }) { (source, link) ->
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(source.title, fontWeight = FontWeight.SemiBold)
                                    Text(link.amount?.let { "${it.amount} ${it.currency}" } ?: "Без суммы")
                                }
                                TextButton(onClick = { onRemoveCoverage(link.id) }) { Text("Убрать") }
                            }
                        }
                    }
                }
            } ?: item {
                Text("Не удалось построить план покрытия для этого объекта.")
            }
        }
    }
}
