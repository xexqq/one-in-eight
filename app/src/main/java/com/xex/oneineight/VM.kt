package com.xex.oneineight

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

enum class Scr { HOME, ROULETTE, BIRTH, LIFE, SLIDES, RECORD, SETTINGS }
class Slide(val head: String?, val lines: List<String>, val sc: String?, val age: Int, val text: String, val fin: Boolean)
val SPEEDS = listOf(1, 2, 4, 8, 12, 16)

class GameVM(app: Application) : AndroidViewModel(app) {
    private val sp = app.getSharedPreferences("s", 0)
    val countries: List<Country>
    val real: Boolean

    // --- настройки ---
    var theme by mutableIntStateOf(sp.getInt("theme", 0)); private set
    var len by mutableIntStateOf(sp.getInt("len", 60)); private set
    var mode by mutableIntStateOf(sp.getInt("mode", 0)); private set
    var auto by mutableStateOf(sp.getBoolean("auto", false)); private set
    var hide by mutableStateOf(sp.getBoolean("hide", false)); private set
    var anim by mutableStateOf(sp.getBoolean("anim", true)); private set
    fun setTheme(v: Int) { theme = v; sp.edit().putInt("theme", v).apply() }
    fun setLen(v: Int) { len = v; sp.edit().putInt("len", v).apply() }
    fun setMode(v: Int) { mode = v; sp.edit().putInt("mode", v).apply() }
    fun setAuto(v: Boolean) { auto = v; autoT = 0f; sp.edit().putBoolean("auto", v).apply() }
    fun setHide(v: Boolean) { hide = v; sp.edit().putBoolean("hide", v).apply() }
    fun setAnim(v: Boolean) { anim = v; sp.edit().putBoolean("anim", v).apply() }

    // --- состояние игры ---
    var screen by mutableStateOf(Scr.HOME); private set
    private var before = Scr.HOME
    var tab by mutableIntStateOf(0)
    var life by mutableStateOf<Life?>(null); private set
    var tick by mutableIntStateOf(0); private set
    var prog by mutableFloatStateOf(0f); private set
    var pend by mutableStateOf<Decision?>(null); private set
    var paused by mutableStateOf(false); private set
    var speed by mutableIntStateOf(1); private set
    var ff by mutableStateOf(false); private set
    var autoT by mutableFloatStateOf(0f); private set
    var rName by mutableStateOf(""); private set
    var rProb by mutableStateOf(""); private set
    var rLat by mutableStateOf(Double.NaN); private set
    var rLon by mutableStateOf(Double.NaN); private set
    var slides by mutableStateOf<List<Slide>>(emptyList()); private set
    var slideI by mutableIntStateOf(0); private set
    var recFilter by mutableIntStateOf(-1)
    private var active = true
    private var rJob: kotlinx.coroutines.Job? = null

    fun setActive(b: Boolean) { active = b }
    fun openSettings() { before = screen; screen = Scr.SETTINGS }
    fun closeSettings() { screen = before }
    fun toRecord() { recFilter = -1; screen = Scr.RECORD }
    fun home() { rJob?.cancel(); life = null; pend = null; paused = false; screen = Scr.HOME }
    fun onBack() {
        when (screen) {
            Scr.SETTINGS -> closeSettings()
            Scr.LIFE -> if (pend == null) tab = if (tab != 0) 0 else 3
            Scr.SLIDES -> toRecord()
            Scr.HOME -> {}
            else -> home()
        }
    }

    fun startBirth() {
        rJob?.cancel()
        val w = countries.map { if (mode == 0) it.b else it.p }
        val tot = w.sum()
        var x = Random.nextDouble() * tot
        var idx = 0
        while (idx < countries.size - 1) { x -= w[idx]; if (x <= 0) break; idx++ }
        val c = countries[idx]
        val l = Life(c, if (Random.nextDouble() < 0.488) 1 else 0, tot / w[idx], w[idx] / tot * 100)
        life = l; pend = null; paused = false; ff = false; prog = 0f; speed = 1; tab = 0; autoT = 0f
        screen = Scr.ROULETTE
        rJob = viewModelScope.launch {
            val tb = countries.sumOf { it.b }
            for (n in 0 until 12) {
                val cc = if (n < 11) countries.random() else c
                rName = cc.n; rProb = pct(cc.b / tb * 100)
                if (cc.hasPos) { rLat = cc.lat; rLon = cc.lon }
                delay(45L + n * 9L)
            }
            rName = c.n; rProb = pct(l.pr)
            if (c.hasPos) { rLat = c.lat; rLon = c.lon }
            delay(500)
            screen = Scr.BIRTH
        }
    }
    private fun pct(x: Double) = "Вероятность: " + "%.2f".format(x).replace('.', ',') + "%"

    fun beginLife() {
        val L = life ?: return
        L.note("В январе 2026 года вы родились ${if (L.sex == 1) "девочкой" else "мальчиком"}: ${L.home.n}, семья из ${QT[L.q5 - 1]}. Старших братьев и сестёр: ${L.sib}.", true, "home")
        tab = 0; paused = false; speed = 1; ff = false; prog = 0f; tick++
        screen = Scr.LIFE
    }

    fun togglePause() { paused = !paused }
    fun setSpeed(s: Int) { speed = s; ff = false }
    fun fastForward() { ff = true; paused = false }
    fun setFocus(id: String) { life?.focus = id; tick++ }
    fun toggleStar(e: Entry) { e.star = !e.star; tick++ }

    fun choose(k: Int) {
        val d = pend ?: return
        val o = d.opts.getOrNull(k) ?: return
        autoT = 0f; pend = null
        val r = try { o.act() } catch (e: Exception) { null }
        if (r != null) pend = r
        tick++
    }
    private fun resolve(d0: Decision) {
        var d: Decision? = d0; var n = 0
        while (d != null && n++ < 6) d = try { d.opts[0].act() } catch (e: Exception) { null }
    }

    private fun frame(dt: Float) {
        if (!active || screen != Scr.LIFE) return
        val L = life ?: return
        if (L.dead) return
        if (pend != null) {
            if (auto) { autoT += dt; if (autoT >= 2.6f) choose(0) }
            return
        }
        if (paused) return
        val ys = len * 60f / L.le.toFloat()
        prog += dt * (if (ff) 16 else speed) / ys
        var guard = 0
        while (prog >= 1f && pend == null && !L.dead && guard++ < 40) {
            prog -= 1f
            val d = L.step()
            if (L.dead) { prog = 0f; onDeath(L); tick++; return }
            if (d != null) {
                if (hide) resolve(d) else { pend = d; ff = false; autoT = 0f; prog = min(prog, 0.99f) }
            }
            tick++
        }
    }

    private fun onDeath(L: Life) {
        val M = L.maj; val pick = ArrayList<Maj>()
        if (M.size <= 5) pick.addAll(M) else for (k in 0 until 5) pick.add(M[(k * (M.size - 1) / 4.0).roundToInt()])
        val s = ArrayList<Slide>()
        s.add(Slide("Последний год: возраст ${L.age}", listOf("Вы тихо ушли из жизни, в ${L.c.n}.", "Причина смерти · ${L.cause}", "Последние слова перед сном: «${L.lw}»", "Когда ваши глаза закрылись, перед ними пронеслись прожитые дни."), null, L.age, "", false))
        pick.forEach { s.add(Slide(null, emptyList(), it.sc, it.age, it.text, false)) }
        s.add(Slide(null, emptyList(), "mourn", L.age, "На похороны пришли близкие и давние друзья.", false))
        s.add(Slide(null, emptyList(), null, L.age, "", true))
        slides = s; slideI = 0; pend = null; screen = Scr.SLIDES
    }
    fun nextSlide() { if (slideI < slides.size - 1) slideI++ }

    init {
        val (l, r) = loadCountries(app)
        countries = l; real = r; ALL = l
        viewModelScope.launch {
            var last = System.nanoTime()
            while (true) {
                delay(33)
                val now = System.nanoTime()
                val dt = ((now - last) / 1e9f).coerceAtMost(0.25f); last = now
                try { frame(dt) } catch (e: Exception) { paused = true }
            }
        }
    }
}
