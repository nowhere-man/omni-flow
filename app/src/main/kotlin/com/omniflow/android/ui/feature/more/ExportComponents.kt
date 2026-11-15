package com.omniflow.android.ui.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.mutedContent

internal enum class ExportFormatChoice(val label: String, val extension: String, val mimeType: String) {
    CSV("CSV 表格", "csv", "text/csv"),
    JSON("JSON 备份", "json", "application/json"),
}

internal enum class ExportTimeSpan(val label: String) {
    ALL("全部日期"),
    THIS_MONTH("本月"),
    THIS_YEAR("本年"),
    CUSTOM("自定义"),
}

@Composable
internal fun ExportPreviewHubCard(
    count: Int?,
    isLoading: Boolean,
    format: ExportFormatChoice,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("导出实时预览", style = OmniText.caption, color = mutedContent())
                Text(
                    text = when {
                        isLoading -> "正在计算…"
                        count != null -> "$count 笔明细"
                        else -> "—"
                    },
                    style = OmniText.amountPrimary,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("按当前条件筛选 · 将导出为 ${format.label}", style = OmniText.caption, color = mutedContent())
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
