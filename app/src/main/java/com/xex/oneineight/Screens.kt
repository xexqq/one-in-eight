package com.xex.oneineight

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlin.math.*

fun f1(x: Double) = "%.1f".format(x).replace('.', ',')

// ---------- корень ----------
@Composable
fun App(vm: GameVM) {
    val dark = when (vm.theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val pal = if (dark) DarkPal else LightPal
    val view = LocalView.current
    SideEffect {
        val w = (view.context as? Activity)?.window
        if (w != null) WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !dark
    }
    CompositionLocalProvider(LocalPal provides pal, LocalAnim provides vm.anim) {
        BackHandler(enabled = vm.screen != Scr.HOME) { vm.onBack() }
        Box(Modifier.fillMaxSize().background(pal.bg).windowInsetsPadding(WindowInsets.systemBars)) {
            Crossfade(targetState = vm.screen, animationSpec = tween(260), label = "screen") { s ->
                when (s) {
                    Scr.HOME -> HomeScreen(vm)
                    Scr.SETTINGS -> SettingsScreen(vm)
                    Scr.ROULETTE -> RouletteScreen(vm)
                    Scr.BIRTH -> BirthScreen(vm)
                    Scr.LIFE -> LifeScreen(vm)
                    Scr.SLIDES -> SlidesScreen(vm)
                    Scr.RECORD -> RecordScreen(vm)
                }
            }
        }
    }
}

@Composable
fun Chips(opts: List<String>, sel: Int, onSel: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opts.forEachIndexed { i, s -> PBtn(s, on = i == sel) { onSel(i) } }
    }
}

@Composable
fun SwitchRow(title: String, sub: String, v: Boolean, on: (Boolean) -> Unit) {
    val p = P()
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { T(title, 14, bold = true); T(sub, 11, p.mute) }
        Switch(
            checked = v, onCheckedChange = on,
            colors = SwitchDefaults.colors(
                checkedTrackColor = p.cr, checkedThumbColor = Color.White,
                uncheckedTrackColor = p.line, uncheckedThumbColor = p.mute, uncheckedBorderColor = p.line
            )
        )
    }
}

@Composable
fun SettingsBody(vm: GameVM) {
    val p = P()
    PCard { T("ТЕМА", 11, p.mute, mono = true); Spacer(Modifier.height(6.dp)); Chips(listOf("Системная", "Светлая", "Тёмная"), vm.theme) { vm.setTheme(it) } }
    PCard {
        T("ДЛИТЕЛЬНОСТЬ ОДНОЙ ЖИЗНИ", 11, p.mute, mono = true); Spacer(Modifier.height(6.dp))
        Chips(listOf("~30 мин", "~1 ч", "~1,5 ч"), listOf(30, 60, 90).indexOf(vm.len)) { vm.setLen(listOf(30, 60, 90)[it]) }
        T("Столько времени уйдёт на жизнь средней продолжительности. Действует сразу.", 11, p.mute)
    }
    PCard {
        T("КАК ВЫБИРАЕТСЯ СТРАНА", 11, p.mute, mono = true); Spacer(Modifier.height(6.dp))
        Chips(listOf("По числу рождений", "По населению"), vm.mode) { vm.setMode(it) }
    }
    PCard {
        SwitchRow("Авторежим", "Решения выбираются сами, первый вариант", vm.auto) { vm.setAuto(it) }
        SwitchRow("Скрывать окна решений", "Решения принимаются без окон", vm.hide) { vm.setHide(it) }
        SwitchRow("Анимации", "Движение в сценах и на глобусе", vm.anim) { vm.setAnim(it) }
    }
    PCard {
        T("О ПРОЕКТЕ", 11, p.mute, mono = true)
        T("Статистика: Всемирный банк (WDI)${if (vm.real) "" else ", демо-набор"}. Стран в базе: ${vm.countries.size}. Игровая модель, а не точный прогноз.", 12, p.mute)
    }
}

@Composable
fun SettingsScreen(vm: GameVM) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PBtn("← Назад") { vm.closeSettings() }; Spacer(Modifier.width(10.dp)); T("Настройки", 22, serif = true, bold = true)
        }
        Spacer(Modifier.height(8.dp))
        SettingsBody(vm)
    }
}

// ---------- главный экран ----------
@Composable
fun HomeScreen(vm: GameVM) {
    val p = P()
    val share = remember(vm.mode, vm.countries) {
        val w = vm.countries.map { if (vm.mode == 0) it.b else it.p }; val t = w.sum()
        vm.countries.mapIndexed { i, c -> Pair(c, w[i] / t * 100) }.sortedByDescending { it.second }
    }
    val withPos = remember(vm.countries) { vm.countries.filter { it.hasPos } }
    var gl by remember { mutableStateOf(withPos.randomOrNull()) }
    LaunchedEffect(withPos) { while (true) { delay(3500); gl = withPos.randomOrNull() } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            T("v0.2 · симуляция жизни · ${vm.countries.size} стран${if (vm.real) "" else " (демо)"}", 11, p.mute, mono = true, modifier = Modifier.weight(1f))
            IconBox(onClick = { vm.openSettings() }) { Icon(Icons.Filled.Settings, null, tint = p.ink, modifier = Modifier.size(22.dp)) }
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
            T("1", 84, p.cr, bold = true, serif = true)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.padding(bottom = 10.dp)) { T("из 8", 34, bold = true, serif = true); T("миллиардов", 34, bold = true, serif = true) }
        }
        Globe(gl?.lat ?: Double.NaN, gl?.lon ?: Double.NaN, Modifier.align(Alignment.CenterHorizontally).size(250.dp))
        T("Прямо сейчас каждую секунду на свет появляются около четырёх младенцев. Вы становитесь одним из них и проживаете эту единственную жизнь до самого конца. Где вы родитесь и в чьей семье — выбрать нельзя.", 14, p.mute, lh = 20)
        PCard {
            T("ЕСЛИ БЫ ВЫ РОДИЛИСЬ СЕЙЧАС", 11, p.mute, mono = true); Spacer(Modifier.height(6.dp))
            share.take(7).forEach { (c, pc) ->
                Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    T(c.n, 13, modifier = Modifier.width(96.dp), lh = 15)
                    Box(Modifier.weight(1f)) { Bar((pc / 20).toFloat(), p.tl, 8) }
                    T(f1(pc) + "%", 11, p.mute, mono = true, align = TextAlign.End, modifier = Modifier.width(52.dp))
                }
            }
        }
        PCard {
            T("КАК ВЫБИРАЕТСЯ СТРАНА", 11, p.mute, mono = true); Spacer(Modifier.height(6.dp))
            Chips(listOf("По числу рождений", "По населению"), vm.mode) { vm.setMode(it) }
            Spacer(Modifier.height(8.dp))
            T("ДЛИТЕЛЬНОСТЬ ЖИЗНИ", 11, p.mute, mono = true); Spacer(Modifier.height(6.dp))
            Chips(listOf("~30 мин", "~1 ч", "~1,5 ч"), listOf(30, 60, 90).indexOf(vm.len)) { vm.setLen(listOf(30, 60, 90)[it]) }
        }
        PBtn("Родиться", Modifier.fillMaxWidth(), primary = true) { vm.startBirth() }
        Spacer(Modifier.height(10.dp))
        T("Тяжёлые события — смерть, болезни, бедность — происходят здесь так же часто, как в реальной статистике, но без сцен. Если тема отозвалась тяжело, поговорите с близкими или обратитесь на линию помощи в вашей стране. От 15 лет.", 11, p.mute, lh = 15)
    }
}

@Composable
fun RouletteScreen(vm: GameVM) {
    val p = P()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        T("ОПРЕДЕЛЯЕТСЯ МЕСТО РОЖДЕНИЯ", 11, p.mute, mono = true)
        Globe(vm.rLat, vm.rLon, Modifier.size(290.dp), interactive = false)
        T(vm.rName, 32, serif = true, bold = true, align = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        T(vm.rProb, 12, p.mute, mono = true)
    }
}

@Composable
fun BirthScreen(vm: GameVM) {
    val L = vm.life ?: return
    val p = P(); val c = L.home
    var appear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appear = true }
    val wle = remember(vm.countries) { vm.countries.sumOf { (it.m + it.f) / 2 * it.b } / vm.countries.sumOf { it.b } }
    val wu = remember(vm.countries) { vm.countries.sumOf { it.u * it.b } / vm.countries.sumOf { it.b } }
    val ws = remember(vm.countries) { vm.countries.sumOf { it.s * it.b } / vm.countries.sumOf { it.b } }
    val wg = remember(vm.countries) { vm.countries.sumOf { it.g * it.b } / vm.countries.sumOf { it.b } }
    AnimatedVisibility(visible = appear, enter = fadeIn(tween(350)) + slideInVertically(tween(450)) { it / 8 }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
            Row(Modifier.fillMaxWidth().pcard(p.cr, p.cr).padding(horizontal = 14.dp, vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                T("Регистрация рождения", 14, Color.White, bold = true, serif = true)
                T("No. ${c.n.take(3).uppercase()}-2026-${L.mo}${L.fa}", 10, Color.White, mono = true)
            }
            PCard {
                T("ИМЯ", 10, p.mute, mono = true)
                T(L.name, 30, serif = true, bold = true)
                Spacer(Modifier.height(6.dp))
                T("В январе 2026 года вы родились ${if (L.sex == 1) "девочкой" else "мальчиком"} ${if (L.urb) "в городе" else "в сельской местности"}, ${c.n}, в семье из ${QT[L.q5 - 1]}. Старших братьев и сестёр у вас: ${L.sib}.", 14, p.mute, lh = 20)
            }
            Globe(c.lat, c.lon, Modifier.align(Alignment.CenterHorizontally).size(210.dp))
            PCard {
                KV("Пол", if (L.sex == 1) "Женский" else "Мужской"); KV("Страна рождения", c.n)
                KV("Место", if (L.urb) "город" else "село")
                KV("Доход семьи", "●".repeat(L.q5) + "○".repeat(5 - L.q5) + " квинтиль ${L.q5}")
                KV("Возраст родителей", "Мать ${L.mo}, отец ${L.fa}")
                KV("Работа родителей", "Мать: ${L.pjob[0]}; отец: ${L.pjob[1]}")
                KV("Ожидаемая продолжительность жизни", f1(L.le) + " года")
                KV("Вероятность умереть до 5 лет", f1(c.u / 10) + "%")
                KV("ВВП на человека (ППС)", fm(c.g) + " $")
            }
            PCard {
                T("Из младенцев, рождающихся сегодня, ${"%.2f".format(L.pr).replace('.', ',')}% рождаются здесь. Это примерно 1 из ${fm(L.odds)}.", 13, p.tl)
            }
            PCard {
                Row(Modifier.fillMaxWidth()) { Spacer(Modifier.weight(1.2f)); T("Мир", 11, p.mute, mono = true, modifier = Modifier.weight(1f)); T(c.n.take(14), 11, p.ink, mono = true, bold = true, modifier = Modifier.weight(1f)) }
                listOf(
                    Triple("Жизнь, лет", f1(wle), f1(L.le)), Triple("Смерть до 5 лет", f1(wu / 10) + "%", f1(c.u / 10) + "%"),
                    Triple("Обучение, лет", f1(ws), f1(c.s)), Triple("ВВП (ППС), $", fm(wg), fm(c.g))
                ).forEach { (a, b, d) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        T(a, 12, p.mute, modifier = Modifier.weight(1.2f)); T(b, 12, modifier = Modifier.weight(1f)); T(d, 12, bold = true, modifier = Modifier.weight(1f))
                    }
                }
            }
            PBtn("Начать эту жизнь", Modifier.fillMaxWidth(), primary = true) { vm.beginLife() }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------- экран жизни ----------
@Composable
fun LifeScreen(vm: GameVM) {
    val L = vm.life ?: return
    val t = vm.tick
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(vm, L, t)
            Box(Modifier.weight(1f)) {
                when (vm.tab) {
                    0 -> TabLife(vm, L, t)
                    1 -> TabLog(vm, L, t)
                    2 -> TabWorld(vm, L, t)
                    else -> TabMenu(vm)
                }
            }
            NavBar(vm)
        }
        DecisionOverlay(vm)
    }
}

@Composable
fun TopBar(vm: GameVM, L: Life, t: Int) {
    val p = P()
    Column(Modifier.fillMaxWidth().background(p.card).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                T("${L.name} · ${2026 + L.age} г.", 12, p.mute, mono = true)
                T("1 год ≈ ${f1(vm.len * 60.0 / L.le / (if (vm.ff) 16 else vm.speed))} с", 10, p.mute, mono = true)
            }
            T("Возраст ", 12, p.mute, mono = true)
            T("${L.age}", 34, serif = true, bold = true)
        }
        Spacer(Modifier.height(4.dp))
        RawBar(vm.prog, p.cr, 4)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBox(on = vm.paused, onClick = { vm.togglePause() }) { PlayPauseIcon(vm.paused, if (vm.paused) p.bg else p.ink) }
            SPEEDS.forEach { s -> PBtn("${s}×", on = vm.speed == s && !vm.ff) { vm.setSpeed(s) } }
            IconBox(on = vm.ff, onClick = { vm.fastForward() }) { FfIcon(if (vm.ff) p.bg else p.ink) }
            PBtn("Авто", on = vm.auto) { vm.setAuto(!vm.auto) }
        }
    }
}

@Composable
fun NavBar(vm: GameVM) {
    val p = P()
    Row(Modifier.fillMaxWidth().background(p.card).padding(vertical = 6.dp)) {
        listOf(Triple("Жизнь", Icons.Filled.Person, 0), Triple("Летопись", Icons.Filled.DateRange, 1), Triple("Мир", Icons.Filled.Place, 2), Triple("Меню", Icons.Filled.Settings, 3)).forEach { (name, ic, i) ->
            val sel = vm.tab == i
            val col by animateColorAsState(if (sel) p.cr else p.mute, tween(200), label = "nav")
            Column(
                Modifier.weight(1f).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.tab = i }.padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(ic, null, tint = col, modifier = Modifier.size(22.dp))
                T(name, 10, col, mono = true, bold = sel)
            }
        }
    }
}

@Composable
fun DecisionOverlay(vm: GameVM) {
    val p = P(); val d = vm.pend
    AnimatedVisibility(visible = d != null, enter = fadeIn(tween(180)), exit = fadeOut(tween(150))) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            contentAlignment = Alignment.Center
        ) {
            if (d != null) {
                var shown by remember(d) { mutableStateOf(false) }
                LaunchedEffect(d) { shown = true }
                val sc by animateFloatAsState(if (shown) 1f else 0.9f, spring(dampingRatio = 0.7f), label = "dlg")
                val L = vm.life
                Column(
                    Modifier.padding(14.dp).graphicsLayer { scaleX = sc; scaleY = sc }.fillMaxWidth().widthIn(max = 460.dp)
                        .heightIn(max = 620.dp).pcard(p.card, p.cr).padding(18.dp).verticalScroll(rememberScrollState())
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        T("ВАЖНОЕ РЕШЕНИЕ", 11, p.cr, mono = true, bold = true)
                        T("${L?.age ?: 0} лет · ${2026 + (L?.age ?: 0)}", 11, p.mute, mono = true)
                    }
                    Spacer(Modifier.height(6.dp))
                    T(d.title, 22, serif = true, bold = true, lh = 27)
                    if (d.desc.isNotEmpty()) { Spacer(Modifier.height(4.dp)); T(d.desc, 13, p.mute, lh = 18) }
                    Spacer(Modifier.height(8.dp))
                    d.opts.forEachIndexed { k, o -> OptBtn(k + 1, o, vm.auto && k == 0) { vm.choose(k) } }
                    if (vm.auto) {
                        Spacer(Modifier.height(6.dp))
                        RawBar(1f - vm.autoT / 2.6f, p.cr, 4)
                        T("Через мгновение выбирается «${d.opts[0].label}»", 11, p.mute, mono = true)
                    }
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (vm.auto) PBtn("Отключить «Авто»") { vm.setAuto(false) }
                        PBtn("Скрыть окна") { vm.setHide(true); vm.choose(0) }
                    }
                }
            }
        }
    }
}

@Composable
fun OptBtn(n: Int, o: Opt, hi: Boolean, onClick: () -> Unit) {
    val p = P()
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.97f else 1f, tween(80), label = "opt")
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).graphicsLayer { scaleX = sc; scaleY = sc }
            .pcard(if (hi) p.cr.copy(alpha = 0.18f).compositeOver(p.card) else p.card, if (hi) p.cr else p.line)
            .clickable(interactionSource = src, indication = null, onClick = onClick).padding(12.dp)
    ) {
        T("$n · ${o.label}", 14, bold = true)
        if (o.sub.isNotEmpty()) T(o.sub, 11, p.mute, mono = true)
    }
}

@Composable
fun SpendBar(inc: Double, rich: Boolean) {
    val p = P()
    val t = (log10(max(1.0, inc / 1000)) / 1.7).coerceIn(0.0, 1.0)
    val items = listOf(
        Triple("Налоги", 0.05 + (if (rich) 0.15 else 0.05) * t, p.mute), Triple("Питание", 0.45 - 0.3 * t, p.gold),
        Triple("Жильё", 0.2, p.cr), Triple("Транспорт", 0.04 + 0.04 * t, p.tl),
        Triple("Медицина", 0.04 + 0.03 * t, Color(0xFFA24A4A)), Triple("Досуг", 0.15 + 0.1 * t, p.line)
    )
    val all = items + Triple("Сбережения", max(0.02, 1 - items.sumOf { it.second }), p.ink)
    val tot = all.sumOf { it.second }
    Canvas(Modifier.fillMaxWidth().height(10.dp)) {
        var x = 0f
        for (s in all) { val w = (s.second / tot).toFloat() * size.width; drawRect(s.third, Offset(x, 0f), Size(w, size.height)); x += w }
    }
    T(all.joinToString(" · ") { "${it.first} ${(it.second / tot * 100).roundToInt()}%" }, 10, p.mute, mono = true)
}

@Composable
fun StatBar(name: String, v: Double, col: Color) {
    val p = P()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { T(name, 12, p.mute); T("${v.roundToInt()}", 12, bold = true, mono = true) }
    Bar((v / 100).toFloat(), col, 7)
    Spacer(Modifier.height(5.dp))
}

@Composable
fun TabLife(vm: GameVM, L: Life, t: Int) {
    val p = P(); val c = L.c; val a = L.age; val i = L.inc(); val v = if (i > 0) i else L.hh
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
        PCard(pad = 8) {
            Scene(if (a < 18) "home" else "street", people(L), a)
            Spacer(Modifier.height(6.dp))
            T("${c.n} · ${if (L.urb) "город" else "село"} · " + (if (a < 18) "с родителями" + (if (L.sib > 0) " и ${L.sib} братьями и сёстрами" else "") else if (L.pt != null) "с партнёром" else "один"), 12, p.mute)
        }
        PCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Portrait(L, 60); Spacer(Modifier.width(12.dp))
                Column { T(L.name, 20, serif = true, bold = true); T("${if (L.sex == 1) "Женский" else "Мужской"} пол · ${L.home.n}", 11, p.mute, mono = true) }
            }
            Spacer(Modifier.height(6.dp))
            KV("Образование", EDU[L.edu])
            KV("Профессия", L.job?.n ?: if (L.ret) "на пенсии" else if (a < 15) "—" else if (L.busy > a) "учится" else "в поиске работы")
            KV("Происхождение семьи", "●".repeat(L.q5) + "○".repeat(5 - L.q5))
        }
        PCard {
            T("СОСТОЯНИЕ", 11, p.mute, mono = true); Spacer(Modifier.height(6.dp))
            StatBar("Здоровье", L.h, p.grn); StatBar("Счастье", L.j, p.gold); StatBar("Знания", L.m, p.tl); StatBar("Отношения", L.b, p.cr)
            KV("Чистый капитал", fm(L.cap) + " $ PPP")
            KV(if (i > 0) "Доход за год" else "Доход семьи на человека", fm(v) + " $ PPP")
            if (v < 785) T("Доход ниже черты крайней бедности (2,15 $ в день)", 11, p.cr, mono = true)
            Spacer(Modifier.height(6.dp))
            T("КУДА УХОДИТ ДОХОД (ОЦЕНКА)", 10, p.mute, mono = true); Spacer(Modifier.height(4.dp))
            SpendBar(v, c.g > 15000)
        }
        PCard {
            T("ФОКУС ЭТОГО ГОДА", 11, p.mute, mono = true)
            Spacer(Modifier.height(2.dp))
            T(L.job?.let { "${it.n} · стаж ${L.ten} лет" } ?: if (a < 6) "Вы пока младенец: вас растят родители. Фокус можно менять в любой момент." else "Выберите, чему посвятить год", 12, p.mute)
            Spacer(Modifier.height(6.dp))
            FOCUS.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { f -> FocusChip(f, L.focus == f.id, Modifier.weight(1f)) { vm.setFocus(f.id) } }
                }
            }
        }
        PCard {
            T("СЕМЬЯ", 11, p.mute, mono = true); Spacer(Modifier.height(4.dp))
            KV("Мама", if (a + L.mo >= L.mD) "умерла в ${L.mD} лет" else "${a + L.mo} лет · ${L.pjob[0]}")
            KV("Папа", if (a + L.fa >= L.fD) "умер в ${L.fD} лет" else "${a + L.fa} лет · ${L.pjob[1]}")
            KV("Братья и сёстры", "${L.sib}")
            L.pt?.let { KV("Партнёр", it) }
            if (L.kids.isNotEmpty()) KV("Дети", "${L.kids.size}")
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun FocusChip(f: Focus, sel: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = P()
    val bg by animateColorAsState(if (sel) p.ink else p.card, tween(200), label = "fc")
    val fg = if (sel) p.bg else p.ink
    Column(
        modifier.padding(vertical = 4.dp).pcard(bg, if (sel) p.ink else p.line)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick).padding(10.dp)
    ) { T(f.name, 13, fg, bold = true); T(f.hint, 10, if (sel) fg else p.mute, mono = true, lh = 13) }
}

@Composable
fun TabLog(vm: GameVM, L: Life, t: Int) {
    val p = P()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        item { T("ЛЕТОПИСЬ ЖИЗНИ · СНАЧАЛА НОВЫЕ · НАЖМИТЕ НА СТРОКУ, ЧТОБЫ ОТМЕТИТЬ ★", 10, p.mute, mono = true); Spacer(Modifier.height(6.dp)) }
        items(L.log.toList(), key = { it.id }) { e -> LogRow(e) { vm.toggleStar(e) } }
    }
}

@Composable
fun LogRow(e: Entry, onClick: () -> Unit) {
    val p = P()
    Row(
        Modifier.fillMaxWidth().background(if (e.star) p.gold.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick).padding(vertical = 7.dp, horizontal = 4.dp)
    ) {
        T("${e.age} лет", 11, p.mute, mono = true, modifier = Modifier.width(52.dp))
        Column(Modifier.weight(1f)) {
            T(e.text, 14, bold = e.major, lh = 19)
            e.data?.let { T("Данные · $it", 10, p.tl, mono = true) }
        }
        if (e.star) T("★", 14, p.gold)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(p.line.copy(alpha = 0.6f)))
}

@Composable
fun countryRows(c: Country) {
    KV("Рождений в год", f1(c.b / 1000) + " млн"); KV("Население", f1(c.p) + " млн")
    KV("Жизнь, Ж / М", f1(c.f) + " / " + f1(c.m)); KV("Смертность до 5 лет", "${c.u.roundToInt()} на 1 000")
    KV("ВВП на человека (ППС)", fm(c.g) + " $"); KV("Коэффициент Джини", "%.2f".format(c.k / 100).replace('.', ','))
    c.ur?.let { KV("Городское население", "${it.roundToInt()}%") }; c.ag?.let { KV("Заняты в с/х", "${it.roundToInt()}%") }
    KV("Ожидаемое обучение", f1(c.s) + " года"); c.te?.let { KV("Высшее образование", "${it.roundToInt()}%") }
    c.nt?.let { KV("Интернет", "${it.roundToInt()}%") }; KV("Рождаемость", f1(c.t))
    KV("Материнская смертность", "${c.r.roundToInt()} на 100 000"); KV("ВИЧ (15–49)", f1(c.v) + "%"); KV("Убийства", f1(c.h) + " на 100 000")
}

@Composable
fun TabWorld(vm: GameVM, L: Life, t: Int) {
    val p = P(); val ws = remember(vm.countries) { worldSurvival(vm.countries) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Globe(L.c.lat, L.c.lon, Modifier.fillMaxWidth().height(310.dp))
        T("Потяните глобус, чтобы повернуть", 10, p.mute, mono = true, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        PCard { T(L.c.n.uppercase(), 11, p.mute, mono = true); Spacer(Modifier.height(4.dp)); countryRows(L.c); T("Источник: Всемирный банк (WDI), последний доступный год.", 10, p.mute, mono = true) }
        SurvivalCard(L, ws)
        MapCard(L)
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
fun SurvivalCard(L: Life, ws: DoubleArray) {
    val p = P(); val S = remember(L.c, L.sex) { survival(L.q) }
    PCard {
        T("КРИВАЯ ВЫЖИВАЕМОСТИ", 11, p.mute, mono = true)
        Canvas(Modifier.fillMaxWidth().height(150.dp).padding(top = 6.dp)) {
            val w = size.width; val h = size.height
            for (q in 0..4) drawLine(p.line, Offset(0f, h * q / 4f), Offset(w, h * q / 4f), 1.dp.toPx())
            fun path(a: DoubleArray) = Path().apply { for (k in 0..100) { val x = w * k / 100f; val y = h * (1f - a[k].toFloat()); if (k == 0) moveTo(x, y) else lineTo(x, y) } }
            drawPath(path(ws), p.mute, style = Stroke(2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f))))
            drawPath(path(S), p.tl, style = Stroke(3.dp.toPx()))
            val ax = w * min(L.age, 100) / 100f
            drawLine(p.ink, Offset(ax, 0f), Offset(ax, h), 1.dp.toPx())
        }
        T("0 — 100 лет · сплошная: ${L.c.n}, ${if (L.sex == 1) "женщины" else "мужчины"} · пунктир: мир", 10, p.mute, mono = true)
    }
}

@Composable
fun MapCard(L: Life) {
    val p = P()
    PCard {
        T("КАРТА ЖИЗНИ · 1 КЛЕТКА = 1 ГОД", 11, p.mute, mono = true)
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 6.dp)) {
            val g = 3.dp.toPx(); val cell = (size.width - g * 9) / 10f
            for (k in 0 until 100) {
                val x = (k % 10) * (cell + g); val y = (k / 10) * (cell + g)
                val j = L.jy.getOrNull(k)
                val col = if (j == null) p.line.copy(alpha = 0.5f) else lerpCol(p.tl.copy(alpha = 0.35f), p.tl, j / 100f)
                drawRect(col, Offset(x, y), Size(cell, cell))
                if (k == L.le.roundToInt()) drawRect(p.cr, Offset(x, y), Size(cell, cell), style = Stroke(2.dp.toPx()))
            }
        }
        T("Ярче цвет — счастливее год · рамка — ожидаемая продолжительность жизни", 10, p.mute, mono = true)
    }
}
fun lerpCol(a: Color, b: Color, f: Float) = androidx.compose.ui.graphics.lerp(a, b, f.coerceIn(0f, 1f))

@Composable
fun TabMenu(vm: GameVM) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp)) {
        T("Меню", 22, serif = true, bold = true)
        SettingsBody(vm)
        PBtn("Выйти в главное меню", Modifier.fillMaxWidth()) { vm.home() }
        Spacer(Modifier.height(10.dp))
    }
}

// ---------- финал ----------
@Composable
fun SlidesScreen(vm: GameVM) {
    CompositionLocalProvider(LocalPal provides DarkPal) {
        val p = DarkPal; val L = vm.life ?: return@CompositionLocalProvider
        val i = vm.slideI; val s = vm.slides.getOrNull(i) ?: return@CompositionLocalProvider
        LaunchedEffect(i) { if (!s.fin) { delay(4200); vm.nextSlide() } }
        Box(
            Modifier.fillMaxSize().background(Color(0xFF101214))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { vm.nextSlide() }
        ) {
            AnimatedContent(targetState = i, transitionSpec = { fadeIn(tween(550)) togetherWith fadeOut(tween(300)) }, label = "slide") { k ->
                val sl = vm.slides.getOrNull(k)
                if (sl != null) Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    if (sl.fin) {
                        T(L.name, 30, p.ink, bold = true, serif = true, align = TextAlign.Center)
                        T("2026 – ${2026 + L.age}", 12, p.mute, mono = true)
                        Spacer(Modifier.height(10.dp))
                        T("Родились в ${L.home.n}${if (L.c.n != L.home.n) ", прожили часть жизни в ${L.c.n}" else " и прожили там всю жизнь"}.\n${L.age} лет: примерно ${fm(L.age * 365.25)} дней жизни.", 13, p.ink, align = TextAlign.Center, lh = 19)
                        Spacer(Modifier.height(14.dp))
                        T("Одна жизнь среди 8 миллиардов, прожитая лишь один раз.", 18, p.ink, serif = true, align = TextAlign.Center, lh = 24)
                        Spacer(Modifier.height(18.dp))
                        PBtn("Посмотреть запись", primary = true) { vm.toRecord() }
                    } else if (sl.head != null) {
                        T(sl.head, 28, p.ink, bold = true, serif = true, align = TextAlign.Center, lh = 34)
                        Spacer(Modifier.height(10.dp))
                        sl.lines.forEachIndexed { n, ln -> T(ln, if (n == 1) 11 else 13, p.mute, mono = n == 1, align = TextAlign.Center, lh = 19); Spacer(Modifier.height(5.dp)) }
                    } else {
                        val k0 = SKIN[L.sk]
                        val pp = when (sl.sc) {
                            "home" -> listOf(Pp(60f, Color(0xFF7A6A9C), k0, 12f), Pp(70f, Color(0xFF4D6F8A), k0, 12f), Pp(82f, Color(0xFF8A5A8C), k0, 7f))
                            "mourn" -> listOf(Pp(56f, Color(0xFF555555), k0, 10f, Color(0xFFD8D8D8)), Pp(96f, Color(0xFF555555), k0, 10f, Color(0xFFD8D8D8)))
                            "park" -> listOf(Pp(58f, Color(0xFF2F7A8A), k0, 10f), Pp(66f, Color(0xFFA0506A), SKIN[(L.sk + 1) % 5], 10f))
                            else -> listOf(Pp(70f, Color(0xFF2F7A8A), k0, 11f, if (sl.age > 62) Color(0xFFD8D8D8) else Color(0xFF2B1D14)))
                        }
                        Scene(sl.sc ?: "street", pp, sl.age + 3, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        T("${sl.age} лет", 11, p.mute, mono = true)
                        Spacer(Modifier.height(4.dp))
                        T(sl.text, 17, p.ink, serif = true, align = TextAlign.Center, lh = 23)
                        Spacer(Modifier.height(14.dp))
                        RawBar((i + 1f) / vm.slides.size, p.tl, 3)
                    }
                }
            }
            Box(Modifier.align(Alignment.TopEnd).padding(10.dp)) { PBtn("Пропустить") { vm.toRecord() } }
            if (!s.fin) T("Нажмите на экран, чтобы продолжить", 10, p.mute, mono = true, align = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp))
        }
    }
}

@Composable
fun PairSlider(l: String, r: String, pos: Double) {
    val p = P()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { T(l, 10, p.mute, mono = true); T(r, 10, p.mute, mono = true) }
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        drawRect(p.line, Offset(0f, size.height / 2 - 3.dp.toPx()), Size(size.width, 6.dp.toPx()))
        drawCircle(p.tl, 6.dp.toPx(), Offset((size.width * pos.coerceIn(0.0, 1.0)).toFloat().coerceIn(6.dp.toPx(), size.width - 6.dp.toPx()), size.height / 2))
    }
}

@Composable
fun RecordScreen(vm: GameVM) {
    val L = vm.life ?: return
    val p = P(); val c = L.c; val a = L.age; val d = a - L.le
    val ws = remember(vm.countries) { worldSurvival(vm.countries) }
    val S = remember(L) { survival(L.q) }
    val tot = max(1, L.fc.values.sum()); fun fc(k: String) = (L.fc[k] ?: 0).toDouble() / tot
    val dom = L.fc.maxByOrNull { it.value }?.key ?: "bal"
    val bj = L.jy.indices.maxByOrNull { L.jy[it] } ?: 0
    val jobs = L.log.filter { it.text.startsWith("Вы начали работать:") }.map { it.text.removePrefix("Вы начали работать: ").removeSuffix(".") }.distinct().reversed()
    val decades = L.log.map { it.age / 10 }.distinct().sorted()
    val shown = L.log.filter { vm.recFilter < 0 || it.age / 10 == vm.recFilter }.reversed()
    val maxCap = max(1.0, L.hist.maxOfOrNull { it[0] } ?: 1.0)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentPadding = PaddingValues(vertical = 10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { PBtn("← На первый экран") { vm.home() } }
            Column(Modifier.fillMaxWidth().padding(top = 6.dp).pcard(p.card, p.line).padding(15.dp)) {
                Row(Modifier.fillMaxWidth().background(Color(0xFF2A2D31)).padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    T("ЗАПИСЬ О СМЕРТИ", 11, Color.White, mono = true); T("${c.n.take(3).uppercase()}-2026-${100 + a}", 11, Color.White, mono = true)
                }
                Spacer(Modifier.height(10.dp))
                T("${L.home.n.uppercase()} · ${if (L.sex == 1) "ЖЕНСКИЙ" else "МУЖСКОЙ"} ПОЛ", 10, p.mute, mono = true)
                T(L.name, 32, serif = true, bold = true)
                T("2026 – ${2026 + a} · прожито $a лет · ${c.n}", 11, p.mute, mono = true)
                Spacer(Modifier.height(8.dp))
                T("Родились ${if (L.sex == 1) "девочкой" else "мальчиком"} в семье из ${QT[L.q5 - 1]}, ${if (L.urb) "в городе" else "в сельской местности"}, ${L.home.n}. Образование: ${EDU[L.edu].lowercase()}." +
                    (if (jobs.isNotEmpty()) " Работали: ${jobs.joinToString(", ").lowercase()}." else "") +
                    " " + (L.pt?.let { "Партнёр — ${it.substringBefore(' ')}" + (if (L.kids.isNotEmpty()) ", детей: ${L.kids.size}." else ".") } ?: "В отношениях надолго не остались.") + " Жизнь закончилась в $a лет.", 14, lh = 20)
                Spacer(Modifier.height(8.dp))
                T("Причина смерти: ${L.cause}", 17, p.cr, serif = true, bold = true, lh = 22)
                T("Ожидаемая продолжительность жизни для ${if (L.sex == 1) "женщины" else "мужчины"} в стране рождения: ${f1(L.le)} года. Вы прожили на ${f1(abs(d))} года ${if (d >= 0) "больше" else "меньше"}. По модели вероятность прожить дольше вас была около ${((S.getOrElse(a) { 0.0 }) * 100).roundToInt()}%.", 12, p.mute, lh = 17)
                Spacer(Modifier.height(8.dp))
                KV("Последняя работа", L.job?.n ?: if (L.ret) "на пенсии" else "—"); KV("Самый счастливый год", "$bj лет (${2026 + bj})")
                KV("Дети", "${L.kids.size}"); KV("Страны, где жили", if (c.n != L.home.n) "${L.home.n}, ${c.n}" else L.home.n)
                KV("Оставшееся состояние", fm(L.cap) + " $ PPP"); KV("Средний доход в рабочие годы", fm(if (L.wy > 0) L.sum / L.wy else 0.0) + " $ PPP/год")
                Spacer(Modifier.height(8.dp))
                T(ARCH[dom] ?: "", 18, serif = true, bold = true); T("УЗОР ЭТОЙ ЖИЗНИ", 10, p.mute, mono = true); Spacer(Modifier.height(4.dp))
                PairSlider("Рядом", "Работа", fc("work") * 2.2 + 0.2); PairSlider("Корни", "Ветер", if (L.mig) 0.9 else 0.12)
                PairSlider("Тишина", "Волны", 0.5 + (fc("dev") - fc("hea")) * 0.6 + (if (L.mig) 0.2 else 0.0)); PairSlider("Сегодня", "Завтра", (fc("dev") + fc("work")) * 1.2)
            }
            SurvivalCard(L, ws)
            MapCard(L)
            PCard {
                T("БОГАТСТВО НА ПРОТЯЖЕНИИ ЖИЗНИ · ЧИСТЫЙ КАПИТАЛ", 10, p.mute, mono = true)
                Canvas(Modifier.fillMaxWidth().height(120.dp).padding(top = 6.dp)) {
                    val w = size.width; val h = size.height; val n = max(1, L.hist.size - 1)
                    val path = Path()
                    L.hist.forEachIndexed { k, hh -> val x = w * k / n; val y = h * (1f - (hh[0] / maxCap).toFloat()); if (k == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                    drawPath(path, p.tl, style = Stroke(3.dp.toPx()))
                    drawLine(p.line, Offset(0f, h), Offset(w, h), 1.dp.toPx())
                }
            }
            PCard {
                T("ВСЯ ЗАПИСЬ О ЖИЗНИ", 11, p.mute, mono = true); Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PBtn("Все", on = vm.recFilter < 0) { vm.recFilter = -1 }
                    decades.forEach { x -> PBtn(if (x == 0) "Детство" else "${x * 10}-е", on = vm.recFilter == x) { vm.recFilter = x } }
                }
            }
        }
        items(shown, key = { it.id }) { e -> LogRow(e) { vm.toggleStar(e) } }
        item {
            Spacer(Modifier.height(10.dp))
            PCard { T("СТРАНА: ${c.n.uppercase()}", 11, p.mute, mono = true); Spacer(Modifier.height(4.dp)); countryRows(c) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PBtn("Родиться снова", primary = true) { vm.startBirth() }
                PBtn("На первый экран") { vm.home() }
            }
            Spacer(Modifier.height(8.dp))
            T("Если тема отозвалась тяжело, поговорите с близкими или обратитесь на линию помощи в вашей стране.", 10, p.mute, mono = true)
        }
    }
}
