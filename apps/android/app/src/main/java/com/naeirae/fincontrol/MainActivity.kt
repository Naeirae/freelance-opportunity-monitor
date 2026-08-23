package com.naeirae.fincontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.naeirae.fincontrol.domain.CurrencyCode
import com.naeirae.fincontrol.domain.MoneyAmount
import com.naeirae.fincontrol.ui.DashboardViewModel
import com.naeirae.fincontrol.ui.FinancialObjectsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FinancialControlApp()
                }
            }
        }
    }
}

@Composable
private fun FinancialControlApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "dashboard"

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "dashboard",
                    onClick = {
                        navController.navigate("dashboard") {
                            launchSingleTop = true
                            popUpTo("dashboard") { inclusive = false }
                        }
                    },
                    icon = { Text("Ф") },
                    label = { Text("Главная") },
                )
                NavigationBarItem(
                    selected = currentRoute == "objects",
                    onClick = { navController.navigate("objects") { launchSingleTop = true } },
                    icon = { Text("О") },
                    label = { Text("Объекты") },
                )
            }
        },
    ) { outerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(outerPadding),
        ) {
            composable("dashboard") { DashboardScreen() }
            composable("objects") { FinancialObjectsScreen() }
        }
    }
}

@Composable
private fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Text("Финансовый центр", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Крупные деньги, обязательства и решения", style = MaterialTheme.typography.bodyMedium)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Доступно", state.available, Modifier.weight(1f))
                MetricCard("Защищено", state.protected, Modifier.weight(1f))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Свободно для решений", style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatMoney(state.allocatable),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Это максимум, который можно распределять без нарушения защищённой ликвидности.")
                }
            }
        }

        item { SectionTitle("Ближайшие обязательства") }
        items(state.obligations) { obligation ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(obligation.name, fontWeight = FontWeight.SemiBold)
                    Text("Долг: ${formatMoney(obligation.balance)}")
                    obligation.nextPayment?.let { Text("Следующий платёж: ${formatMoney(it)}") }
                    obligation.nextDueDate?.let { Text("Срок: $it") }
                    obligation.graceUntil?.let { Text("Льготный период до: $it") }
                }
            }
        }

        item { SectionTitle("Ожидаемые деньги") }
        items(state.expectedIncome) { income ->
            androidx.compose.material3.ListItem(
                headlineContent = { Text(income.name) },
                supportingContent = { Text("Вероятность: ${income.probabilityPercent}%${income.expectedDate?.let { ", ожидается $it" } ?: ""}") },
                trailingContent = { Text(formatMoney(income.amount), fontWeight = FontWeight.SemiBold) },
            )
            HorizontalDivider()
        }

        item { SectionTitle("Требует внимания") }
        items(state.attentionItems) { item ->
            Text("• $item", style = MaterialTheme.typography.bodyLarge)
        }

        item {
            Spacer(Modifier.height(12.dp))
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Быстрое финансовое действие")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MetricCard(title: String, amount: MoneyAmount, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(formatMoney(amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

private fun formatMoney(value: MoneyAmount): String {
    val symbol = when (value.currency) {
        CurrencyCode.RUB -> "₽"
        CurrencyCode.KZT -> "₸"
        CurrencyCode.USD -> "$"
        CurrencyCode.EUR -> "€"
    }
    return "${"%,.0f".format(value.amount)} $symbol"
}
