package com.omniflow.android.ui.feature.more

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.components.OmniSettingsGroup
import com.omniflow.android.ui.designsystem.components.OmniSettingsRow
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.components.OmniSubPageScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniSpacingTokens
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType

private enum class AboutDialogType {
    TERMS,
    PRIVACY,
    LICENSES,
}

@Composable
fun AboutPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val versionName = remember(context) { resolveVersionName(context) }
    var activeSheet by remember { mutableStateOf<AboutDialogType?>(null) }

    OmniSubPageScaffold(
        title = "关于",
        onBack = onBack,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OmniSpacingTokens.pageHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(OmniSpacingTokens.medium))

            // 居中品牌区 (80dp Canvas 矢量 Logo + OmniFlow + 版本胶囊 + 一句使命)
            BrandHero(versionName = versionName)

            Spacer(Modifier.height(OmniSpacingTokens.xxl))

            // 功能行 (8dp 独立卡片，所属第 1 组 SYSTEM 配色)
            OmniSettingsGroup {
                OmniSettingsRow(
                    title = "检查更新",
                    icon = Icons.Default.SystemUpdate,
                    badgeType = SemanticBadgeType.SYSTEM,
                    subtitle = "前往 GitHub Releases 查看新版本",
                    onClick = {
                        openUrl(context, "https://github.com/nowhere-man/omni-flow/releases")
                    },
                )
                OmniSettingsRow(
                    title = "意见反馈",
                    icon = Icons.Default.Feedback,
                    badgeType = SemanticBadgeType.SYSTEM,
                    subtitle = "提交 Issue 与改进建议",
                    onClick = {
                        openUrl(context, "https://github.com/nowhere-man/omni-flow/issues")
                    },
                )
                OmniSettingsRow(
                    title = "用户协议",
                    icon = Icons.Default.Description,
                    badgeType = SemanticBadgeType.SYSTEM,
                    subtitle = "服务条款与使用准则",
                    onClick = { activeSheet = AboutDialogType.TERMS },
                )
                OmniSettingsRow(
                    title = "隐私政策",
                    icon = Icons.Default.Security,
                    badgeType = SemanticBadgeType.SYSTEM,
                    subtitle = "本地优先、零上传承诺",
                    onClick = { activeSheet = AboutDialogType.PRIVACY },
                )
                OmniSettingsRow(
                    title = "开源许可",
                    icon = Icons.Default.Code,
                    badgeType = SemanticBadgeType.SYSTEM,
                    subtitle = "第三方开源组件声明",
                    onClick = { activeSheet = AboutDialogType.LICENSES },
                )
            }

            Spacer(Modifier.height(OmniSpacingTokens.xxl))

            // 底部版权行
            Text(
                text = "© 2026 OmniFlow · Material 3 Expressive",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(OmniSpacingTokens.large))
        }
    }

    // 弹层展示协议与开源声明
    when (activeSheet) {
        AboutDialogType.TERMS -> AboutTextSheet(
            title = "用户协议",
            content = TermsOfServiceText,
            onDismiss = { activeSheet = null },
        )
        AboutDialogType.PRIVACY -> AboutTextSheet(
            title = "隐私政策",
            content = PrivacyPolicyText,
            onDismiss = { activeSheet = null },
        )
        AboutDialogType.LICENSES -> AboutTextSheet(
            title = "开源许可",
            content = OpenSourceLicensesText,
            onDismiss = { activeSheet = null },
        )
        null -> Unit
    }
}

@Composable
private fun BrandHero(versionName: String) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OmniSpacingTokens.small),
    ) {
        // 80dp Canvas 矢量 Logo
        Canvas(modifier = Modifier.size(80.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = primaryContainer,
                radius = size.width / 2f,
                center = center,
            )

            val flowPath = Path().apply {
                val w = size.width
                val h = size.height
                moveTo(w * 0.25f, h * 0.55f)
                cubicTo(w * 0.35f, h * 0.35f, w * 0.45f, h * 0.70f, w * 0.60f, h * 0.42f)
                cubicTo(w * 0.68f, h * 0.28f, w * 0.75f, h * 0.45f, w * 0.78f, h * 0.48f)
            }
            drawPath(
                path = flowPath,
                color = primary,
                style = Stroke(width = 6f, cap = StrokeCap.Round),
            )

            drawCircle(
                color = primary,
                radius = 7f,
                center = Offset(size.width * 0.78f, size.height * 0.48f),
            )
        }

        Spacer(Modifier.height(OmniSpacingTokens.extraSmall))

        Text(
            text = "OmniFlow",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        // 版本胶囊
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Text(
                text = "v$versionName",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        // 一句使命
        Text(
            text = "记录每一笔，理清每一分",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AboutTextSheet(
    title: String,
    content: String,
    onDismiss: () -> Unit,
) {
    OmniSheetScaffold(
        title = title,
        onDismiss = onDismiss,
        cancelText = "关闭",
        onCancel = onDismiss,
    ) {
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = OmniSpacingTokens.medium),
        )
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

internal fun resolveVersionName(context: Context): String = runCatching {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(
            context.packageName,
            android.content.pm.PackageManager.PackageInfoFlags.of(0),
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    packageInfo.versionName ?: "0.21.0"
}.getOrNull() ?: "0.21.0"

private const val TermsOfServiceText = """欢迎使用 OmniFlow。

1. 软件使用
OmniFlow 是一款开源的本地优先个人记账工具。您可以自由使用本应用管理您的个人财务数据。

2. 责任限制
本应用按“原样”提供，不提供任何明示或暗示的担保。在任何情况下，开发者均不对因使用或无法使用本软件而导致的任何直接、间接损失承担责任。

3. 规则合规
请遵守当地法律法规，严禁将本应用用于任何非法用途。"""

private const val PrivacyPolicyText = """OmniFlow 隐私保护承诺：

1. 本地优先与数据主权
您的所有账单、分类、规则、账户及记账数据完全保存在您的设备本地。我们没有中心化服务器，不会搜集、上传或分析您的任何记账隐私。

2. 网络权限与云同步
仅在您主动配置并启用 WebDAV 同步时，应用才会连接您指定的私有服务器；同步传输全程加密，密钥在本地保护。

3. 第三方服务与 AI
若您开启大模型智能分类功能，仅在您手动发起或授权时将必要的脱敏账单描述发送给您配置的服务提供方，API Key 在本地安全保存。"""

private const val OpenSourceLicensesText = """OmniFlow 采用以下开源组件与技术构建：

• Kotlin & Coroutines (Apache License 2.0)
• Jetpack Compose & Material 3 Expressive (Apache License 2.0)
• SQLDelight (Apache License 2.0)
• Kotlinx Serialization & Datetime (Apache License 2.0)
• Android Jetpack Navigation & Lifecycle (Apache License 2.0)

感谢所有开源贡献者。"""
