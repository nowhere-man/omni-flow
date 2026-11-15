package com.omniflow.android.ui.feature.ledger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniSpacingTokens
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerAggregates
import com.omniflow.core.domain.model.LedgerId

/**
 * 账本管理页：标准 Material 3 Expressive 卡片网格。
 *
 * 规格契约：
 * - 统一脚手架 OmniSubPageScaffold
 * - 2 列 MD3 ElevatedCard 卡片网格 (20dp 大圆角，surfaceContainerLow)
 * - 左上角 44dp 圆形账本徽章，右上角默认状态 Pill 徽章
 * - 账本名称与交易笔数概览
 * - 末尾为 MD3 虚线/细边框新增账本卡片
 * - 归档账本独立沉底，去饱和弱化展示
 */
@Composable
internal fun LedgerListScreen(
    viewModel: LedgerViewModel,
    onOpenLedger: (LedgerId) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val state by viewModel.listState.collectAsState()

    var showNewSheet by remember { mutableStateOf(false) }
    var selectedForDetail by remember { mutableStateOf<Ledger?>(null) }
    var editingLedger by remember { mutableStateOf<Ledger?>(null) }
    var coverStudioLedger by remember { mutableStateOf<Ledger?>(null) }
    var deletingLedger by remember { mutableStateOf<Ledger?>(null) }

    val activeLedgers = remember(state.ledgers) { state.ledgers.filter { it.deletedAt == null } }
    val archivedLedgers = remember(state.ledgers) { state.ledgers.filter { it.deletedAt != null } }

    OmniSubPageScaffold(
        title = "账本管理",
        onBack = onBack,
        modifier = modifier,
    ) {
        if (state.ledgers.isEmpty()) {
            OmniEmptyState(
                status = "暂无账本",
                value = "创建一本专属账本，开始记录清晰有度的生活",
                actionLabel = "新建账本",
                onAction = { showNewSheet = true },
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = OmniSpacingTokens.pageHorizontal),
            ) {
                Spacer(Modifier.height(OmniSpacingTokens.mediumSmall))

                // 2 列卡片网格：activeLedgers + 结尾的新建卡片
                val gridItems: List<Ledger?> = activeLedgers + listOf<Ledger?>(null)
                val gridRows = gridItems.chunked(2)

                gridRows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 左侧卡片
                        val left = rowItems[0]
                        Box(modifier = Modifier.weight(1f)) {
                            if (left != null) {
                                LedgerCard(
                                    ledger = left,
                                    stat = state.ledgerStats[left.id],
                                    isDefault = state.defaultLedgerId == left.id,
                                    onClick = { selectedForDetail = left },
                                )
                            } else {
                                NewLedgerCard(onClick = { showNewSheet = true })
                            }
                        }

                        // 右侧卡片 (若存在)
                        Box(modifier = Modifier.weight(1f)) {
                            if (rowItems.size > 1) {
                                val right = rowItems[1]
                                if (right != null) {
                                    LedgerCard(
                                        ledger = right,
                                        stat = state.ledgerStats[right.id],
                                        isDefault = state.defaultLedgerId == right.id,
                                        onClick = { selectedForDetail = right },
                                    )
                                } else {
                                    NewLedgerCard(onClick = { showNewSheet = true })
                                }
                            } else {
                                Spacer(Modifier.fillMaxWidth())
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }

                // 归档账本沉底弱化专区
                if (archivedLedgers.isNotEmpty()) {
                    Spacer(Modifier.height(OmniSpacingTokens.medium))
                    Text(
                        text = "已归档账本",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    Spacer(Modifier.height(OmniSpacingTokens.small))

                    val archivedRows = archivedLedgers.chunked(2)
                    archivedRows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val left = rowItems[0]
                            Box(modifier = Modifier.weight(1f)) {
                                LedgerCard(
                                    ledger = left,
                                    stat = state.ledgerStats[left.id],
                                    isDefault = false,
                                    isArchived = true,
                                    onClick = { selectedForDetail = left },
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (rowItems.size > 1) {
                                    val right = rowItems[1]
                                    LedgerCard(
                                        ledger = right,
                                        stat = state.ledgerStats[right.id],
                                        isDefault = false,
                                        isArchived = true,
                                        onClick = { selectedForDetail = right },
                                    )
                                } else {
                                    Spacer(Modifier.fillMaxWidth())
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                Spacer(Modifier.height(OmniSpacingTokens.xxl))
            }
        }
    }

    // 新建账本弹层
    if (showNewSheet) {
        LedgerSheet(
            ledger = null,
            onDismiss = { showNewSheet = false },
            onSave = { name, cover ->
                viewModel.createLedger(name, cover)
                showNewSheet = false
            },
        )
    }

    // 账本详情抽屉 (严禁「默认币种」)
    selectedForDetail?.let { ledger ->
        val stat = state.ledgerStats[ledger.id]

        LedgerDetailSheet(
            ledger = ledger,
            stat = stat,
            isDefault = state.defaultLedgerId == ledger.id,
            onDismiss = { selectedForDetail = null },
            onSetDefault = {
                viewModel.setDefaultLedger(ledger.id)
                selectedForDetail = null
            },
            onOpenCoverStudio = {
                coverStudioLedger = ledger
                selectedForDetail = null
            },
            onRename = {
                editingLedger = ledger
                selectedForDetail = null
            },
            onDelete = {
                deletingLedger = ledger
                selectedForDetail = null
            },
        )
    }

    // 编辑账本名称弹层
    editingLedger?.let { ledger ->
        LedgerSheet(
            ledger = ledger,
            onDismiss = { editingLedger = null },
            onSave = { name, cover ->
                viewModel.updateLedger(ledger.id, name, cover)
                editingLedger = null
            },
        )
    }

    // 封面工坊弹层 (可替换预置或自定义图)
    coverStudioLedger?.let { ledger ->
        LedgerCoverStudioSheet(
            initialCoverKey = ledger.coverKey,
            ledgerName = ledger.name,
            onDismiss = { coverStudioLedger = null },
            onSaveCover = { newCover ->
                viewModel.updateLedger(ledger.id, ledger.name, newCover)
                coverStudioLedger = null
            },
            ledgerId = ledger.id,
        )
    }

    // 确认删除弹层 (级联清理本地自定义图片文件)
    deletingLedger?.let { ledger ->
        ConfirmDeleteLedgerDialog(
            ledger = ledger,
            onDismiss = { deletingLedger = null },
            onConfirm = {
                deleteLedgerCoverFile(context, ledger.coverKey)
                viewModel.deleteLedger(ledger.id)
                deletingLedger = null
            },
        )
    }
}

/**
 * 符合 MD3 规范的 ElevatedCard 账本卡片。
 */
@Composable
private fun LedgerCard(
    ledger: Ledger,
    stat: LedgerAggregates?,
    isDefault: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isArchived: Boolean = false,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isArchived) Modifier.alpha(0.6f) else Modifier)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 圆形账本徽章 (44dp)
                OmniLedgerBadge(
                    coverKey = ledger.coverKey,
                    ledgerId = ledger.id,
                    size = 44.dp,
                    iconSize = 22.dp,
                )

                if (isDefault) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = "默认",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                } else if (isArchived) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "已归档",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = ledger.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val txCount = stat?.transactionCount ?: 0
                Text(
                    text = if (isArchived) "历史归档" else if (txCount > 0) "$txCount 笔交易" else "暂无交易",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 新建账本卡片。
 */
@Composable
private fun NewLedgerCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "新建账本",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "新建账本",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "创建新账本",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}
