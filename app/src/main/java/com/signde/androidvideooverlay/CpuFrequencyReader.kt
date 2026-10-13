package com.signde.androidvideooverlay

/** Keep one slot per CPU even when sysfs denies or omits a reading. Zero means unknown. */
object CpuFrequencyReader {
    fun read(coreCount: Int, readCore: (Int) -> String): List<Long> =
        List(coreCount.coerceAtLeast(0)) { core ->
            runCatching { readCore(core).trim().toLongOrNull()?.takeIf { it > 0 } ?: 0L }
                .getOrDefault(0L)
        }
}
