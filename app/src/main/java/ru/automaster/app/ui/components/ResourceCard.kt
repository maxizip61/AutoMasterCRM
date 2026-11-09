package ru.automaster.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.automaster.app.ui.screens.dashboard.PostState
import ru.automaster.app.ui.theme.*

@Composable
fun ResourceCard(
    postState: PostState,
    modifier: Modifier = Modifier
) {
    val isBusy = postState.activeStage != null

    val phaseColor = when (postState.activeStage?.phase?.name) {
        "BODY" -> StatusBody
        "PREP" -> StatusPrep
        "PAINT" -> StatusPaint
        else -> StatusFree
    }

    val animatedColor by animateColorAsState(
        targetValue = phaseColor,
        animationSpec = tween(300),
        label = "cardColor"
    )

    // Темная стеклянная карточка
    Box(
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
            .border(BorderStroke(1.dp, Color(0x22FFFFFF)), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Шапка карточки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (postState.post.name.name.contains("PAINT")) Icons.Default.Build else Icons.Default.Home,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = postState.post.name.name.replace("_", " "),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Цветная таблетка статуса
                Surface(
                    color = animatedColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isBusy) "В работе" else "Свободен",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Контент
            Column {
                if (isBusy) {
                    Text(
                        text = postState.activeStage?.phase?.name ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text("Машина: ожидание...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    Text(
                        text = "Готов к приему",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )
                }
            }

            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBusy) animatedColor else IndigoPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isBusy) "Детали" else "Назначить", color = Color.White)
            }
        }
    }
}