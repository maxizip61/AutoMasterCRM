package ru.automaster.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.automaster.app.domain.model.CarPanels
import ru.automaster.app.ui.theme.IndigoPrimary

/** Схема авто вид-сверху: тап по зоне включает/выключает повреждение. */
@Composable
fun CarDamageMap(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        CarPanels.layout.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { zone ->
                    if (zone == null) {
                        Spacer(modifier = Modifier.weight(1f).height(44.dp))
                    } else {
                        val isSel = zone in selected
                        Surface(
                            onClick = { onToggle(zone) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) IndigoPrimary else Color(0x22FFFFFF),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    CarPanels.short[zone] ?: zone,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}