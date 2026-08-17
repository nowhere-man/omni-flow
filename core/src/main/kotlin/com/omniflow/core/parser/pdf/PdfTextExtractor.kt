package com.omniflow.core.parser.pdf

/**
 * 页面上的一段文字。[x] / [y] 是 PDF 用户空间坐标（原点在左下角），[width] 是排版宽度。
 * 表格账单靠 [centerX] 判断落在哪一列——银行流水里数字是右对齐、汉字是居中，
 * 只看起始 x 会把长金额判到隔壁列去。
 */
data class PdfTextRun(val x: Float, val y: Float, val width: Float, val text: String) {
    val centerX: Float get() = x + width / 2f
}

/**
 * 从 PDF 里取出带坐标的文字。只支持未加密、FlateDecode、Identity-H / 简单编码的文件，
 * 也就是银行和账单平台导出的那一类；其他情况返回空列表，由上层提示换文件。
 */
object PdfTextExtractor {
    /** 逐页返回文字段，页内顺序即绘制顺序。 */
    fun extract(bytes: ByteArray): List<List<PdfTextRun>> = runCatching {
        val document = PdfDocument(bytes)
        document.pages().map { page ->
            ContentInterpreter(document.fontsOf(page)).read(document.contentOf(page))
        }
    }.getOrElse { emptyList() }
}

private class Matrix(
    val a: Float,
    val b: Float,
    val c: Float,
    val d: Float,
    val e: Float,
    val f: Float,
) {
    fun times(other: Matrix) = Matrix(
        a = a * other.a + b * other.c,
        b = a * other.b + b * other.d,
        c = c * other.a + d * other.c,
        d = c * other.b + d * other.d,
        e = e * other.a + f * other.c + other.e,
        f = e * other.b + f * other.d + other.f,
    )

    companion object {
        val Identity = Matrix(1f, 0f, 0f, 1f, 0f, 0f)
        fun translation(x: Float, y: Float) = Matrix(1f, 0f, 0f, 1f, x, y)
    }
}

private class PdfName(val value: String)
private class PdfOperator(val value: String)
private class PdfString(val codes: IntArray)

/**
 * 内容流解释器。只实现文字定位相关的算子，图形算子里只跟 `q` / `Q` / `cm`，
 * 因为文字位置要乘当前变换矩阵。
 *
 * 一段文字什么时候算「一段」：从设置位置（`Tm` / `Td` / `TD` / `T*` / `BT`）到下一次设置位置之间
 * 的所有显示算子合成一段。这样不必依赖字宽估算就能拿到准确的段起点，
 * 段宽度则按 `/W` 里的真实字宽累加得到。
 */
private class ContentInterpreter(private val fonts: Map<String, PdfFont>) {
    private var ctm = Matrix.Identity
    private val graphicsStack = ArrayDeque<Matrix>()
    private var textMatrix = Matrix.Identity
    private var lineMatrix = Matrix.Identity
    private var font: PdfFont = PdfFont.Fallback
    private var fontSize = 0f
    private var leading = 0f
    private var charSpacing = 0f
    private var wordSpacing = 0f
    private var horizontalScale = 1f

    private val runs = mutableListOf<PdfTextRun>()
    private val pending = StringBuilder()
    private var pendingX = 0f
    private var pendingY = 0f
    private var pendingAdvance = 0f
    private var hasPending = false

    fun read(content: String): List<PdfTextRun> {
        val operands = mutableListOf<Any>()
        var index = 0
        while (index < content.length) {
            val start = PdfSyntax.skipWhitespace(content, index)
            if (start >= content.length) break
            val (token, next) = lex(content, start) ?: break
            index = next
            if (token is PdfOperator) {
                execute(token.value, operands)
                operands.clear()
            } else {
                operands += token
                // 内容流里不该出现长操作数序列，出现说明解析跑偏了，丢掉避免无限堆积
                if (operands.size > MAX_OPERANDS) operands.clear()
            }
        }
        flush()
        return runs
    }

    private fun lex(content: String, start: Int): Pair<Any, Int>? = when {
        content[start] == '[' -> {
            val items = mutableListOf<Any>()
            var index = start + 1
            while (index < content.length) {
                val cursor = PdfSyntax.skipWhitespace(content, index)
                if (cursor >= content.length || content[cursor] == ']') {
                    index = (cursor + 1).coerceAtMost(content.length)
                    break
                }
                val element = lex(content, cursor)
                if (element == null) {
                    index = content.length
                    break
                }
                items += element.first
                index = element.second
            }
            (items as Any) to index
        }
        content[start] == '(' -> {
            val (literal, next) = PdfSyntax.token(content, start) ?: return null
            PdfString(decodeLiteral(literal)) to next
        }
        content.startsWith("<<", start) -> {
            val (dict, next) = PdfSyntax.token(content, start) ?: return null
            PdfName(dict) to next
        }
        content[start] == '<' -> {
            val (hex, next) = PdfSyntax.token(content, start) ?: return null
            PdfString(decodeHex(hex.removeSurrounding("<", ">"))) to next
        }
        content[start] == '/' -> {
            val (name, next) = PdfSyntax.token(content, start) ?: return null
            PdfName(name) to next
        }
        else -> {
            val (word, next) = PdfSyntax.token(content, start) ?: return null
            val number = word.toFloatOrNull()
            if (number != null) number to next else PdfOperator(word) to next
        }
    }

    private fun execute(operator: String, operands: List<Any>) {
        when (operator) {
            "q" -> graphicsStack.addLast(ctm)
            "Q" -> graphicsStack.removeLastOrNull()?.let { ctm = it }
            "cm" -> matrix(operands)?.let { ctm = it.times(ctm) }
            "BT" -> {
                flush()
                textMatrix = Matrix.Identity
                lineMatrix = Matrix.Identity
            }
            "ET" -> flush()
            "Tf" -> {
                font = (operands.firstOrNull() as? PdfName)?.let { fonts[it.value] } ?: PdfFont.Fallback
                fontSize = operands.number(1) ?: fontSize
            }
            "TL" -> leading = operands.number(0) ?: leading
            "Tc" -> charSpacing = operands.number(0) ?: charSpacing
            "Tw" -> wordSpacing = operands.number(0) ?: wordSpacing
            "Tz" -> horizontalScale = (operands.number(0) ?: 100f) / 100f
            "Tm" -> matrix(operands)?.let {
                flush()
                textMatrix = it
                lineMatrix = it
            }
            "Td" -> nextLine(operands.number(0) ?: 0f, operands.number(1) ?: 0f)
            "TD" -> {
                leading = -(operands.number(1) ?: 0f)
                nextLine(operands.number(0) ?: 0f, operands.number(1) ?: 0f)
            }
            "T*" -> nextLine(0f, -leading)
            "Tj" -> (operands.lastOrNull() as? PdfString)?.let(::show)
            "'" -> {
                nextLine(0f, -leading)
                (operands.lastOrNull() as? PdfString)?.let(::show)
            }
            "\"" -> {
                wordSpacing = operands.number(0) ?: wordSpacing
                charSpacing = operands.number(1) ?: charSpacing
                nextLine(0f, -leading)
                (operands.lastOrNull() as? PdfString)?.let(::show)
            }
            "TJ" -> @Suppress("UNCHECKED_CAST") (operands.lastOrNull() as? List<Any>)?.forEach { element ->
                when (element) {
                    is PdfString -> show(element)
                    is Float -> pendingAdvance += -element / 1000f * fontSize * horizontalScale
                    else -> Unit
                }
            }
        }
    }

    private fun nextLine(x: Float, y: Float) {
        flush()
        lineMatrix = Matrix.translation(x, y).times(lineMatrix)
        textMatrix = lineMatrix
    }

    private fun show(string: PdfString) {
        if (!hasPending) {
            val origin = textMatrix.times(ctm)
            pendingX = origin.e
            pendingY = origin.f
            pendingAdvance = 0f
            pending.setLength(0)
            hasPending = true
        }
        val codes = if (font.twoByte) string.codes.pairs() else string.codes
        codes.forEach { code ->
            pending.append(font.text(code))
            val glyph = font.width(code) / 1000f * fontSize
            val spacing = charSpacing + if (!font.twoByte && code == SPACE) wordSpacing else 0f
            pendingAdvance += (glyph + spacing) * horizontalScale
        }
    }

    private fun flush() {
        if (!hasPending) return
        hasPending = false
        val text = pending.toString()
        pending.setLength(0)
        if (text.isBlank()) return
        val combined = textMatrix.times(ctm)
        runs += PdfTextRun(
            x = pendingX,
            y = pendingY,
            width = pendingAdvance * combined.a,
            text = text,
        )
    }

    private fun matrix(operands: List<Any>): Matrix? {
        if (operands.size < 6) return null
        val values = operands.takeLast(6).map { it as? Float ?: return null }
        return Matrix(values[0], values[1], values[2], values[3], values[4], values[5])
    }

    private fun List<Any>.number(index: Int): Float? = getOrNull(index) as? Float

    private fun decodeLiteral(literal: String): IntArray {
        val body = literal.removeSurrounding("(", ")")
        val codes = mutableListOf<Int>()
        var index = 0
        while (index < body.length) {
            val character = body[index]
            if (character != '\\') {
                codes += character.code and 0xFF
                index++
                continue
            }
            index++
            if (index >= body.length) break
            when (val escaped = body[index]) {
                'n' -> { codes += '\n'.code; index++ }
                'r' -> { codes += '\r'.code; index++ }
                't' -> { codes += '\t'.code; index++ }
                'b' -> { codes += 0x08; index++ }
                'f' -> { codes += 0x0C; index++ }
                '\n' -> index++
                '\r' -> {
                    index++
                    if (index < body.length && body[index] == '\n') index++
                }
                in '0'..'7' -> {
                    var digits = 0
                    var value = 0
                    while (index < body.length && digits < 3 && body[index] in '0'..'7') {
                        value = value * 8 + (body[index] - '0')
                        index++
                        digits++
                    }
                    codes += value and 0xFF
                }
                else -> { codes += escaped.code and 0xFF; index++ }
            }
        }
        return codes.toIntArray()
    }

    private fun decodeHex(hex: String): IntArray {
        val digits = hex.filter { !it.isPdfWhitespace() }
        return digits.chunked(2)
            .mapNotNull { it.padEnd(2, '0').toIntOrNull(16) }
            .toIntArray()
    }

    /** 两字节编码：高位在前，落单的尾字节按 0 补齐。 */
    private fun IntArray.pairs(): IntArray = IntArray((size + 1) / 2) { index ->
        val high = this[index * 2]
        val low = getOrElse(index * 2 + 1) { 0 }
        (high shl 8) or low
    }

    private companion object {
        const val SPACE = 32
        const val MAX_OPERANDS = 64
    }
}
