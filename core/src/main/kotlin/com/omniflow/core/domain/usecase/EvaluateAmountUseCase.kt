package com.omniflow.core.domain.usecase

import com.omniflow.core.domain.model.Money
import java.math.BigDecimal
import java.math.RoundingMode

class EvaluateAmountUseCase {

    operator fun invoke(expression: String): Result<Money> = runCatching {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("请输入有效金额")
        }

        val tokens = tokenize(trimmed)
        if (tokens.isEmpty()) {
            throw IllegalArgumentException("请输入有效金额")
        }

        val rpn = shuntingYard(tokens)
        val value = evaluateRpn(rpn)

        if (value <= BigDecimal.ZERO) {
            throw IllegalArgumentException("金额必须大于零")
        }

        val rounded = value.setScale(2, RoundingMode.HALF_UP)
        val minor = rounded.movePointRight(2).longValueExact()
        if (minor <= 0L) {
            throw IllegalArgumentException("金额必须大于零")
        }

        Money(minor)
    }

    fun formatToInput(money: Money): String {
        val minor = money.minor
        val yuan = minor / 100L
        val cents = kotlin.math.abs(minor % 100L)
        return when {
            cents == 0L -> yuan.toString()
            cents % 10L == 0L -> "$yuan.${cents / 10L}"
            else -> "$yuan.${cents.toString().padStart(2, '0')}"
        }
    }

    fun evaluateToInput(expression: String): Result<String> =
        invoke(expression).map(::formatToInput)

    private sealed interface Token {
        data class Number(val value: BigDecimal) : Token
        data class Op(val symbol: Char, val precedence: Int) : Token
        data object OpenParen : Token
        data object CloseParen : Token
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val len = expr.length

        while (i < len) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c in "0123456789." -> {
                    val start = i
                    var dotCount = 0
                    while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
                        if (expr[i] == '.') dotCount++
                        if (dotCount > 1) throw IllegalArgumentException("数字格式错误")
                        i++
                    }
                    val numStr = expr.substring(start, i)
                    if (numStr == ".") throw IllegalArgumentException("数字格式错误")
                    val bd = numStr.toBigDecimalOrNull() ?: throw IllegalArgumentException("数字格式错误")
                    tokens.add(Token.Number(bd))
                }
                c == '+' -> { tokens.add(Token.Op('+', 1)); i++ }
                c == '-' -> {
                    // Check if unary minus at the beginning or after an operator/open paren
                    val isUnary = tokens.isEmpty() || tokens.last() is Token.Op || tokens.last() is Token.OpenParen
                    if (isUnary) {
                        // Unary minus: parse following number or treat as 0 - x
                        tokens.add(Token.Number(BigDecimal.ZERO))
                        tokens.add(Token.Op('-', 1))
                    } else {
                        tokens.add(Token.Op('-', 1))
                    }
                    i++
                }
                c == '*' || c == '×' -> { tokens.add(Token.Op('*', 2)); i++ }
                c == '/' || c == '÷' -> { tokens.add(Token.Op('/', 2)); i++ }
                c == '(' -> { tokens.add(Token.OpenParen); i++ }
                c == ')' -> { tokens.add(Token.CloseParen); i++ }
                else -> throw IllegalArgumentException("包含非法字符: '$c'")
            }
        }
        return tokens
    }

    private fun shuntingYard(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = ArrayDeque<Token>()

        var prevToken: Token? = null
        for (token in tokens) {
            when (token) {
                is Token.Number -> output.add(token)
                is Token.Op -> {
                    if (prevToken is Token.Op || prevToken is Token.OpenParen) {
                        throw IllegalArgumentException("算式不完整")
                    }
                    while (stack.isNotEmpty()) {
                        val top = stack.first()
                        if (top is Token.Op && top.precedence >= token.precedence) {
                            output.add(stack.removeFirst())
                        } else {
                            break
                        }
                    }
                    stack.addFirst(token)
                }
                is Token.OpenParen -> stack.addFirst(token)
                is Token.CloseParen -> {
                    var foundOpen = false
                    while (stack.isNotEmpty()) {
                        val top = stack.removeFirst()
                        if (top is Token.OpenParen) {
                            foundOpen = true
                            break
                        }
                        output.add(top)
                    }
                    if (!foundOpen) throw IllegalArgumentException("括号不匹配")
                }
            }
            prevToken = token
        }

        if (prevToken is Token.Op) {
            throw IllegalArgumentException("算式不完整")
        }

        while (stack.isNotEmpty()) {
            val top = stack.removeFirst()
            if (top is Token.OpenParen || top is Token.CloseParen) {
                throw IllegalArgumentException("括号不匹配")
            }
            output.add(top)
        }

        return output
    }

    private fun evaluateRpn(rpn: List<Token>): BigDecimal {
        val stack = ArrayDeque<BigDecimal>()
        for (token in rpn) {
            when (token) {
                is Token.Number -> stack.addFirst(token.value)
                is Token.Op -> {
                    if (stack.size < 2) throw IllegalArgumentException("算式不完整")
                    val b = stack.removeFirst()
                    val a = stack.removeFirst()
                    val res = when (token.symbol) {
                        '+' -> a + b
                        '-' -> a - b
                        '*' -> a * b
                        '/' -> {
                            if (b.compareTo(BigDecimal.ZERO) == 0) {
                                throw IllegalArgumentException("除数不能为零")
                            }
                            a.divide(b, 8, RoundingMode.HALF_UP)
                        }
                        else -> throw IllegalArgumentException("未知运算符")
                    }
                    stack.addFirst(res)
                }
                else -> throw IllegalArgumentException("无效标记")
            }
        }
        if (stack.size != 1) throw IllegalArgumentException("算式解析失败")
        return stack.removeFirst()
    }
}
