package com.xex.oneineight

import android.content.Context
import org.json.JSONArray

class Country(
    val n: String, val b: Double, val p: Double, val m: Double, val f: Double,
    val i: Double, val u: Double, val g: Double, val k: Double, val s: Double,
    val r: Double, val h: Double, val v: Double, val ur: Double?, val ag: Double?,
    val te: Double?, val nt: Double?, val t: Double, val lat: Double, val lon: Double
) {
    val hasPos: Boolean get() = !lat.isNaN() && !lon.isNaN()
}

// Демо-набор (приблизительные значения). В сборке подменяется реальными данными (assets/data.json).
private const val DEMO = """Индия;23000;1428;67.7;72.4;26;31;9000;32;12.7;103;3;0.2;36;43;29;46;2;20.6;78.9
Китай;9000;1411;75;80.5;5;6;22000;38;14;23;0.5;0.1;65;22;60;76;1.2;35.9;104.2
Нигерия;7500;223;52;54.5;70;102;6000;35;10;1047;9;1.3;54;35;10;45;5.1;9.1;8.7
Пакистан;6800;240;65;68;52;63;6000;30;8;154;4;0.2;38;37;12;21;3.5;30.4;69.3
Индонезия;4500;277;67;71;19;22;14000;38;13;173;0.4;0.4;58;29;36;66;2.2;-0.8;113.9
ДР Конго;4300;102;59;63;64;82;1500;42;8;547;10;0.7;47;60;8;23;6;-4;21.8
Эфиопия;4000;126;64;68;35;48;2900;35;9;267;7;0.9;23;60;10;17;4;9.1;40.5
США;3600;335;75;80.5;5.4;6.3;75000;41;16;22;6;0.4;83;1.4;88;92;1.6;37.1;-95.7
Бразилия;2600;216;72;79;12;14;20000;52;15;72;20;0.5;88;8;55;81;1.6;-14.2;-51.9
Россия;1300;144;68;78;4.5;5.2;35000;36;15;17;6;0.9;75;5.8;82;90;1.5;61.5;105.3
Япония;760;124;81.5;87.5;1.8;2.3;50000;33;15;4;0.2;0.01;92;3;65;83;1.3;36.2;138.3
Германия;690;84;79;83.5;3.1;3.7;66000;31;17;4;0.8;0.1;78;1.2;73;93;1.5;51.2;10.5"""

private fun parseDemo(): List<Country> = DEMO.trim().lines().map { ln ->
    val x = ln.split(';')
    fun d(i: Int) = x[i].toDouble()
    Country(x[0], d(1), d(2), d(3), d(4), d(5), d(6), d(7), d(8), d(9), d(10), d(11), d(12), d(13), d(14), d(15), d(16), d(17), d(18), d(19))
}

/** Возвращает (страны, true если загружены реальные данные). */
fun loadCountries(ctx: Context): Pair<List<Country>, Boolean> {
    try {
        val txt = ctx.assets.open("data.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(txt)
        val out = ArrayList<Country>()
        for (k in 0 until arr.length()) {
            val r = arr.getJSONArray(k)
            fun d(i: Int): Double? = if (i >= r.length() || r.isNull(i)) null else r.getDouble(i)
            val b = d(1) ?: continue
            val p = d(2) ?: continue
            val i = d(5) ?: 15.0
            out.add(
                Country(
                    r.getString(0), b, p, d(3) ?: 68.0, d(4) ?: 73.0, i, maxOf(d(6) ?: 18.0, i),
                    d(7) ?: 15000.0, minOf(d(8) ?: 38.0, 70.0), d(9) ?: 12.0, d(10) ?: 100.0,
                    d(11) ?: 5.0, d(12) ?: 0.3, d(13), d(14), d(15), d(16), d(17) ?: 2.5,
                    d(18) ?: Double.NaN, d(19) ?: Double.NaN
                )
            )
        }
        if (out.size > 50) return Pair(out, true)
    } catch (e: Exception) {
    }
    return Pair(parseDemo(), false)
}
