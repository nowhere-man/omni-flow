package com.omniflow.shared.parser

class BillFormatDetector {
    fun detect(fileName: String, text: String): List<ImportFormat> {
        val normalizedName = fileName.lowercase()
        return when {
            normalizedName.endsWith(".json") && text.contains("entryJsonString") -> listOf(ImportFormat.QINGZI)
            text.contains("支付宝支付科技有限公司") && text.contains("交易对方") -> listOf(ImportFormat.ALIPAY)
            text.contains("京东账号名") && text.contains("商户名称") -> listOf(ImportFormat.JD)
            text.contains("美团交易账单明细") && text.contains("订单标题") -> listOf(ImportFormat.MEITUAN)
            else -> emptyList()
        }
    }
}
