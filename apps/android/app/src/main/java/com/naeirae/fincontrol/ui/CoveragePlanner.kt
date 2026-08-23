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
import com.naeirae.fincontrol.domain.CoverageSummary
import com.naeirae.fincontrol.domain.CurrencyCode
import com.naeirae.fincontrol.domain.MoneyAmount

@Composable
fun CoveragePlanner(summary: CoverageSummary, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Покрытие обязательства", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(summary.obligation.title, style = MaterialTheme.typography.titleMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CoverageMetric("Нужно", summary.target)
                CoverageMetric("Номинально покрыто", summary.nominalCovered)
                CoverageMetric("С учётом вероятности", summary.probabilityWeightedCovered)
                CoverageMetric("Дефицит", summary.deficit)
            }
        }

        Text("Источники покрытия", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (summary.sources.isEmpty()) {
            Text("Пока не назначено ни одного источника. Это не означает, что надо направить на обязательство весь доступный баланс.")
        } else {
            summary.sources.forEach { (source, link) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(source.title, fontWeight = FontWeight.SemiBold)
                        link.amount?.let { Text(formatCoverageMoney(it)) }
                        Text("Вероятность: ${minOf(source.probabilityPercent ?: 100, link.probabilityPercent)}%")
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverageMetric(label: String, value: MoneyAmount) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(formatCoverageMoney(value), fontWeight = FontWeight.SemiBold)
    }
}

private fun formatCoverageMoney(value: MoneyAmount): String = "${"%,.0f".format(value.amount)} ${when (value.currency) {
    CurrencyCode.RUB -> "₽"
    CurrencyCode.KZT -> "₸"
    CurrencyCode.USD -> "$"
    CurrencyCode.EUR -> "€"
}}"
