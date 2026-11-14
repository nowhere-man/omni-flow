package com.omniflow.core.data.export

import com.omniflow.core.domain.model.AppTimeZone
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

object JsonToCsvExporter {
    fun export(payload: String, timeZone: TimeZone = AppTimeZone): String {
        val root = try {
            Json.parseToJsonElement(payload).jsonObject
        } catch (_: Exception) {
            return ""
        }

        val books = mutableMapOf<String, String>()
        root["bookJsonString"]?.jsonArray?.forEach { el ->
            val obj = el.jsonObject
            val id = obj["id"]?.jsonPrimitive?.contentOrNull
            val name = obj["name"]?.jsonPrimitive?.contentOrNull
            if (id != null && name != null) {
                books[id] = name
            }
        }

        val accounts = mutableMapOf<String, String>()
        root["accountJsonString"]?.jsonArray?.forEach { el ->
            val obj = el.jsonObject
            val id = obj["identifier"]?.jsonPrimitive?.contentOrNull
            val name = obj["name"]?.jsonPrimitive?.contentOrNull
            if (id != null && name != null) {
                accounts[id] = name
            }
        }

        val categories = mutableMapOf<String, Pair<String, String>>()
        root["categoryJsonString"]?.jsonArray?.forEach { el ->
            val obj = el.jsonObject
            val id = obj["identifier"]?.jsonPrimitive?.contentOrNull
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: ""
            val typeVal = obj["type"]?.jsonPrimitive?.intOrNull ?: 1
            val typeStr = if (typeVal == 0) "收入" else "支出"
            if (id != null) {
                categories[id] = Pair(name, typeStr)
            }
        }

        val marks = mutableMapOf<String, String>()
        root["markJsonString"]?.jsonArray?.forEach { el ->
            val obj = el.jsonObject
            val id = obj["id"]?.jsonPrimitive?.contentOrNull
            val name = obj["name"]?.jsonPrimitive?.contentOrNull
            if (id != null && name != null) {
                marks[id] = name
            }
        }

        val sb = StringBuilder()
        sb.append("\uFEFF")
        sb.append("时间,账本,收支类型,分类,账户,金额,标签,备注\n")

        val entries = root["entryJsonString"]?.jsonArray ?: emptyList()
        for (el in entries) {
            val entry = el.jsonObject
            val epochSeconds = entry["createDate"]?.jsonPrimitive?.longOrNull ?: 0L
            val timeText = if (epochSeconds > 0) {
                formatEpochSeconds(epochSeconds, timeZone)
            } else ""
            val bookName = books[entry["bookID"]?.jsonPrimitive?.contentOrNull] ?: ""
            val accountName = accounts[entry["accountID"]?.jsonPrimitive?.contentOrNull] ?: ""
            val catInfo = categories[entry["categoryID"]?.jsonPrimitive?.contentOrNull]
            val catName = catInfo?.first ?: ""
            val typeName = catInfo?.second ?: "支出"
            val value = entry["value"]?.jsonPrimitive?.contentOrNull ?: ""
            val markIds = (entry["markIDs"]?.jsonPrimitive?.contentOrNull ?: "")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val tagsStr = markIds.mapNotNull { marks[it] }.joinToString(";")
            val note = entry["content"]?.jsonPrimitive?.contentOrNull ?: ""

            sb.append(escapeCsv(timeText)).append(",")
            sb.append(escapeCsv(bookName)).append(",")
            sb.append(escapeCsv(typeName)).append(",")
            sb.append(escapeCsv(catName)).append(",")
            sb.append(escapeCsv(accountName)).append(",")
            sb.append(escapeCsv(value)).append(",")
            sb.append(escapeCsv(tagsStr)).append(",")
            sb.append(escapeCsv(note)).append("\n")
        }
        return sb.toString()
    }

    fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun formatEpochSeconds(epochSeconds: Long, timeZone: TimeZone): String {
        val local = Instant.fromEpochMilliseconds(epochSeconds * 1000).toLocalDateTime(timeZone)
        val month = local.monthNumber.toString().padStart(2, '0')
        val day = local.dayOfMonth.toString().padStart(2, '0')
        val hour = local.hour.toString().padStart(2, '0')
        val min = local.minute.toString().padStart(2, '0')
        val sec = local.second.toString().padStart(2, '0')
        return "${local.year}-$month-$day $hour:$min:$sec"
    }
}
