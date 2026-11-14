package com.omniflow.android.ui.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.designsystem.tokens.OmniColorTokens
import com.omniflow.android.ui.mutedContent
import com.omniflow.android.ui.surfaceInset

@Composable
internal fun AiPlayableDemoBox(
    modifier: Modifier = Modifier,
) {
    var inputText by remember { mutableStateOf("午饭在麦当劳吃了 38.5 元微信支付") }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(OmniColorTokens.BadgeAiContainerLight, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = OmniColorTokens.BadgeAiContentLight,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "实时智能解析演示",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "输入任意自然语言句子，实时预览智能识别效果",
                        style = OmniText.caption,
                        color = mutedContent(),
                    )
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如：打车去公司花了 25 元支付宝") },
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
            )

            // 解析结果筹码行
            val amount = parseDemoAmount(inputText)
            val category = parseDemoCategory(inputText)
            val account = parseDemoAccount(inputText)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("识别结果：", style = OmniText.caption, color = mutedContent())
                DemoResultChip(label = amount, containerColor = OmniColorTokens.BadgePlanningContainerLight, contentColor = OmniColorTokens.BadgePlanningContentLight)
                DemoResultChip(label = category, containerColor = OmniColorTokens.BadgeAiContainerLight, contentColor = OmniColorTokens.BadgeAiContentLight)
                DemoResultChip(label = account, containerColor = OmniColorTokens.BadgeSystemContainerLight, contentColor = OmniColorTokens.BadgeSystemContentLight)
            }
        }
    }
}

@Composable
private fun DemoResultChip(
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
        )
    }
}

private fun parseDemoAmount(text: String): String {
    val regex = Regex("""(\d+(\.\d+)?)""")
    val match = regex.find(text)
    return match?.let { "¥${it.value}" } ?: "¥0.00"
}

private fun parseDemoCategory(text: String): String = when {
    text.contains("饭") || text.contains("吃") || text.contains("麦当劳") || text.contains("咖啡") || text.contains("外卖") -> "餐饮美食"
    text.contains("车") || text.contains("地铁") || text.contains("打车") || text.contains("滴滴") -> "交通出行"
    text.contains("买") || text.contains("超市") || text.contains("商场") -> "日常购物"
    text.contains("工资") || text.contains("兼职") || text.contains("收入") -> "职业薪酬"
    else -> "其他支出"
}

private fun parseDemoAccount(text: String): String = when {
    text.contains("微信") -> "微信零钱"
    text.contains("支付宝") -> "支付宝余额"
    text.contains("银行") || text.contains("卡") -> "招商银行卡"
    text.contains("现金") -> "现金"
    else -> "默认账户"
}

@Composable
internal fun DecisionFlowCard(modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "三级决策分流体系",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DecisionStepNode(title = "1. 用户规则", desc = "明确指令优先", icon = Icons.AutoMirrored.Filled.Rule)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                DecisionStepNode(title = "2. 本地记忆", desc = "历史商户匹配", icon = Icons.Default.Memory)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                DecisionStepNode(title = "3. 大模型", desc = "语义推断兜底", icon = Icons.Default.AutoAwesome)
            }
        }
    }
}

@Composable
private fun DecisionStepNode(
    title: String,
    desc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(OmniColorTokens.BadgeAiContainerLight, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = OmniColorTokens.BadgeAiContentLight)
        }
        Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
        Text(desc, style = OmniText.caption, color = mutedContent())
    }
}

@Composable
internal fun AiPrivacyCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = surfaceInset(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = OmniColorTokens.BadgeAiContentLight,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "本地隐私与安全承诺",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "仅在规则和记忆未命中时，发送脱敏后的交易摘要与分类候选集；" +
                        "绝不发送银行卡号、账号及余额；API Key 由设备 Keystore 硬件级加密存储，" +
                        "绝不随备份文件明文导出。",
                    style = OmniText.caption,
                    color = mutedContent(),
                )
            }
        }
    }
}
