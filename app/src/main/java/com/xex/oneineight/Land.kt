package com.xex.oneineight

// Сгенерировано: грубая маска суши 4° (для стилизованного глобуса)
object Land {
    private val ROWS = arrayOf(
        "00000000000000000000000",
        "00000000000000000000000",
        "0000000fff0000000000000",
        "0000000fff0000003ff8000",
        "003ffe07ff001f07fffff80",
        "1fffffc3f800ffffffffffc",
        "1ffffff1e003ffffffffff8",
        "001ffffc0001fffffffff80",
        "000ffffe0019ffffffff800",
        "0007fffc0007ffffffff000",
        "0003fff8000fffffffff000",
        "0003fff0001cf1bffff8000",
        "0003ffc000103fbfffe1800",
        "0001ff80003fc3ffffe2000",
        "0000ff80003ffbffffe0000",
        "00007800007ffbffffe0000",
        "00003800007ffdf3ff80000",
        "00000e00007fffe1cf00000",
        "00000200007ffe808710000",
        "00000038003fff800410000",
        "0000003e001fff800000000",
        "0000007f0001ff000060000",
        "0000007fc001fe0000c0000",
        "0000007ff001fe000000c00",
        "0000003ff000fe000000000",
        "0000003fe000fe000007800",
        "0000001fe000fe00001f800",
        "0000001fc000fc80007fc00",
        "0000001f80007c00007fe00",
        "0000001f00007800003fe00",
        "0000001e000070000003e00",
        "0000001c000000000000008",
        "00000018000000000000000",
        "00000010000000000000000",
        "00000030000000000000000",
        "00000000000000000000000",
        "00000000000000000000000",
        "00000000000000000000000",
        "00000000000000000000000",
        "00000000000000000000000",
        "00000000000000000000000",
        "00000000000000000000000",
        "00000000000000000000000",
        "00000000000000000000000"
    )
    /** [latRad, lonRad, sinLat, cosLat] */
    val pts: List<DoubleArray> by lazy {
        val o = ArrayList<DoubleArray>()
        for (i in ROWS.indices) for (j in 0 until 90) {
            val bit = (ROWS[i][j / 4].digitToInt(16) shr (3 - j % 4)) and 1
            if (bit == 1) {
                val la = Math.toRadians(86.0 - 4 * i); val lo = Math.toRadians(-178.0 + 4 * j)
                o.add(doubleArrayOf(la, lo, Math.sin(la), Math.cos(la)))
            }
        }
        o
    }
}
