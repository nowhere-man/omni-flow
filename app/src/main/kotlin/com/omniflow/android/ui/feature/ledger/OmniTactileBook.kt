package com.omniflow.android.ui.feature.ledger

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniflow.android.ui.designsystem.tokens.OmniColorTokens
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.designsystem.tokens.ledgerCoverGradient
import com.omniflow.core.domain.model.LedgerCoverSpec
import java.io.File

/**
 * 3:4 拟物实体书账本组件。
 *
 * 规格契约：
 * - 3:4 比例 Canvas 绘制
 * - 左侧 12dp 书脊折痕渐变 (spine crease gradient)
 * - 右侧 3dp 纸页厚度微影 (page-stack micro-shadow)
 * - 默认/当前账本顶部垂落燕尾书签带 (ribbon)
 * - 封面排印：大图标 + 书名，严禁印任何金额与笔数
 */
@Composable
fun OmniTactileBook(
    name: String,
    coverKey: String?,
    modifier: Modifier = Modifier,
    ledgerId: String = "",
    isDefault: Boolean = false,
    isArchived: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val spec = remember(coverKey, ledgerId) { LedgerCoverSpec.parse(coverKey, ledgerId) }
    val customBitmap = remember(spec) {
        if (spec is LedgerCoverSpec.Custom) {
            loadLedgerCoverBitmap(context, spec.fileName)
        } else null
    }

    val effectivePreset = when {
        spec is LedgerCoverSpec.Preset -> spec
        customBitmap == null -> LedgerCoverSpec.fallback(ledgerId)
        else -> null
    }

    val gradient = effectivePreset?.let { ledgerCoverGradient(it.colorKey) }
    val iconKey = effectivePreset?.iconKey ?: "books"
    val patternKey = effectivePreset?.patternKey ?: "clean"

    Box(
        modifier = modifier
            .aspectRatio(3f / 4f)
            .shadow(if (isArchived) 2.dp else 6.dp, BookShape)
            .clip(BookShape)
            .then(if (isArchived) Modifier.alpha(0.55f) else Modifier)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (customBitmap != null) {
                drawCustomCoverImage(customBitmap, isArchived)
            } else if (gradient != null) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(gradient.first, gradient.second),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                    ),
                )
                drawPattern(patternKey)
            }

            // 左侧 12dp 书脊折痕渐变
            val spineWidthPx = 12.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    0.0f to Color.Black.copy(alpha = 0.28f),
                    0.45f to Color.White.copy(alpha = 0.16f),
                    1.0f to Color.Transparent,
                    startX = 0f,
                    endX = spineWidthPx,
                ),
                size = Size(spineWidthPx, size.height),
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.18f),
                start = Offset(spineWidthPx, 0f),
                end = Offset(spineWidthPx, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(spineWidthPx + 1f, 0f),
                end = Offset(spineWidthPx + 1f, size.height),
                strokeWidth = 1.dp.toPx(),
            )

            // 右侧 3dp 纸页厚度微影
            val pageStackWidthPx = 3.dp.toPx()
            drawRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(size.width - pageStackWidthPx, 0f),
                size = Size(pageStackWidthPx, size.height),
            )
            drawLine(
                color = Color.Black.copy(alpha = 0.15f),
                start = Offset(size.width - pageStackWidthPx, 0f),
                end = Offset(size.width - pageStackWidthPx, size.height),
                strokeWidth = 1f,
            )

            if (isDefault) {
                drawBookmarkRibbon()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 12.dp, top = 28.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.22f),
                modifier = Modifier.size(54.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getCategoryIcon(iconKey),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.weight(1.2f))
        }
    }
}

private fun DrawScope.drawPattern(patternKey: String) {
    val patternColor = Color.White.copy(alpha = 0.18f)
    when (patternKey) {
        "grid" -> {
            val step = 14.dp.toPx()
            var x = step
            while (x < size.width) {
                drawLine(patternColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(patternColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }
        "dots" -> {
            val step = 16.dp.toPx()
            var x = step
            while (x < size.width) {
                var y = step
                while (y < size.height) {
                    drawCircle(patternColor, radius = 2f, center = Offset(x, y))
                    y += step
                }
                x += step
            }
        }
        "diagonal" -> {
            val step = 16.dp.toPx()
            var offset = -size.height
            while (offset < size.width + size.height) {
                drawLine(
                    patternColor,
                    Offset(offset, 0f),
                    Offset(offset + size.height, size.height),
                    strokeWidth = 1.5f,
                )
                offset += step
            }
        }
        "waves" -> {
            val center = Offset(size.width / 2f, size.height * 0.45f)
            for (i in 1..6) {
                drawCircle(
                    color = patternColor,
                    radius = i * 22.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.5f),
                )
            }
        }
        "clean" -> Unit
    }
}

private fun DrawScope.drawBookmarkRibbon() {
    val ribbonWidth = 16.dp.toPx()
    val ribbonHeight = 28.dp.toPx()
    val x = size.width - 24.dp.toPx()
    val ribbonColor = OmniColorTokens.ExpenseLight

    val ribbonPath = Path().apply {
        moveTo(x, 0f)
        lineTo(x + ribbonWidth, 0f)
        lineTo(x + ribbonWidth, ribbonHeight)
        lineTo(x + ribbonWidth / 2f, ribbonHeight - 6.dp.toPx())
        lineTo(x, ribbonHeight)
        close()
    }
    drawPath(ribbonPath, ribbonColor)
}

private fun DrawScope.drawCustomCoverImage(bitmap: Bitmap, isArchived: Boolean) {
    val imageBitmap = bitmap.asImageBitmap()
    val filter = if (isArchived) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.3f) })
    } else null
    drawImage(
        image = imageBitmap,
        dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
        colorFilter = filter,
    )
}

internal fun loadLedgerCoverBitmap(context: Context, fileName: String): Bitmap? = runCatching {
    val file = File(context.filesDir, "ledger_covers/$fileName")
    if (file.exists()) {
        BitmapFactory.decodeFile(file.absolutePath)
    } else null
}.getOrNull()
