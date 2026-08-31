import java.io.File

fun main() {
    println("招商银行PDF解析演示")

    // 读取招商银行PDF文件
    val pdfFile = File("/Users/liushaojie/Downloads/招商银行交易流水(申请时间2026年08月31日19时27分04秒).pdf")
    if (!pdfFile.exists()) {
        println("错误：PDF文件不存在")
        return
    }

    println("PDF文件大小: ${pdfFile.length()} bytes")

    try {
        // 读取PDF字节
        val pdfBytes = pdfFile.readBytes()

        // 测试格式识别
        println("\n=== 测试格式识别 ===")
        val matches = com.omniflow.core.parser.pdf.CmbPdfBillParser.matches(pdfBytes)
        println("是否匹配招商银行格式: $matches")

        if (matches) {
            println("\n=== 解析PDF内容 ===")
            val result = com.omniflow.core.parser.pdf.CmbPdfBillParser.parse(pdfBytes)

            when {
                result.isSuccess -> {
                    val transactions = result.getOrThrow()
                    println("成功解析 ${transactions.size} 条交易记录")

                    // 显示前5条交易
                    transactions.take(5).forEachIndexed { index, transaction ->
                        println("\n交易记录 ${index + 1}:")
                        println("  日期: ${transaction.occurredAt}")
                        println("  金额: ${transaction.amount}")
                        println("  类型: ${transaction.type}")
                        println("  对手: ${transaction.counterparty}")
                        println("  备注: ${transaction.note}")
                        println("  来源分类: ${transaction.sourceCategory}")
                    }

                    if (transactions.size > 5) {
                        println("\n... 还有 ${transactions.size - 5} 条交易记录")
                    }
                }
                else -> {
                    println("解析失败: ${result.exceptionOrNull()?.message}")
                }
            }
        } else {
            println("文件不匹配招商银行格式")
        }
    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    }
}