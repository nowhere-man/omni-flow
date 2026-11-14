package com.omniflow.android.ui.designsystem.tokens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryIconOption(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

val CategoryIconOptions: List<CategoryIconOption> = listOf(
    CategoryIconOption("utensils", "餐饮", Icons.Filled.Restaurant),
    CategoryIconOption("hot-beverage", "饮品", Icons.Filled.Coffee),
    CategoryIconOption("bread", "面包", Icons.Filled.BakeryDining),
    CategoryIconOption("pizza", "披萨", Icons.Filled.LocalPizza),
    CategoryIconOption("hamburger", "快餐", Icons.Filled.Fastfood),
    CategoryIconOption("cake", "甜点", Icons.Filled.Cake),
    CategoryIconOption("groceries", "买菜", Icons.Filled.LocalGroceryStore),
    CategoryIconOption("bus", "公交", Icons.Filled.DirectionsBus),
    CategoryIconOption("bicycle", "骑行", Icons.Filled.DirectionsBike),
    CategoryIconOption("train", "火车", Icons.Filled.Train),
    CategoryIconOption("taxi", "打车", Icons.Filled.LocalTaxi),
    CategoryIconOption("fuel", "加油", Icons.Filled.LocalGasStation),
    CategoryIconOption("parking", "停车", Icons.Filled.LocalParking),
    CategoryIconOption("shopping-bag", "购物", Icons.Filled.ShoppingBag),
    CategoryIconOption("shirt", "服饰", Icons.Filled.Checkroom),
    CategoryIconOption("beauty", "美妆", Icons.Filled.Spa),
    CategoryIconOption("house", "住房", Icons.Filled.Home),
    CategoryIconOption("furniture", "家居", Icons.Filled.Weekend),
    CategoryIconOption("electricity", "电费", Icons.Filled.Bolt),
    CategoryIconOption("water", "水费", Icons.Filled.WaterDrop),
    CategoryIconOption("internet", "网络", Icons.Filled.Wifi),
    CategoryIconOption("phone", "话费", Icons.Filled.Phone),
    CategoryIconOption("smartphone", "数码", Icons.Filled.Smartphone),
    CategoryIconOption("education", "教育", Icons.Filled.School),
    CategoryIconOption("books", "书籍", Icons.Filled.MenuBook),
    CategoryIconOption("baby", "育儿", Icons.Filled.ChildCare),
    CategoryIconOption("pet", "宠物", Icons.Filled.Pets),
    CategoryIconOption("film", "电影", Icons.Filled.Movie),
    CategoryIconOption("game", "游戏", Icons.Filled.SportsEsports),
    CategoryIconOption("music", "音乐", Icons.Filled.MusicNote),
    CategoryIconOption("sports", "运动", Icons.Filled.SportsBasketball),
    CategoryIconOption("fitness", "健身", Icons.Filled.FitnessCenter),
    CategoryIconOption("camera", "摄影", Icons.Filled.PhotoCamera),
    CategoryIconOption("party", "聚会", Icons.Filled.Celebration),
    CategoryIconOption("wine", "酒水", Icons.Filled.WineBar),
    CategoryIconOption("plane", "旅行", Icons.Filled.Flight),
    CategoryIconOption("car", "汽车", Icons.Filled.DirectionsCar),
    CategoryIconOption("heart-pulse", "医疗", Icons.Filled.MedicalServices),
    CategoryIconOption("medicine", "药品", Icons.Filled.Medication),
    CategoryIconOption("insurance", "保险", Icons.Filled.HealthAndSafety),
    CategoryIconOption("wrench", "维修", Icons.Filled.Build),
    CategoryIconOption("donation", "公益", Icons.Filled.VolunteerActivism),
    CategoryIconOption("tax", "税费", Icons.Filled.Receipt),
    CategoryIconOption("refund", "退款", Icons.Filled.AssignmentReturn),
    CategoryIconOption("banknote", "工资", Icons.Filled.Payments),
    CategoryIconOption("chart-line", "理财", Icons.Filled.TrendingUp),
    CategoryIconOption("briefcase-business", "工作", Icons.Filled.Work),
    CategoryIconOption("office", "办公", Icons.Filled.BusinessCenter),
    CategoryIconOption("trophy", "奖金", Icons.Filled.EmojiEvents),
    CategoryIconOption("gift", "礼物", Icons.Filled.CardGiftcard),
    CategoryIconOption("wallet", "钱包", Icons.Filled.AccountBalanceWallet),
    CategoryIconOption("wallet-cards", "卡包", Icons.Filled.AccountBalanceWallet),
    CategoryIconOption("landmark", "银行", Icons.Filled.AccountBalance),
    CategoryIconOption("play", "短视频", Icons.Filled.PlayArrow),
    CategoryIconOption("category", "默认", Icons.Filled.Category),
)

private val CategoryIconMap: Map<String, ImageVector> =
    CategoryIconOptions.associate { it.key to it.icon }

/**
 * 根据图标 key 获取对应的 Material Icons（Filled）图标。
 * 若 key 为空或不存在，返回默认的分类图标 [Icons.Filled.Category]。
 */
fun getCategoryIcon(key: String?): ImageVector =
    if (key == null) Icons.Filled.Category else (CategoryIconMap[key] ?: Icons.Filled.Category)
