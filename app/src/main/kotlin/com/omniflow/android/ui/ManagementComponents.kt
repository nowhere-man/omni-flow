package com.omniflow.android.ui

import com.omniflow.android.ui.feature.ledger.OmniLedgerBadge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.omniflow.android.ui.designsystem.tokens.CategoryIconOption
import com.omniflow.android.ui.designsystem.tokens.OmniColorTokens
import com.omniflow.android.ui.designsystem.tokens.categoryBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.core.domain.model.Ledger

/** 图标选择网格，账户和分类共用；以前账户是把 `wallet-cards` 这种 key 字符串直接列给用户看。 */
@Composable
internal fun IconPickerGrid(
    options: List<CategoryIconOption>,
    selectedKey: String,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(56.dp),
        modifier = modifier.heightIn(max = 200.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(options, key = { it.key }) { option ->
            val selected = option.key == selectedKey
            val colors = categoryBadgeColors(option.key)
            Surface(
                onClick = { onSelected(option.key) },
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else colors.container,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.label,
                        modifier = Modifier.size(26.dp),
                        tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else colors.content,
                    )
                }
            }
        }
    }
}

internal data class LedgerCover(val key: String, val label: String, val start: Color, val end: Color, val iconKey: String)

/**
 * 内置账本封面。存的仍然是 [Ledger.coverKey] 这个字符串，
 * 所以不需要图片存储、也不影响备份恢复；以前这里是让用户手输 key 的裸文本框。
 */
internal val LedgerCovers = listOf(
    LedgerCover("cover-daily", "日常", OmniColorTokens.LedgerCoverDailyStart, OmniColorTokens.LedgerCoverDailyEnd, "shopping-bag"),
    LedgerCover("cover-family", "家庭", OmniColorTokens.LedgerCoverFamilyStart, OmniColorTokens.LedgerCoverFamilyEnd, "house"),
    LedgerCover("cover-travel", "旅行", OmniColorTokens.LedgerCoverTravelStart, OmniColorTokens.LedgerCoverTravelEnd, "plane"),
    LedgerCover("cover-work", "工作", OmniColorTokens.LedgerCoverWorkStart, OmniColorTokens.LedgerCoverWorkEnd, "briefcase-business"),
    LedgerCover("cover-study", "学习", OmniColorTokens.LedgerCoverStudyStart, OmniColorTokens.LedgerCoverStudyEnd, "books"),
    LedgerCover("cover-car", "爱车", OmniColorTokens.LedgerCoverCarStart, OmniColorTokens.LedgerCoverCarEnd, "car"),
    LedgerCover("cover-fitness", "健康", OmniColorTokens.LedgerCoverFitnessStart, OmniColorTokens.LedgerCoverFitnessEnd, "fitness"),
    LedgerCover("cover-invest", "理财", OmniColorTokens.LedgerCoverInvestStart, OmniColorTokens.LedgerCoverInvestEnd, "chart-line"),
)

internal fun ledgerCover(coverKey: String?): LedgerCover =
    LedgerCovers.firstOrNull { it.key == coverKey } ?: LedgerCovers.first()

/** 治愈缺陷 8.2：账本小图标全场景统一为圆形徽章，取缔方角。 */
@Composable
internal fun LedgerCoverBox(
    coverKey: String?,
    modifier: Modifier = Modifier,
    iconSize: Int = 24,
) {
    OmniLedgerBadge(
        coverKey = coverKey,
        modifier = modifier,
        iconSize = iconSize.dp,
    )
}

/** 分类/标签/规则页顶部的账本选择条，取代原来那个裸 ValueMenu。 */
@Composable
internal fun LedgerPickerBar(
    ledgers: List<Ledger>,
    selectedId: String?,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = ledgers.firstOrNull { it.id == selectedId }
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(OmniRadius.medium),
            color = surfaceCard(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected != null) {
                    LedgerCoverBox(selected.coverKey, Modifier.size(32.dp), iconSize = 18)
                } else {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(24.dp), tint = mutedContent())
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("当前账本", style = OmniText.caption, color = mutedContent())
                    Text(
                        selected?.name ?: "选择账本",
                        style = OmniText.titleRow,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp), tint = mutedContent())
            }
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ledgers.forEach { ledger ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(ledger.name) },
                    onClick = { expanded = false; onSelected(ledger.id) },
                    leadingIcon = { LedgerCoverBox(ledger.coverKey, Modifier.size(28.dp), iconSize = 16) },
                )
            }
        }
    }
}

/** 圆形图标底，账户行和分类行共用。 */
@Composable
internal fun RoundIcon(iconKey: String?, modifier: Modifier = Modifier, size: Int = 40) {
    val colors = categoryBadgeColors(iconKey)
    Surface(
        modifier = modifier.size(size.dp),
        shape = CircleShape,
        color = colors.container,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = getCategoryIcon(iconKey),
                contentDescription = null,
                modifier = Modifier.size((size * 0.55).toInt().dp),
                tint = colors.content,
            )
        }
    }
}
