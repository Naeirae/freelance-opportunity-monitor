package com.naeirae.fincontrol.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naeirae.fincontrol.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionScenarioScreen(
    currency: CurrencyCode = CurrencyCode.RUB,
    candidates: List<ActionCandidate>,
    onBack: () -> Unit,
) {
    var freeText by remember { mutableStateOf("5000") }
    var keepText by remember { mutableStateOf("0") }

    val free = freeText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val keep = keepText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val result = DecisionScenarioEngine.evaluate(
        DecisionScenarioInput(
            freeNow = MoneyAmount(free, currency),
            keepLiquid = MoneyAmount(keep, currency),
            candidates = candidates,
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Что делать со свободными деньгами") },
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
                Text(
                    "Здесь указывается не весь баланс, а только сумма, которую реально можно распределять после еды, жилья, детей, обязательных платежей и других защищённых нужд.",
                )
            }
            item {
                OutlinedTextField(
                    value = freeText,
                    onValueChange = { freeText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Свободно сейчас") },
                )
            }
            item {
                OutlinedTextField(
                    value = keepText,
                    onValueChange = { keepText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Из этой суммы ещё оставить ликвидным") },
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Можно распределить", style = MaterialTheme.typography.labelLarge)
                        Text(
                            formatDecisionMoney(result.distributableNow),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            item { Text("Варианты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(result.ranked, key = { it.candidate.id }) { ranked ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(ranked.candidate.title, fontWeight = FontWeight.SemiBold)
                        Text("Нужно: ${formatDecisionMoney(ranked.candidate.required)}")
                        ranked.candidate.guaranteedBenefit?.let { Text("Гарантированный эффект: ${formatDecisionMoney(it)}") }
                        ranked.candidate.expectedBenefit?.let { Text("Ожидаемый эффект: ${formatDecisionMoney(it)}") }
                        Text(if (ranked.feasible) "Доступно сейчас" else "Сейчас не помещается в свободную сумму")
                        Text(ranked.reason, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun formatDecisionMoney(value: MoneyAmount): String = "${"%,.0f".format(value.amount)} ${when (value.currency) {
    CurrencyCode.RUB -> "₽"
    CurrencyCode.KZT -> "₸"
    CurrencyCode.USD -> "$"
    CurrencyCode.EUR -> "€"
}}"
