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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.designsystem.tokens.ledgerCoverGradient
import com.omniflow.core.domain.model.LedgerCoverSpec
import java.io.File

/**
 * 扁平 MD3 账本封面预览卡片（替代拟物实体书）。
 * 保留封面色彩系统（渐变色、几何底纹、自定义图），彻底移除拟本书脊折痕、边缘厚度纸影与书签丝带。
 */
@Composable
fun LedgerCoverPreview(
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

    val shape = RoundedCornerShape(16.dp)

    ElevatedCard(
        shape = shape,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .aspectRatio(3f / 4f)
            .then(if (isArchived) Modifier.alpha(0.55f) else Modifier)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
            }

            // 默认徽章 (扁平 MD3 风格)
            if (isDefault) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                ) {
                    Text(
                        text = "默认",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getCategoryIcon(iconKey),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
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

private fun DrawScope.drawCustomCoverImage(bitmap: Bitmap, isArchived: Boolean) {
    val imageBitmap = bitmap.asImageBitmap()
    val filter = if (isArchived) {
        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.3f) })
    } else null
    drawImage(
        image = imageBitmap,
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        colorFilter = filter,
    )
}

internal fun loadLedgerCoverBitmap(context: Context, fileName: String): Bitmap? = runCatching {
    val file = File(context.filesDir, "ledger_covers/$fileName")
    if (file.exists()) {
        BitmapFactory.decodeFile(file.absolutePath)
    } else null
}.getOrNull()
