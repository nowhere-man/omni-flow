package com.omniflow.android.ui.feature.batch

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniText

@Composable
fun BatchActionBar(
    selectedCount: Int,
    totalCount: Int,
    isAllSelected: Boolean,
    onSelectAllToggle: () -> Unit,
    onBatchDelete: () -> Unit,
    onBatchChangeCategory: () -> Unit,
    onBatchChangeAccount: () -> Unit,
    onBatchChangeLedger: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(OmniRadius.medium),
        shadowElevation = 8.dp,
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "已选 $selectedCount 笔",
                    style = OmniText.bodyRow,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSelectAllToggle) {
                    Text(if (isAllSelected) "取消全选" else "全选")
                }
                IconButton(onClick = onExit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "退出多选",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onBatchChangeCategory,
                    enabled = selectedCount > 0,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(OmniRadius.small),
                ) {
                    Text("改分类", style = OmniText.caption)
                }

                OutlinedButton(
                    onClick = onBatchChangeAccount,
                    enabled = selectedCount > 0,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(OmniRadius.small),
                ) {
                    Text("改账户", style = OmniText.caption)
                }

                OutlinedButton(
                    onClick = onBatchChangeLedger,
                    enabled = selectedCount > 0,
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(OmniRadius.small),
                ) {
                    Text("改账本", style = OmniText.caption)
                }

                Button(
                    onClick = onBatchDelete,
                    enabled = selectedCount > 0,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(OmniRadius.small),
                ) {
                    Text("批量删除", style = OmniText.caption)
                }
            }
        }
    }
}
