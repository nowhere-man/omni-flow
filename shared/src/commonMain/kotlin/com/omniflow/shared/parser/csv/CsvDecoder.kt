package com.omniflow.shared.parser.csv

enum class CsvCharset { UTF8, GB18030 }

expect object CsvDecoder {
    fun decode(bytes: ByteArray, charset: CsvCharset): String
}
