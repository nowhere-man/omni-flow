package com.omniflow.android.ui.feature.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniSpacingTokens
import com.omniflow.core.domain.model.Ledger
import com.omniflow.core.domain.model.LedgerId
import com.omniflow.core.domain.model.LedgerOverview

/**
 * 账本管理页 (书架陈列母题，治愈缺陷 8.2)。
 *
 * 规格契约：
 * - 统一脚手架 OmniSubPageScaffold
 * - 2 列书架拟物陈列，每行书本下方承托暖色实木托板 ShelfPlank (浅浮雕层 + 下沉阴影)
 * - 当前默认账本垂燕尾书签带
 * - 封面排印大图标 + 书名，严禁印任何金额
 * - 归档账本沉底至去饱和弱化区
 * - 书架末尾为虚线拟物书 (DashedNewLedgerBook)
 * - 点书弹出详情抽屉 (LedgerDetailSheet，严禁虚构默认币种)
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
                status = "书架上暂无账本",
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

                // 2 列书架：activeLedgers + 结尾的虚线空书
                val shelfItems: List<Ledger?> = activeLedgers + listOf<Ledger?>(null)
                val shelfRows = shelfItems.chunked(2)

                shelfRows.forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        // 左侧书
                        val left = rowItems[0]
                        Box(modifier = Modifier.weight(1f)) {
                            if (left != null) {
                                OmniTactileBook(
                                    name = left.name,
                                    coverKey = left.coverKey,
                                    ledgerId = left.id,
                                    isDefault = state.defaultLedgerId == left.id,
                                    onClick = { selectedForDetail = left },
                                )
                            } else {
                                DashedNewLedgerBook(onClick = { showNewSheet = true })
                            }
                        }

                        // 右侧书 (若存在)
                        Box(modifier = Modifier.weight(1f)) {
                            if (rowItems.size > 1) {
                                val right = rowItems[1]
                                if (right != null) {
                                    OmniTactileBook(
                                        name = right.name,
                                        coverKey = right.coverKey,
                                        ledgerId = right.id,
                                        isDefault = state.defaultLedgerId == right.id,
                                        onClick = { selectedForDetail = right },
                                    )
                                } else {
                                    DashedNewLedgerBook(onClick = { showNewSheet = true })
                                }
                            } else {
                                Spacer(Modifier.fillMaxWidth())
                            }
                        }
                    }

                    // 暖色实木托板
                    Spacer(Modifier.height(2.dp))
                    ShelfPlank()
                    Spacer(Modifier.height(24.dp))
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            val left = rowItems[0]
                            Box(modifier = Modifier.weight(1f)) {
                                OmniTactileBook(
                                    name = left.name,
                                    coverKey = left.coverKey,
                                    ledgerId = left.id,
                                    isArchived = true,
                                    onClick = { selectedForDetail = left },
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                if (rowItems.size > 1) {
                                    val right = rowItems[1]
                                    OmniTactileBook(
                                        name = right.name,
                                        coverKey = right.coverKey,
                                        ledgerId = right.id,
                                        isArchived = true,
                                        onClick = { selectedForDetail = right },
                                    )
                                } else {
                                    Spacer(Modifier.fillMaxWidth())
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
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

    // 封面工坊弹层 (可替换预置或自定义 3:4 图)
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
