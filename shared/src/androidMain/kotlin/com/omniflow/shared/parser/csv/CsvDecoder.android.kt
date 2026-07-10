package com.omniflow.shared.parser.csv

import java.nio.charset.Charset

actual object CsvDecoder {
    actual fun decode(bytes: ByteArray, charset: CsvCharset): String = bytes.toString(
        Charset.forName(
            when (charset) {
                CsvCharset.UTF8 -> "UTF-8"
                CsvCharset.GB18030 -> "GB18030"
            },
        ),
    )
}
