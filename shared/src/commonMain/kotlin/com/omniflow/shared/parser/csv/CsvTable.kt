package com.omniflow.shared.parser.csv

data class CsvTable(
    val header: List<String>,
    val rows: List<List<String>>,
) {
    fun value(row: List<String>, column: String): String? {
        val index = header.indexOf(column)
        return row.getOrNull(index)?.trim()?.takeIf(String::isNotEmpty)
    }
}

object CsvTableParser {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var index = 0

        while (index < text.length) {
            when (val character = text[index]) {
                '"' -> {
                    if (quoted && text.getOrNull(index + 1) == '"') {
                        cell.append('"')
                        index += 1
                    } else {
                        quoted = !quoted
                    }
                }
                ',' -> if (quoted) cell.append(character) else {
                    row += cell.toString()
                    cell.clear()
                }
                '\n' -> if (quoted) cell.append(character) else {
                    row += cell.toString().removeSuffix("\r")
                    cell.clear()
                    rows += row
                    row = mutableListOf()
                }
                else -> cell.append(character)
            }
            index += 1
        }

        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row += cell.toString().removeSuffix("\r")
            rows += row
        }
        return rows
    }

    fun tableAfterHeader(text: String, headerFirstColumn: String): CsvTable {
        val rows = parse(text)
        val headerIndex = rows.indexOfFirst { it.firstOrNull()?.trim()?.removePrefix("\uFEFF") == headerFirstColumn }
        require(headerIndex >= 0) { "未找到账单明细表头" }
        val header = rows[headerIndex].map { it.trim().removePrefix("\uFEFF") }
        return CsvTable(header, rows.drop(headerIndex + 1).filter { row -> row.any { it.isNotBlank() } })
    }
}
