package ru.automaster.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.automaster.app.domain.model.displayName
import ru.automaster.app.ui.components.MoneyFormatter
import ru.automaster.app.ui.components.PremiumGlassBackground
import ru.automaster.app.ui.components.ResourceCard
import ru.automaster.app.ui.components.glassCard
import ru.automaster.app.ui.theme.GraphiteDark
import ru.automaster.app.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAddOrder: () -> Unit,
    onNavigateToOrderDetails: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    PremiumGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Дашборд", fontWeight = FontWeight.Bold, color = Color.White) },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onNavigateToAddOrder, containerColor = IndigoPrimary) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "cash_box") {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GraphiteDark)
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Чистая прибыль",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                var expanded by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { expanded = true },
                                        modifier = Modifier.height(36.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        Text(state.profitPeriod.displayName(), color = Color.White)
                                    }
                                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                        ProfitPeriod.values().forEach { period ->
                                            DropdownMenuItem(
                                                text = { Text(period.displayName()) },
                                                onClick = {
                                                    viewModel.setProfitPeriod(period)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val profit = state.profitBreakdown
                            val profitColor = if (profit.netProfitCents >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)

                            Text(
                                MoneyFormatter.format(profit.netProfitCents),
                                color = profitColor,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Разбивка
                            ProfitRow("Выручка", profit.revenueCents, Color(0xFF4CAF50))
                            ProfitRow("− Материалы", -profit.materialsCents, Color.Gray)
                            ProfitRow("− Опер. расходы", -profit.operatingCents, Color.Gray)
                            ProfitRow("− ЗП мастерам", -profit.salaryPaymentsCents, Color.Gray)
                            ProfitRow("− Налоги (УСН)", -profit.taxCents, Color.Gray)

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Чистая прибыль", color = Color.White, fontWeight = FontWeight.Bold)
                                Text(
                                    MoneyFormatter.format(profit.netProfitCents),
                                    color = profitColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                item(key = "posts_title") {
                    Text("Посты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp))
                }
                item(key = "posts_row") {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.posts, key = { it.post.id }) { postState ->
                            ResourceCard(
                                postState = postState,
                                modifier = Modifier.width(260.dp),
                                onDetailsClick = { orderId -> onNavigateToOrderDetails(orderId) },
                                onAcceptClick = onNavigateToAddOrder
                            )
                        }
                    }
                }

                item(key = "in_work_title") {
                    Text("В работе (${state.inWorkOrders.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp))
                }
                if (state.inWorkOrders.isEmpty()) {
                    item(key = "in_work_empty") { Text("Нет машин в работе", color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp)) }
                } else {
                    items(state.inWorkOrders, key = { it.id }) { order ->
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).glassCard()
                                .clickable { onNavigateToOrderDetails(order.id) }.padding(16.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Заказ #${order.id}", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Статус: ${order.status.name}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }

                item(key = "waiting_title") {
                    Text("Ожидают (${state.waitingOrders.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp))
                }
                if (state.waitingOrders.isEmpty()) {
                    item(key = "waiting_empty") { Text("Нет ожидающих машин", color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp)) }
                } else {
                    items(state.waitingOrders, key = { it.id }) { order ->
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).glassCard()
                                .clickable { onNavigateToOrderDetails(order.id) }.padding(16.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Заказ #${order.id}", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Ожидает назначения", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfitRow(label: String, amountCents: Long, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(
            (if (amountCents >= 0) "+" else "") + MoneyFormatter.format(amountCents),
            color = color,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
