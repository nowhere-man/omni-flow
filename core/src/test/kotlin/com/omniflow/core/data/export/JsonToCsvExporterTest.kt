package com.omniflow.core.data.export

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonToCsvExporterTest {

    @Test
    fun exportsEmptyOrInvalidPayloadGracefully() {
        assertEquals("", JsonToCsvExporter.export(""))
        assertEquals("", JsonToCsvExporter.export("invalid json"))
    }

    @Test
    fun exportsValidJsonPayloadToCsv() {
        val payload = """
        {
            "bookJsonString": [{"id": "b1", "name": "日常账本"}],
            "accountJsonString": [{"identifier": "a1", "name": "招商银行"}],
            "categoryJsonString": [{"identifier": "c1", "name": "餐饮", "type": 1}],
            "markJsonString": [{"id": "m1", "name": "午餐"}],
            "entryJsonString": [
                {
                    "createDate": 1700000000,
                    "bookID": "b1",
                    "accountID": "a1",
                    "categoryID": "c1",
                    "value": "25.50",
                    "markIDs": "m1",
                    "content": "麦当劳,超值套餐"
                }
            ]
        }
        """.trimIndent()

        val csv = JsonToCsvExporter.export(payload, TimeZone.of("Asia/Shanghai"))
        assertTrue(csv.startsWith("\uFEFF时间,账本,收支类型,分类,账户,金额,标签,备注\n"))
        assertTrue(csv.contains("日常账本,支出,餐饮,招商银行,25.50,午餐,\"麦当劳,超值套餐\""))
    }

    @Test
    fun escapeCsvHandlesCommasQuotesAndNewlines() {
        assertEquals("normal", JsonToCsvExporter.escapeCsv("normal"))
        assertEquals("\"hello,world\"", JsonToCsvExporter.escapeCsv("hello,world"))
        assertEquals("\"he said \"\"hi\"\"\"", JsonToCsvExporter.escapeCsv("he said \"hi\""))
        assertEquals("\"line1\nline2\"", JsonToCsvExporter.escapeCsv("line1\nline2"))
    }
}
