package com.omniflow.android.ui.feature.asset

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniBarSegment
import com.omniflow.android.ui.designsystem.components.OmniSegmentBar
import com.omniflow.android.ui.designsystem.tokens.BrandIcons
import com.omniflow.android.ui.designsystem.tokens.accountTypeBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.feature.account.RoleBadge
import com.omniflow.android.ui.incomeColor
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountSummary
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money

@Composable
internal fun AssetHeroCard(
    summary: AccountSummary,
    totalCreditLimit: Money,
    totalDebt: Money,
    selectedRole: AccountRole?,
    segmentItems: List<OmniBarSegment>,
    selectedKey: String?,
    onSegmentClick: (OmniBarSegment) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (selectedRole == null) "家庭净资产" else "${selectedRole.label}净资产",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // C1 / MD3: RoundedCornerShape(8.dp) instead of CircleShape for badges
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Text(
                        text = if (selectedRole == null) "家庭合并视角" else selectedRole.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = summary.netAssets.asRmb(),
                style = OmniText.amountHero,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (segmentItems.isNotEmpty()) {
                OmniSegmentBar(
                    segments = segmentItems,
                    selectedKey = selectedKey,
                    onSegmentClick = onSegmentClick,
                )
            }

            // Sub-metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "总资产储备",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = summary.assets.asRmb(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = incomeColor(),
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "当前负债欠款",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = summary.liabilities.asRmb(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (summary.liabilities.minor > 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                    )
                }

                if (totalCreditLimit.minor > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "可用授信缓冲",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val availBuffer = Money((totalCreditLimit.minor - totalDebt.minor).coerceAtLeast(0L))
                        Text(
                            text = availBuffer.asRmb(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Credit utilization bar if credit cards exist
            if (totalCreditLimit.minor > 0) {
                val usageRatio = (totalDebt.minor.toFloat() / totalCreditLimit.minor.toFloat()).coerceIn(0f, 1f)
                val usageColor by animateColorAsState(
                    targetValue = when {
                        usageRatio > 0.7f -> expenseColor()
                        usageRatio > 0.4f -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    label = "usage_color",
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "授信使用率",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${(usageRatio * 100).toInt()}% (已用 ${totalDebt.asRmb()} / 额度 ${totalCreditLimit.asRmb()})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = usageColor,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { usageRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(3.dp)),
                        color = usageColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

/**
 * C5: 资产页账户行 — 遵循 Google 账号分组卡片标准 (OmniSettingsGroup 内嵌行)
 */
@Composable
internal fun AssetAccountRow(
    account: Account,
    onQuickEdit: (Account) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCredit = account.type == AccountType.CREDIT_CARD
    val isInvest = account.type == AccountType.INVESTMENT
    val debt = account.debt
    val brandRes = BrandIcons.findBrandDrawable(account.iconKey, account.name)
    val badgeColors = accountTypeBadgeColors(account.type)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onQuickEdit(account) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: Brand Logo + Name + Role Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            if (brandRes != null) {
                Image(
                    painter = painterResource(id = brandRes),
                    contentDescription = account.name,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(badgeColors.container),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = getCategoryIcon(account.iconKey),
                        contentDescription = null,
                        tint = badgeColors.content,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    RoleBadge(role = account.ownerRole)
                }
                if (isCredit && account.creditLimit.minor > 0) {
                    Text(
                        text = "授信: ${account.creditLimit.asRmb()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Right: Values and Quick Edit Trigger
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (isCredit) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "可用: ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = account.availableLimit?.asRmb() ?: account.balance.asRmb(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (debt.minor > 0) {
                        Text(
                            text = "欠款 ${debt.asRmb()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = expenseColor(),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else if (isInvest) {
                    Text(
                        text = account.balance.asRmb(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (account.profitLoss.minor != 0L) {
                        val isGain = account.profitLoss.minor > 0
                        Text(
                            text = "${if (isGain) "+" else ""}${account.profitLoss.asRmb()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isGain) incomeColor() else expenseColor(),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    Text(
                        text = account.balance.asRmb(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "快速对账",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}


