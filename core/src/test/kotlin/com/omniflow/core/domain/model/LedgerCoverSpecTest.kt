package com.omniflow.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LedgerCoverSpecTest {
    @Test
    fun parsesLegacyKeysCorrectly() {
        val daily = LedgerCoverSpec.parse("cover-daily")
        assertIs<LedgerCoverSpec.Preset>(daily)
        assertEquals("daily", daily.colorKey)
        assertEquals("grid", daily.patternKey)
        assertEquals("shopping-bag", daily.iconKey)

        val invest = LedgerCoverSpec.parse("cover-invest")
        assertIs<LedgerCoverSpec.Preset>(invest)
        assertEquals("invest", invest.colorKey)
        assertEquals("diagonal", invest.patternKey)
        assertEquals("chart-line", invest.iconKey)
    }

    @Test
    fun encodesAndParsesPresetFormat() {
        val original = LedgerCoverSpec.Preset("travel", "waves", "plane")
        val encoded = original.encode()
        assertEquals("preset:travel:waves:plane", encoded)

        val parsed = LedgerCoverSpec.parse(encoded)
        assertEquals(original, parsed)
    }

    @Test
    fun encodesAndParsesCustomFormat() {
        val original = LedgerCoverSpec.Custom("cover_12345.jpg")
        val encoded = original.encode()
        assertEquals("custom:cover_12345.jpg", encoded)

        val parsed = LedgerCoverSpec.parse(encoded)
        assertEquals(original, parsed)
    }

    @Test
    fun fallsBackDeterministicallyOnNullOrInvalidKey() {
        val fallbackA1 = LedgerCoverSpec.parse(null, "ledger-xyz")
        val fallbackA2 = LedgerCoverSpec.parse("invalid-corrupted-key", "ledger-xyz")
        assertIs<LedgerCoverSpec.Preset>(fallbackA1)
        assertIs<LedgerCoverSpec.Preset>(fallbackA2)
        assertEquals(fallbackA1, fallbackA2)

        val fallbackB = LedgerCoverSpec.parse(null, "ledger-abc")
        assertIs<LedgerCoverSpec.Preset>(fallbackB)
        assertTrue(fallbackB.colorKey in LedgerCoverSpec.PresetColors)
        assertTrue(fallbackB.patternKey in LedgerCoverSpec.PresetPatterns)
        assertTrue(fallbackB.iconKey in LedgerCoverSpec.PresetIcons)
    }
}
