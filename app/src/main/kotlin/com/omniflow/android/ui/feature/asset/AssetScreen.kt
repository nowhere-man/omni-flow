package com.omniflow.android.ui.feature.asset

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.MorePage
import com.omniflow.android.ui.MoreUiState
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniBarSegment
import com.omniflow.android.ui.designsystem.components.OmniEmptyState
import com.omniflow.android.ui.designsystem.components.OmniSegmentBar
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniColorTokens
import com.omniflow.android.ui.designsystem.tokens.accountTypeBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.incomeColor
import com.omniflow.android.ui.label
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money

@Composable
internal fun AssetScreen(
    state: MoreUiState,
    onBack: () -> Unit,
    onPage: (MorePage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeAccounts = remember(state.accounts) {
        state.accounts.filter { it.deletedAt == null && it.includeInTotalAssets }
    }

    var selectedSegmentKey by remember { mutableStateOf<String?>(null) }

    // Aggregate assets by account type
    val assetAggregates: List<Pair<AccountType, Money>> = remember(activeAccounts) {
        AccountType.entries.mapNotNull { type ->
            val matching = activeAccounts.filter { it.type == type && it.balance.minor > 0 }
            if (matching.isEmpty()) null
            else {
                val totalMinor = matching.fold(0L) { acc, a -> acc + a.balance.minor }
                type to Money(totalMinor)
            }
        }
    }

    // Aggregate liabilities by account type
    val liabilityAggregates: List<Pair<AccountType, Money>> = remember(activeAccounts) {
        AccountType.entries.mapNotNull { type ->
            val matching = activeAccounts.filter { it.type == type && it.balance.minor < 0 }
            if (matching.isEmpty()) null
            else {
                val totalMinor = matching.fold(0L) { acc, a -> acc + (-a.balance.minor) }
                type to Money(totalMinor)
            }
        }
    }

    val totalAssets = state.accountSummary.assets
    val totalLiabilities = state.accountSummary.liabilities
    val totalVolume = totalAssets.minor + totalLiabilities.minor

    // Prepare OmniSegmentBar items
    val segmentItems: List<OmniBarSegment> = remember(assetAggregates, totalLiabilities, totalVolume) {
        if (totalVolume <= 0L) emptyList()
        else {
            val list = mutableListOf<OmniBarSegment>()
            assetAggregates.forEach { (type, amount) ->
                list.add(
                    OmniBarSegment(
                        key = type.name,
                        weight = amount.minor.toFloat(),
                        color = OmniColorTokens.IncomeLight,
                        label = type.label,
                    ),
                )
            }
            if (totalLiabilities.minor > 0L) {
                list.add(
                    OmniBarSegment(
                        key = "LIABILITY",
                        weight = totalLiabilities.minor.toFloat(),
                        color = OmniColorTokens.ExpenseLight,
                        label = "负债",
                    ),
                )
            }
            list
        }
    }

    OmniSubPageScaffold(
        title = "资产管理",
        onBack = onBack,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (activeAccounts.isEmpty()) {
                item {
                    OmniEmptyState(
                        status = "暂无资产数据",
                        value = "添加账户后，在此查看资产构成与负债分布",
                        actionLabel = "去管理账户",
                        onAction = { onPage(MorePage.ACCOUNTS) },
                        modifier = Modifier.padding(top = 40.dp),
                    )
                }
            } else {
                // Hero card
                item(key = "asset_hero") {
                    AssetHeroCard(
                        summary = state.accountSummary,
                        segmentItems = segmentItems,
                        selectedKey = selectedSegmentKey,
                        onSegmentClick = { segment ->
                            selectedSegmentKey = if (selectedSegmentKey == segment.key) null else segment.key
                        },
                    )
                }

                // Asset breakdown
                if (assetAggregates.isNotEmpty()) {
                    item(key = "asset_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "资产构成",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = totalAssets.asRmb(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = incomeColor(),
                            )
                        }
                    }

                    items(assetAggregates, key = { "asset_${it.first.name}" }) { (type, amount) ->
                        val share = if (totalAssets.minor > 0) amount.minor.toFloat() / totalAssets.minor else 0f
                        val isHighlighted = selectedSegmentKey == type.name
                        TypeCompositionCard(
                            type = type,
                            amount = amount,
                            share = share,
                            isLiability = false,
                            isHighlighted = isHighlighted,
                            onClick = {
                                selectedSegmentKey = if (isHighlighted) null else type.name
                            },
                        )
                    }
                }

                // Liability breakdown
                if (liabilityAggregates.isNotEmpty()) {
                    item(key = "liability_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "负债构成",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = totalLiabilities.asRmb(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = expenseColor(),
                            )
                        }
                    }

                    items(liabilityAggregates, key = { "liability_${it.first.name}" }) { (type, amount) ->
                        val share = if (totalLiabilities.minor > 0) amount.minor.toFloat() / totalLiabilities.minor else 0f
                        val isHighlighted = selectedSegmentKey == "LIABILITY" || selectedSegmentKey == type.name
                        TypeCompositionCard(
                            type = type,
                            amount = amount,
                            share = share,
                            isLiability = true,
                            isHighlighted = isHighlighted,
                            onClick = {
                                selectedSegmentKey = if (isHighlighted) null else "LIABILITY"
                            },
                        )
                    }
                }
            }
        }
    }
}
