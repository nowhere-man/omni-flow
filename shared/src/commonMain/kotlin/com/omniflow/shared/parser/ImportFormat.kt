package com.omniflow.shared.parser

import com.omniflow.shared.domain.model.TransactionSource

enum class ImportFormat(val transactionSource: TransactionSource?) {
    ALIPAY(TransactionSource.ALIPAY),
    WECHAT(TransactionSource.WECHAT),
    JD(TransactionSource.JD),
    MEITUAN(TransactionSource.MEITUAN),
    CCB(TransactionSource.CCB),
    QINGZI(null),
}
