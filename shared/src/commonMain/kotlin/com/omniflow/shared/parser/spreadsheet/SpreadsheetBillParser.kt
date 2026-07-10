package com.omniflow.shared.parser.spreadsheet

import com.omniflow.shared.parser.ImportFormat
import com.omniflow.shared.parser.RawTransaction

expect object SpreadsheetBillParser {
    fun parse(format: ImportFormat, bytes: ByteArray): Result<List<RawTransaction>>
}
