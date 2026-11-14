package ru.automaster.app.ui.screens.taxes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.automaster.app.domain.model.TaxPaymentKind
import ru.automaster.app.ui.components.MoneyFormatter
import ru.automaster.app.ui.components.RowLabelValue // <-- ДОБАВЛЕН ИМПОРТ
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxScreen(
    viewModel: TaxesViewModel = hiltViewModel()
) {
    val payments by viewModel.taxPayments.collectAsState()
    val config by viewModel.config.collectAsState()
    val report by viewModel.reportState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Налоги", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. НОВЫЙ БЛОК С РАСЧЕТОМ НАЛОГА
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Расчет налога (с 1 января)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(12.dp))

                        RowLabelValue("Доход:", MoneyFormatter.format(report.incomeCents))
                        RowLabelValue("Начислено (${report.taxRatePercent}%):", MoneyFormatter.format(report.accruedCents), MaterialTheme.colorScheme.onSurface)
                        RowLabelValue("Вычет (взносы):", "-${MoneyFormatter.format(report.deductionCents)}", MaterialTheme.colorScheme.primary)
                        RowLabelValue("Уплачено авансов:", "-${MoneyFormatter.format(report.advancesCents)}", MaterialTheme.colorScheme.tertiary)

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

                        RowLabelValue(
                            label = "К УПЛАТЕ:",
                            value = MoneyFormatter.format(report.payableCents),
                            valueColor = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // 2. БЛОК С НАСТРОЙКАМИ
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Налоговый режим: УСН ${config.taxRatePercent}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Взносы ИП (год): ${MoneyFormatter.format(config.insuranceContributionsCents)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // 3. ИСТОРИЯ ПЛАТЕЖЕЙ
            item {
                Text("История платежей", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }

            if (payments.isEmpty()) {
                item {
                    Text("Платежей пока нет", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                }
            } else {
                items(payments, key = { it.id }) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val kindName = when (payment.kind) {
                                    TaxPaymentKind.USN_TAX -> "Налог УСН"
                                    TaxPaymentKind.IP_FIXED_CONTRIBUTIONS -> "Взносы ИП (фикс)"
                                    TaxPaymentKind.IP_EXTRA_CONTRIBUTIONS -> "Взносы ИП (доп)"
                                }
                                Text(kindName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault()).format(payment.date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = MoneyFormatter.format(payment.amountCents),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // ДИАЛОГ ДОБАВЛЕНИЯ
    if (showAddDialog) {
        var amount by remember { mutableStateOf("") }
        var comment by remember { mutableStateOf("") }
        var selectedKind by remember { mutableStateOf(TaxPaymentKind.USN_TAX) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Добавить налоговый платеж") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedKind == TaxPaymentKind.USN_TAX,
                            onClick = { selectedKind = TaxPaymentKind.USN_TAX },
                            label = { Text("УСН") }
                        )
                        FilterChip(
                            selected = selectedKind == TaxPaymentKind.IP_FIXED_CONTRIBUTIONS,
                            onClick = { selectedKind = TaxPaymentKind.IP_FIXED_CONTRIBUTIONS },
                            label = { Text("Фикс. взносы") }
                        )
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it.filter { c -> c.isDigit() } },
                        label = { Text("Сумма в рублях") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Комментарий") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    amount.toLongOrNull()?.let { viewModel.addTaxPayment(it, selectedKind, comment) }
                    showAddDialog = false
                }) { Text("Добавить") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Отмена") }
            }
        )
    }
}