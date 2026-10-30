package com.xex.oneineight

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

// ---------- Тема ----------
class Pal(val dark: Boolean, val bg: Color, val card: Color, val line: Color, val ink: Color, val mute: Color,
          val cr: Color, val tl: Color, val gold: Color, val grn: Color)

val LightPal = Pal(false, Color(0xFFE8EEE9), Color(0xFFF8FAF6), Color(0xFFC3CEC5), Color(0xFF131F2B), Color(0xFF6D7B80),
    Color(0xFF8C1D3C), Color(0xFF1C6B62), Color(0xFF9A7A2A), Color(0xFF3F7F55))
val DarkPal = Pal(true, Color(0xFF0D131A), Color(0xFF16202A), Color(0xFF2B3948), Color(0xFFE7ECF1), Color(0xFF8FA1B3),
    Color(0xFFE0557A), Color(0xFF52B9AA), Color(0xFFD1A94B), Color(0xFF5BB27A))
val LocalPal = staticCompositionLocalOf { LightPal }
val LocalAnim = staticCompositionLocalOf { true }

@Composable
fun P(): Pal = LocalPal.current

@Composable
fun T(s: String, size: Int = 14, col: Color = P().ink, bold: Boolean = false, mono: Boolean = false,
      serif: Boolean = false, align: TextAlign? = null, modifier: Modifier = Modifier, lh: Int = 0) {
    Text(
        s, modifier, color = col, fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontFamily = if (mono) FontFamily.Monospace else if (serif) FontFamily.Serif else FontFamily.Default,
        textAlign = align, lineHeight = if (lh > 0) lh.sp else TextUnit.Unspecified
    )
}

/** Карточка с «пиксельными» срезанными углами. */
fun Modifier.pcard(bg: Color, ln: Color): Modifier = drawBehind {
    val e = 3.dp.toPx(); val b = 1.5.dp.toPx(); val w = size.width; val h = size.height
    drawRect(ln, Offset(e, 0f), Size(w - 2 * e, h))
    drawRect(ln, Offset(0f, e), Size(w, h - 2 * e))
    drawRect(bg, Offset(b, b + e / 2), Size(w - 2 * b, h - 2 * b - e))
    drawRect(bg, Offset(b + e / 2, b), Size(w - 2 * b - e, h - 2 * b))
}

@Composable
fun PCard(modifier: Modifier = Modifier, pad: Int = 12, content: @Composable ColumnScope.() -> Unit) {
    val p = P()
    Column(modifier.fillMaxWidth().padding(vertical = 5.dp).pcard(p.card, p.line).padding((pad + 3).dp), content = content)
}

@Composable
fun PBtn(label: String, modifier: Modifier = Modifier, on: Boolean = false, primary: Boolean = false, onClick: () -> Unit) {
    val p = P()
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.94f else 1f, tween(80), label = "btn")
    val bg = if (primary) p.cr else if (on) p.ink else p.card
    val fg = if (primary) Color.White else if (on) p.bg else p.ink
    Box(
        modifier.graphicsLayer { scaleX = sc; scaleY = sc }.pcard(bg, if (primary) p.cr else p.line)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) { T(label, 13, fg, bold = primary || on, mono = true) }
}

@Composable
fun Bar(v: Float, col: Color, h: Int = 7) {
    val p = P()
    val a by animateFloatAsState(v.coerceIn(0f, 1f), tween(500), label = "bar")
    Box(Modifier.fillMaxWidth().height(h.dp).background(p.line.copy(alpha = 0.5f))) {
        Box(Modifier.fillMaxWidth(a).fillMaxHeight().background(col))
    }
}
@Composable
fun RawBar(v: Float, col: Color, h: Int = 4) {
    val p = P()
    Box(Modifier.fillMaxWidth().height(h.dp).background(p.line.copy(alpha = 0.5f))) {
        Box(Modifier.fillMaxWidth(v.coerceIn(0f, 1f)).fillMaxHeight().background(col))
    }
}

@Composable
fun KV(k: String, v: String) {
    val p = P()
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        T(k, 13, p.mute, modifier = Modifier.weight(1f))
        T(v, 13, align = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

// ---------- Пиксельные сцены ----------
class Pp(val x: Float, val shirt: Color, val skin: Color, val hp: Float, val hair: Color = Color(0xFF2B1D14))
val SKIN = listOf(Color(0xFFF1C9A5), Color(0xFFD9A679), Color(0xFFB57A50), Color(0xFF8A5A3C), Color(0xFF5E3A26))

fun people(L: Life): List<Pp> {
    val a = L.age; val k = SKIN[L.sk]; val o = ArrayList<Pp>(); var x = 36f
    fun add(s: Long, hp: Float, sk: Color = k, hr: Color = Color(0xFF2B1D14)) { o.add(Pp(x, Color(s), sk, max(7f, hp), hr)); x += 9f }
    if (a < 18) {
        add(0xFF7A6A9C, 12f); add(0xFF4D6F8A, 12f)
        for (i in 0 until min(L.sib, 5)) add(0xFFA2866A, 8f + i % 3)
        add(0xFF8A5A8C, 4f + a * 0.45f)
    } else {
        add(0xFF2F7A8A, 11f, k, if (a > 62) Color(0xFFD8D8D8) else Color(0xFF2B1D14))
        if (L.pt != null) add(0xFFA0506A, 10f, SKIN[(L.sk + 1) % 5])
        L.kids.take(4).forEach { add(0xFFC9A24A, min(10f, 5f + (a - it) * 0.5f)) }
    }
    return o
}

@Composable
fun Scene(type: String, ppl: List<Pp>, seed: Int, modifier: Modifier = Modifier) {
    val p = P(); val anim = LocalAnim.current
    val inf = rememberInfiniteTransition(label = "scene")
    val tt by inf.animateFloat(0f, 1000f, infiniteRepeatable(tween(1_000_000, easing = LinearEasing)), label = "t")
    val t = if (anim) tt else 0f
    val night = p.dark
    Canvas(modifier.fillMaxWidth().aspectRatio(160f / 56f)) {
        val ps = size.width / 160f
        fun r(c: Color, x: Float, y: Float, w: Float, h: Float) { drawRect(c, Offset(x * ps, y * ps), Size(w * ps, h * ps)) }
        val rd = Random(seed)
        if (type == "home") {
            r(if (night) Color(0xFF1D2530) else Color(0xFFECEEE9), 0f, 0f, 160f, 56f)
            r(if (night) Color(0xFF2A323E) else Color(0xFFD8DBD5), 0f, 38f, 160f, 3f)
            r(if (night) Color(0xFF4A3A2E) else Color(0xFF9C7A5E), 0f, 41f, 160f, 15f)
            for (i in 0 until 160 step 13) r(if (night) Color(0xFF3A2E24) else Color(0xFF86664C), i.toFloat(), 41f, 1f, 15f)
            for (wx in listOf(18f, 118f)) {
                r(Color(0xFFD98A96), wx - 1, 6f, 22f, 26f)
                r(if (night) Color(0xFF16204A) else Color(0xFFBCD3E3), wx + 1, 8f, 18f, 22f)
                r(Color(0xFFD98A96), wx + 9, 8f, 1f, 22f); r(Color(0xFFD98A96), wx + 1, 18f, 18f, 1f)
                if (night) { val tw = 0.4f + 0.6f * abs(sin(t * 1.5f + wx)); r(Color.White.copy(alpha = tw), wx + 3, 11f, 1f, 1f) }
            }
            r(Color(0xFFC77A86), 60f, 50f, 40f, 4f)
        } else {
            val (c0, c1) = when {
                type == "park" || type == "mourn" -> Pair(Color(0xFF6E6BA8), Color(0xFFF2B48F))
                night -> Pair(Color(0xFF0B1330), Color(0xFF2B3A6B))
                else -> Pair(Color(0xFF8FBCE0), Color(0xFFDDEBF2))
            }
            for (i in 0 until 13) r(lerp(c0, c1, i / 12f), 0f, i * 3f, 160f, 3.1f)
            if (night && type != "park" && type != "mourn") for (k in 0 until 26) {
                val a = 0.4f + 0.6f * abs(sin(t * 1.3f + k)); r(Color.White.copy(alpha = a), (k * 37 % 160).toFloat(), (k * 13 % 30).toFloat(), 1f, 1f)
            }
            val sunC = if (night && type != "park") Color(0xFFF1EFD8) else Color(0xFFFFE08A)
            val sx = 128f; val sy = 9f
            r(sunC.copy(alpha = 0.25f), sx - 2, sy - 2, 10f, 10f); r(sunC, sx, sy - 1, 6f, 8f); r(sunC, sx - 1, sy, 8f, 6f)
            for (k in 0 until 3) {
                val cx = ((t * 4f + k * 62f) % 220f) - 30f; val cy = 6f + k * 7f
                val cc = if (night) Color(0xFF3B4A7A).copy(alpha = 0.7f) else Color.White.copy(alpha = 0.9f)
                r(cc, cx + 2, cy, 10f, 2f); r(cc, cx, cy + 2, 14f, 2f); r(cc, cx + 4, cy - 1, 5f, 1f)
            }
            for (i in 0 until 160 step 9) {
                val bh = 8f + rd.nextInt(18)
                r(if (night) Color(0xFF18233F) else Color(0xFFC3D6E2), i.toFloat(), 36f - bh, 8f, bh)
                var yy = 2f
                while (yy < bh - 2) {
                    val lit = night && ((i * 7 + yy.toInt() * 3) % 5 != 0)
                    r(if (lit) Color(0xFFF2D27A) else if (night) Color(0xFF26335C) else Color(0xFFDFEAF0), i + 2f, 36f - bh + yy, 2f, 2f)
                    yy += 4f
                }
            }
            r(if (type == "park") Color(0xFF9CBF7A) else if (night) Color(0xFF3B4046) else Color(0xFF8C9195), 0f, 36f, 160f, 20f)
            if (type == "park") {
                r(Color(0xFF6B4A2E), 52f, 33f, 22f, 2f); r(Color(0xFF6B4A2E), 53f, 35f, 1f, 3f); r(Color(0xFF6B4A2E), 72f, 35f, 1f, 3f)
                r(Color(0xFF4F6F3A), 10f, 22f, 14f, 10f); r(Color(0xFF6B4A2E), 16f, 32f, 2f, 5f)
                r(Color(0xFF3A3F45), 100f, 20f, 1f, 17f); r(Color(0xFFFFE08A), 99f, 18f, 3f, 3f)
                for (k in 0 until 2) { val hy = 26f - ((t * 5f + k * 6f) % 12f); r(Color(0xFFD9637C).copy(alpha = (hy / 26f).coerceIn(0.2f, 1f)), 62f + k * 6f, hy, 2f, 2f) }
            } else {
                for (i in 0 until 160 step 14) r(Color(0xFFE8E8E0), ((i - t * 8f) % 168f + 168f) % 168f - 4f, 47f, 7f, 1f)
                r(Color(0xFF4F6F3A), 4f, 26f, 8f, 8f); r(Color(0xFF6B4A2E), 7f, 34f, 2f, 4f)
            }
            if (type == "mourn") {
                r(Color.White, 74f, 22f, 12f, 13f); r(Color(0xFF6B4A2E), 76f, 24f, 8f, 9f)
                val fl = if (anim && sin(t * 9f) > 0) 1f else 0f
                r(Color(0xFFFFD27A), 70f, 30f - fl, 2f, 2f); r(Color(0xFFFFD27A), 88f, 30f - fl, 2f, 2f)
            }
        }
        val by = if (type == "home") 49f else 45f
        ppl.forEachIndexed { idx, q ->
            val bob = if (anim && ((t * 2f + idx).toInt() % 2 == 0)) 0f else -1f
            val top = by - q.hp + bob
            r(q.hair, q.x, top, 3f, 1f); r(q.skin, q.x, top + 1, 3f, 2f)
            r(q.shirt, q.x, top + 3, 3f, q.hp - 6)
            r(Color(0xFF222222), q.x, top + 1, 1f, 1f)
            r(Color(0xFF2F3544), q.x, by - 3 + bob, 1f, 3f); r(Color(0xFF2F3544), q.x + 2, by - 3 + bob, 1f, 3f)
        }
    }
}

@Composable
fun Portrait(L: Life, size: Int = 56) {
    val p = P()
    Canvas(Modifier.size(size.dp).background(p.line.copy(alpha = 0.4f))) {
        val ps = this.size.width / 24f
        fun r(c: Color, x: Float, y: Float, w: Float, h: Float) { drawRect(c, Offset(x * ps, y * ps), Size(w * ps, h * ps)) }
        r(if (L.sex == 1) Color(0xFF8A5A8C) else Color(0xFF2F7A8A), 4f, 17f, 16f, 7f)
        r(SKIN[L.sk], 8f, 6f, 8f, 10f)
        val hair = if (L.age > 62) Color(0xFFD8D8D8) else Color(0xFF2B1D14)
        r(hair, 8f, 4f, 8f, 3f)
        if (L.sex == 1) { r(hair, 6f, 6f, 2f, 12f); r(hair, 16f, 6f, 2f, 12f) }
        r(Color(0xFF222222), 10f, 10f, 1f, 1f); r(Color(0xFF222222), 13f, 10f, 1f, 1f)
    }
}

// ---------- Глобус ----------
@Composable
fun Globe(lat: Double, lon: Double, modifier: Modifier = Modifier, interactive: Boolean = true) {
    val p = P(); val scope = rememberCoroutineScope()
    val cl = remember { Animatable(if (lon.isNaN()) 20f else lon.toFloat()) }
    val cp = remember { Animatable(if (lat.isNaN()) 15f else lat.toFloat().coerceIn(-60f, 60f)) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(lat, lon) {
        if (!lat.isNaN() && !lon.isNaN() && !dragging) {
            val d = (((lon.toFloat() - cl.value + 540f) % 360f) + 360f) % 360f - 180f
            launch { cl.animateTo(cl.value + d, tween(650, easing = FastOutSlowInEasing)) }
            cp.animateTo(lat.toFloat().coerceIn(-60f, 60f), tween(650, easing = FastOutSlowInEasing))
        }
    }
    val inf = rememberInfiniteTransition(label = "globe")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "pulse")
    val mod = if (interactive) modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { dragging = true }, onDragEnd = { dragging = false }, onDragCancel = { dragging = false }
        ) { ch, d ->
            ch.consume()
            scope.launch { cl.snapTo(cl.value - d.x * 0.35f); cp.snapTo((cp.value + d.y * 0.3f).coerceIn(-75f, 75f)) }
        }
    } else modifier
    val landCol = if (p.dark) Color(0xFF7FC3B3) else Color(0xFFE6EFDD)
    val hiCol = p.cr
    Canvas(mod) {
        val r = size.minDimension / 2f * 0.9f
        val cx = size.width / 2f; val cy = size.height / 2f
        val l0 = Math.toRadians(cl.value.toDouble()); val p0 = Math.toRadians(cp.value.toDouble())
        val sp0 = sin(p0); val cp0 = cos(p0)
        drawCircle(p.tl.copy(alpha = 0.12f), r * 1.12f, Offset(cx, cy))
        drawCircle(p.tl.copy(alpha = 0.2f), r * 1.05f, Offset(cx, cy))
        drawCircle(
            Brush.radialGradient(listOf(Color(0xFF2E7F8A), Color(0xFF123A4A)), Offset(cx - r * 0.35f, cy - r * 0.35f), r * 1.7f),
            r, Offset(cx, cy)
        )
        val hasT = !lat.isNaN() && !lon.isNaN()
        val tla = Math.toRadians(lat); val tlo = Math.toRadians(lon)
        val stl = sin(tla); val ctl = cos(tla)
        for (pt in Land.pts) {
            val la = pt[0]; val lo = pt[1] - l0
            val cosc = sp0 * pt[2] + cp0 * pt[3] * cos(lo)
            if (cosc <= 0.02) continue
            val x = cx + r * pt[3] * sin(lo); val y = cy - r * (cp0 * pt[2] - sp0 * pt[3] * cos(lo))
            val d = r * 0.017f * (0.55f + cosc.toFloat() * 0.7f)
            val hi = hasT && (stl * pt[2] + ctl * pt[3] * cos(pt[1] - tlo)) > 0.9925
            drawRect(if (hi) hiCol else landCol.copy(alpha = 0.35f + 0.65f * cosc.toFloat()), Offset(x.toFloat() - d, y.toFloat() - d), Size(d * 2, d * 2))
        }
        if (hasT) {
            val lo = tlo - l0
            val cosc = sp0 * stl + cp0 * ctl * cos(lo)
            if (cosc > 0) {
                val x = (cx + r * ctl * sin(lo)).toFloat(); val y = (cy - r * (cp0 * stl - sp0 * ctl * cos(lo))).toFloat()
                drawCircle(hiCol.copy(alpha = 1f - pulse), r * (0.03f + 0.09f * pulse), Offset(x, y), style = Stroke(2.dp.toPx()))
                drawRect(Color.White, Offset(x - 3.dp.toPx(), y - 3.dp.toPx()), Size(6.dp.toPx(), 6.dp.toPx()))
                drawRect(hiCol, Offset(x - 2.dp.toPx(), y - 2.dp.toPx()), Size(4.dp.toPx(), 4.dp.toPx()))
            }
        }
    }
}

// ---------- Иконки управления (рисуются, без эмодзи) ----------
@Composable
fun PlayPauseIcon(paused: Boolean, col: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(20.dp)) {
        val w = size.width; val h = size.height
        if (paused) {
            val pa = Path().apply { moveTo(w * 0.22f, h * 0.1f); lineTo(w * 0.9f, h * 0.5f); lineTo(w * 0.22f, h * 0.9f); close() }
            drawPath(pa, col)
        } else {
            drawRect(col, Offset(w * 0.15f, h * 0.1f), Size(w * 0.25f, h * 0.8f))
            drawRect(col, Offset(w * 0.6f, h * 0.1f), Size(w * 0.25f, h * 0.8f))
        }
    }
}
@Composable
fun FfIcon(col: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(20.dp)) {
        val w = size.width; val h = size.height
        for (k in 0..1) {
            val o = k * w * 0.42f
            drawPath(Path().apply { moveTo(w * 0.05f + o, h * 0.15f); lineTo(w * 0.5f + o, h * 0.5f); lineTo(w * 0.05f + o, h * 0.85f); close() }, col)
        }
    }
}
@Composable
fun IconBox(on: Boolean = false, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val p = P()
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.9f else 1f, tween(80), label = "ib")
    Box(
        Modifier.size(42.dp).graphicsLayer { scaleX = sc; scaleY = sc }.pcard(if (on) p.ink else p.card, p.line)
            .clickable(interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center, content = content
    )
}
