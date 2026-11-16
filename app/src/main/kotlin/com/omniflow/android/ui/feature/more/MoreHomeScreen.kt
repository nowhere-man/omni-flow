package com.omniflow.android.ui.feature.more

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.MorePage
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.designsystem.components.OmniSettingsGroup
import com.omniflow.android.ui.designsystem.components.OmniSettingsRow
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.designsystem.tokens.OmniSpacingTokens
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.mutedContent
import com.omniflow.android.ui.readableContentWidth
import com.omniflow.core.domain.model.AppTimeZone
import com.omniflow.core.domain.model.SyncPhase
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime

/**
 * 更多页主视图：遵循 Gmail M3 Expressive 规范。
 * 顶部个人名片头 + 分组圆角大卡片（Pastel 语义图标徽章、独立 8dp 行卡片、无金额/笔数数字）。
 */
@Composable
fun MoreHomeScreen(
    state: MoreUiState,
    onPage: (MorePage) -> Unit,
    modifier: Modifier = Modifier,
    onSaveName: (String) -> Unit = {},
) {
    val context = LocalContext.current
    var showProfileSheet by remember { mutableStateOf(false) }
    var avatarVersion by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val avatarBitmap = remember(avatarVersion) { loadAvatarBitmap(context) }
    val versionName = remember(context) { resolveVersionName(context) }

    LazyColumn(
        modifier
            .readableContentWidth()
            .fillMaxHeight()
            .padding(horizontal = OmniSpacingTokens.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(OmniSpacingTokens.groupSpacing),
    ) {
        item { Spacer(Modifier.height(OmniSpacingTokens.small)) }

        // 顶部个人名片（轻盈无底色，现代 M3 Expressive）
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = OmniShapeTokens.card,
                color = Color.Transparent,
                onClick = { showProfileSheet = true },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 64dp 圆形头像 (有图显示图，图缺失回退 Canvas Monogram)
                    OmniAvatar(
                        userName = state.preferences.userName,
                        avatarBitmap = avatarBitmap,
                        size = 64.dp,
                    )

                    Spacer(Modifier.width(OmniSpacingTokens.medium))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(OmniSpacingTokens.extraSmall),
                    ) {
                        Text(
                            text = state.preferences.userName.ifBlank { "我" },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        // 状态副题：有数据「已坚持记账 N 天」/ 无数据「记录每一笔，理清每一分」
                        val subtitleText = remember(state.earliestTransactionOccurredAt) {
                            state.earliestTransactionOccurredAt?.let { earliest ->
                                val today = Clock.System.now().toLocalDateTime(AppTimeZone).date
                                val firstDate = earliest.toLocalDateTime(AppTimeZone).date
                                val days = (today.toEpochDays() - firstDate.toEpochDays() + 1).coerceAtLeast(1)
                                "已坚持记账 $days 天"
                            } ?: "记录每一笔，理清每一分"
                        }

                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "编辑个人资料",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // 分组 1：{通用设置、数据与同步} — 配色：SYSTEM
        item {
            OmniSettingsGroup {
                item {
                    MoreOptionItem(
                        page = MorePage.SETTINGS,
                        badgeType = SemanticBadgeType.SYSTEM,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.SETTINGS) },
                    )
                }
                item {
                    MoreOptionItem(
                        page = MorePage.DATA,
                        badgeType = SemanticBadgeType.SYSTEM,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.DATA) },
                    )
                }
            }
        }

        // 分组 2：{账本管理、账户管理、资产管理} — 配色：FINANCIAL
        item {
            OmniSettingsGroup {
                item {
                    MoreOptionItem(
                        page = MorePage.LEDGERS,
                        badgeType = SemanticBadgeType.FINANCIAL,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.LEDGERS) },
                    )
                }
                item {
                    MoreOptionItem(
                        page = MorePage.ACCOUNTS,
                        badgeType = SemanticBadgeType.FINANCIAL,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.ACCOUNTS) },
                    )
                }
                item {
                    MoreOptionItem(
                        page = MorePage.ASSETS,
                        badgeType = SemanticBadgeType.FINANCIAL,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.ASSETS) },
                    )
                }
                item {
                    MoreOptionItem(
                        page = MorePage.RECONCILIATION,
                        badgeType = SemanticBadgeType.FINANCIAL,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.RECONCILIATION) },
                    )
                }
            }
        }

        // 分组 3：{导入账单、智能分类、规则管理、导出账单} — 配色：DATA_SYNC
        item {
            OmniSettingsGroup {
                item {
                    MoreOptionItem(
                        page = MorePage.IMPORT,
                        badgeType = SemanticBadgeType.DATA_SYNC,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.IMPORT) },
                    )
                }
                item {
                    MoreOptionItem(
                        page = MorePage.AI,
                        badgeType = SemanticBadgeType.DATA_SYNC,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.AI) },
                    )
                }
                item {
                    MoreOptionItem(
                        page = MorePage.RULES,
                        badgeType = SemanticBadgeType.DATA_SYNC,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.RULES) },
                    )
                }
                item {
                    MoreOptionItem(
                        page = MorePage.EXPORT,
                        badgeType = SemanticBadgeType.DATA_SYNC,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.EXPORT) },
                    )
                }
            }
        }

        // 分组 4：{分类管理、标签管理} — 配色：CATEGORY
        item {
            OmniSettingsGroup {
                item {
                    MoreOptionItem(
                        page = MorePage.CATEGORIES,
                        badgeType = SemanticBadgeType.CATEGORY,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.CATEGORIES) },
                    )
                }
                item {
                    MoreOptionItem(
                        page = MorePage.TAGS,
                        badgeType = SemanticBadgeType.CATEGORY,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.TAGS) },
                    )
                }
            }
        }

        // 分组 5：{提醒、预算管理} — 配色：PLANNING
        item {
            OmniSettingsGroup {
                item {
                    MoreOptionItem(
                        page = MorePage.REMINDERS,
                        badgeType = SemanticBadgeType.PLANNING,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.REMINDERS) },
                    )
                }
                item {
                    MoreOptionItem(
                        page = MorePage.BUDGETS,
                        badgeType = SemanticBadgeType.PLANNING,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.BUDGETS) },
                    )
                }
            }
        }

        // 分组 6：{关于} — 独立置底单项分组
        item {
            OmniSettingsGroup {
                item {
                    MoreOptionItem(
                        page = MorePage.ABOUT,
                        badgeType = SemanticBadgeType.SYSTEM,
                        state = state,
                        versionName = versionName,
                        onClick = { onPage(MorePage.ABOUT) },
                    )
                }
            }
        }

        item { Spacer(Modifier.height(OmniSpacingTokens.extraLarge)) }
    }

    if (showProfileSheet) {
        ProfileEditSheet(
            currentName = state.preferences.userName,
            onDismiss = { showProfileSheet = false },
            onSaveName = onSaveName,
            onAvatarUpdated = { avatarVersion = System.currentTimeMillis() },
        )
    }
}

@Composable
private fun MoreOptionItem(
    page: MorePage,
    badgeType: SemanticBadgeType,
    state: MoreUiState,
    versionName: String,
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
                page.badge(state, versionName)?.let { badgeText ->
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

private fun MorePage.badge(state: MoreUiState, versionName: String): String? = when (this) {
    MorePage.DATA -> if (state.syncState.phase == SyncPhase.RUNNING) "同步中" else null
    MorePage.ABOUT -> "v$versionName"
    else -> null
}

private fun MorePage.description(state: MoreUiState): String = when (this) {
    MorePage.HOME -> ""
    MorePage.SETTINGS -> "外观、记账习惯与提醒设置"
    MorePage.DATA -> syncLabel(state)
    MorePage.LEDGERS -> "账本封面、默认账本与收支概览"
    MorePage.ACCOUNTS -> "账户余额、卡片信息与流水"
    MorePage.ASSETS -> "净资产构成与负债"
    MorePage.RECONCILIATION -> "月度收支对账、储蓄率与财富天梯"
    MorePage.IMPORT -> "从支付宝、微信、银行流水批量导入"
    MorePage.AI -> "导入时让大模型建议一级分类"
    MorePage.RULES -> "自动分类和排除规则"
    MorePage.EXPORT -> "导出 CSV 表格或 JSON 备份"
    MorePage.CATEGORIES -> "收支分类、图标与排序"
    MorePage.TAGS -> "账本标签"
    MorePage.REMINDERS -> "还款与周期记账提醒"
    MorePage.BUDGETS -> budgetLabel(state)
    MorePage.ABOUT -> "版本、开源许可与协议"
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
