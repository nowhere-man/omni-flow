package com.omniflow.android.ui.designsystem.tokens

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object OmniShapeTokens {
    /** 4.dp: 柱状图单柱顶部、进度条端点、紧凑 Badge */
    val extraSmall = RoundedCornerShape(4.dp)

    /** 8.dp: 键盘按键、分类格子、内嵌输入框 */
    val small = RoundedCornerShape(8.dp)

    /** 12.dp: 次级卡片、选择条容器 */
    val mediumSmall = RoundedCornerShape(12.dp)

    /** 16.dp: M3 Expressive 标准卡片、菜单浮层、弹窗主体 */
    val medium = RoundedCornerShape(16.dp)

    /** 24.dp: 大操作区、全屏卡片 */
    val large = RoundedCornerShape(24.dp)

    /** 28.dp: 底部弹层顶部两角、特大浮层 */
    val extraLarge = RoundedCornerShape(28.dp)

    /** 药丸胶囊端点、圆形 FAB、头像 */
    val full = CircleShape
}
