package ru.automaster.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.automaster.app.R
import ru.automaster.app.ui.theme.GraphiteDark
import ru.automaster.app.ui.theme.IndigoPrimary

/**
 * Фон с эффектом Glassmorphism. Использует заранее размытую картинку для производительности.
 */
@Composable
fun PremiumGlassBackground(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_car),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GraphiteDark.copy(alpha = 0.85f)) // Чуть темнее, так как картинка теперь без блюра
        )
        content()
    }
}

/**
 * Модификатор для стеклянных карточек.
 */
fun Modifier.glassCard(): Modifier = this
    .clip(RoundedCornerShape(16.dp))
    .background(Color(0x1AFFFFFF))
    .border(BorderStroke(1.dp, Color(0x22FFFFFF)), RoundedCornerShape(16.dp))
    .graphicsLayer { alpha = 0.99f } // Форсируем аппаратное ускорение

/**
 * Премиальная кнопка с тактильным откликом (вибрацией).
 */
@Composable
fun PrimaryGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
        enabled = enabled
    ) {
        Text(text = text, color = Color.White)
    }
}