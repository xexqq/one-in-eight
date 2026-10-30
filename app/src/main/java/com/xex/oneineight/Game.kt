package com.xex.oneineight

import kotlin.math.*
import kotlin.random.Random

lateinit var ALL: List<Country>

fun rnd() = Random.nextDouble()
fun rn(): Double = sqrt(-2 * ln(1 - rnd())) * cos(6.2832 * rnd())
fun pn(p: Double): Double {
    val t = sqrt(-2 * ln(1 - p))
    return t - (2.515517 + 0.802853 * t + 0.010328 * t * t) / (1 + 1.432788 * t + 0.189269 * t * t + 0.001308 * t * t * t)
}
fun erf(x: Double): Double {
    val t = 1 / (1 + 0.3275911 * abs(x))
    val y = 1 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * exp(-x * x)
    return if (x < 0) -y else y
}
fun ph(z: Double) = 0.5 * (1 + erf(z / sqrt(2.0)))
fun sgm(c: Country) = sqrt(2.0) * pn((c.k / 100 + 1) / 2)
fun fm(x: Double): String {
    val s = abs(x.roundToLong()).toString().reversed().chunked(3).joinToString(" ").reversed()
    return if (x < 0) "-$s" else s
}
fun <T> wp(o: List<Pair<T, Double>>): T {
    var t = 0.0
    o.forEach { t += it.second }
    var x = rnd() * t
    for (e in o) { x -= e.second; if (x <= 0) return e.first }
    return o[0].first
}

// Таблица смертности: детская по данным страны, взрослая — Гомпертц, подогнанный под ожидаемую продолжительность жизни
fun mk(c: Country, sex: Int): DoubleArray {
    val le = if (sex == 1) c.f else c.m
    val x = max(0.0, (c.u - c.i) / (1000 - c.i) / 1000)
    val q4 = 1 - (1 - x).pow(0.25)
    var lo = 1e-7
    var hi = 0.1
    val q = DoubleArray(120)
    repeat(40) {
        val a0 = (lo + hi) / 2
        for (a in 0 until 120) q[a] = if (a == 0) c.i / 1000 else if (a < 5) q4 else min(1.0, 3e-4 + a0 * exp(0.085 * (a - 5)))
        var s = 1.0
        var e = 0.0
        for (v in q) { e += s * (1 - v / 2); s *= 1 - v }
        if (e > le) lo = a0 else hi = a0
    }
    return q
}
private val MQ = HashMap<String, DoubleArray>()
fun mq(c: Country, s: Int): DoubleArray = MQ.getOrPut(c.n + s) { mk(c, s) }
fun dth(q: DoubleArray, from: Int): Int { for (a in from until 120) if (rnd() < q[a]) return a; return 119 }
fun survival(q: DoubleArray): DoubleArray {
    val o = DoubleArray(121); var s = 1.0; o[0] = 1.0
    for (a in 0 until 120) { s *= 1 - q[a]; o[a + 1] = s }
    return o
}
fun worldSurvival(cs: List<Country>): DoubleArray {
    val S = DoubleArray(121); var tw = 0.0
    for (c in cs) for (s in 0..1) { val sv = survival(mq(c, s)); for (a in 0..120) S[a] += sv[a] * c.b / 2 }
    for (c in cs) tw += c.b
    return DoubleArray(121) { S[it] / tw }
}
private fun w(s: String, x: Number) = Pair(s, x.toDouble())
fun deathCause(c: Country, a: Int, sx: Int): String {
    val p = if (c.g < 8000) 1.0 else if (c.g < 25000) 0.5 else 0.0
    val o: List<Pair<String, Double>> = when {
        a < 1 -> listOf(w("осложнения при родах или недоношенность", 3), w("врождённые пороки", 2), w("тяжёлая инфекция", p * 3))
        a < 5 -> listOf(w("пневмония", 1 + p * 3), w("диарея", p * 2.5), w("малярия", p * 2), w("недоедание", p * 2), w("несчастный случай", 1), w("врождённая болезнь", 1))
        a < 15 -> listOf(w("инфекция", 0.5 + p * 2), w("несчастный случай", 2), w("болезнь", 1.5), w("утопление", 1))
        a < 40 -> listOf(w("несчастный случай или травма", 2), w("насилие", c.h / 3), w("болезнь", 1.5), w("ВИЧ-связанная болезнь", c.v * 3), w("осложнения беременности и родов", if (sx == 1) c.r / 150 else 0.0), w("инфекция", p * 2))
        a < 65 -> listOf(w("болезнь сердца и сосудов", 4), w("онкологическое заболевание", 3.5), w("диабет", 1), w("инфекция", p * 1.5), w("несчастный случай", 1), w("цирроз печени", 1), w("ВИЧ-связанная болезнь", c.v * 1.5))
        else -> listOf(w("ишемическая болезнь сердца (инфаркт миокарда)", 5), w("онкологическое заболевание", 3), w("хроническая болезнь лёгких", 1.5), w("деменция", 1.5), w("пневмония", 0.5 + p * 1.5))
    }
    return wp(o)
}

val EDU = listOf("Нет образования", "Начальная школа", "Средняя школа", "Высшее образование")
val QT = listOf("самых бедных 20%", "бедных 20% (второй квинтиль)", "средних 20%", "обеспеченных 20%", "самых богатых 20%")
class Job(val minEdu: Int, val n: String, val m: Double)
val JOBS = listOf(
    Job(0, "Рабочий на заводе", 0.9), Job(0, "Строительный рабочий", 0.85), Job(0, "Ремесленник (плотник, портной)", 0.8),
    Job(2, "Водитель", 1.0), Job(2, "Продавец", 0.9), Job(2, "Электрик", 1.1),
    Job(3, "Инженер", 2.0), Job(3, "Врач", 2.6), Job(3, "Учитель", 1.2), Job(3, "Программист", 2.4), Job(3, "Бухгалтер", 1.5)
)
class Focus(val id: String, val name: String, val hint: String, val fx: DoubleArray)
val FOCUS = listOf(
    Focus("work", "Упор на работу", "Доход ↑ · здоровье и отношения ↓", doubleArrayOf(-0.8, -0.5, 0.5, -1.5, 2.0)),
    Focus("bal", "Баланс", "Понемногу всего", doubleArrayOf(0.0, 0.0, 0.3, 0.0, 0.0)),
    Focus("fam", "Семья и друзья", "Отношения и счастье ↑", doubleArrayOf(0.0, 1.5, 0.0, 2.5, 0.0)),
    Focus("hea", "Забота о здоровье", "Здоровье ↑ · деньги немного ↓", doubleArrayOf(2.5, 0.0, 0.0, 0.0, -0.3)),
    Focus("dev", "Саморазвитие", "Знания ↑ · предложения по работе ↑", doubleArrayOf(0.0, 0.0, 2.5, 0.0, 1.0)),
    Focus("hob", "Увлечения", "Счастье ↑", doubleArrayOf(0.0, 2.5, 0.5, 0.0, 0.0))
)
val ARCH = mapOf("work" to "Тот, кто держит ритм", "bal" to "Тот, кто идёт ровно", "fam" to "Тот, кто держится за близких",
    "hea" to "Тот, кто бережёт силы", "dev" to "Тот, кто начинает новое", "hob" to "Тот, кто ищет радость в малом")

class Entry(val id: Int, val age: Int, val text: String, val major: Boolean, val data: String?) { var star = false }
class Maj(val age: Int, val text: String, val sc: String)
class Opt(val label: String, val sub: String, val act: () -> Decision?)
class Decision(val title: String, val desc: String, val opts: List<Opt>)

private val FN = listOf(
    listOf("Anna", "Maria", "Sofia", "Lina", "Aisha", "Mei", "Tiana", "Yuki", "Nora", "Esther", "Amara", "Priya"),
    listOf("Omar", "Liam", "Uri", "Wei", "Ivan", "Kofi", "Mateo", "Arjun", "Noah", "Hiro", "Tariq", "Jonas")
)
private val LN = listOf("Friedman", "Okafor", "Rossi", "Nguyen", "Silva", "Kim", "Haddad", "Ivanov", "Singh", "Mbeki", "Garcia", "Tanaka", "Weber", "Andersen", "Khan", "Santos")
fun nm(s: Int) = FN[s].random() + " " + LN.random()
private val PJ = listOf("владелец малого бизнеса", "учитель", "медсестра", "швея", "продавец", "водитель", "инженер", "автомеханик", "рабочий на заводе", "бухгалтер")
private val LWS = listOf("В целом жизнь получилась вполне хорошей.", "Я ни о чём не жалею.", "Жаль, что не всё успел.", "Берегите друг друга.")

private class CD(val age: Int, val t: String, val d: String, val o: List<Triple<String, String, (Life) -> Unit>>)
private fun t3(l: String, s: String, f: (Life) -> Unit) = Triple(l, s, f)
private val CDS = listOf(
    CD(3, "Что вам нравится больше всего", "Вам три года, мир кажется огромным.", listOf(
        t3("Играть с другими детьми", "Отношения ↑") { it.b += 4; it.note("Вы подружились с соседскими детьми.") },
        t3("Слушать сказки и песни", "Знания и счастье ↑") { it.m += 3; it.j += 3; it.note("Вы полюбили сказки и песни.") },
        t3("Исследовать всё вокруг", "Знания ↑ · риск ↑") { it.m += 4; it.h -= 2; it.note("Вы всё трогали и пробовали на вкус.") })),
    CD(5, "Чем занять свободное время", "День почти целиком свободен.", listOf(
        t3("Играть во дворе", "Счастье ↑") { it.j += 4; it.note("Вы целыми днями играли во дворе.") },
        t3("Помогать родителям", "Отношения ↑") { it.b += 3; it.m += 1; it.note("Вы помогали родителям по дому.") },
        t3("Рисовать и лепить", "Знания ↑") { it.m += 3; it.j += 2; it.note("Вы увлеклись рисованием.") })),
    CD(8, "Друг зовёт гулять", "А уроки ещё не сделаны.", listOf(
        t3("Пойти гулять", "Счастье ↑ · знания ↓") { it.j += 4; it.m -= 1; it.note("Вы пошли гулять с другом.") },
        t3("Делать уроки", "Знания ↑") { it.m += 4; it.note("Вы сначала сделали уроки.") },
        t3("Позвать друга делать уроки вместе", "Отношения и знания ↑") { it.b += 4; it.m += 2; it.note("Вы делали уроки вместе с другом.") })),
    CD(10, "Кружок или секция", "Родные предлагают чем-нибудь заняться после школы.", listOf(
        t3("Спорт", "Здоровье ↑") { it.h += 5; it.b += 2; it.note("Вы записались в спортивную секцию.") },
        t3("Музыка", "Счастье и знания ↑") { it.j += 3; it.m += 3; it.note("Вы начали учиться музыке.") },
        t3("Кружок науки", "Знания ↑") { it.m += 5; it.note("Вы ходили в кружок науки.") },
        t3("Ничего", "Свободное время") { it.j += 2; it.note("Вы предпочли свободное время.") })),
    CD(13, "Одноклассника обижают", "Вы заметили, как его дразнят.", listOf(
        t3("Заступиться", "Отношения ↑ · риск ↑") { it.b += 3; it.j -= 1; it.note("Вы заступились за одноклассника.", true) },
        t3("Позвать взрослых", "Безопасно") { it.b += 2; it.note("Вы рассказали об этом взрослым.") },
        t3("Промолчать", "Совесть ↓") { it.j -= 2; it.note("Вы промолчали, и это осталось с вами.") })),
    CD(16, "Что после школы", "Время решать, чему уделить силы.", listOf(
        t3("Подработать", "Деньги ↑ · знания ↓") { it.cap += it.est(0.5) * 0.3; it.m -= 1; it.note("Вы подрабатывали после школы.") },
        t3("Учиться больше", "Знания ↑") { it.m += 5; it.note("Вы много занимались.") },
        t3("Проводить время с друзьями", "Отношения и счастье ↑") { it.b += 3; it.j += 3; it.note("Вы проводили время с друзьями.") }))
)
private val RDS = listOf(
    CD(0, "Родственник просит денег в долг", "Он обещает вернуть, но вы не уверены.", listOf(
        t3("Дать всю сумму", "Отношения ↑ · деньги ↓") { it.cap *= 0.9; it.b += 4; it.note("Вы одолжили родственнику деньги.") },
        t3("Дать часть", "Компромисс") { it.cap *= 0.95; it.b += 1; it.note("Вы дали родственнику часть суммы.") },
        t3("Отказать", "Отношения ↓") { it.b -= 3; it.note("Вы отказали родственнику в долг.") })),
    CD(0, "Предложили подработку", "Это дополнительный доход, но меньше отдыха.", listOf(
        t3("Согласиться", "Деньги ↑ · здоровье ↓") { it.cap += it.est(0.8) * 0.2; it.h -= 2; it.note("Вы взяли дополнительную подработку.") },
        t3("Отказаться", "Счастье ↑") { it.j += 2; it.note("Вы отказались от подработки.") })),
    CD(0, "Сосед предлагает открыть дело", "Небольшая мастерская вместе с вами.", listOf(
        t3("Рискнуть", "Может выйти и в плюс, и в минус") { if (rnd() < 0.5) { it.cap *= 1.4; it.note("Дело пошло: капитал вырос.", true) } else { it.cap *= 0.7; it.note("Дело не пошло, часть денег потеряна.") } },
        t3("Отказаться", "Без риска") { it.j += 1; it.note("Вы не стали рисковать.") })),
    CD(0, "Вы решили изменить привычки", "Здоровье начинает напоминать о себе.", listOf(
        t3("Заняться спортом", "Здоровье ↑") { it.h += 6; it.note("Вы стали регулярно заниматься спортом.") },
        t3("Бросить вредные привычки", "Здоровье ↑") { it.h += 4; it.j -= 1; it.note("Вы бросили вредные привычки.") },
        t3("Ничего не менять", "Счастье ↑") { it.j += 2; it.note("Вы ничего не стали менять.") })),
    CD(0, "Друг зовёт в путешествие", "Поездка стоит денег и времени.", listOf(
        t3("Поехать", "Счастье ↑ · деньги ↓") { it.cap *= 0.96; it.j += 8; it.b += 3; it.note("Вы съездили в путешествие с другом.", true) },
        t3("Остаться", "Деньги целее") { it.note("Вы остались дома.") })),
    CD(0, "Возможность переехать в другой город", "Там зарплаты выше, но вы никого не знаете.", listOf(
        t3("Переехать", "Доход ↑ · отношения ↓") { it.mm *= 1.1; it.b -= 4; it.note("Вы переехали в другой город.", true) },
        t3("Остаться", "Отношения ↑") { it.b += 2; it.note("Вы остались на прежнем месте.") }))
)
private class REv(val a0: Int, val a1: Int, val p: Double, val t: String, val d: String? = null, val fx: (Life) -> Unit)
private val EVS = listOf(
    REv(0, 3, 0.5, "Вы сделали основные прививки (АКДС и корь).", "В 2023 году 84% младенцев в мире получили все три дозы вакцины АКДС (ВОЗ·ЮНИСЕФ)") { it.h += 3 },
    REv(4, 14, 0.12, "Вы подружились с соседским ребёнком.") { it.b += 4 },
    REv(5, 17, 0.08, "Вы увлеклись игрой во дворе до темноты.") { it.j += 3; it.h += 1 },
    REv(1, 15, 0.06, "Вы тяжело переболели, пришлось лежать несколько недель.") { it.h -= 6 },
    REv(15, 60, 0.05, "Вы заболели и неделю провели в постели.") { it.h -= 4 },
    REv(18, 70, 0.05, "Друг позвал вас на свадьбу, вы отлично провели время.") { it.j += 4; it.b += 3 },
    REv(18, 70, 0.04, "Мировая экономика просела: заказы сократились, зарплаты замерли.") { it.cap *= 0.85 },
    REv(25, 85, 0.05, "Фондовый рынок вырос, и ваши накопления увеличились.") { it.cap *= 1.15 },
    REv(20, 80, 0.04, "В жару вы несколько ночей почти не спали.") { it.h -= 2 },
    REv(16, 70, 0.05, "Вы потеряли счёт времени за интересной книгой.") { it.m += 3; it.j += 2 },
    REv(30, 90, 0.04, "Вы помогли соседу, которому стало плохо.") { it.b += 3; it.j += 2 },
    REv(60, 100, 0.07, "Память уже не та, что раньше.") { it.m -= 3 },
    REv(60, 100, 0.06, "Давление высокое, но диагноз так и не поставили.") { it.h -= 2 },
    REv(25, 60, 0.04, "Вы потеряли работу: предприятие закрылось.") { if (it.job != null) { it.job = null; it.need = true } }
)

class Life(val home: Country, val sex: Int, val odds: Double, val pr: Double) {
    var c: Country = home
    var q: DoubleArray = mq(home, sex)
    val le = if (sex == 1) home.f else home.m
    var age = 0
    var h = 80.0; var j = 70.0; var m = 50.0; var b = 60.0
    val z0 = rn(); val z1 = rn(); var sg = sgm(home)
    var edu = 0; var busy = 0; var mm = 1.0; var ret = false; var pen = 0.0
    var job: Job? = null; var ten = 0
    var focus = "bal"; var sum = 0.0; var wy = 0; var cap = 0.0; var rate = 0.03
    val log = ArrayList<Entry>(); private var lid = 0
    val maj = ArrayList<Maj>(); val jy = ArrayList<Int>(); val hist = ArrayList<DoubleArray>(); val fc = HashMap<String, Int>()
    var dead = false; var cause = ""; var lw = ""; var mig = false; var need = false; private var lastDec = 0
    val name = nm(sex); val sk = Random.nextInt(5)
    val q5 = max(1, min(5, ceil(ph(z0) * 5).toInt()))
    val urb = rnd() * 100 < (home.ur ?: 55.0)
    val sib = max(0, (home.t * (0.5 + rnd()) - 1 + 0.5).toInt())
    val mo = 19 + Random.nextInt(19); val fa = mo + 3 + Random.nextInt(6)
    val mD = dth(mq(home, 1), mo); val fD = dth(mq(home, 0), fa)
    val pjob = listOf(PJ.random(), PJ.random())
    var pt: String? = null; val kids = ArrayList<Int>()
    val hh = home.g * exp(sg * z0 - sg * sg / 2)

    fun est(mult: Double) = c.g * 1.5 * exp(sg * (0.5 * z0 + 0.87 * z1) - sg * sg / 2) * mult * mm / 1.3
    fun inc(): Double {
        if (age < 15) return 0.0
        if (ret) return pen
        val jb = job ?: return 0.0
        if (busy > age) return 0.0
        return est(jb.m)
    }
    fun note(t: String, major: Boolean = false, sc: String = "street", data: String? = null) {
        log.add(0, Entry(lid++, age, t, major, data))
        if (major) maj.add(Maj(age, t, sc))
        if (log.size > 500) log.removeAt(log.size - 1)
    }
    private fun o(l: String, s: String, f: () -> Unit) = Opt(l, s) { f(); null }
    private fun mkDec(cd: CD) = Decision(cd.t, cd.d, cd.o.map { (l, s, f) -> Opt(l, s) { f(this); null } })

    fun jobsDec(): Decision {
        val ok = JOBS.filter { edu >= it.minEdu }.shuffled().take(3).toMutableList()
        if ((c.ag ?: 0.0) > 30 && edu < 2) ok.add(0, Job(0, "Фермер", 0.55))
        val opts = ok.map { jb ->
            Opt(jb.n, "${fm(est(jb.m))} $ PPP в год · официальная работа") {
                job = jb; ten = 0; busy = 0; note("Вы начали работать: ${jb.n}.", true); null
            }
        } + Opt("Подождать лучшей работы", "Ещё год поисков работы") { job = null; note("Вы ещё год искали работу."); null }
        return Decision("Выберите новую работу", "Образование: ${EDU[edu].lowercase()}. Это работы, которые можно найти в вашей стране.", opts)
    }
    private fun migrate() {
        val n = ALL.filter { it.g > 25000 }.ifEmpty { ALL }.random()
        val ok = rnd() < 0.5
        c = n; q = mq(n, sex); sg = sgm(n); mm = if (ok) 1.0 else 0.55; b -= 10; busy = 0; mig = true
        note("Вы переехали: ${n.n}. " + (if (ok) "Нашли работу по специальности." else "Работаете на случайных подработках."), true)
    }

    private fun ev(a: Int): Decision? {
        when {
            a == 6 -> {
                val ok = rnd() < min(0.98, c.s / 9 + 0.1 + 0.05 * z0); edu = if (ok) 1 else 0
                note(if (ok) "Вас записали в школу." else "Школа далеко или не по карману: вы остаётесь дома.")
            }
            a == 15 && edu < 2 -> {
                val ok = edu == 1 && rnd() < min(0.97, (c.s - 3) / 9 + 0.05 * z0)
                if (ok) { edu = 2; note("Вы продолжаете учёбу в старшей школе.") }
                else { val jb = JOBS.filter { it.minEdu == 0 }.random(); job = jb; ten = 0; note("Учёба закончилась: пора зарабатывать. Вы стали: ${jb.n}.", true) }
            }
            a == 18 -> {
                val u = edu == 2 && rnd() < min(0.9, c.s / 16 + 0.08 * z0 + 0.1)
                val l = ArrayList<Opt>()
                if (edu >= 2) {
                    if (u) l.add(Opt("Поступить в вуз", "Учёба до 22 лет, затем доход выше") { edu = 3; busy = 22; job = null; note("Вы поступили в университет.", true); null })
                    l.add(Opt("Начать работать", "Выбрать работу сейчас") { jobsDec() })
                } else l.add(o("Продолжить работать", "Остаться на прежней работе") {})
                l.add(Opt("Уехать в другую страну", "Рискованно: другая страна, другие возможности") { migrate(); if (job == null) jobsDec() else null })
                return Decision("Вам 18. Что дальше?", "Выбор повлияет на всю дальнейшую жизнь.", l)
            }
            a == 22 && edu == 3 && busy > 0 -> { busy = 0; return jobsDec() }
            a == 25 -> return Decision("Вам 25. Жизнь выходит на новый виток.", "", listOf(
                o("Сосредоточиться на семье", "Отношения и счастье ↑") { b += 12; j += 6; note("Вы сосредоточились на семье.") },
                o("Строить карьеру", "Доход ↑ · отношения ↓") { mm *= 1.12; b -= 5; note("Вы вложились в карьеру.") },
                o("Путешествовать", "Счастье и знания ↑") { j += 10; m += 5; note("Вы много путешествовали.") }))
            a == 35 && job != null -> return Decision("Карьерная развилка", "Вам предлагают повышение с большей ответственностью.", listOf(
                o("Принять повышение", "Доход ↑ · здоровье ↓") { mm *= 1.2; h -= 4; note("Вы получили повышение.", true) },
                o("Остаться на месте", "Спокойнее") { j += 3; note("Вы отказались от повышения.") },
                Opt("Сменить профессию", "Новый старт") { jobsDec() }))
            a == 45 -> return Decision("Куда вложить накопления?", "Вы задумались о будущем.", listOf(
                o("Вложиться в акции", "Выше риск и доходность") { rate = 0.06; note("Вы купили акции.") },
                o("Купить недвижимость", "Надёжнее") { rate = 0.04; note("Вы вложились в недвижимость.") },
                o("Копить на вкладе", "Без риска") { rate = 0.02; note("Вы копите на вкладе.") }))
            (a == 40 || a == 55) && rnd() < 0.35 + (60 - h) / 200 -> {
                val rich = inc() > c.g * 1.2
                return Decision("Врач находит серьёзную проблему со здоровьем", "", listOf(
                    o("Лечиться", "Расходы · шанс на выздоровление") { val ok = rich || rnd() < 0.5; h += if (ok) 15 else -5; note(if (ok) "Лечение помогло." else "Лечение оказалось недоступным или слишком дорогим.") },
                    o("Не обращаться", "Здоровье ↓") { h -= 12; note("Вы решили не лечиться.") }))
            }
            a == 65 && !ret -> return Decision("Вам 65. Выйти на пенсию?", "", listOf(
                o("Выйти на пенсию", "Доход ниже · счастье ↑") { pen = inc() * (if (c.g > 20000) 0.6 else 0.25); ret = true; job = null; j += 5; note("Вы вышли на пенсию.", true) },
                o("Продолжить работать", "Доход прежний · здоровье ↓") { h -= 3; note("Вы продолжили работать.") }))
        }
        return null
    }

    /** Один прожитый год. Возвращает решение, если игроку нужно выбрать. */
    fun step(): Decision? {
        val a = age
        val F = FOCUS.first { it.id == focus }.fx
        val i = inc()
        while (jy.size <= a) jy.add(0)
        jy[a] = j.roundToInt()
        while (hist.size <= a) hist.add(doubleArrayOf(0.0, 0.0))
        hist[a] = doubleArrayOf(cap, i)
        fc[focus] = (fc[focus] ?: 0) + 1
        h += F[0] - (if (a > 35) 0.5 else 0.0) - (if (i > 0 && i < 800) 0.4 else 0.0) + (65 - h) * 0.03
        j += F[1] + (55 - j) * 0.05; m += F[2] + (55 - m) * 0.04; b += F[3] + (55 - b) * 0.05
        if (job != null) { ten++; mm *= 1 + F[4] / 100 }
        if (i > 0) { sum += i; wy++; cap += i * (if (i < 2000) 0.03 else if (i < 8000) 0.1 else 0.18) }
        cap = max(0.0, cap * (1 + rate + (if (rate > 0.04) rn() * 0.1 else 0.0)))
        clampAll()
        if (rnd() < q[min(a, 119)] * (if (a < 5) 1.0 else max(0.4, 1.5 - h / 100))) { die(); return null }
        age++
        val A = age
        if (A + mo == mD) { note("Ваша мама умерла в $mD лет.", true); b -= 10; j -= 8 }
        if (A + fa == fD) { note("Ваш папа умер в $fD лет.", true); b -= 10; j -= 8 }
        if (pt == null && A in 22..35 && rnd() < 0.12) { pt = nm(1 - sex); note("Вы начали встречаться с ${pt!!.substringBefore(' ')}.", true, "park") }
        else if (pt != null && A in 24..42 && kids.size < 6 && rnd() < c.t / 18) { kids.add(A); note("У вас родился ребёнок.", true, "home") }
        val e = EVS.firstOrNull { A >= it.a0 && A <= it.a1 && rnd() < it.p }
        if (e != null) { e.fx(this); note(e.t, false, "street", e.d) }
        var d: Decision? = ev(A)
        if (d == null && need && !ret && A < 65) { need = false; d = jobsDec() }
        if (d == null) CDS.firstOrNull { it.age == A }?.let { d = mkDec(it) }
        if (d == null && A in 20..70 && A - lastDec >= 3 && rnd() < 0.09) { lastDec = A; d = mkDec(RDS.random()) }
        clampAll()
        return d
    }
    private fun clampAll() { h = h.coerceIn(0.0, 100.0); j = j.coerceIn(0.0, 100.0); m = m.coerceIn(0.0, 100.0); b = b.coerceIn(0.0, 100.0) }
    private fun die() {
        dead = true; cause = deathCause(c, age, sex)
        lw = LWS[if (j > 60) Random.nextInt(2) else if (j > 35) 2 else 3]
        note("В возрасте $age лет ваша жизнь закончилась ($cause).", true)
    }
}
