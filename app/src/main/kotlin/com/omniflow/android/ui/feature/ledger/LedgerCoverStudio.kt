package com.omniflow.android.ui.feature.ledger

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.designsystem.tokens.OmniSpacingTokens
import com.omniflow.android.ui.designsystem.tokens.getCategoryIcon
import com.omniflow.android.ui.designsystem.tokens.ledgerCoverGradient
import com.omniflow.core.domain.model.LedgerCoverSpec
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerCoverStudioSheet(
    initialCoverKey: String?,
    ledgerName: String,
    onDismiss: () -> Unit,
    onSaveCover: (String) -> Unit,
    modifier: Modifier = Modifier,
    ledgerId: String = "",
) {
    val context = LocalContext.current
    var currentKey by remember(initialCoverKey) {
        mutableStateOf(initialCoverKey ?: LedgerCoverSpec.fallback(ledgerId).encode())
    }

    val parsed = remember(currentKey) { LedgerCoverSpec.parse(currentKey, ledgerId) }

    var selectedColor by remember(currentKey) {
        mutableStateOf(
            if (parsed is LedgerCoverSpec.Preset) parsed.colorKey else LedgerCoverSpec.PresetColors.first(),
        )
    }
    var selectedPattern by remember(currentKey) {
        mutableStateOf(
            if (parsed is LedgerCoverSpec.Preset) parsed.patternKey else LedgerCoverSpec.PresetPatterns.first(),
        )
    }
    var selectedIcon by remember(currentKey) {
        mutableStateOf(
            if (parsed is LedgerCoverSpec.Preset) parsed.iconKey else LedgerCoverSpec.PresetIcons.first(),
        )
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = saveLedgerCoverFromUri(context, uri)
            if (fileName != null) {
                // 若旧封面是自定义图，删除旧图
                if (currentKey.startsWith("custom:")) {
                    deleteLedgerCoverFile(context, currentKey)
                }
                currentKey = LedgerCoverSpec.Custom(fileName).encode()
            }
        }
    }

    OmniSheetScaffold(
        title = "封面工坊",
        onDismiss = onDismiss,
        onSave = {
            onSaveCover(currentKey)
            onDismiss()
        },
        saveText = "确定",
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 扁平 MD3 封面预览
            Box(modifier = Modifier.width(130.dp)) {
                LedgerCoverPreview(
                    name = ledgerName.ifBlank { "账本名" },
                    coverKey = currentKey,
                    ledgerId = ledgerId,
                )
            }

            Spacer(Modifier.height(OmniSpacingTokens.medium))

            // 自定义相册图片入口
            OutlinedButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                shape = OmniShapeTokens.button,
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("从相册选取 3:4 自定义图")
            }

            Spacer(Modifier.height(OmniSpacingTokens.medium))

            // 预置调色板
            Text(
                text = "预置色彩",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.align(Alignment.Start),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(OmniSpacingTokens.small))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(LedgerCoverSpec.PresetColors) { colorKey ->
                    val gradient = ledgerCoverGradient(colorKey)
                    val isSelected = parsed is LedgerCoverSpec.Preset && selectedColor == colorKey

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(gradient.first)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape,
                            )
                            .clickable {
                                selectedColor = colorKey
                                currentKey = LedgerCoverSpec.Preset(selectedColor, selectedPattern, selectedIcon).encode()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(OmniSpacingTokens.medium))

            // 纹样选择 (5 种 Canvas 几何纹样)
            Text(
                text = "质感纹样",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.align(Alignment.Start),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(OmniSpacingTokens.small))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                val patternLabels = mapOf(
                    "grid" to "方眼",
                    "dots" to "波点",
                    "diagonal" to "斜纹",
                    "waves" to "同心波",
                    "clean" to "素面",
                )
                LedgerCoverSpec.PresetPatterns.forEach { pKey ->
                    FilterChip(
                        selected = parsed is LedgerCoverSpec.Preset && selectedPattern == pKey,
                        onClick = {
                            selectedPattern = pKey
                            currentKey = LedgerCoverSpec.Preset(selectedColor, selectedPattern, selectedIcon).encode()
                        },
                        label = { Text(patternLabels[pKey] ?: pKey) },
                    )
                }
            }

            Spacer(Modifier.height(OmniSpacingTokens.medium))

            // 图标选择网格 (56dp 格子)
            Text(
                text = "封面图标",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.align(Alignment.Start),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(OmniSpacingTokens.small))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(56.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(LedgerCoverSpec.PresetIcons) { iKey ->
                    val isSelected = parsed is LedgerCoverSpec.Preset && selectedIcon == iKey
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(52.dp)
                            .clickable {
                                selectedIcon = iKey
                                currentKey = LedgerCoverSpec.Preset(selectedColor, selectedPattern, selectedIcon).encode()
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getCategoryIcon(iKey),
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun saveLedgerCoverFromUri(context: Context, uri: Uri): String? = runCatching {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
            decoder.isMutableRequired = true
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        @Suppress("DEPRECATION")
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } ?: return null

    // 3:4 居中裁剪
    val targetRatio = 3f / 4f
    val currentRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val (cropW, cropH) = if (currentRatio > targetRatio) {
        Pair((bitmap.height * targetRatio).toInt(), bitmap.height)
    } else {
        Pair(bitmap.width, (bitmap.width / targetRatio).toInt())
    }
    val startX = (bitmap.width - cropW) / 2
    val startY = (bitmap.height - cropH) / 2
    val cropped = Bitmap.createBitmap(bitmap, startX, startY, cropW, cropH)

    // 降采样: 最长边 <= 720px
    val maxDim = maxOf(cropW, cropH)
    val scaled = if (maxDim > 720) {
        val scale = 720f / maxDim
        Bitmap.createScaledBitmap(cropped, (cropW * scale).toInt(), (cropH * scale).toInt(), true)
    } else {
        cropped
    }

    val dir = File(context.filesDir, "ledger_covers")
    if (!dir.exists()) dir.mkdirs()
    val fileName = "cover_${System.currentTimeMillis()}.jpg"
    val file = File(dir, fileName)
    FileOutputStream(file).use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
    }
    fileName
}.getOrNull()

internal fun deleteLedgerCoverFile(context: Context, coverKey: String?) {
    if (coverKey == null || !coverKey.startsWith("custom:")) return
    val fileName = coverKey.removePrefix("custom:").trim()
    if (fileName.isEmpty()) return
    runCatching {
        val file = File(context.filesDir, "ledger_covers/$fileName")
        if (file.exists()) file.delete()
    }
}
