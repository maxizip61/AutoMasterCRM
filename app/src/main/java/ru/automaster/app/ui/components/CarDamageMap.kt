package ru.automaster.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.sceneview.SceneView
import io.github.sceneview.math.Float3
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelNodeLoader
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import kotlinx.coroutines.isActive
import ru.automaster.app.domain.model.CarPanels
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

private data class Box3(val id: String, val x0: Float, val x1: Float, val y0: Float, val y1: Float, val z0: Float, val z1: Float)

private val ZONES = listOf(
    Box3(CarPanels.FRONT_BUMPER, 2.05f, 2.45f, -1f, 1f, 0f, 0.75f),
    Box3(CarPanels.REAR_BUMPER, -2.45f, -2.05f, -1f, 1f, 0f, 0.75f),
    Box3(CarPanels.HOOD, 0.80f, 2.10f, -1f, 1f, 0.80f, 1.15f),
    Box3(CarPanels.ROOF, -0.95f, 0.15f, -1f, 1f, 1.20f, 1.60f),
    Box3(CarPanels.TRUNK, -2.20f, -1.50f, -1f, 1f, 0.80f, 1.15f),
    Box3(CarPanels.FENDER_FL, 0.80f, 2.10f, -1f, -0.45f, 0.20f, 0.90f),
    Box3(CarPanels.FENDER_FR, 0.80f, 2.10f, 0.45f, 1f, 0.20f, 0.90f),
    Box3(CarPanels.DOOR_FL, -0.20f, 0.80f, -1f, -0.45f, 0.20f, 1.05f),
    Box3(CarPanels.DOOR_FR, -0.20f, 0.80f, 0.45f, 1f, 0.20f, 1.05f),
    Box3(CarPanels.DOOR_RL, -1.50f, -0.20f, -1f, -0.45f, 0.20f, 1.05f),
    Box3(CarPanels.DOOR_RR, -1.50f, -0.20f, 0.45f, 1f, 0.20f, 1.05f),
    Box3(CarPanels.FENDER_RL, -2.10f, -1.50f, -1f, -0.45f, 0.20f, 0.90f),
    Box3(CarPanels.FENDER_RR, -2.10f, -1.50f, 0.45f, 1f, 0.20f, 0.90f)
)

private const val FOV = 45f
private val TARGET = Float3(0f, 0.7f, 0f)

private class Cam(val yaw: Float, val pitch: Float, val dist: Float, val w: Int, val h: Int) {
    val eye: Float3
    val fwd: Float3; val right: Float3; val up: Float3
    private val tanH = tan(Math.toRadians(FOV / 2.0)).toFloat()
    init {
        val cp = cos(pitch); val sp = sin(pitch); val cy = cos(yaw); val sy = sin(yaw)
        eye = Float3(TARGET.x + dist * cp * sy, TARGET.y + dist * sp, TARGET.z + dist * cp * cy)
        fwd = norm3(sub3(TARGET, eye)); right = norm3(cross3(fwd, Float3(0f, 1f, 0f))); up = cross3(right, fwd)
    }
    fun ray(sx: Float, sy: Float): Float3 {
        val nx = (2f * sx / w - 1f) * tanH * (w.toFloat() / h)
        val ny = (1f - 2f * sy / h) * tanH
        return norm3(add3(fwd, add3(mul3(right, nx), mul3(up, ny))))
    }
    fun project(p: Float3): Offset? {
        val v = sub3(p, eye); val z = dot3(v, fwd)
        if (z <= 0.05f) return null
        val x = dot3(v, right) / (z * tanH * (w.toFloat() / h))
        val y = dot3(v, up) / (z * tanH)
        return Offset((x + 1f) / 2f * w, (1f - y) / 2f * h)
    }
}
private fun sub3(a: Float3, b: Float3) = Float3(a.x - b.x, a.y - b.y, a.z - b.z)
private fun add3(a: Float3, b: Float3) = Float3(a.x + b.x, a.y + b.y, a.z + b.z)
private fun mul3(a: Float3, k: Float) = Float3(a.x * k, a.y * k, a.z * k)
private fun dot3(a: Float3, b: Float3) = a.x * b.x + a.y * b.y + a.z * b.z
private fun cross3(a: Float3, b: Float3) = Float3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)
private fun norm3(a: Float3): Float3 { val l = kotlin.math.sqrt(dot3(a, a)).coerceAtLeast(1e-6f); return mul3(a, 1f / l) }

private fun hitZone(cam: Cam, sx: Float, sy: Float): String? {
    val o = cam.eye; val d = cam.ray(sx, sy)
    var best: String? = null; var bestT = Float.MAX_VALUE
    for (b in ZONES) {
        var t0 = 0.01f; var t1 = 100f; var ok = true
        val oA = floatArrayOf(o.x, o.y, o.z); val dA = floatArrayOf(d.x, d.y, d.z)
        val mn = floatArrayOf(b.x0, b.y0, b.z0); val mx = floatArrayOf(b.x1, b.y1, b.z1)
        for (i in 0..2) {
            if (kotlin.math.abs(dA[i]) < 1e-6f) { if (oA[i] < mn[i] || oA[i] > mx[i]) { ok = false; break } }
            else {
                var ta = (mn[i] - oA[i]) / dA[i]; var tb = (mx[i] - oA[i]) / dA[i]
                if (ta > tb) { val t = ta; ta = tb; tb = t }
                t0 = maxOf(t0, ta); t1 = minOf(t1, tb)
                if (t0 > t1) { ok = false; break }
            }
        }
        if (ok && t0 < bestT) { bestT = t0; best = b.id }
    }
    return best
}

/** GLB-рендер E39 (SceneView): орбита, зум, тап-выбор зон, маркеры. Сигнатура прежняя. */
@Composable
fun CarDamageMap(selected: Set<String>, onToggle: (String) -> Unit, modifier: Modifier = Modifier) {
    val engine = rememberEngine()
    val view = rememberView(engine)
    val scene = rememberScene(view)
    val camera = rememberCameraNode(scene)
    val loader = rememberModelNodeLoader()

    var yawT by remember { mutableStateOf(-38f) }; var pitchT by remember { mutableStateOf(12f) }
    var distT by remember { mutableStateOf(6.5f) }; var yawC by remember { mutableStateOf(-38f) }
    var pitchC by remember { mutableStateOf(12f) }; var distC by remember { mutableStateOf(6.5f) }
    var spin by remember { mutableStateOf(false) }; var t by remember { mutableStateOf(0f) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var lastTap by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching {
            val m = ModelNode(loader, "models/car.glb")
            scene.addChild(m)
        }.onFailure { loadError = true }
    }
    LaunchedEffect(yawC, pitchC, distC) {
        val cp = cos(pitchC); val sp = sin(pitchC); val cy = cos(yawC); val sy = sin(yawC)
        camera.position = Float3(TARGET.x + distC * cp * sy, TARGET.y + distC * sp, TARGET.z + distC * cp * cy)
        runCatching { camera.lookAt(TARGET) }
    }
    LaunchedEffect(spin) { while (isActive) { withFrameNanos { if (spin) yawT += 0.10f; yawC += (yawT - yawC) * 0.16f; pitchC += (pitchT - pitchC) * 0.16f; distC += (distT - distC) * 0.16f; t += 0.016f } } }

    val presets = listOf("3/4" to Pair(-38f, 12f), "Слева" to Pair(0f, 4f), "Справа" to Pair(180f, 4f), "Спереди" to Pair(-90f, 4f), "Сзади" to Pair(90f, 4f), "Сверху" to Pair(-25f, 80f))

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF10141C)).onSizeChanged { size = it }) {
            SceneView(scene = scene, cameraNode = camera, modifier = Modifier.fillMaxSize())
            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                detectTransformGestures { c, pan, zoom, _ ->
                    yawT += pan.x * 0.4f; pitchT = (pitchT - pan.y * 0.3f).coerceIn(2f, 85f); distT = (distT / zoom).coerceIn(3f, 10f); c.consume()
                }
            }.pointerInput(Unit) {
                detectTapGestures { off ->
                    if (size.width == 0) return@detectTapGestures
                    val cam = Cam(yawC, pitchC, distC, size.width, size.height)
                    hitZone(cam, off.x, off.y)?.let { z -> lastTap = z; onToggle(z) }
                }
            }) {
                if (size.width > 0) {
                    val cam = Cam(yawC, pitchC, distC, size.width, size.height)
                    val pulse = 11f + 2.5f * sin(t * 4f)
                    for (z in selected) {
                        val b = ZONES.firstOrNull { it.id == z } ?: continue
                        val corners = listOf(
                            Float3(b.x0, b.y0, b.z0), Float3(b.x1, b.y0, b.z0), Float3(b.x1, b.y1, b.z0), Float3(b.x0, b.y1, b.z0),
                            Float3(b.x0, b.y0, b.z1), Float3(b.x1, b.y0, b.z1), Float3(b.x1, b.y1, b.z1), Float3(b.x0, b.y1, b.z1)
                        ).mapNotNull { cam.project(it) }
                        if (corners.size >= 4) {
                            val xs = corners.map { it.x }; val ys = corners.map { it.y }
                            val r = Rect(xs.min(), ys.min(), xs.max(), ys.max())
                            drawRoundRect(Color(0x33FF4D3D), r.topLeft, r.size, androidx.compose.ui.geometry.CornerRadius(10f, 10f))
                            drawRoundRect(Color(0xFFFF4D3D), r.topLeft, r.size, androidx.compose.ui.geometry.CornerRadius(10f, 10f), style = Stroke(2f))
                        }
                        val anchor = cam.project(Float3((b.x0 + b.x1) / 2f, (b.y0 + b.y1) / 2f, b.z1))
                        if (anchor != null) {
                            drawCircle(Color(0xFFFF4D3D).copy(alpha = 0.25f), pulse + 6f, anchor)
                            drawCircle(Color(0xFFFF4D3D), pulse, anchor, style = Stroke(3f))
                            drawCircle(Color(0xFFFF4D3D), 4.5f, anchor)
                        }
                    }
                }
            }
            Text(
                if (loadError) "car.glb не найден в assets/models" else "${selected.size} зон отмечено",
                color = if (loadError) Color(0xFFFF8A7A) else Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x66000000)).padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        if (selected.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            selected.forEach { z -> FilterChip(selected = true, onClick = { onToggle(z) }, label = { Text(CarPanels.short[z] ?: z) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0x33FF4D3D), selectedLabelColor = Color(0xFFFF8A7A))) }
        }
        Text(lastTap?.let { CarPanels.labels[it] ?: it } ?: "Свайп — поворот · щипок — зум · тап по детали — отметка", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.forEach { (l, a) -> FilterChip(selected = false, onClick = { yawT = a.first; pitchT = a.second }, label = { Text(l) }) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Дистанция", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(distT, { distT = it }, valueRange = 3f..10f, modifier = Modifier.weight(1f).padding(horizontal = 6.dp))
            Text("Авто", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Switch(spin, { spin = it })
            IconButton(onClick = { yawT = -38f; pitchT = 12f; distT = 6.5f }) { Icon(Icons.Default.Refresh, contentDescription = "Сброс", tint = MaterialTheme.colorScheme.onSurface) }
            IconButton(onClick = { selected.forEach(onToggle) }) { Icon(Icons.Default.Delete, contentDescription = "Снять отметки", tint = Color(0xFFFF4D3D)) }
        }
    }
}