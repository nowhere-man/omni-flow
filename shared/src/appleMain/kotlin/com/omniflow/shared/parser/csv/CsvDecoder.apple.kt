package com.omniflow.shared.parser.csv

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFStringConvertEncodingToNSStringEncoding
import platform.CoreFoundation.kCFStringEncodingGB_18030_2000
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create

@OptIn(ExperimentalForeignApi::class)
actual object CsvDecoder {
    actual fun decode(bytes: ByteArray, charset: CsvCharset): String {
        val data = bytes.usePinned { pinned -> NSData.create(pinned.addressOf(0), bytes.size.toULong()) }
        val encoding = when (charset) {
            CsvCharset.UTF8 -> NSUTF8StringEncoding
            CsvCharset.GB18030 -> CFStringConvertEncodingToNSStringEncoding(kCFStringEncodingGB_18030_2000.toUInt())
        }
        return NSString.create(data, encoding)?.toString() ?: error("账单文本编码无效")
    }
}
