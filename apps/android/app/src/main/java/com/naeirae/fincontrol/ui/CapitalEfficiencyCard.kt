package com.naeirae.fincontrol.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.naeirae.fincontrol.domain.CapitalEfficiencyMetrics
import com.naeirae.fincontrol.domain.CurrencyCode
import com.naeirae.fincontrol.domain.MoneyAmount

@Composable
fun CapitalEfficiencyCard(
    metrics: CapitalEfficiencyMetrics,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Экономика вложения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            MetricRow("Нужно вложить", money(metrics.capitalRequired))
            MetricRow("Ожидаемый возврат", money(metrics.grossExpectedReturn))
            MetricRow("Прибыль до поправки на вероятность", money(metrics.expectedProfit))
            MetricRow("Ожидаемая прибыль", money(metrics.probabilityWeightedProfit))
            metrics.roiPercent?.let { MetricRow("ROI", "${"%.1f".format(it)}%") }
            metrics.probabilityWeightedRoiPercent?.let {
                MetricRow("ROI с учётом вероятности", "${"%.1f".format(it)}%")
            }
            metrics.paybackDays?.let { MetricRow("Окупаемость", "≈ ${"%.1f".format(it)} дня") }
            metrics.profitPerLockedDay?.let {
                MetricRow("Ожидаемая прибыль на день блокировки", money(MoneyAmount(it, metrics.capitalRequired.currency)))
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

private fun money(value: MoneyAmount): String = "${"%,.0f".format(value.amount)} ${when (value.currency) {
    CurrencyCode.RUB -> "₽"
    CurrencyCode.KZT -> "₸"
    CurrencyCode.USD -> "$"
    CurrencyCode.EUR -> "€"
}}"
