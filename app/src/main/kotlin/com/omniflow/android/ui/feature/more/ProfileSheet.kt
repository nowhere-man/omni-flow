package com.omniflow.android.ui.feature.more

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.omniflow.android.ui.OmniTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniflow.android.ui.designsystem.components.OmniSheetScaffold
import com.omniflow.android.ui.designsystem.tokens.OmniShapeTokens
import com.omniflow.android.ui.designsystem.tokens.OmniSpacingTokens
import com.omniflow.android.ui.designsystem.tokens.SemanticBadgeType
import com.omniflow.android.ui.designsystem.tokens.semanticBadgeColors
import java.io.File
import java.io.FileOutputStream

@Composable
fun OmniAvatar(
    userName: String,
    avatarBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
) {
    if (avatarBitmap != null) {
        Image(
            bitmap = avatarBitmap.asImageBitmap(),
            contentDescription = "用户头像",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        val colors = semanticBadgeColors(SemanticBadgeType.SYSTEM)
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(colors.container),
            contentAlignment = Alignment.Center,
        ) {
            val char = userName.trim().firstOrNull()?.toString() ?: "我"
            Text(
                text = char,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.44f).sp,
                ),
                color = colors.content,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditSheet(
    currentName: String,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onAvatarUpdated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var avatarVersion by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val avatarBitmap = remember(avatarVersion) { loadAvatarBitmap(context) }
    var nameInput by remember(currentName) { mutableStateOf(currentName) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            val success = saveAvatarFromUri(context, uri)
            if (success) {
                avatarVersion = System.currentTimeMillis()
                onAvatarUpdated()
            }
        }
    }

    OmniSheetScaffold(
        title = "编辑个人资料",
        onDismiss = onDismiss,
        onSave = {
            onSaveName(nameInput.trim().ifEmpty { "我" })
            onDismiss()
        },
        saveText = "保存",
        cancelText = "取消",
        onCancel = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(OmniSpacingTokens.medium))

            // 头像大预览
            OmniAvatar(
                userName = nameInput,
                avatarBitmap = avatarBitmap,
                size = 80.dp,
            )

            Spacer(Modifier.height(OmniSpacingTokens.medium))

            Row(
                horizontalArrangement = Arrangement.spacedBy(OmniSpacingTokens.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    shape = OmniShapeTokens.button,
                ) {
                    Text("更换头像")
                }

                if (avatarBitmap != null) {
                    TextButton(
                        onClick = {
                            deleteAvatarFile(context)
                            avatarVersion = System.currentTimeMillis()
                            onAvatarUpdated()
                        },
                        shape = OmniShapeTokens.button,
                    ) {
                        Text("恢复默认头像")
                    }
                }
            }

            Spacer(Modifier.height(OmniSpacingTokens.large))

            // 用户名输入框 (16dp 圆角，不胶囊化)
            OmniTextField(
                value = nameInput,
                onValueChange = { nameInput = it.take(20) },
                label = "用户名",
            )

            Spacer(Modifier.height(OmniSpacingTokens.mediumSmall))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                TextButton(
                    onClick = {
                        deleteAvatarFile(context)
                        avatarVersion = System.currentTimeMillis()
                        nameInput = "我"
                        onSaveName("我")
                        onAvatarUpdated()
                        onDismiss()
                    },
                    shape = OmniShapeTokens.button,
                ) {
                    Text("恢复全部默认")
                }
            }
        }
    }
}

internal fun loadAvatarBitmap(context: Context): Bitmap? = runCatching {
    val file = File(context.filesDir, "profile/avatar.jpg")
    if (file.exists()) {
        BitmapFactory.decodeFile(file.absolutePath)
    } else null
}.getOrNull()

internal fun deleteAvatarFile(context: Context) {
    runCatching {
        val file = File(context.filesDir, "profile/avatar.jpg")
        if (file.exists()) file.delete()
    }
}

internal fun saveAvatarFromUri(context: Context, uri: Uri): Boolean = runCatching {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
            decoder.isMutableRequired = true
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        @Suppress("DEPRECATION")
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    } ?: return false

    val minDim = minOf(bitmap.width, bitmap.height)
    val startX = (bitmap.width - minDim) / 2
    val startY = (bitmap.height - minDim) / 2
    val cropped = Bitmap.createBitmap(bitmap, startX, startY, minDim, minDim)

    val targetSize = minOf(minDim, 720)
    val scaled = if (minDim > targetSize) {
        Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
    } else {
        cropped
    }

    val dir = File(context.filesDir, "profile")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "avatar.jpg")
    FileOutputStream(file).use { out ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    true
}.getOrDefault(false)
