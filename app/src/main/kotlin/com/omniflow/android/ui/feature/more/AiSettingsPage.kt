package com.omniflow.android.ui.feature.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.omniflow.android.AiCategoryAdapter
import com.omniflow.android.AiConfig
import com.omniflow.android.AiCredentials
import com.omniflow.android.ui.OmniRadius
import com.omniflow.android.ui.OmniText
import com.omniflow.android.ui.OmniTextField
import com.omniflow.android.ui.designsystem.components.OmniBadgeIcon
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.designsystem.tokens.semanticBadgeColors
import com.omniflow.android.ui.mutedContent
import kotlinx.coroutines.launch

private enum class AiVendor(val title: String, val endpoint: String, val model: String) {
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
    OLLAMA("Ollama", "http://localhost:11434/v1", "llama3"),
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
    CUSTOM("自定义", "", ""),
}

/**
 * 智能分类控制台：全页锁第 3 组 AI 语义色。
 * 顶部可玩演示框 + 3 段强度胶囊 + 厂商预置 + 3 级决策流可视化与隐私卡。
 */
@Composable
fun AiSettingsPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiColors = semanticBadgeColors(SemanticBadgeType.AI)
    val saved = remember { AiCredentials.load(context) }
    var enabled by remember { mutableStateOf(saved.enabled) }
    var baseUrl by remember { mutableStateOf(saved.baseUrl) }
    var apiKey by remember { mutableStateOf(saved.apiKey) }
    var model by remember { mutableStateOf(saved.model) }
    var message by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var selectedVendor by remember {
        mutableStateOf(
            when {
                baseUrl.contains("deepseek") -> AiVendor.DEEPSEEK
                baseUrl.contains("localhost") || baseUrl.contains("11434") -> AiVendor.OLLAMA
                baseUrl.contains("openai") -> AiVendor.OPENAI
                else -> AiVendor.CUSTOM
            },
        )
    }

    val config = AiConfig(enabled = enabled, baseUrl = baseUrl, apiKey = apiKey, model = model)

    OmniSubPageScaffold(
        title = "智能分类",
        onBack = onBack,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. 实时可玩演示框
            item {
                AiPlayableDemoBox()
            }

            // 2. 自动分类强度三段胶囊
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OmniBadgeIcon(
                                icon = Icons.Default.Tune,
                                badgeType = SemanticBadgeType.AI,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("分类建议强度", style = OmniText.titleRow)
                                Text(
                                    if (enabled) "导入账单未命中规则时自动请求分类建议" else "已关闭 AI 辅助分类",
                                    style = OmniText.caption,
                                    color = mutedContent(),
                                )
                            }
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val options = listOf("关", "仅建议", "自动入账")
                            val selectedIndex = if (!enabled) 0 else 1
                            options.forEachIndexed { index, label ->
                                SegmentedButton(
                                    selected = selectedIndex == index,
                                    onClick = {
                                        enabled = index > 0
                                        message = null
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    label = { Text(label) },
                                )
                            }
                        }
                    }
                }
            }

            // 3. 模型服务配置卡
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OmniBadgeIcon(
                                icon = Icons.Default.AutoAwesome,
                                badgeType = SemanticBadgeType.AI,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("模型服务连接", style = OmniText.titleRow)
                                Text("兼容 OpenAI API 规范的各家服务", style = OmniText.caption, color = mutedContent())
                            }
                        }

                        // 厂商预置 Chip
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AiVendor.entries.forEach { vendor ->
                                FilterChip(
                                    selected = selectedVendor == vendor,
                                    onClick = {
                                        selectedVendor = vendor
                                        if (vendor != AiVendor.CUSTOM) {
                                            baseUrl = vendor.endpoint
                                            model = vendor.model
                                        }
                                        message = null
                                    },
                                    label = { Text(vendor.title) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = aiColors.container,
                                        selectedLabelColor = aiColors.content,
                                    ),
                                )
                            }
                        }

                        OmniTextField(
                            value = baseUrl,
                            onValueChange = {
                                baseUrl = it
                                selectedVendor = AiVendor.CUSTOM
                                message = null
                            },
                            label = "服务端点 (Base URL)",
                            placeholder = "https://api.deepseek.com/v1",
                            leadingIcon = Icons.Default.CloudSync,
                        )

                        OmniTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it; message = null },
                            label = "API Key",
                            placeholder = "sk-...",
                            isPassword = true,
                            leadingIcon = Icons.Default.Lock,
                        )

                        OmniTextField(
                            value = model,
                            onValueChange = {
                                model = it
                                selectedVendor = AiVendor.CUSTOM
                                message = null
                            },
                            label = "模型名称 (Model)",
                            placeholder = "deepseek-chat",
                            leadingIcon = Icons.Default.AutoAwesome,
                        )

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    AiCredentials.save(context, config)
                                    message = "配置已保存"
                                },
                                shape = OmniShapeTokens.button,
                                modifier = Modifier.weight(1f).height(46.dp),
                            ) {
                                Text("保存配置")
                            }
                            OutlinedButton(
                                onClick = {
                                    testing = true
                                    message = null
                                    scope.launch {
                                        val result = AiCategoryAdapter(context).testConnection(config)
                                        testing = false
                                        message = result.getOrElse { "连接失败：${it.message ?: "未知错误"}" }
                                    }
                                },
                                enabled = !testing && baseUrl.isNotBlank() && model.isNotBlank(),
                                shape = OmniShapeTokens.button,
                                modifier = Modifier.weight(1f).height(46.dp),
                            ) {
                                Text(if (testing) "测试中…" else "测试连通性")
                            }
                        }

                        message?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(OmniRadius.small),
                                color = if (msg.contains("失败")) MaterialTheme.colorScheme.errorContainer else aiColors.container,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    msg,
                                    style = OmniText.caption,
                                    color = if (msg.contains("失败")) MaterialTheme.colorScheme.onErrorContainer else aiColors.content,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 4. 三级决策流体系卡
            item {
                DecisionFlowCard()
            }

            // 5. 隐私声明卡
            item {
                AiPrivacyCard()
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
