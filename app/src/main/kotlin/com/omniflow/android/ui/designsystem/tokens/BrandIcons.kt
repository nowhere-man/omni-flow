package com.omniflow.android.ui.designsystem.tokens

import androidx.annotation.DrawableRes
import com.omniflow.android.R

object BrandIcons {
    @DrawableRes
    fun findBrandDrawable(iconKey: String?, name: String? = null): Int? {
        val key = iconKey?.lowercase() ?: ""
        val n = name?.lowercase() ?: ""
        return when {
            key == "brand_abc" || n.contains("农业") || n.contains("农行") -> R.drawable.brand_abc
            key == "brand_boc" || n.contains("中国银行") || n.contains("中行") -> R.drawable.brand_boc
            key == "brand_bocom" || n.contains("交通") || n.contains("交行") -> R.drawable.brand_bocom
            key == "brand_ccb" || n.contains("建设") || n.contains("建行") -> R.drawable.brand_ccb
            key == "brand_cgb" || n.contains("广发") -> R.drawable.brand_cgb
            key == "brand_citic" || n.contains("中信") -> R.drawable.brand_citic
            key == "brand_cmb" || n.contains("招商") || n.contains("招行") -> R.drawable.brand_cmb
            key == "brand_czb" || n.contains("浙商") -> R.drawable.brand_czb
            key == "brand_hxb" || n.contains("华夏") -> R.drawable.brand_hxb
            key == "brand_icbc" || n.contains("工商") || n.contains("工行") -> R.drawable.brand_icbc
            key == "brand_psbc" || n.contains("邮政") || n.contains("邮储") -> R.drawable.brand_psbc
            key == "brand_spdb" || n.contains("浦发") || n.contains("浦东") -> R.drawable.brand_spdb
            key == "brand_xab" || n.contains("西安") -> R.drawable.brand_xab
            key == "brand_ths" || n.contains("同花顺") || n.contains("证券") || n.contains("股票") -> R.drawable.brand_ths
            key == "brand_alipay" || n.contains("支付宝") -> R.drawable.brand_alipay
            key == "brand_wechat" || n.contains("微信") -> R.drawable.brand_wechat
            else -> null
        }
    }
}
