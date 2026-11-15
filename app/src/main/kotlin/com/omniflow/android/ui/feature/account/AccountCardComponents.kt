package com.omniflow.android.ui.feature.account

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.asRmb
import com.omniflow.android.ui.designsystem.tokens.accountCardGradient
import com.omniflow.android.ui.designsystem.tokens.accountTypeBadgeColors
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.expenseColor
import com.omniflow.android.ui.label
import com.omniflow.core.domain.model.Account
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
internal fun OmniBankCard(
    account: Account,
    hideAmount: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alpha: Float = 1.0f,
) {
    val isLiability = account.balance.minor < 0
    val badgeColors = accountTypeBadgeColors(account.type)
    val cardGradient = accountCardGradient(account.type)
    val tailOrNote = listOfNotNull(
        account.cardNumber?.takeLast(4)?.takeIf(String::isNotBlank)?.let { "尾号 $it" },
        account.note?.takeIf(String::isNotBlank),
    ).joinToString(" · ")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(138.dp)
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
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top: type badge and liability tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                    Text(
                        text = account.type.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (isLiability) {
                    Surface(
                        shape = CircleShape,
                        color = expenseColor().copy(alpha = 0.25f),
                    ) {
                        Text(
                            text = "负债",
                            color = expenseColor(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            // Center: big balance
            Text(
                text = if (hideAmount) "••••••" else account.balance.asRmb(),
                style = OmniText.amountHero,
                color = if (isLiability) expenseColor() else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // Bottom: account name + tail / note
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
                    Spacer(Modifier.size(8.dp))
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
