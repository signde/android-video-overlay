package com.signde.androidvideooverlay

import java.io.FileNotFoundException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class CpuFrequencyReaderTest {
    @Test fun preservesValidFrequencyValuesAndCoreOrder() {
        assertEquals(listOf(2208000L, 1800000L),
            CpuFrequencyReader.read(2) { listOf("2208000\n", " 1800000 ")[it] })
    }

    @Test fun missingOrDeniedReadingsDoNotDropOrShiftCores() {
        assertEquals(listOf(0L, 2000000L, 0L, 0L), CpuFrequencyReader.read(4) {
            when (it) {
                0 -> throw FileNotFoundException("denied")
                1 -> "2000000"
                2 -> throw IOException("unavailable")
                else -> throw SecurityException("denied")
            }
        })
    }

    @Test fun emptyInvalidZeroAndNegativeReadingsAreUnknown() {
        val values = listOf("", "unknown", "0", "-100", "99999999999999999999999999")
        assertEquals(List(values.size) { 0L }, CpuFrequencyReader.read(values.size) { values[it] })
    }

    @Test fun independentFrequencyListsStayAlignedOnG10Failure() {
        val current = CpuFrequencyReader.read(4) { "1100000" }
        val minimum = CpuFrequencyReader.read(4) { throw FileNotFoundException("denied") }
        val maximum = CpuFrequencyReader.read(4) { "1300000" }
        assertEquals(current.size, minimum.size)
        assertEquals(current.size, maximum.size)
        assertEquals(List(4) { 0L }, minimum)
    }
}
