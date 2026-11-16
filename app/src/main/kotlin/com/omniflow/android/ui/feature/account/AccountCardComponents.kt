package com.omniflow.android.ui.feature.account

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.tokens.BrandIcons
import com.omniflow.android.ui.designsystem.tokens.accountCardGradient
import com.omniflow.android.ui.designsystem.tokens.accountTypeBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.label
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountRole
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.Money

@Composable
internal fun ThinSubtotalStrip(
    type: AccountType,
    subtotal: Money,
    hideAmount: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = type.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (hideAmount) "••••••" else subtotal.asRmb(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun RoleBadge(
    role: AccountRole,
    modifier: Modifier = Modifier,
) {
    val (containerColor, contentColor) = when (role) {
        AccountRole.HUSBAND -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) to MaterialTheme.colorScheme.onPrimaryContainer
        AccountRole.WIFE -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f) to MaterialTheme.colorScheme.onTertiaryContainer
        AccountRole.FAMILY -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        modifier = modifier,
    ) {
        Text(
            text = role.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
internal fun OmniBankCard(
    account: Account,
    hideAmount: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1.0f,
) {
    val isCreditCard = account.type == AccountType.CREDIT_CARD
    val hasCreditLimit = account.creditLimit.minor > 0
    val debt = account.debt
    val isLiability = debt.minor > 0 || account.balance.minor < 0
    val badgeColors = accountTypeBadgeColors(account.type)
    val cardGradient = accountCardGradient(account.type)
    val brandRes = BrandIcons.findBrandDrawable(account.iconKey, account.name)

    val tailOrNote = listOfNotNull(
        account.cardNumber?.takeLast(4)?.takeIf(String::isNotBlank)?.let { "尾号 $it" },
        account.note?.takeIf(String::isNotBlank),
    ).joinToString(" · ")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardGradient)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f * alpha),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Top row: Brand / Type Icon + Type Name + Role Badge + Liability Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (brandRes != null) {
                        Image(
                            painter = painterResource(id = brandRes),
                            contentDescription = account.name,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(badgeColors.container),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(account.iconKey),
                                contentDescription = null,
                                tint = badgeColors.content,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Text(
                        text = account.type.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    RoleBadge(role = account.ownerRole)
                }

                if (isLiability) {
                    Surface(
                        shape = CircleShape,
                        color = expenseColor().copy(alpha = 0.2f),
                    ) {
                        Text(
                            text = if (isCreditCard && debt.minor > 0) "待还欠款" else "负债",
                            color = expenseColor(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // Center amount
            if (isCreditCard && hasCreditLimit) {
                // For credit cards: Show available limit hero or debt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = "可用额度",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (hideAmount) "••••••" else (account.availableLimit?.asRmb() ?: account.balance.asRmb()),
                            style = OmniText.amountHero,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (debt.minor > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "当期欠款",
                                style = MaterialTheme.typography.labelSmall,
                                color = expenseColor(),
                            )
                            Text(
                                text = if (hideAmount) "••••••" else debt.asRmb(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = expenseColor(),
                            )
                        }
                    }
                }

                // Credit line & utilization bar
                val limitMinor = account.creditLimit.minor
                val availMinor = account.availableLimit?.minor ?: limitMinor
                val usedMinor = (limitMinor - availMinor).coerceAtLeast(0L)
                val utilizationRatio = if (limitMinor > 0) (usedMinor.toFloat() / limitMinor.toFloat()).coerceIn(0f, 1f) else 0f

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { utilizationRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (utilizationRatio > 0.8f) expenseColor() else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "总授信 ${if (hideAmount) "••••" else account.creditLimit.asRmb()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "使用率 ${(utilizationRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // Deposit / Cash / Investment accounts
                Column {
                    Text(
                        text = if (account.type == AccountType.INVESTMENT) "持仓总资产" else "账户余额",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (hideAmount) "••••••" else account.balance.asRmb(),
                        style = OmniText.amountHero,
                        color = if (isLiability) expenseColor() else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Bottom row: account name + card number / note
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (tailOrNote.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tailOrNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DashedNewAccountCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strokeColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = strokeColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "新建账户",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
