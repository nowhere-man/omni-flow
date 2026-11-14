package com.omniflow.android.ui.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.MorePage
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniSettingsDivider
import com.omniflow.android.ui.designsystem.components.OmniSettingsGroup
import com.omniflow.android.ui.designsystem.components.OmniSettingsRow
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.mutedContent
import com.omniflow.android.ui.readableContentWidth
import com.omniflow.core.domain.model.SyncPhase

/**
 * 更多页主视图：遵循 Gmail M3 Expressive 规范。
 * 顶部净资产 Hero 卡片 + 分组圆角大卡片（Pastel 语义图标徽章、标准 M3 ListItem、行间细分割线）。
 */
@Composable
fun MoreHomeScreen(
    state: MoreUiState,
    onPage: (MorePage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hideAmount by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier
            .readableContentWidth()
            .fillMaxHeight()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // 顶部净资产 Hero 卡片
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("净资产", style = OmniText.caption, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                        IconButton(
                            onClick = { hideAmount = !hideAmount },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                if (hideAmount) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (hideAmount) "显示金额" else "隐藏金额",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            )
                        }
                    }
                    Text(
                        if (hideAmount) "••••••" else state.accountSummary.netAssets.asRmb(),
                        style = OmniText.amountHero,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                    )
                    Text(
                        if (hideAmount) {
                            "资产 •••••• · 负债 ••••••"
                        } else {
                            "资产 ${state.accountSummary.assets.asRmb()} · 负债 ${state.accountSummary.liabilities.asRmb()}"
                        },
                        style = OmniText.caption,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    )
                }
            }
        }

        // 分组 1：数据与备份
        item {
            OmniSettingsGroup {
                MoreOptionItem(
                    page = MorePage.DATA,
                    badgeType = SemanticBadgeType.DATA_SYNC,
                    state = state,
                    onClick = { onPage(MorePage.DATA) },
                )
                OmniSettingsDivider()
                MoreOptionItem(
                    page = MorePage.AI,
                    badgeType = SemanticBadgeType.AI,
                    state = state,
                    onClick = { onPage(MorePage.AI) },
                )
                OmniSettingsDivider()
                MoreOptionItem(
                    page = MorePage.IMPORT,
                    badgeType = SemanticBadgeType.IMPORT_EXPORT,
                    state = state,
                    onClick = { onPage(MorePage.IMPORT) },
                )
                OmniSettingsDivider()
                MoreOptionItem(
                    page = MorePage.EXPORT,
                    badgeType = SemanticBadgeType.IMPORT_EXPORT,
                    state = state,
                    onClick = { onPage(MorePage.EXPORT) },
                )
            }
        }

        // 分组 2：账本与资产
        item {
            OmniSettingsGroup {
                MoreOptionItem(
                    page = MorePage.LEDGERS,
                    badgeType = SemanticBadgeType.FINANCIAL,
                    state = state,
                    onClick = { onPage(MorePage.LEDGERS) },
                )
                OmniSettingsDivider()
                MoreOptionItem(
                    page = MorePage.ACCOUNTS,
                    badgeType = SemanticBadgeType.FINANCIAL,
                    state = state,
                    onClick = { onPage(MorePage.ACCOUNTS) },
                )
                OmniSettingsDivider()
                MoreOptionItem(
                    page = MorePage.ASSETS,
                    badgeType = SemanticBadgeType.FINANCIAL,
                    state = state,
                    onClick = { onPage(MorePage.ASSETS) },
                )
                OmniSettingsDivider()
                MoreOptionItem(
                    page = MorePage.CATEGORIES,
                    badgeType = SemanticBadgeType.CATEGORY,
                    state = state,
                    onClick = { onPage(MorePage.CATEGORIES) },
                )
                OmniSettingsDivider()
                MoreOptionItem(
                    page = MorePage.TAGS,
                    badgeType = SemanticBadgeType.CATEGORY,
                    state = state,
                    onClick = { onPage(MorePage.TAGS) },
                )
            }
        }

        // 分组 3：规划与规则
        item {
            OmniSettingsGroup {
                MoreOptionItem(
                    page = MorePage.BUDGETS,
                    badgeType = SemanticBadgeType.PLANNING,
                    state = state,
                    onClick = { onPage(MorePage.BUDGETS) },
                )
                OmniSettingsDivider()
                MoreOptionItem(
                    page = MorePage.RULES,
                    badgeType = SemanticBadgeType.PLANNING,
                    state = state,
                    onClick = { onPage(MorePage.RULES) },
                )
                OmniSettingsDivider()
                MoreOptionItem(
                    page = MorePage.REMINDERS,
                    badgeType = SemanticBadgeType.PLANNING,
                    state = state,
                    onClick = { onPage(MorePage.REMINDERS) },
                )
            }
        }

        // 分组 4：系统设置
        item {
            OmniSettingsGroup {
                MoreOptionItem(
                    page = MorePage.SETTINGS,
                    badgeType = SemanticBadgeType.SYSTEM,
                    state = state,
                    onClick = { onPage(MorePage.SETTINGS) },
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun MoreOptionItem(
    page: MorePage,
    badgeType: SemanticBadgeType,
    state: MoreUiState,
    onClick: () -> Unit,
) {
    OmniSettingsRow(
        title = page.label,
        icon = page.icon,
        badgeType = badgeType,
        subtitle = page.description(state),
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                page.badge(state)?.let { badgeText ->
                    Surface(
                        shape = RoundedCornerShape(OmniRadius.small),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = OmniText.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = mutedContent(),
                )
            }
        },
    )
}

private fun MorePage.badge(state: MoreUiState): String? = when (this) {
    MorePage.LEDGERS -> "${state.ledgers.size}"
    MorePage.ACCOUNTS -> "${state.accounts.size}"
    MorePage.CATEGORIES -> "${state.categories.count { it.parentId == null }}"
    MorePage.TAGS -> if (state.tags.isNotEmpty()) "${state.tags.size}" else null
    MorePage.RULES -> if (state.rules.isNotEmpty()) "${state.rules.size}" else null
    MorePage.REMINDERS -> if (state.reminders.isNotEmpty()) "${state.reminders.size}" else null
    MorePage.DATA -> if (state.syncState.phase == SyncPhase.RUNNING) "同步中" else null
    else -> null
}

private fun MorePage.description(state: MoreUiState): String = when (this) {
    MorePage.HOME -> ""
    MorePage.DATA -> syncLabel(state)
    MorePage.AI -> "导入时让大模型建议一级分类"
    MorePage.IMPORT -> "从支付宝、微信、银行流水批量导入"
    MorePage.EXPORT -> "导出 CSV 表格或 JSON 备份"
    MorePage.SETTINGS -> "外观、主题色、应用锁与缓存"
    MorePage.LEDGERS -> "账本封面、默认账本与收支概览"
    MorePage.ACCOUNTS -> "账户余额、卡片信息与流水"
    MorePage.ASSETS -> "净资产构成与负债"
    MorePage.CATEGORIES -> "收支分类、图标与排序"
    MorePage.TAGS -> "账本标签"
    MorePage.BUDGETS -> budgetLabel(state)
    MorePage.RULES -> "自动分类和排除规则"
    MorePage.REMINDERS -> "还款与周期记账提醒"
}

private fun budgetLabel(state: MoreUiState): String {
    val overall = state.budgets.firstOrNull { it.budget.isOverall } ?: return "设置月度预算，盯住整体开销"
    return if (overall.isOverspent) {
        "本月已超支 ${(-overall.remaining.minor / 100).grouped()} 元"
    } else {
        "本月还可用 ${(overall.remaining.minor / 100).grouped()} 元"
    }
}

private fun syncLabel(state: MoreUiState): String = when (state.syncState.phase) {
    SyncPhase.IDLE -> if (state.preferences.syncTarget == null) "未配置同步" else "等待备份"
    SyncPhase.RUNNING -> "备份进行中 ${((state.syncState.progress ?: 0f) * 100).toInt()}%"
    SyncPhase.SUCCESS -> "最近备份"
    SyncPhase.ERROR -> state.syncState.errorMessage ?: "备份失败"
}

private fun Long.grouped(): String = "%,d".format(this)
