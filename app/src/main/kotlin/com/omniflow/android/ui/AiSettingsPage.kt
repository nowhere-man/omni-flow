package com.omniflow.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.omniflow.android.AiCategoryAdapter
import com.omniflow.android.AiConfig
import com.omniflow.android.AiCredentials
import kotlinx.coroutines.launch

/**
 * AI 分类配置。和 WebDAV 那页一样直接读写 SharedPreferences：
 * 密钥不能进数据库（`AppPreferences` 会被整表写进备份的明文 JSON），
 * 所以也没必要穿过 ViewModel 和 core。
 */
@Composable
internal fun AiSettingsPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved = remember { AiCredentials.load(context) }
    var enabled by remember { mutableStateOf(saved.enabled) }
    var baseUrl by remember { mutableStateOf(saved.baseUrl) }
    var apiKey by remember { mutableStateOf(saved.apiKey) }
    var model by remember { mutableStateOf(saved.model) }
    var showKey by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val config = AiConfig(enabled = enabled, baseUrl = baseUrl, apiKey = apiKey, model = model)

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(OmniRadius.medium), color = surfaceCard()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("导入时用 AI 建议分类", style = OmniText.titleRow)
                            Text(
                                "只有规则和分类记忆都没命中的条目才会请求",
                                style = OmniText.caption,
                                color = mutedContent(),
                            )
                        }
                        Switch(enabled, { enabled = it; message = null })
                    }
                    OutlinedTextField(
                        baseUrl,
                        { baseUrl = it; message = null },
                        label = { Text("服务地址") },
                        placeholder = { Text("https://api.deepseek.com/v1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        apiKey,
                        { apiKey = it; message = null },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "隐藏" else "显示") }
                        },
                    )
                    OutlinedTextField(
                        model,
                        { model = it; message = null },
                        label = { Text("模型名称") },
                        placeholder = { Text("deepseek-chat") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                AiCredentials.save(context, config)
                                message = "已保存"
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text("保存配置") }
                        // 地址少写 /v1、Key 粘错是最常见的失败，没有这个按钮就只能等导入跑到一半才发现
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
                            modifier = Modifier.weight(1f),
                        ) { Text(if (testing) "测试中…" else "测试连接") }
                    }
                    message?.let {
                        Text(
                            it,
                            style = OmniText.caption,
                            color = if (it.contains("失败")) MaterialTheme.colorScheme.error else mutedContent(),
                        )
                    }
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(OmniRadius.medium), color = surfaceInset()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("关于隐私", style = OmniText.caption, color = mutedContent())
                    Text(
                        "开启后，待分类条目的交易对方、商品说明、来源分类和金额会发送到你填写的服务地址；" +
                            "账号、卡号、对方开户行不会发送。服务地址由你自己指定，也可以填局域网里的 Ollama。" +
                            "API Key 经设备密钥库加密后存放，不会进入云端备份。",
                        style = OmniText.caption,
                        color = mutedContent(),
                    )
                }
            }
        }
        item {
            Text(
                "同一个商户只会请求一次；确认导入后结果会写进分类记忆，下次导入直接命中，不再消耗额度。",
                style = OmniText.caption,
                color = mutedContent(),
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}
