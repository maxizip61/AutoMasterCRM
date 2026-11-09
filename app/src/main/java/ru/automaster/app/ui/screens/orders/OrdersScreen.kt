package ru.automaster.app.ui.screens.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.automaster.app.domain.model.OrderStatus
import ru.automaster.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onNavigateToOrderDetails: (Long) -> Unit,
    onNavigateToAddOrder: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val orders by viewModel.ordersList.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Все заказы", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddOrder,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить", tint = Color.White)
            }
        }
    ) { padding ->
        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Заказов пока нет", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(orders) { item ->
                    OrderCard(item = item, onClick = { onNavigateToOrderDetails(item.order.id) })
                }
            }
        }
    }
}

@Composable
fun OrderCard(item: OrderListItem, onClick: () -> Unit) {
    val statusColor = when (item.order.status) {
        OrderStatus.ACCEPTED, OrderStatus.WAITING -> StatusWaiting
        OrderStatus.BODY -> StatusBody
        OrderStatus.PREP -> StatusPrep
        OrderStatus.PAINT -> StatusPaint
        OrderStatus.ISSUE -> StatusFree
        OrderStatus.DONE -> Color.Gray
        OrderStatus.CANCELLED -> StatusOverdue
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Цветная полоска статуса слева
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .padding(end = 12.dp)
                    .background(statusColor, shape = MaterialTheme.shapes.small)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#${item.order.id} · ${item.car?.make ?: ""} ${item.car?.model ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.car?.licensePlate ?: "Нет номера"} · ${item.client?.name ?: "Нет клиента"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Текущий статус
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = item.order.status.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}