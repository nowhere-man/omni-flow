package com.omniflow.android.ui.designsystem.tokens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.omniflow.core.domain.model.AccountType

data class CategoryBadgeColors(
    val container: Color,
    val content: Color,
)

/**
 * 分类图标逐个粉彩配色体系。
 * 遵循 Gmail M3 Expressive 设计规范：每个分类具备专属色相、低饱和粉彩底色 + 同色相柔和深色前景。
 * 浅色与深色模式均满足 WCAG AA 文本/图形对比度 (>= 4.5:1)。
 */
object CategoryColorTokens {
    private val LightMap: Map<String, CategoryBadgeColors> = mapOf(
        "utensils" to CategoryBadgeColors(Color(0xFFFCE6D8), Color(0xFF8A3E1B)),
        "hot-beverage" to CategoryBadgeColors(Color(0xFFF5E6DA), Color(0xFF784624)),
        "bread" to CategoryBadgeColors(Color(0xFFFDF0D5), Color(0xFF7F5714)),
        "pizza" to CategoryBadgeColors(Color(0xFFFCE5DC), Color(0xFF8E3D1C)),
        "hamburger" to CategoryBadgeColors(Color(0xFFFCEBD2), Color(0xFF865513)),
        "cake" to CategoryBadgeColors(Color(0xFFFDE3EB), Color(0xFF8E2E50)),
        "groceries" to CategoryBadgeColors(Color(0xFFE0F4E6), Color(0xFF266738)),
        "bus" to CategoryBadgeColors(Color(0xFFDDEAFB), Color(0xFF245898)),
        "bicycle" to CategoryBadgeColors(Color(0xFFE5F4D8), Color(0xFF446D20)),
        "train" to CategoryBadgeColors(Color(0xFFDBE6F4), Color(0xFF284F80)),
        "taxi" to CategoryBadgeColors(Color(0xFFFDF1D1), Color(0xFF7E590E)),
        "fuel" to CategoryBadgeColors(Color(0xFFFDE7DA), Color(0xFF894419)),
        "parking" to CategoryBadgeColors(Color(0xFFDDE7F1), Color(0xFF29547E)),
        "shopping-bag" to CategoryBadgeColors(Color(0xFFFCE3F0), Color(0xFF852A6B)),
        "shirt" to CategoryBadgeColors(Color(0xFFE3E7FA), Color(0xFF394E8E)),
        "beauty" to CategoryBadgeColors(Color(0xFFFDE5ED), Color(0xFF893358)),
        "house" to CategoryBadgeColors(Color(0xFFF6EAE1), Color(0xFF754C2F)),
        "furniture" to CategoryBadgeColors(Color(0xFFE5ECE2), Color(0xFF40603A)),
        "electricity" to CategoryBadgeColors(Color(0xFFFDF4CE), Color(0xFF7E5D07)),
        "water" to CategoryBadgeColors(Color(0xFFD9F1FA), Color(0xFF186484)),
        "internet" to CategoryBadgeColors(Color(0xFFDCF4F2), Color(0xFF1B6865)),
        "phone" to CategoryBadgeColors(Color(0xFFDFEAFC), Color(0xFF2D5493)),
        "smartphone" to CategoryBadgeColors(Color(0xFFECE5FA), Color(0xFF553988)),
        "education" to CategoryBadgeColors(Color(0xFFDCE5F7), Color(0xFF274985)),
        "books" to CategoryBadgeColors(Color(0xFFF5E8D8), Color(0xFF6F4720)),
        "baby" to CategoryBadgeColors(Color(0xFFFCE7E0), Color(0xFF864432)),
        "pet" to CategoryBadgeColors(Color(0xFFF5E8DC), Color(0xFF724B25)),
        "film" to CategoryBadgeColors(Color(0xFFEAE1F7), Color(0xFF573381)),
        "game" to CategoryBadgeColors(Color(0xFFEBE3F8), Color(0xFF573989)),
        "music" to CategoryBadgeColors(Color(0xFFF5E1F2), Color(0xFF722E69)),
        "sports" to CategoryBadgeColors(Color(0xFFFDE3DE), Color(0xFF8B3A2E)),
        "fitness" to CategoryBadgeColors(Color(0xFFDCF3E8), Color(0xFF216942)),
        "camera" to CategoryBadgeColors(Color(0xFFDFE6EC), Color(0xFF34526A)),
        "party" to CategoryBadgeColors(Color(0xFFFDE4E6), Color(0xFF8D2F41)),
        "wine" to CategoryBadgeColors(Color(0xFFFBE1EA), Color(0xFF7E254A)),
        "plane" to CategoryBadgeColors(Color(0xFFDAEEFB), Color(0xFF1D608C)),
        "car" to CategoryBadgeColors(Color(0xFFDEE9F7), Color(0xFF2B5388)),
        "heart-pulse" to CategoryBadgeColors(Color(0xFFFCE3E4), Color(0xFF8D2935)),
        "medicine" to CategoryBadgeColors(Color(0xFFDAF3EE), Color(0xFF18675B)),
        "insurance" to CategoryBadgeColors(Color(0xFFDAE8F6), Color(0xFF265686)),
        "wrench" to CategoryBadgeColors(Color(0xFFEEE7DE), Color(0xFF604C39)),
        "donation" to CategoryBadgeColors(Color(0xFFFDE4EC), Color(0xFF8A3057)),
        "tax" to CategoryBadgeColors(Color(0xFFE6E8EE), Color(0xFF3E4962)),
        "refund" to CategoryBadgeColors(Color(0xFFD9F3EC), Color(0xFF1D6657)),
        "banknote" to CategoryBadgeColors(Color(0xFFDCF4E4), Color(0xFF21693A)),
        "chart-line" to CategoryBadgeColors(Color(0xFFDEF1EB), Color(0xFF226653)),
        "briefcase-business" to CategoryBadgeColors(Color(0xFFDEE7FA), Color(0xFF2B4D8C)),
        "office" to CategoryBadgeColors(Color(0xFFE2E7ED), Color(0xFF37506A)),
        "trophy" to CategoryBadgeColors(Color(0xFFFCF1D2), Color(0xFF805D12)),
        "gift" to CategoryBadgeColors(Color(0xFFFCE2EB), Color(0xFF892D58)),
        "wallet" to CategoryBadgeColors(Color(0xFFF3E8DC), Color(0xFF6D4827)),
        "wallet-cards" to CategoryBadgeColors(Color(0xFFEFE7E0), Color(0xFF61493A)),
        "landmark" to CategoryBadgeColors(Color(0xFFDCE6F5), Color(0xFF284F84)),
        "play" to CategoryBadgeColors(Color(0xFFFDE3E7), Color(0xFF8B2E46)),
        "category" to CategoryBadgeColors(Color(0xFFEAE7DF), Color(0xFF504C44)),
    )

    private val DarkMap: Map<String, CategoryBadgeColors> = mapOf(
        "utensils" to CategoryBadgeColors(Color(0xFFFDB481), Color(0xFF763404)),
        "hot-beverage" to CategoryBadgeColors(Color(0xFFEEB78B), Color(0xFF4C2810)),
        "bread" to CategoryBadgeColors(Color(0xFFFDD663), Color(0xFF6B4605)),
        "pizza" to CategoryBadgeColors(Color(0xFFFCA582), Color(0xFF752807)),
        "hamburger" to CategoryBadgeColors(Color(0xFFFAC880), Color(0xFF573404)),
        "cake" to CategoryBadgeColors(Color(0xFFFDACD2), Color(0xFF8E004B)),
        "groceries" to CategoryBadgeColors(Color(0xFF64C881), Color(0xFF09421A)),
        "bus" to CategoryBadgeColors(Color(0xFF7ECEFD), Color(0xFF014A75)),
        "bicycle" to CategoryBadgeColors(Color(0xFFB9ED8C), Color(0xFF2C4C10)),
        "train" to CategoryBadgeColors(Color(0xFF8CBDF5), Color(0xFF083D6F)),
        "taxi" to CategoryBadgeColors(Color(0xFFFDD663), Color(0xFF6B4605)),
        "fuel" to CategoryBadgeColors(Color(0xFFFDB481), Color(0xFF763404)),
        "parking" to CategoryBadgeColors(Color(0xFF98BDE1), Color(0xFF122E4A)),
        "shopping-bag" to CategoryBadgeColors(Color(0xFFFDADE2), Color(0xFF8E0054)),
        "shirt" to CategoryBadgeColors(Color(0xFF8196F9), Color(0xFF122049)),
        "beauty" to CategoryBadgeColors(Color(0xFFFDADE2), Color(0xFF8E0054)),
        "house" to CategoryBadgeColors(Color(0xFFEBB68E), Color(0xFF492912)),
        "furniture" to CategoryBadgeColors(Color(0xFFBDCDB7), Color(0xFF283725)),
        "electricity" to CategoryBadgeColors(Color(0xFFFAE280), Color(0xFF5A4102)),
        "water" to CategoryBadgeColors(Color(0xFF6EDEF4), Color(0xFF004D5A)),
        "internet" to CategoryBadgeColors(Color(0xFF6EDEF4), Color(0xFF004D5A)),
        "phone" to CategoryBadgeColors(Color(0xFF7ECEFD), Color(0xFF014A75)),
        "smartphone" to CategoryBadgeColors(Color(0xFFD8BAFA), Color(0xFF5529A4)),
        "education" to CategoryBadgeColors(Color(0xFF87ABF3), Color(0xFF10264C)),
        "books" to CategoryBadgeColors(Color(0xFFF0C28A), Color(0xFF4C2E0F)),
        "baby" to CategoryBadgeColors(Color(0xFFFA9E80), Color(0xFF491E12)),
        "pet" to CategoryBadgeColors(Color(0xFFEDBB8D), Color(0xFF4A2E12)),
        "film" to CategoryBadgeColors(Color(0xFFD8BAFA), Color(0xFF5529A4)),
        "game" to CategoryBadgeColors(Color(0xFFD8BAFA), Color(0xFF5529A4)),
        "music" to CategoryBadgeColors(Color(0xFFE892DB), Color(0xFF491242)),
        "sports" to CategoryBadgeColors(Color(0xFFFA9380), Color(0xFF4A1912)),
        "fitness" to CategoryBadgeColors(Color(0xFF6ED58C), Color(0xFF0F5324)),
        "camera" to CategoryBadgeColors(Color(0xFFB7C3CD), Color(0xFF252F37)),
        "party" to CategoryBadgeColors(Color(0xFFFA8089), Color(0xFF49121D)),
        "wine" to CategoryBadgeColors(Color(0xFFFA80AA), Color(0xFF4C1029)),
        "plane" to CategoryBadgeColors(Color(0xFF7ECEFD), Color(0xFF014A75)),
        "car" to CategoryBadgeColors(Color(0xFF85B6F5), Color(0xFF0A376F)),
        "heart-pulse" to CategoryBadgeColors(Color(0xFFFA8085), Color(0xFF4C1017)),
        "medicine" to CategoryBadgeColors(Color(0xFF91E9D7), Color(0xFF0C5046)),
        "insurance" to CategoryBadgeColors(Color(0xFF88BDF1), Color(0xFF0F2E4D)),
        "wrench" to CategoryBadgeColors(Color(0xFFCDC3B7), Color(0xFF372E25)),
        "donation" to CategoryBadgeColors(Color(0xFFFA80A7), Color(0xFF49122A)),
        "tax" to CategoryBadgeColors(Color(0xFFB7BCCD), Color(0xFF252A37)),
        "refund" to CategoryBadgeColors(Color(0xFF6EDEF4), Color(0xFF004D5A)),
        "banknote" to CategoryBadgeColors(Color(0xFF64C881), Color(0xFF09421A)),
        "chart-line" to CategoryBadgeColors(Color(0xFF98E1CA), Color(0xFF12493A)),
        "briefcase-business" to CategoryBadgeColors(Color(0xFF80A7FA), Color(0xFF11254B)),
        "office" to CategoryBadgeColors(Color(0xFFB7C1CD), Color(0xFF252E37)),
        "trophy" to CategoryBadgeColors(Color(0xFFFDD663), Color(0xFF6B4605)),
        "gift" to CategoryBadgeColors(Color(0xFFFA80AA), Color(0xFF4A122C)),
        "wallet" to CategoryBadgeColors(Color(0xFFE7BF93), Color(0xFF492C12)),
        "wallet-cards" to CategoryBadgeColors(Color(0xFFCDC1B7), Color(0xFF372C25)),
        "landmark" to CategoryBadgeColors(Color(0xFF8DB3ED), Color(0xFF10294B)),
        "play" to CategoryBadgeColors(Color(0xFFFA8092), Color(0xFF4A1221)),
        "category" to CategoryBadgeColors(Color(0xFFD3D7DD), Color(0xFF2C323B)),
    )

    private val DefaultLight = LightMap.getValue("category")
    private val DefaultDark = DarkMap.getValue("category")

    fun get(key: String?, isDark: Boolean): CategoryBadgeColors {
        val map = if (isDark) DarkMap else LightMap
        val default = if (isDark) DefaultDark else DefaultLight
        return if (key == null) default else (map[key] ?: default)
    }

    fun accountTypeColors(type: AccountType, isDark: Boolean): CategoryBadgeColors = when (type) {
        AccountType.CASH -> if (isDark) {
            CategoryBadgeColors(Color(0xFF64C881), Color(0xFF09421A))
        } else {
            CategoryBadgeColors(Color(0xFFD3EED9), Color(0xFF09421A))
        }
        AccountType.DEBIT_CARD -> if (isDark) {
            CategoryBadgeColors(Color(0xFF7ECEFD), Color(0xFF014A75))
        } else {
            CategoryBadgeColors(Color(0xFFDDEAFB), Color(0xFF014A75))
        }
        AccountType.CREDIT_CARD -> if (isDark) {
            CategoryBadgeColors(Color(0xFFFDB481), Color(0xFF763404))
        } else {
            CategoryBadgeColors(Color(0xFFFEDEC5), Color(0xFF763404))
        }
        AccountType.E_WALLET -> if (isDark) {
            CategoryBadgeColors(Color(0xFF6EDEF4), Color(0xFF004D5A))
        } else {
            CategoryBadgeColors(Color(0xFFAEF0FE), Color(0xFF004D5A))
        }
        AccountType.INVESTMENT -> if (isDark) {
            CategoryBadgeColors(Color(0xFFD8BAFA), Color(0xFF5529A4))
        } else {
            CategoryBadgeColors(Color(0xFFEDE4FF), Color(0xFF5529A4))
        }
    }
}

@Composable
@ReadOnlyComposable
fun categoryBadgeColors(key: String?): CategoryBadgeColors =
    CategoryColorTokens.get(key, MaterialTheme.colorScheme.background.luminance() < 0.5f)

@Composable
@ReadOnlyComposable
fun accountTypeBadgeColors(type: AccountType): CategoryBadgeColors =
    CategoryColorTokens.accountTypeColors(type, MaterialTheme.colorScheme.background.luminance() < 0.5f)
