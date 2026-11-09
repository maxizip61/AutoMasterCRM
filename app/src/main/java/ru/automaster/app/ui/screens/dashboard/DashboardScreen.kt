package ru.automaster.app.ui.screens.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ru.automaster.app.R
import ru.automaster.app.ui.components.MoneyFormatter
import ru.automaster.app.ui.components.ResourceCard
import ru.automaster.app.ui.theme.GraphiteDark
import ru.automaster.app.ui.theme.IndigoPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAddOrder: () -> Unit,
    onNavigateToOrderDetails: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. ФОН: Размытая картинка машины (Убедись, что bg_car.jpg есть в res/drawable)
        Image(
            painter = painterResource(id = R.drawable.bg_car),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(16.dp)
        )

        // 2. ОВЕРЛЕЙ: Затемнение
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GraphiteDark.copy(alpha = 0.75f))
        )

        // 3. КОНТЕНТ
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Дашборд", fontWeight = FontWeight.Bold, color = Color.White) },
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
                // Блок 1: Касса дня
                item {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(GraphiteDark)
                            .padding(24.dp)
                    ) {
                        Column {
                            Text("Касса дня", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Приход", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    Text(MoneyFormatter.format(state.dailyCashInCents), color = Color.White, style = MaterialTheme.typography.titleMedium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Расход", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    Text(MoneyFormatter.format(state.dailyCashOutCents), color = Color.White, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Чистыми сегодня", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            Text(
                                text = MoneyFormatter.format(state.dailyCashInCents - state.dailyCashOutCents),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Блок 2: Посты
                item {
                    Text("Посты", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp))
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.posts) { postState ->
                            ResourceCard(postState = postState, modifier = Modifier.width(280.dp))
                        }
                    }
                }

                // Блок 3: В работе
                item {
                    Text("В работе (${state.inWorkOrders.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp))
                }
                if (state.inWorkOrders.isEmpty()) {
                    item { Text("Нет машин в работе", color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp)) }
                } else {
                    items(state.inWorkOrders) { order ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                                .clickable { onNavigateToOrderDetails(order.id) }
                                .padding(16.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Заказ #${order.id}", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Статус: ${order.status.name}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }

                // Блок 4: Ожидают
                item {
                    Text("Ожидают (${state.waitingOrders.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp))
                }
                if (state.waitingOrders.isEmpty()) {
                    item { Text("Нет ожидающих машин", color = Color.Gray, modifier = Modifier.padding(horizontal = 16.dp)) }
                } else {
                    items(state.waitingOrders) { order ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                                .clickable { onNavigateToOrderDetails(order.id) }
                                .padding(16.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Заказ #${order.id}", fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Ожидает назначения", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}