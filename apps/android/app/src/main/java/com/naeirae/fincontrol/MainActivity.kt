package com.naeirae.fincontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.naeirae.fincontrol.domain.*
import com.naeirae.fincontrol.ui.*

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
    val showBottomBar = currentRoute == "dashboard" || currentRoute == "objects"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
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
            }
        },
    ) { outerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(outerPadding),
        ) {
            composable("dashboard") {
                DashboardScreen(onOpenScenario = { navController.navigate("scenario") })
            }
            composable("objects") {
                FinancialObjectsScreen(
                    onOpenObligation = { id -> navController.navigate("obligation/$id") },
                )
            }
            composable(
                route = "obligation/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val id = entry.arguments?.getString("id") ?: return@composable
                val vm: ObligationDetailViewModel = viewModel()
                val flow = remember(id) { vm.observe(id) }
                val state by flow.collectAsStateWithLifecycle()
                ObligationDetailScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onAssignCoverage = { source, amount, probability ->
                        vm.assignCoverage(id, source, amount, probability)
                    },
                    onRemoveCoverage = vm::removeCoverage,
                )
            }
            composable("scenario") {
                DecisionScenarioScreen(
                    candidates = demoActionCandidates(),
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onOpenScenario: () -> Unit,
) {
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
            ListItem(
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
            Button(onClick = onOpenScenario, modifier = Modifier.fillMaxWidth()) {
                Text("У меня есть свободная сумма — сравнить варианты")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun demoActionCandidates(): List<ActionCandidate> = listOf(
    ActionCandidate(
        id = "keep-liquid",
        title = "Оставить деньги ликвидными",
        required = MoneyAmount(0.0, CurrencyCode.RUB),
        liquidityCost = MoneyAmount(0.0, CurrencyCode.RUB),
        expectedBenefit = MoneyAmount(0.0, CurrencyCode.RUB),
        urgencyBonus = 15.0,
        notes = "Подходит, если впереди неопределённые обязательные расходы.",
    ),
    ActionCandidate(
        id = "debt-partial",
        title = "Частично уменьшить дорогой долг",
        required = MoneyAmount(5_000.0, CurrencyCode.RUB),
        liquidityCost = MoneyAmount(5_000.0, CurrencyCode.RUB),
        guaranteedBenefit = MoneyAmount(250.0, CurrencyCode.RUB),
        urgencyBonus = 5.0,
    ),
    ActionCandidate(
        id = "income-tool",
        title = "Вложить в действие, повышающее доход",
        required = MoneyAmount(3_000.0, CurrencyCode.RUB),
        liquidityCost = MoneyAmount(3_000.0, CurrencyCode.RUB),
        expectedBenefit = MoneyAmount(9_000.0, CurrencyCode.RUB),
        riskPenalty = 25.0,
        notes = "Ожидаемый, а не гарантированный эффект.",
    ),
)

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
