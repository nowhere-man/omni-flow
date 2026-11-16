package com.omniflow.android.ui.feature.asset

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.OmniTextField
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.components.OmniBarSegment
import com.omniflow.android.ui.designsystem.components.OmniSegmentBar
import com.omniflow.android.ui.designsystem.tokens.BrandIcons
import com.omniflow.android.ui.designsystem.tokens.accountTypeBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.feature.account.RoleBadge
import com.omniflow.android.ui.incomeColor
import com.omniflow.android.ui.label
import com.omniflow.android.ui.toDecimal
import com.omniflow.android.ui.toMoneyOrNull
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
                Surface(
                    shape = CircleShape,
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
                    modifier = Modifier.fillMaxWidth(),
                    onSegmentClick = onSegmentClick,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column {
                    Text(
                        text = "总资产",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = summary.assets.asRmb(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = incomeColor(),
                    )
                }
                Column {
                    Text(
                        text = "总负债",
                        style = MaterialTheme.typography.labelMedium,
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
                    val utilization = (totalDebt.minor.toDouble() / totalCreditLimit.minor.toDouble() * 100).coerceIn(0.0, 100.0)
                    Column {
                        Text(
                            text = "授信使用率",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = String.format("%.1f%%", utilization),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (utilization > 50.0) expenseColor() else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AssetAccountRowCard(
    account: Account,
    onQuickEdit: (Account) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCredit = account.type == AccountType.CREDIT_CARD
    val isInvest = account.type == AccountType.INVESTMENT
    val debt = account.debt
    val brandRes = BrandIcons.findBrandDrawable(account.iconKey, account.name)
    val badgeColors = accountTypeBadgeColors(account.type)

    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onQuickEdit(account) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                                text = "欠款: ${debt.asRmb()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = expenseColor(),
                            )
                        }
                    } else if (isInvest) {
                        Text(
                            text = account.balance.asRmb(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = incomeColor(),
                        )
                        Text(
                            text = "持仓市值",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = account.balance.asRmb(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (account.balance.minor < 0) expenseColor() else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "存款余额",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickEditAccountSheet(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (balance: Money, availableLimit: Money?, profitLoss: Money) -> Unit,
) {
    val isCredit = account.type == AccountType.CREDIT_CARD
    val isInvest = account.type == AccountType.INVESTMENT

    var availText by remember(account) {
        mutableStateOf(account.availableLimit?.toDecimal() ?: if (isCredit) account.creditLimit.toDecimal() else "")
    }
    var balanceText by remember(account) {
        mutableStateOf(account.balance.toDecimal())
    }
    var plText by remember(account) {
        mutableStateOf(account.profitLoss.toDecimal())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "更新对账信息 · ${account.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "修改后自动保存并记录到对账中心审计台账",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RoleBadge(role = account.ownerRole)
            }

            if (isCredit) {
                Text(
                    text = "授信总额度: ${account.creditLimit.asRmb()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )

                OmniTextField(
                    value = availText,
                    onValueChange = { availText = it },
                    label = "当前可用额度（元）",
                    placeholder = "如 10000.00",
                )

                val avail = availText.toMoneyOrNull()
                if (avail != null) {
                    val debtMinor = (account.creditLimit.minor - avail.minor).coerceAtLeast(0L)
                    Text(
                        text = "自动计算本期欠款: ${Money(debtMinor).asRmb()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = expenseColor(),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else if (isInvest) {
                OmniTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = "当前持仓总市值（元）",
                    placeholder = "0.00",
                )
                OmniTextField(
                    value = plText,
                    onValueChange = { plText = it },
                    label = "本月累计盈亏（元，可选）",
                    placeholder = "0.00",
                )
            } else {
                OmniTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = "存款最新余额（元）",
                    placeholder = "0.00",
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        if (isCredit) {
                            val avail = availText.toMoneyOrNull() ?: account.creditLimit
                            val debtMinor = (account.creditLimit.minor - avail.minor).coerceAtLeast(0L)
                            onConfirm(Money(-debtMinor), avail, Money.Zero)
                        } else if (isInvest) {
                            val bal = balanceText.toMoneyOrNull() ?: Money.Zero
                            val pl = plText.toMoneyOrNull() ?: Money.Zero
                            onConfirm(bal, null, pl)
                        } else {
                            val bal = balanceText.toMoneyOrNull() ?: Money.Zero
                            onConfirm(bal, null, Money.Zero)
                        }
                    },
                ) {
                    Text("确认更新")
                }
            }
        }
    }
}

@Composable
internal fun MonthlyNotesCard(
    notes: String,
    onEditNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEditNotes),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "本月对账重大收支与备忘",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "编辑备忘",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = if (notes.isNotBlank()) notes else "暂无本月备忘，点击此处记录（如：转账、年终奖、大额支出等）",
                style = MaterialTheme.typography.bodySmall,
                color = if (notes.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
