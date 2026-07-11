package com.omniflow.shared.parser.spreadsheet

import com.omniflow.shared.parser.ImportFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SpreadsheetBillParserTest {
    @Test
    fun parsesWechatAndCcbFixtures() {
        assertEquals(86, SpreadsheetBillParser.parse(ImportFormat.WECHAT, fixture("微信.xlsx").readBytes()).getOrThrow().size)
        assertEquals(152, SpreadsheetBillParser.parse(ImportFormat.CCB, fixture("CCB.xls").readBytes()).getOrThrow().size)
    }

    private fun fixture(name: String): File = File("../examples/$name").takeIf(File::exists)
        ?: File("examples/$name")
}
