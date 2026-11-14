package com.omniflow.core.domain.usecase

import com.omniflow.core.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvaluateAmountUseCaseTest {
    private val evaluate = EvaluateAmountUseCase()

    @Test
    fun evaluatesSingleNumber() {
        assertEquals(Money(2000), evaluate("20").getOrThrow())
        assertEquals(Money(2050), evaluate("20.5").getOrThrow())
        assertEquals(Money(2055), evaluate("20.55").getOrThrow())
        assertEquals(Money(99), evaluate("0.99").getOrThrow())
    }

    @Test
    fun evaluatesAdditionAndFormatsCorrectly() {
        val result = evaluate("12+8")
        assertTrue(result.isSuccess)
        assertEquals(Money(2000), result.getOrThrow())
        assertEquals("20", evaluate.formatToInput(result.getOrThrow()))
    }

    @Test
    fun evaluatesSubtractionAndDecimals() {
        val result = evaluate("50-15.5")
        assertTrue(result.isSuccess)
        assertEquals(Money(3450), result.getOrThrow())
        assertEquals("34.5", evaluate.formatToInput(result.getOrThrow()))
    }

    @Test
    fun evaluatesMultiplication() {
        val result = evaluate("15*3")
        assertTrue(result.isSuccess)
        assertEquals(Money(4500), result.getOrThrow())
        assertEquals("45", evaluate.formatToInput(result.getOrThrow()))

        val unicodeMult = evaluate("2.5×4")
        assertTrue(unicodeMult.isSuccess)
        assertEquals(Money(1000), unicodeMult.getOrThrow())
        assertEquals("10", evaluate.formatToInput(unicodeMult.getOrThrow()))
    }

    @Test
    fun evaluatesDivisionWithRounding() {
        val result = evaluate("100/3")
        assertTrue(result.isSuccess)
        assertEquals(Money(3333), result.getOrThrow())
        assertEquals("33.33", evaluate.formatToInput(result.getOrThrow()))

        val unicodeDiv = evaluate("10÷4")
        assertTrue(unicodeDiv.isSuccess)
        assertEquals(Money(250), unicodeDiv.getOrThrow())
        assertEquals("2.5", evaluate.formatToInput(unicodeDiv.getOrThrow()))
    }

    @Test
    fun respectsOperatorPrecedenceAndParentheses() {
        assertEquals(Money(2000), evaluate("10+5*2").getOrThrow())
        assertEquals(Money(3000), evaluate("(10+5)*2").getOrThrow())
    }

    @Test
    fun rejectsDivisionByZero() {
        val result = evaluate("10/0")
        assertTrue(result.isFailure)
        assertEquals("除数不能为零", result.exceptionOrNull()?.message)
    }

    @Test
    fun rejectsZeroOrNegativeResult() {
        val zero = evaluate("10-10")
        assertTrue(zero.isFailure)
        assertEquals("金额必须大于零", zero.exceptionOrNull()?.message)

        val negative = evaluate("5-10")
        assertTrue(negative.isFailure)
        assertEquals("金额必须大于零", negative.exceptionOrNull()?.message)
    }

    @Test
    fun rejectsIncompleteOrInvalidExpressions() {
        assertTrue(evaluate("").isFailure)
        assertTrue(evaluate("  ").isFailure)
        assertTrue(evaluate("12+").isFailure)
        assertTrue(evaluate("+").isFailure)
        assertTrue(evaluate("12++3").isFailure)
        assertTrue(evaluate("12+abc").isFailure)
        assertTrue(evaluate("(12+3").isFailure)
    }

    @Test
    fun evaluateToInputReturnsFormattedStringDirectly() {
        assertEquals("20", evaluate.evaluateToInput("12+8").getOrThrow())
        assertEquals("33.33", evaluate.evaluateToInput("100/3").getOrThrow())
    }
}
