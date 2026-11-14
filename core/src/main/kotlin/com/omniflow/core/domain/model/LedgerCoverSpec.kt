package com.omniflow.core.domain.model

import kotlin.math.abs

/**
 * 账本封面规格契约。
 *
 * 格式契约（存储于现有 Ledger.coverKey 字段，不改表结构）：
 * - 预置封面：`preset:{colorKey}:{patternKey}:{iconKey}`
 *   8 种预置色彩盘 × 5 种 Canvas 几何纹样（方眼/波点/斜纹/同心波/素面）× 常用图标
 * - 自定义封面：`custom:{fileName}`，图片存本地 filesDir/ledger_covers/{fileName}
 * - 旧版 key（cover-daily 等）通过兼容映射表无缝迁移
 * - 自定义图文件缺失时，通过 ledger.id 的哈希确定性回退到一款预置封面，永不破图
 */
sealed interface LedgerCoverSpec {
    fun encode(): String

    data class Preset(
        val colorKey: String,
        val patternKey: String,
        val iconKey: String,
    ) : LedgerCoverSpec {
        override fun encode(): String = "preset:$colorKey:$patternKey:$iconKey"
    }

    data class Custom(
        val fileName: String,
    ) : LedgerCoverSpec {
        override fun encode(): String = "custom:$fileName"
    }

    companion object {
        val PresetColors = listOf(
            "daily",
            "family",
            "travel",
            "work",
            "study",
            "car",
            "fitness",
            "invest",
        )

        val PresetPatterns = listOf(
            "grid",     // 方眼
            "dots",     // 波点
            "diagonal", // 斜纹
            "waves",    // 同心波
            "clean",    // 素面
        )

        val PresetIcons = listOf(
            "shopping-bag",
            "house",
            "plane",
            "briefcase-business",
            "books",
            "car",
            "fitness",
            "chart-line",
            "wallet",
            "utensils",
            "coffee",
            "heart",
        )

        private val LegacyMapping = mapOf(
            "cover-daily" to Preset("daily", "grid", "shopping-bag"),
            "cover-family" to Preset("family", "dots", "house"),
            "cover-travel" to Preset("travel", "diagonal", "plane"),
            "cover-work" to Preset("work", "waves", "briefcase-business"),
            "cover-study" to Preset("study", "clean", "books"),
            "cover-car" to Preset("car", "grid", "car"),
            "cover-fitness" to Preset("fitness", "dots", "fitness"),
            "cover-invest" to Preset("invest", "diagonal", "chart-line"),
        )

        fun parse(coverKey: String?, ledgerId: String = ""): LedgerCoverSpec {
            if (coverKey.isNullOrBlank()) {
                return fallback(ledgerId)
            }

            // 1. 尝试 legacy 兼容映射
            LegacyMapping[coverKey]?.let { return it }

            // 2. 预置格式: preset:{colorKey}:{patternKey}:{iconKey}
            if (coverKey.startsWith("preset:")) {
                val parts = coverKey.split(':')
                if (parts.size >= 4) {
                    val colorKey = parts[1].takeIf { it in PresetColors } ?: PresetColors.first()
                    val patternKey = parts[2].takeIf { it in PresetPatterns } ?: PresetPatterns.first()
                    val iconKey = parts[3].takeIf { it.isNotBlank() } ?: PresetIcons.first()
                    return Preset(colorKey, patternKey, iconKey)
                }
            }

            // 3. 自定义格式: custom:{fileName}
            if (coverKey.startsWith("custom:")) {
                val fileName = coverKey.removePrefix("custom:").trim()
                if (fileName.isNotEmpty()) {
                    return Custom(fileName)
                }
            }

            // 4. 未知格式走确定性 fallback
            return fallback(ledgerId)
        }

        fun fallback(ledgerId: String): Preset {
            val hash = if (ledgerId.isBlank()) 0 else abs(ledgerId.hashCode())
            val colorKey = PresetColors[hash % PresetColors.size]
            val patternKey = PresetPatterns[(hash / PresetColors.size) % PresetPatterns.size]
            val iconKey = PresetIcons[(hash / (PresetColors.size * PresetPatterns.size)) % PresetIcons.size]
            return Preset(colorKey, patternKey, iconKey)
        }
    }
}
