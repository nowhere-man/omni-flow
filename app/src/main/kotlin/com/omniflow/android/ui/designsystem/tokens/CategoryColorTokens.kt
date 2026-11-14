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
        "utensils" to CategoryBadgeColors(Color(0xFF3D2214), Color(0xFFF8C8AE)),
        "hot-beverage" to CategoryBadgeColors(Color(0xFF382516), Color(0xFFECC5A9)),
        "bread" to CategoryBadgeColors(Color(0xFF3E2E10), Color(0xFFF9DE9E)),
        "pizza" to CategoryBadgeColors(Color(0xFF3E2015), Color(0xFFF8C5B3)),
        "hamburger" to CategoryBadgeColors(Color(0xFF3E2B0F), Color(0xFFF9D49E)),
        "cake" to CategoryBadgeColors(Color(0xFF411927), Color(0xFFF9B7CB)),
        "groceries" to CategoryBadgeColors(Color(0xFF14361E), Color(0xFFB3E8C1)),
        "bus" to CategoryBadgeColors(Color(0xFF152C4D), Color(0xFFB3D3FA)),
        "bicycle" to CategoryBadgeColors(Color(0xFF243714), Color(0xFFC3E7A2)),
        "train" to CategoryBadgeColors(Color(0xFF172B44), Color(0xFFB4CCE9)),
        "taxi" to CategoryBadgeColors(Color(0xFF3E2E0A), Color(0xFFF9DF98)),
        "fuel" to CategoryBadgeColors(Color(0xFF3E2313), Color(0xFFF9C8AB)),
        "parking" to CategoryBadgeColors(Color(0xFF182C40), Color(0xFFB5CEE5)),
        "shopping-bag" to CategoryBadgeColors(Color(0xFF411736), Color(0xFFF8B5DF)),
        "shirt" to CategoryBadgeColors(Color(0xFF1D2549), Color(0xFFBDC8F6)),
        "beauty" to CategoryBadgeColors(Color(0xFF401C2B), Color(0xFFF8BDD3)),
        "house" to CategoryBadgeColors(Color(0xFF39281B), Color(0xFFEACBB5)),
        "furniture" to CategoryBadgeColors(Color(0xFF233220), Color(0xFFC3D8BD)),
        "electricity" to CategoryBadgeColors(Color(0xFF3F3006), Color(0xFFF9E58E)),
        "water" to CategoryBadgeColors(Color(0xFF103342), Color(0xFFA9E2F6)),
        "internet" to CategoryBadgeColors(Color(0xFF103433), Color(0xFFB1EAE6)),
        "phone" to CategoryBadgeColors(Color(0xFF162847), Color(0xFFB6D1FA)),
        "smartphone" to CategoryBadgeColors(Color(0xFF271D3F), Color(0xFFD1BCF6)),
        "education" to CategoryBadgeColors(Color(0xFF162541), Color(0xFFB3C8F2)),
        "books" to CategoryBadgeColors(Color(0xFF372513), Color(0xFFE9C69E)),
        "baby" to CategoryBadgeColors(Color(0xFF3E211A), Color(0xFFF9C9BB)),
        "pet" to CategoryBadgeColors(Color(0xFF382514), Color(0xFFE8C7A5)),
        "film" to CategoryBadgeColors(Color(0xFF281A3C), Color(0xFFCEB5F2)),
        "game" to CategoryBadgeColors(Color(0xFF271D3E), Color(0xFFCFBBF3)),
        "music" to CategoryBadgeColors(Color(0xFF371833), Color(0xFFE9B2E3)),
        "sports" to CategoryBadgeColors(Color(0xFF411D18), Color(0xFFF9B7AC)),
        "fitness" to CategoryBadgeColors(Color(0xFF123623), Color(0xFFAEE7C7)),
        "camera" to CategoryBadgeColors(Color(0xFF192834), Color(0xFFB8CDDC)),
        "party" to CategoryBadgeColors(Color(0xFF421720), Color(0xFFF9B9C0)),
        "wine" to CategoryBadgeColors(Color(0xFF3C1424), Color(0xFFF4B0C7)),
        "plane" to CategoryBadgeColors(Color(0xFF113248), Color(0xFFADE0F9)),
        "car" to CategoryBadgeColors(Color(0xFF162943), Color(0xFFB7D1F3)),
        "heart-pulse" to CategoryBadgeColors(Color(0xFF42151B), Color(0xFFF9B5B9)),
        "medicine" to CategoryBadgeColors(Color(0xFF10332D), Color(0xFFAEE9DE)),
        "insurance" to CategoryBadgeColors(Color(0xFF152C43), Color(0xFFB2CEEE)),
        "wrench" to CategoryBadgeColors(Color(0xFF2E251D), Color(0xFFD7C7B3)),
        "donation" to CategoryBadgeColors(Color(0xFF41192A), Color(0xFFF9B8CE)),
        "tax" to CategoryBadgeColors(Color(0xFF1E2330), Color(0xFFC2C8D8)),
        "refund" to CategoryBadgeColors(Color(0xFF11342D), Color(0xFFADE8DC)),
        "banknote" to CategoryBadgeColors(Color(0xFF123720), Color(0xFFAEE8C2)),
        "chart-line" to CategoryBadgeColors(Color(0xFF13352C), Color(0xFFB1E5D7)),
        "briefcase-business" to CategoryBadgeColors(Color(0xFF172746), Color(0xFFB7CEF8)),
        "office" to CategoryBadgeColors(Color(0xFF1A2734), Color(0xFFBED0E1)),
        "trophy" to CategoryBadgeColors(Color(0xFF3E2F0A), Color(0xFFF9E198)),
        "gift" to CategoryBadgeColors(Color(0xFF40182C), Color(0xFFF8B5CE)),
        "wallet" to CategoryBadgeColors(Color(0xFF362415), Color(0xFFE5C5A4)),
        "wallet-cards" to CategoryBadgeColors(Color(0xFF30241D), Color(0xFFDAC3B3)),
        "landmark" to CategoryBadgeColors(Color(0xFF162842), Color(0xFFB4CBEA)),
        "play" to CategoryBadgeColors(Color(0xFF411822), Color(0xFFF9B7C3)),
        "category" to CategoryBadgeColors(Color(0xFF262420), Color(0xFFCECAC0)),
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
            CategoryBadgeColors(Color(0xFF1A3D25), Color(0xFFB4E8C2))
        } else {
            CategoryBadgeColors(Color(0xFFD3EED9), Color(0xFF206337))
        }
        AccountType.DEBIT_CARD -> if (isDark) {
            CategoryBadgeColors(Color(0xFF152C4D), Color(0xFFB3D3FA))
        } else {
            CategoryBadgeColors(Color(0xFFDDEAFB), Color(0xFF245898))
        }
        AccountType.CREDIT_CARD -> if (isDark) {
            CategoryBadgeColors(Color(0xFF4A3215), Color(0xFFFED8A7))
        } else {
            CategoryBadgeColors(Color(0xFFFFE5C4), Color(0xFF7D4E15))
        }
        AccountType.E_WALLET -> if (isDark) {
            CategoryBadgeColors(Color(0xFF153B4A), Color(0xFFB6E6F7))
        } else {
            CategoryBadgeColors(Color(0xFFD7F0FA), Color(0xFF165F7B))
        }
        AccountType.INVESTMENT -> if (isDark) {
            CategoryBadgeColors(Color(0xFF382A52), Color(0xFFD6C2FF))
        } else {
            CategoryBadgeColors(Color(0xFFEDE4FF), Color(0xFF5A3E85))
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
