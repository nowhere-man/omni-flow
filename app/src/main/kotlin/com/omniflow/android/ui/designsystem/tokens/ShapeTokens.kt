package com.omniflow.android.ui.designsystem.tokens

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Material 3 Expressive 形状令牌。
 *
 * 定稿口径 (Jack 拍板)：
 * 1. 全圆角胶囊 (corner full / CircleShape) 仅适用于按钮类控件：
 *    - 填充按钮 (Filled Button)
 *    - 描边按钮 (Outlined Button)
 *    - 文字按钮 (Text Button)
 *    - 图标按钮 (Icon Button)
 *    - 悬浮操作按钮 (FAB)
 * 2. 输入框与搜索框明确排除在胶囊之外，维持现有 M3 输入框形状 (RoundedCornerShape(16.dp))。
 * 3. 开关、滑杆、chip、切换条等非按钮控件按既有规范形状处理，不强制胶囊。
 * 4. 大容器维持现有大圆角规格，不得回退：
 *    - 分组卡片 / 统计卡片 / Hero 卡片: RoundedCornerShape(24.dp)
 *    - 对话框: RoundedCornerShape(28.dp)
 *    - 底部弹层: RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
 */
object OmniShapeTokens {
    /** 4.dp: 柱状图单柱顶部、进度条端点、紧凑 Badge */
    val extraSmall = RoundedCornerShape(4.dp)

    /** 8.dp: 键盘按键、分类格子 */
    val small = RoundedCornerShape(8.dp)

    /** 12.dp: 次级卡片、选择条容器 */
    val mediumSmall = RoundedCornerShape(12.dp)

    /** 16.dp: M3 Expressive 标准输入框、搜索框、次级卡片、菜单浮层 */
    val medium = RoundedCornerShape(16.dp)

    /** 24.dp: 大操作区、全屏卡片、分组大卡片 (M3 Expressive 卡片标准) */
    val large = RoundedCornerShape(24.dp)

    /** 28.dp: 底部弹层顶部两角、特大浮层、对话框主体 */
    val extraLarge = RoundedCornerShape(28.dp)

    /** 全圆角胶囊 (Corner Full): 按钮类控件标准形状 */
    val full = CircleShape

    // --- 语义化控件形状绑定 ---
    /** 按钮类形状：全圆角胶囊 */
    val button = full
    val filledButton = full
    val outlinedButton = full
    val textButton = full
    val iconButton = full
    val fab = full

    /** 输入框与搜索框：明确排除在胶囊之外，维持 M3 16dp 形状 */
    val textField = medium
    val searchBar = medium

    /** 容器类形状：维持大圆角规格，不回退、不胶囊化 */
    val card = large
    val bottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val dialog = extraLarge
}
