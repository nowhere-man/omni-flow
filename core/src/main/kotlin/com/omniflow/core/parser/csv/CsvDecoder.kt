package com.omniflow.core.parser.csv

import java.nio.charset.Charset

enum class CsvCharset { UTF8, GB18030 }

object CsvDecoder {
    fun decode(bytes: ByteArray, charset: CsvCharset): String = bytes.toString(
        Charset.forName(
            when (charset) {
                CsvCharset.UTF8 -> "UTF-8"
                CsvCharset.GB18030 -> "GB18030"
            },
        ),
    )
}
