package com.omniflow.core.parser.pdf

import java.io.ByteArrayOutputStream
import java.util.zip.Inflater

/**
 * 够用就好的 PDF 对象层。
 *
 * 银行流水 PDF 只需要「按坐标取出文字」，为此引入 PDFBox 这类完整实现代价太大
 * （体积、方法数、又一个需要跟进的依赖），所以这里只实现真正用得到的一小块：
 * 对象扫描、FlateDecode、页面树、字体宽度和 ToUnicode。加密、混合滤镜、
 * 非 Identity CMap 一律不支持，遇到就让上层解析失败并提示换文件。
 *
 * 不解析交叉引用表：银行导出的文件常带增量更新，顺序扫描 `N G obj` 反而更稳，
 * 后出现的同号对象覆盖先出现的，正好符合增量更新的语义。
 */
internal class PdfDocument(bytes: ByteArray) {
    private val raw = String(bytes, Charsets.ISO_8859_1)
    private val objects: Map<Int, PdfObject> = indexObjects(raw)
    private val streamCache = HashMap<Int, ByteArray?>()

    fun pages(): List<PdfPage> {
        val catalog = objects.entries.firstOrNull { entry ->
            PdfSyntax.value(entry.value.dict, "/Type") == "/Catalog"
        }?.value
        val rootRef = catalog?.let { PdfSyntax.value(it.dict, "/Pages") }
        val root = rootRef?.let(::resolveDict)
        val collected = mutableListOf<PdfPage>()
        if (root != null) collectPages(root, null, collected, HashSet())
        if (collected.isNotEmpty()) return collected
        // 页面树坏掉时退回按对象号扫 /Type /Page，顺序未必对，但总比整份解析失败强
        return objects.entries
            .filter { PdfSyntax.value(it.value.dict, "/Type") == "/Page" }
            .sortedBy { it.key }
            .map { PdfPage(it.value.dict, PdfSyntax.value(it.value.dict, "/Resources")) }
    }

    /** 页面内容流，多个 `/Contents` 会拼成一段。 */
    fun contentOf(page: PdfPage): String {
        val contents = PdfSyntax.value(page.dict, "/Contents") ?: return ""
        val references = if (contents.startsWith("[")) {
            PdfSyntax.tokens(contents.removeSurrounding("[", "]"))
        } else {
            listOf(contents)
        }
        return references
            .mapNotNull { reference -> streamOf(reference) }
            .joinToString("\n") { String(it, Charsets.ISO_8859_1) }
    }

    /** 页面资源里的字体表，键是内容流里 `Tf` 用的名字（如 `/F1`）。 */
    fun fontsOf(page: PdfPage): Map<String, PdfFont> {
        val resources = page.resources?.let(::resolveDict) ?: return emptyMap()
        val fonts = PdfSyntax.value(resources, "/Font")?.let(::resolveDict) ?: return emptyMap()
        return PdfSyntax.entries(fonts).associate { (name, reference) ->
            name to loadFont(reference)
        }
    }

    private fun loadFont(reference: String): PdfFont {
        val dict = resolveDict(reference) ?: return PdfFont.Fallback
        val isType0 = PdfSyntax.value(dict, "/Subtype") == "/Type0"
        val toUnicode = PdfSyntax.value(dict, "/ToUnicode")
            ?.let { streamOf(it) }
            ?.let { PdfCMap.parse(String(it, Charsets.ISO_8859_1)) }
            .orEmpty()
        if (!isType0) {
            val first = PdfSyntax.value(dict, "/FirstChar")?.toIntOrNull() ?: 0
            val widths = PdfSyntax.value(dict, "/Widths")
                ?.let(::resolveArray)
                ?.mapNotNull(String::toFloatOrNull)
                .orEmpty()
            return PdfFont(
                twoByte = false,
                toUnicode = toUnicode,
                defaultWidth = 500f,
                widths = widths.withIndex().associate { (index, width) -> first + index to width },
            )
        }
        val descendant = PdfSyntax.value(dict, "/DescendantFonts")
            ?.let(::resolveArray)
            ?.firstOrNull()
            ?.let(::resolveDict)
        return PdfFont(
            twoByte = true,
            toUnicode = toUnicode,
            defaultWidth = descendant?.let { PdfSyntax.value(it, "/DW")?.toFloatOrNull() } ?: 1000f,
            widths = descendant?.let { PdfSyntax.value(it, "/W")?.let(::resolveArray) }
                ?.let(::parseCidWidths)
                .orEmpty(),
        )
    }

    /** `/W [ c [w1 w2 …] cFirst cLast w … ]`，两种写法混排。 */
    private fun parseCidWidths(tokens: List<String>): Map<Int, Float> {
        val widths = HashMap<Int, Float>()
        var index = 0
        while (index < tokens.size) {
            val first = tokens[index].toIntOrNull() ?: break
            val next = tokens.getOrNull(index + 1) ?: break
            if (next.startsWith("[")) {
                PdfSyntax.tokens(next.removeSurrounding("[", "]")).forEachIndexed { offset, value ->
                    value.toFloatOrNull()?.let { widths[first + offset] = it }
                }
                index += 2
            } else {
                val last = next.toIntOrNull() ?: break
                val width = tokens.getOrNull(index + 2)?.toFloatOrNull() ?: break
                // 畸形文件可能写出跨度极大的区间，挡一下免得把内存打满
                if (last - first in 0..MAX_CID_SPAN) {
                    for (code in first..last) widths[code] = width
                }
                index += 3
            }
        }
        return widths
    }

    private fun collectPages(
        node: String,
        inheritedResources: String?,
        into: MutableList<PdfPage>,
        visited: MutableSet<String>,
    ) {
        val resources = PdfSyntax.value(node, "/Resources") ?: inheritedResources
        if (PdfSyntax.value(node, "/Type") == "/Page") {
            into += PdfPage(node, resources)
            return
        }
        val kids = PdfSyntax.value(node, "/Kids")?.let(::resolveArray) ?: return
        kids.forEach { reference ->
            if (visited.add(reference)) {
                resolveDict(reference)?.let { collectPages(it, resources, into, visited) }
            }
        }
    }

    private fun resolveDict(token: String): String? = when {
        token.startsWith("<<") -> token
        else -> referenceNumber(token)?.let { objects[it]?.dict }
    }

    private fun resolveArray(token: String): List<String>? {
        val array = when {
            token.startsWith("[") -> token
            else -> referenceNumber(token)?.let { objects[it]?.dict }?.takeIf { it.trimStart().startsWith("[") }
        } ?: return null
        return PdfSyntax.tokens(array.trim().removeSurrounding("[", "]"))
    }

    private fun streamOf(token: String): ByteArray? {
        val number = referenceNumber(token) ?: return null
        return streamCache.getOrPut(number) {
            objects[number]?.let { decodeStream(it.dict, it.stream ?: return@getOrPut null) }
        }
    }

    private fun referenceNumber(token: String): Int? =
        REFERENCE.find(token)?.groupValues?.get(1)?.toIntOrNull() ?: token.trim().toIntOrNull()

    private companion object {
        const val MAX_CID_SPAN = 65_535
        val REFERENCE = Regex("""^\s*(\d+)\s+\d+\s+R\s*$""")
    }
}

internal class PdfPage(val dict: String, val resources: String?)

internal class PdfObject(val dict: String, val stream: ByteArray?)

/** 字体的编码宽度信息；[twoByte] 对应 Identity-H 这类两字节 CID 编码。 */
internal class PdfFont(
    val twoByte: Boolean,
    private val toUnicode: Map<Int, String>,
    private val defaultWidth: Float,
    private val widths: Map<Int, Float>,
) {
    fun text(code: Int): String = toUnicode[code]
        ?: if (!twoByte && code in 0x20..0x7E) code.toChar().toString() else ""

    /** 千分之一 em 为单位的字宽。 */
    fun width(code: Int): Float = widths[code] ?: defaultWidth

    companion object {
        val Fallback = PdfFont(twoByte = false, toUnicode = emptyMap(), defaultWidth = 500f, widths = emptyMap())
    }
}

/** `beginbfchar` / `beginbfrange` 两种映射都要认，一份 CMap 里可以出现多段。 */
internal object PdfCMap {
    fun parse(text: String): Map<Int, String> {
        val mapping = HashMap<Int, String>()
        SECTION.findAll(text).forEach { section ->
            val kind = section.groupValues[1]
            val body = section.groupValues[2]
            if (kind == "bfchar") parseChars(body, mapping) else parseRanges(body, mapping)
        }
        return mapping
    }

    private fun parseChars(body: String, into: MutableMap<Int, String>) {
        val tokens = HEX.findAll(body).map { it.groupValues[1] }.toList()
        tokens.chunked(2).forEach { pair ->
            if (pair.size == 2) {
                pair[0].toIntOrNull(16)?.let { code -> into[code] = utf16(pair[1]) }
            }
        }
    }

    private fun parseRanges(body: String, into: MutableMap<Int, String>) {
        var index = 0
        while (index < body.length) {
            val low = HEX.find(body, index) ?: break
            val high = HEX.find(body, low.range.last + 1) ?: break
            val first = low.groupValues[1].toIntOrNull(16) ?: break
            val last = high.groupValues[1].toIntOrNull(16) ?: break
            val rest = PdfSyntax.skipWhitespace(body, high.range.last + 1)
            if (rest < body.length && body[rest] == '[') {
                val close = body.indexOf(']', rest).takeIf { it > 0 } ?: break
                HEX.findAll(body.substring(rest, close)).forEachIndexed { offset, value ->
                    into[first + offset] = utf16(value.groupValues[1])
                }
                index = close + 1
            } else {
                val destination = HEX.find(body, rest) ?: break
                val base = destination.groupValues[1]
                if (last - first in 0..MAX_RANGE) {
                    for (code in first..last) {
                        into[code] = shift(base, code - first)
                    }
                }
                index = destination.range.last + 1
            }
        }
    }

    /** bfrange 的目标是起点，范围内第 n 个码位对应目标的低位加 n。 */
    private fun shift(base: String, offset: Int): String {
        if (offset == 0) return utf16(base)
        val units = base.chunked(4).mapNotNull { it.toIntOrNull(16) }
        if (units.isEmpty()) return ""
        val shifted = units.toMutableList()
        shifted[shifted.lastIndex] = (shifted.last() + offset) and 0xFFFF
        return shifted.map(Int::toChar).joinToString("")
    }

    private fun utf16(hex: String): String = hex.chunked(4)
        .mapNotNull { it.padEnd(4, '0').toIntOrNull(16) }
        .map(Int::toChar)
        .joinToString("")

    private const val MAX_RANGE = 0xFFFF
    private val SECTION = Regex("""begin(bfchar|bfrange)(.*?)end(?:bfchar|bfrange)""", RegexOption.DOT_MATCHES_ALL)
    private val HEX = Regex("""<([0-9A-Fa-f]*)>""")
}

/** PDF 词法层：字典取值、token 切分，只覆盖本仓库用到的语法。 */
internal object PdfSyntax {
    fun skipWhitespace(text: String, from: Int): Int {
        var index = from
        while (index < text.length) {
            val character = text[index]
            when {
                character == '%' -> while (index < text.length && text[index] != '\n' && text[index] != '\r') index++
                character.isPdfWhitespace() -> index++
                else -> return index
            }
        }
        return index
    }

    /** 取字典里某个键的原始值文本，取不到返回 null。 */
    fun value(dict: String, key: String): String? {
        var index = 0
        while (true) {
            val found = dict.indexOf(key, index).takeIf { it >= 0 } ?: return null
            val after = found + key.length
            val boundary = after >= dict.length || dict[after].isPdfWhitespace() || dict[after].isPdfDelimiter()
            if (boundary && isTopLevelKey(dict, found)) return token(dict, after)?.first
            index = found + 1
        }
    }

    /** 把 `<< /A 1 /B 2 >>` 拆成键值对，只看最外层。 */
    fun entries(dict: String): List<Pair<String, String>> {
        val body = dict.trim().removeSurrounding("<<", ">>")
        val result = mutableListOf<Pair<String, String>>()
        var index = 0
        while (true) {
            val (name, afterName) = token(body, index) ?: break
            if (!name.startsWith("/")) {
                index = afterName
                continue
            }
            val (value, afterValue) = token(body, afterName) ?: break
            result += name to value
            index = afterValue
        }
        return result
    }

    /** 把一段内容按 PDF 词法切成 token，数组里的元素也走这里。 */
    fun tokens(text: String): List<String> {
        val result = mutableListOf<String>()
        var index = 0
        while (true) {
            val (value, next) = token(text, index) ?: break
            result += value
            index = next
        }
        return result
    }

    fun token(text: String, from: Int): Pair<String, Int>? {
        val start = skipWhitespace(text, from)
        if (start >= text.length) return null
        return when {
            text.startsWith("<<", start) -> balanced(text, start, "<<", ">>")
            text[start] == '[' -> balanced(text, start, "[", "]")
            text[start] == '(' -> literal(text, start)
            text[start] == '<' -> {
                val close = text.indexOf('>', start).takeIf { it >= 0 } ?: return null
                text.substring(start, close + 1) to close + 1
            }
            text[start] == ']' || text[start] == '>' || text[start] == ')' || text[start] == '}' ->
                text.substring(start, start + 1) to start + 1
            text[start] == '/' -> {
                var index = start + 1
                while (index < text.length && !text[index].isPdfWhitespace() && !text[index].isPdfDelimiter()) index++
                text.substring(start, index) to index
            }
            else -> {
                referenceEnd(text, start)?.let { end -> return text.substring(start, end).trim() to end }
                var index = start
                while (index < text.length && !text[index].isPdfWhitespace() && !text[index].isPdfDelimiter()) index++
                if (index == start) index++
                text.substring(start, index) to index
            }
        }
    }

    /**
     * 就地识别 `12 0 R` 这种间接引用，命中返回结束位置。
     *
     * 这里必须手写而不能用 `Regex.find(text, start)`：内容流里每个数字都会走一次，
     * 而 find 匹配不上就会一路扫到字符串末尾，几万个 token 叠起来就是 O(n²)，
     * 16 页的流水能跑到两分半。
     */
    private fun referenceEnd(text: String, start: Int): Int? {
        var index = digits(text, start) ?: return null
        index = whitespace(text, index) ?: return null
        index = digits(text, index) ?: return null
        index = whitespace(text, index) ?: return null
        if (text.getOrNull(index) != 'R') return null
        index++
        val following = text.getOrNull(index)
        if (following != null && !following.isPdfWhitespace() && !following.isPdfDelimiter()) return null
        return index
    }

    private fun digits(text: String, from: Int): Int? {
        var index = from
        while (index < text.length && text[index] in '0'..'9') index++
        return index.takeIf { it > from }
    }

    private fun whitespace(text: String, from: Int): Int? {
        var index = from
        while (index < text.length && text[index].isPdfWhitespace()) index++
        return index.takeIf { it > from }
    }

    /** 值必须是最外层字典的键，避免匹配到嵌套字典里的同名键。 */
    private fun isTopLevelKey(dict: String, position: Int): Boolean {
        var depth = 0
        var index = dict.indexOf("<<").takeIf { it >= 0 }?.plus(2) ?: 0
        if (index > 0) depth = 1
        while (index < position) {
            when {
                dict.startsWith("<<", index) -> { depth++; index += 2 }
                dict.startsWith(">>", index) -> { depth--; index += 2 }
                dict[index] == '(' -> index = literalEnd(dict, index)
                else -> index++
            }
        }
        return depth <= 1
    }

    private fun balanced(text: String, start: Int, open: String, close: String): Pair<String, Int> {
        var depth = 0
        var index = start
        while (index < text.length) {
            when {
                text.startsWith(open, index) -> { depth++; index += open.length }
                text.startsWith(close, index) -> {
                    depth--
                    index += close.length
                    if (depth == 0) return text.substring(start, index) to index
                }
                text[index] == '(' -> index = literalEnd(text, index)
                else -> index++
            }
        }
        return text.substring(start) to text.length
    }

    private fun literal(text: String, start: Int): Pair<String, Int> {
        val end = literalEnd(text, start)
        return text.substring(start, end) to end
    }

    private fun literalEnd(text: String, start: Int): Int {
        var depth = 0
        var index = start
        while (index < text.length) {
            when (text[index]) {
                '\\' -> index++
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return index + 1
                }
            }
            index++
        }
        return text.length
    }
}

internal fun Char.isPdfWhitespace(): Boolean = this == ' ' || this == '\n' || this == '\r' ||
    this == '\t' || this == '\u000C' || this == '\u0000'

internal fun Char.isPdfDelimiter(): Boolean = this == '(' || this == ')' || this == '<' || this == '>' ||
    this == '[' || this == ']' || this == '{' || this == '}' || this == '/' || this == '%'

private val OBJECT_HEADER = Regex("""(\d{1,10})\s+(\d{1,5})\s+obj(?![A-Za-z0-9])""")
private val LENGTH = Regex("""/Length(?![A-Za-z0-9])\s*(\d+)""")

/**
 * 顺序扫描对象。碰到流对象时按 `/Length` 跳过二进制数据，
 * 否则正则会在压缩字节里匹配到假的 `N G obj`，把后面的对象全扫歪。
 */
private fun indexObjects(raw: String): Map<Int, PdfObject> {
    val objects = LinkedHashMap<Int, PdfObject>()
    var position = 0
    while (position < raw.length) {
        val header = OBJECT_HEADER.find(raw, position) ?: break
        val number = header.groupValues[1].toIntOrNull() ?: break
        val bodyStart = header.range.last + 1
        val streamAt = keywordIndex(raw, "stream", bodyStart)
        val endAt = keywordIndex(raw, "endobj", bodyStart)
        if (streamAt >= 0 && (endAt < 0 || streamAt < endAt)) {
            val dict = raw.substring(bodyStart, streamAt)
            var dataStart = streamAt + "stream".length
            if (raw.getOrNull(dataStart) == '\r') dataStart++
            if (raw.getOrNull(dataStart) == '\n') dataStart++
            val declared = LENGTH.find(dict)?.groupValues?.get(1)?.toIntOrNull()
            val dataEnd = declared
                ?.let { dataStart + it }
                ?.takeIf { it <= raw.length && raw.startsWith("endstream", PdfSyntax.skipWhitespace(raw, it)) }
                ?: keywordIndex(raw, "endstream", dataStart).takeIf { it >= 0 }
                ?: raw.length
            objects[number] = PdfObject(dict, raw.substring(dataStart, dataEnd).toByteArray(Charsets.ISO_8859_1))
            position = dataEnd
        } else {
            val end = if (endAt >= 0) endAt else raw.length
            objects[number] = PdfObject(raw.substring(bodyStart, end), null)
            position = end + "endobj".length
        }
    }
    return objects
}

/** 关键字必须独立成词，否则 `stream` 会先匹配到 `endstream`。 */
private fun keywordIndex(raw: String, keyword: String, from: Int): Int {
    var index = from
    while (true) {
        val found = raw.indexOf(keyword, index).takeIf { it >= 0 } ?: return -1
        val before = raw.getOrNull(found - 1)
        val after = raw.getOrNull(found + keyword.length)
        val startsWord = before == null || before.isPdfWhitespace() || before.isPdfDelimiter()
        val endsWord = after == null || after.isPdfWhitespace() || after.isPdfDelimiter()
        if (startsWord && endsWord) return found
        index = found + 1
    }
}

internal fun decodeStream(dict: String, data: ByteArray): ByteArray? {
    if (!dict.contains("/FlateDecode")) return data
    val inflated = inflate(data) ?: return null
    val predictor = PdfSyntax.value(dict, "/DecodeParms")
        ?.let { PdfSyntax.value(it, "/Predictor") }
        ?.toIntOrNull()
        ?: return inflated
    if (predictor < 10) return inflated
    val columns = PdfSyntax.value(dict, "/DecodeParms")
        ?.let { PdfSyntax.value(it, "/Columns") }
        ?.toIntOrNull()
        ?: 1
    return undoPngPredictor(inflated, columns)
}

private fun inflate(data: ByteArray): ByteArray? = runCatching {
    val inflater = Inflater()
    inflater.setInput(data)
    val output = ByteArrayOutputStream(data.size * 4)
    val buffer = ByteArray(16 * 1024)
    while (!inflater.finished()) {
        val produced = inflater.inflate(buffer)
        if (produced == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
        output.write(buffer, 0, produced)
    }
    inflater.end()
    output.toByteArray()
}.getOrNull()

/** PNG 预测器，只在 `/DecodeParms` 声明时才走。 */
private fun undoPngPredictor(data: ByteArray, columns: Int): ByteArray {
    val rowLength = columns + 1
    if (rowLength <= 1 || data.size < rowLength) return data
    val output = ByteArrayOutputStream(data.size)
    val previous = ByteArray(columns)
    var offset = 0
    while (offset + rowLength <= data.size) {
        val filter = data[offset].toInt() and 0xFF
        val current = data.copyOfRange(offset + 1, offset + rowLength)
        for (index in current.indices) {
            val rawByte = current[index].toInt() and 0xFF
            val left = if (index > 0) current[index - 1].toInt() and 0xFF else 0
            val up = previous[index].toInt() and 0xFF
            val upperLeft = if (index > 0) previous[index - 1].toInt() and 0xFF else 0
            val restored = when (filter) {
                1 -> rawByte + left
                2 -> rawByte + up
                3 -> rawByte + (left + up) / 2
                4 -> rawByte + paeth(left, up, upperLeft)
                else -> rawByte
            }
            current[index] = (restored and 0xFF).toByte()
        }
        output.write(current)
        current.copyInto(previous)
        offset += rowLength
    }
    return output.toByteArray()
}

private fun paeth(left: Int, up: Int, upperLeft: Int): Int {
    val estimate = left + up - upperLeft
    val distanceLeft = kotlin.math.abs(estimate - left)
    val distanceUp = kotlin.math.abs(estimate - up)
    val distanceUpperLeft = kotlin.math.abs(estimate - upperLeft)
    return when {
        distanceLeft <= distanceUp && distanceLeft <= distanceUpperLeft -> left
        distanceUp <= distanceUpperLeft -> up
        else -> upperLeft
    }
}
