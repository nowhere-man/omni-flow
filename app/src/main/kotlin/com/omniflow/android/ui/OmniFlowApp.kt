package com.omniflow.android.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import com.omniflow.core.domain.model.CalendarDaySummary
import com.omniflow.core.domain.model.AppearanceMode
import com.omniflow.core.domain.model.calendarAmountText
import com.omniflow.core.domain.model.hourMinuteText
import com.omniflow.core.domain.model.DayTransactionGroup
import com.omniflow.core.domain.model.LedgerScope
import com.omniflow.core.domain.model.Money
import com.omniflow.core.domain.model.ThemeColor
import com.omniflow.core.domain.model.TransactionDetailState
import com.omniflow.core.domain.model.TransactionDetailDisplayMode
import com.omniflow.core.domain.model.TransactionListItem
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.model.yearMonthText
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import java.time.LocalDate as JavaLocalDate
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.omniflow.android.ReminderScheduler

private enum class MainDestination(val route: OmniRoute, val label: String, val icon: ImageVector) {
    HOME(OmniRoute.Home, "首页", Icons.Default.Home),
    ANALYTICS(OmniRoute.Analytics, "统计", Icons.AutoMirrored.Filled.ShowChart),
    SEARCH(OmniRoute.Search, "搜索", Icons.Default.Search),
    MORE(OmniRoute.More, "更多", Icons.Default.MoreHoriz),
}

private fun androidx.navigation3.runtime.NavKey.toMainDestination(): MainDestination = when (this) {
    OmniRoute.Home -> MainDestination.HOME
    OmniRoute.Analytics -> MainDestination.ANALYTICS
    OmniRoute.Search -> MainDestination.SEARCH
    OmniRoute.More -> MainDestination.MORE
    else -> error("Unknown route: $this")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniFlowApp(viewModel: OmniFlowViewModel, initialTransactionId: Pair<String, Long>? = null) {
    val homeState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val analyticsState by viewModel.analyticsUiState.collectAsStateWithLifecycle()
    val rangeDetailState by viewModel.rangeDetailUiState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val transactionState by viewModel.transactionUiState.collectAsStateWithLifecycle()
    val transactionRecordDetailState by viewModel.transactionRecordDetailUiState.collectAsStateWithLifecycle()
    val moreState by viewModel.moreUiState.collectAsStateWithLifecycle()
    val entityDetailState by viewModel.entityDetailUiState.collectAsStateWithLifecycle()
    val navigationState = rememberOmniNavigationState()
    val pagerState = rememberPagerState(
        initialPage = navigationState.topLevelRoute.toMainDestination().ordinal,
        pageCount = { MainDestination.entries.size },
    )
    val destination = MainDestination.entries[pagerState.targetPage]
    val isEditor = navigationState.currentRoute == OmniRoute.TransactionEditor
    val editorSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDiscardDialog by remember { mutableStateOf(false) }
    var restoreEditorSheet by remember { mutableStateOf(false) }
    var notificationPermissionRequested by rememberSaveable { mutableStateOf(false) }
    var lastBackPressedAt by remember { mutableLongStateOf(0L) }
    val darkTheme = when (homeState.appearanceMode) {
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }
    val context = LocalContext.current
    val view = LocalView.current
    val dynamicColorEnabled = remember {
        mutableStateOf(context.getSharedPreferences("android_ui", 0).getBoolean("dynamic_color", false))
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(moreState.reminders) {
        ReminderScheduler(context).sync(moreState.reminders)
    }
    LaunchedEffect(initialTransactionId) {
        initialTransactionId?.let { (transactionId, _) ->
            val route = OmniRoute.TransactionDetail(transactionId)
            if (navigationState.currentRoute != route) navigationState.navigate(route)
        }
    }
    LaunchedEffect(transactionState.completed) {
        if (transactionState.completed) {
            navigationState.goBack()
            viewModel.consumeTransactionCompletion()
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            navigationState.topLevelRoute = MainDestination.entries[page].route
        }
    }
    LaunchedEffect(navigationState.topLevelRoute) {
        val target = navigationState.topLevelRoute.toMainDestination().ordinal
        if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
    }
    // 有草稿时下滑关闭只弹确认框、不出栈，此时弹层已经滑到隐藏位，选「继续编辑」要把它推回来。
    LaunchedEffect(restoreEditorSheet) {
        if (restoreEditorSheet) {
            editorSheetState.show()
            restoreEditorSheet = false
        }
    }
    fun handleBack() {
        if (isEditor) {
            val hasDraft = transactionState.editingId != null ||
                transactionState.amountInput.isNotBlank() ||
                transactionState.note.isNotBlank() ||
                transactionState.categoryId != null ||
                transactionState.selectedTagIds.isNotEmpty() ||
                transactionState.isExcluded
            if (hasDraft) showDiscardDialog = true else navigationState.goBack()
        } else if (!navigationState.goBack()) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastBackPressedAt <= 2_000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressedAt = now
                Toast.makeText(context, "再返回一次退出应用", Toast.LENGTH_SHORT).show()
            }
        }
    }

    MaterialTheme(
        colorScheme = omniColorScheme(
            moreState.preferences.themeColor,
            darkTheme,
            dynamicColorEnabled.value,
            context,
        ),
        shapes = Shapes(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
    ) {
        SideEffect {
            (context as? Activity)?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
        AppLockGate(moreState.preferences.appLockEnabled) {
        BoxWithConstraints {
            val useNavigationRail = maxWidth >= 600.dp
            // 底部弹层的高度：屏幕高度减一个状态栏。记账页和明细弹层用同一个值，
            // 免得一个几乎贴顶、一个只占半屏。
            val sheetHeight = maxHeight - WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Scaffold(
            bottomBar = {
                if (!useNavigationRail) {
                    PrimaryNavigation(
                        destination = destination,
                        onDestination = {
                            navigationState.navigate(it.route)
                        },
                    )
                }
            },
            floatingActionButton = {
                // 记账按钮只在首页出现：统计、搜索、更多这三页没有「在这里新增一笔」的语境
                if (!useNavigationRail && !isEditor && destination == MainDestination.HOME) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.startNewTransaction()
                            navigationState.navigate(OmniRoute.TransactionEditor)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新增交易")
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.End,
        ) { padding ->
            // nav3 的 NavDisplay 只在自己的 backStack 深度 > 1 时才注册返回处理器，
            // 停在某个 tab 根部时返回键会直接落到 Activity 默认行为把应用关掉。
            // 这里补一个互斥的兜底：根部时交给 handleBack，非首页 tab 滑回首页，首页再按一次才退出。
            BackHandler(enabled = navigationState.currentBackStack.size == 1) { handleBack() }
            val entryProvider: (NavKey) -> NavEntry<NavKey> = { key -> NavEntry(key, metadata = sheetMetadata(key)) {
            when (key) {
                OmniRoute.Home -> HomeScreen(
                    state = homeState,
                    onPreviousMonth = viewModel::previousMonth,
                    onNextMonth = viewModel::nextMonth,
                    onLedgerSelected = viewModel::selectLedger,
                    onDateSelected = { date ->
                        viewModel.showDate(date)
                        navigationState.navigate(OmniRoute.DateDetail(date.toString()))
                    },
                    onMonthSelected = viewModel::selectHomeMonth,
                    onSummary = { type ->
                        viewModel.showHomeSummary(type)
                        navigationState.navigate(OmniRoute.SummaryDetail(type?.name))
                    },
                    onToggleDisplayMode = viewModel::toggleDisplayMode,
                    onEdit = { transactionId ->
                        navigationState.navigate(OmniRoute.TransactionDetail(transactionId))
                    },
                    onManageLedgers = {
                        navigationState.navigate(OmniRoute.More)
                        navigationState.navigate(OmniRoute.MoreLedgers)
                    },
                    modifier = Modifier.padding(padding),
                )
                OmniRoute.Analytics -> AnalyticsScreen(
                    state = analyticsState,
                    onScope = viewModel::setAnalyticsScope,
                    onRangeMode = viewModel::setAnalyticsRangeMode,
                    onShiftRange = viewModel::shiftAnalyticsRange,
                    onCurrentRange = viewModel::resetAnalyticsRange,
                    onCustomRange = viewModel::setAnalyticsCustomRange,
                    onAnalyticsType = viewModel::setAnalyticsType,
                    onCategorySelected = { navigationState.navigate(OmniRoute.CategoryDetail(it)) },
                    onTransactionSelected = { id -> navigationState.navigate(OmniRoute.TransactionDetail(id)) },
                    onMonthSelected = viewModel::selectAnalyticsMonth,
                    onSummary = { type ->
                        viewModel.showHomeSummary(type)
                        navigationState.navigate(OmniRoute.SummaryDetail(type?.name))
                    },
                    onStatementTable = viewModel::loadStatementTable,
                    onDismissStatementTable = viewModel::dismissStatementTable,
                    modifier = Modifier.padding(padding),
                )
                OmniRoute.TransactionEditor -> ModalBottomSheet(
                    onDismissRequest = ::handleBack,
                    sheetState = editorSheetState,
                    contentWindowInsets = { WindowInsets(0) },
                    dragHandle = null,
                ) {
                    TransactionEditorScreen(
                        state = transactionState,
                        onType = viewModel::setTransactionType,
                        onLedger = viewModel::setTransactionLedger,
                        onAccount = viewModel::setTransactionAccount,
                        onCategory = viewModel::setTransactionCategory,
                        onReorderPrimary = viewModel::reorderTransactionPrimaryCategories,
                        onCreateSecondary = viewModel::createTransactionSecondaryCategory,
                        onTag = viewModel::toggleTransactionTag,
                        onNote = viewModel::setTransactionNote,
                        onDate = viewModel::setTransactionDate,
                        onExcluded = viewModel::setTransactionExcluded,
                        onAmountKey = viewModel::pressAmountKey,
                        onSaveAgain = { viewModel.saveTransaction(true) },
                        onDone = { viewModel.saveTransaction(false) },
                        sheetHeight = sheetHeight,
                    )
                }
                OmniRoute.Search -> SearchScreen(
                    state = searchState,
                    onKeyword = viewModel::setSearchKeyword,
                    onScope = viewModel::setSearchScope,
                    onType = viewModel::setSearchType,
                    onPrimaryCategoryText = viewModel::setSearchPrimaryCategoryText,
                    onSecondaryCategoryText = viewModel::setSearchSecondaryCategoryText,
                    onTagText = viewModel::setSearchTagText,
                    onNoteText = viewModel::setSearchNoteText,
                    onAccount = viewModel::setSearchAccount,
                    onAmount = viewModel::setSearchAmount,
                    onDateRange = viewModel::setSearchDateRange,
                    onSearch = viewModel::runSearch,
                    onClear = viewModel::clearSearch,
                    onEditTransaction = { transactionId ->
                        navigationState.navigate(OmniRoute.TransactionDetail(transactionId))
                    },
                    modifier = Modifier.padding(padding),
                )
                OmniRoute.More,
                is OmniRoute.MoreSettings,
                is OmniRoute.MoreData,
                is OmniRoute.MoreImport,
                is OmniRoute.MoreExport,
                is OmniRoute.MoreRules,
                is OmniRoute.MoreReminders,
                is OmniRoute.MoreBudgets,
                is OmniRoute.MoreLedgers,
                is OmniRoute.MoreAccounts,
                is OmniRoute.MoreAssets,
                is OmniRoute.MoreCategories,
                is OmniRoute.MoreTags -> MoreScreen(
                    state = moreState,
                    viewModel = viewModel,
                    page = (key as OmniRoute).morePage() ?: MorePage.HOME,
                    onPage = { page -> navigationState.navigate(page.toRoute()) },
                    onBack = { navigationState.goBack() },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= 33 && !notificationPermissionRequested) {
                            notificationPermissionRequested = true
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onOpenLedger = { navigationState.navigate(OmniRoute.LedgerDetail(it)) },
                    onOpenAccount = { navigationState.navigate(OmniRoute.AccountDetail(it)) },
                    onOpenCategory = { navigationState.navigate(OmniRoute.CategoryDetail(it)) },
                    dynamicColorEnabled = dynamicColorEnabled.value,
                    onDynamicColorChanged = { enabled ->
                        dynamicColorEnabled.value = enabled
                        context.getSharedPreferences("android_ui", 0).edit()
                            .putBoolean("dynamic_color", enabled).apply()
                    },
                    modifier = Modifier.padding(padding),
                )
                is OmniRoute.LedgerDetail -> EntityDetailScreen(
                    state = entityDetailState,
                    moreState = moreState,
                    onLoad = { viewModel.showEntityDetail(EntityDetailKind.LEDGER, key.id) },
                    onOpenTransaction = { navigationState.navigate(OmniRoute.TransactionDetail(it)) },
                    onSeeAll = {
                        viewModel.openSearchFor(EntityDetailKind.LEDGER, key.id)
                        navigationState.navigate(OmniRoute.Search)
                    },
                    modifier = Modifier.padding(padding),
                )
                is OmniRoute.AccountDetail -> EntityDetailScreen(
                    state = entityDetailState,
                    moreState = moreState,
                    onLoad = { viewModel.showEntityDetail(EntityDetailKind.ACCOUNT, key.id) },
                    onOpenTransaction = { navigationState.navigate(OmniRoute.TransactionDetail(it)) },
                    onSeeAll = {
                        viewModel.openSearchFor(EntityDetailKind.ACCOUNT, key.id)
                        navigationState.navigate(OmniRoute.Search)
                    },
                    modifier = Modifier.padding(padding),
                )
                is OmniRoute.CategoryDetail -> EntityDetailScreen(
                    state = entityDetailState,
                    moreState = moreState,
                    onLoad = { viewModel.showEntityDetail(EntityDetailKind.CATEGORY, key.id) },
                    onOpenTransaction = { navigationState.navigate(OmniRoute.TransactionDetail(it)) },
                    onSeeAll = {
                        viewModel.openSearchFor(EntityDetailKind.CATEGORY, key.id)
                        navigationState.navigate(OmniRoute.Search)
                    },
                    modifier = Modifier.padding(padding),
                )
                is OmniRoute.TransactionDetail -> {
                    LaunchedEffect(key.id) { viewModel.showTransactionRecordDetail(key.id) }
                    TransactionRecordDetailSheet(
                        state = transactionRecordDetailState,
                        onDismiss = {
                            viewModel.dismissTransactionRecordDetail()
                            navigationState.goBack()
                        },
                        onEdit = { id ->
                            navigationState.goBack()
                            viewModel.editTransaction(id)
                            navigationState.navigate(OmniRoute.TransactionEditor)
                        },
                        onDelete = {
                            viewModel.deleteTransactionRecordDetail { navigationState.goBack() }
                        },
                    )
                }
                is OmniRoute.DateDetail -> {
                    LaunchedEffect(key.date) { viewModel.showDate(LocalDate.parse(key.date)) }
                    DateDetailSheet(
                        state = rangeDetailState.detail,
                        isLoading = rangeDetailState.isLoading,
                        error = rangeDetailState.error,
                        sheetHeight = sheetHeight,
                        onDismiss = {
                            viewModel.dismissDate()
                            navigationState.goBack()
                        },
                        onEdit = { id ->
                            navigationState.goBack()
                            navigationState.navigate(OmniRoute.TransactionDetail(id))
                        },
                    )
                }
                is OmniRoute.SummaryDetail -> {
                    LaunchedEffect(key.type) {
                        viewModel.showHomeSummary(key.type?.let(TransactionType::valueOf))
                    }
                    DateDetailSheet(
                        state = rangeDetailState.detail,
                        isLoading = rangeDetailState.isLoading,
                        error = rangeDetailState.error,
                        sheetHeight = sheetHeight,
                        onDismiss = {
                            viewModel.dismissDate()
                            navigationState.goBack()
                        },
                        onEdit = { id ->
                            navigationState.goBack()
                            navigationState.navigate(OmniRoute.TransactionDetail(id))
                        },
                    )
                }
            }
            } }
            Row(Modifier.fillMaxSize()) {
                if (useNavigationRail) {
                    PrimaryNavigationRail(
                        destination = destination,
                        onDestination = {
                            navigationState.navigate(it.route)
                        },
                        onAdd = {
                            viewModel.startNewTransaction()
                            navigationState.navigate(OmniRoute.TransactionEditor)
                        },
                    )
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = navigationState.currentBackStack.size == 1,
                    key = { MainDestination.entries[it].name },
                ) { page ->
                    NavDisplay(
                        backStack = navigationState.backStacks.getValue(MainDestination.entries[page].route),
                        entryDecorators = listOf(
                            rememberSaveableStateHolderNavEntryDecorator(rememberSaveableStateHolder()),
                        ),
                        onBack = ::handleBack,
                        transitionSpec = { pushTransition() },
                        popTransitionSpec = { popTransition() },
                        entryProvider = entryProvider,
                    )
                }
            }
        }
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false; restoreEditorSheet = true },
                title = { Text("放弃未保存的交易？") },
                text = { Text("当前输入还没有保存，返回后将丢失这些内容。") },
                confirmButton = {
                    TextButton(onClick = {
                        showDiscardDialog = false
                        navigationState.goBack()
                    }) { Text("放弃") }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false; restoreEditorSheet = true }) { Text("继续编辑") }
                },
            )
        }
        }
        }
    }
}

/**
 * 页面切换动画。nav3 默认是 700ms 的淡入淡出——时长太长，弹层关闭时还会被误认成卡顿，
 * 换成主流的水平共享轴：新页从右侧滑入、旧页向左退出，270ms 收住。
 */
private const val NavTransitionMillis = 270

private fun AnimatedContentTransitionScope<*>.pushTransition(): ContentTransform =
    slideIntoContainer(SlideDirection.Left, tween(NavTransitionMillis, easing = FastOutSlowInEasing)) +
        fadeIn(tween(NavTransitionMillis / 2)) togetherWith
        slideOutOfContainer(SlideDirection.Left, tween(NavTransitionMillis, easing = FastOutSlowInEasing)) +
        fadeOut(tween(NavTransitionMillis / 2))

private fun AnimatedContentTransitionScope<*>.popTransition(): ContentTransform =
    slideIntoContainer(SlideDirection.Right, tween(NavTransitionMillis, easing = FastOutSlowInEasing)) +
        fadeIn(tween(NavTransitionMillis / 2)) togetherWith
        slideOutOfContainer(SlideDirection.Right, tween(NavTransitionMillis, easing = FastOutSlowInEasing)) +
        fadeOut(tween(NavTransitionMillis / 2))

/**
 * 弹层类目的地自己有进出动画，容器不能再横向滑一次，否则遮罩会跟着一起平移。
 * 关闭时用最短的淡出，"完成"之后要立刻消失。
 */
private fun sheetMetadata(key: NavKey): Map<String, Any> = when (key) {
    OmniRoute.TransactionEditor,
    is OmniRoute.TransactionDetail,
    is OmniRoute.DateDetail,
    is OmniRoute.SummaryDetail,
    -> NavDisplay.transitionSpec { fadeIn(tween(80)) togetherWith fadeOut(tween(80)) } +
        NavDisplay.popTransitionSpec { fadeIn(tween(80)) togetherWith fadeOut(tween(80)) }
    else -> emptyMap()
}

internal fun themePrimaryColor(themeColor: ThemeColor, darkTheme: Boolean): Color = when (themeColor) {
    ThemeColor.MIST_BLUE -> Color(if (darkTheme) 0xFF9CC3E5 else 0xFF52779A)
    ThemeColor.SAGE -> Color(if (darkTheme) 0xFF9BC8A8 else 0xFF4F765B)
    ThemeColor.LAVENDER -> Color(if (darkTheme) 0xFFC2B5E5 else 0xFF75679D)
    ThemeColor.SOFT_CORAL -> Color(if (darkTheme) 0xFFE7AAA4 else 0xFFA95850)
    ThemeColor.WARM_AMBER -> Color(if (darkTheme) 0xFFD8B778 else 0xFF8A6532)
    ThemeColor.GRAPHITE -> Color(if (darkTheme) 0xFFF5F5F5 else 0xFF171717)
}

private fun themePrimaryContainerColor(themeColor: ThemeColor, darkTheme: Boolean): Color = when (themeColor) {
    ThemeColor.MIST_BLUE -> Color(if (darkTheme) 0xFF28445E else 0xFFDCEBF7)
    ThemeColor.SAGE -> Color(if (darkTheme) 0xFF2D4935 else 0xFFDDECE1)
    ThemeColor.LAVENDER -> Color(if (darkTheme) 0xFF40375D else 0xFFE9E3F5)
    ThemeColor.SOFT_CORAL -> Color(if (darkTheme) 0xFF5E332F else 0xFFF6E2DF)
    ThemeColor.WARM_AMBER -> Color(if (darkTheme) 0xFF523F24 else 0xFFF2E7D3)
    ThemeColor.GRAPHITE -> Color(if (darkTheme) 0xFF2A2A2A else 0xFFE7E7E7)
}

private fun themeSecondaryColor(themeColor: ThemeColor, darkTheme: Boolean): Color = when (themeColor) {
    ThemeColor.MIST_BLUE -> Color(if (darkTheme) 0xFFB4C7D8 else 0xFF456274)
    ThemeColor.SAGE -> Color(if (darkTheme) 0xFFB6CCBB else 0xFF476653)
    ThemeColor.LAVENDER -> Color(if (darkTheme) 0xFFC7BEDB else 0xFF5D5577)
    ThemeColor.SOFT_CORAL -> Color(if (darkTheme) 0xFFF0BDB7 else 0xFF7B4A45)
    ThemeColor.WARM_AMBER -> Color(if (darkTheme) 0xFFE2C28F else 0xFF6D552D)
    ThemeColor.GRAPHITE -> Color(if (darkTheme) 0xFFD0C5D0 else 0xFF625A62)
}

private fun themeTertiaryColor(themeColor: ThemeColor, darkTheme: Boolean): Color = when (themeColor) {
    ThemeColor.MIST_BLUE -> Color(if (darkTheme) 0xFFD4B9B0 else 0xFF795651)
    ThemeColor.SAGE -> Color(if (darkTheme) 0xFFD2C39D else 0xFF705E2D)
    ThemeColor.LAVENDER -> Color(if (darkTheme) 0xFFE7B9C7 else 0xFF7A4B5E)
    ThemeColor.SOFT_CORAL -> Color(if (darkTheme) 0xFFD1C0E5 else 0xFF654A75)
    ThemeColor.WARM_AMBER -> Color(if (darkTheme) 0xFFC4C8E8 else 0xFF4F587D)
    ThemeColor.GRAPHITE -> Color(if (darkTheme) 0xFFBBCBD5 else 0xFF4B5F6B)
}

private fun omniColorScheme(
    themeColor: ThemeColor,
    darkTheme: Boolean,
    dynamicColorEnabled: Boolean,
    context: android.content.Context,
): ColorScheme {
    if (dynamicColorEnabled && android.os.Build.VERSION.SDK_INT >= 31) {
        return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    val primary = themePrimaryColor(themeColor, darkTheme)
    val primaryContainer = themePrimaryContainerColor(themeColor, darkTheme)
    val secondary = themeSecondaryColor(themeColor, darkTheme)
    val tertiary = themeTertiaryColor(themeColor, darkTheme)
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF111111),
            primaryContainer = primaryContainer,
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color(0xFF172027),
            secondaryContainer = Color(0xFF334853),
            onSecondaryContainer = Color(0xFFD8E9F1),
            tertiary = tertiary,
            onTertiary = Color(0xFF251A22),
            tertiaryContainer = Color(0xFF513B49),
            onTertiaryContainer = Color(0xFFFFD9E8),
            surface = Color(0xFF17171A),
            surfaceVariant = Color(0xFF232327),
            background = Color(0xFF0C0C0E),
            onSurface = Color(0xFFF5F5F5),
            onSurfaceVariant = Color(0xFFB9B9B9),
            outline = Color(0xFF8F8F8F),
            outlineVariant = Color(0xFF454545),
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD9E8EF),
            onSecondaryContainer = Color(0xFF1A313C),
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFF4DCE8),
            onTertiaryContainer = Color(0xFF321624),
            surface = Color.White,
            surfaceVariant = Color(0xFFECECF0),
            background = Color(0xFFF4F4F6),
            onSurface = Color(0xFF171717),
            onSurfaceVariant = Color(0xFF656565),
            outline = Color(0xFF656565),
            outlineVariant = Color(0xFFD2D2D2),
        )
    }
    return scheme.copy(
        error = if (darkTheme) Color(0xFFFFB4AB) else Color(0xFFBA1A1A),
        onError = if (darkTheme) Color(0xFF690005) else Color.White,
        errorContainer = if (darkTheme) Color(0xFF93000A) else Color(0xFFFFDAD6),
        onErrorContainer = if (darkTheme) Color(0xFFFFDAD6) else Color(0xFF410002),
        surfaceDim = if (darkTheme) Color(0xFF0C0C0E) else Color(0xFFE4E4E9),
        surfaceBright = if (darkTheme) Color(0xFF2E2E33) else Color.White,
        surfaceContainerLowest = if (darkTheme) Color(0xFF08080A) else Color.White,
        surfaceContainerLow = if (darkTheme) Color(0xFF141417) else Color(0xFFFAFAFC),
        surfaceContainer = if (darkTheme) Color(0xFF17171A) else Color.White,
        surfaceContainerHigh = if (darkTheme) Color(0xFF232327) else Color(0xFFECECF0),
        surfaceContainerHighest = if (darkTheme) Color(0xFF2C2C31) else Color(0xFFE4E4E9),
        inverseSurface = if (darkTheme) Color(0xFFE6E1E5) else Color(0xFF303030),
        inverseOnSurface = if (darkTheme) Color(0xFF303030) else Color(0xFFF5F5F5),
        inversePrimary = if (darkTheme) primary else primaryContainer,
    )
}

@Composable
private fun PrimaryNavigation(
    destination: MainDestination,
    onDestination: (MainDestination) -> Unit,
) {
    NavigationBar {
        MainDestination.entries.forEach { item ->
            NavigationBarItem(
                selected = destination == item,
                onClick = { onDestination(item) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
private fun PrimaryNavigationRail(
    destination: MainDestination,
    onDestination: (MainDestination) -> Unit,
    onAdd: () -> Unit,
) {
    NavigationRail(
        header = {
            FloatingActionButton(onClick = onAdd, modifier = Modifier.padding(vertical = 12.dp)) {
                Icon(Icons.Default.Add, contentDescription = "新增交易")
            }
        },
    ) {
        listOf(
            MainDestination.HOME,
            MainDestination.ANALYTICS,
            MainDestination.SEARCH,
            MainDestination.MORE,
        ).forEach { item ->
            NavigationRailItem(
                selected = destination == item,
                onClick = { onDestination(item) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
private fun AppLockGate(enabled: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var locked by rememberSaveable { mutableStateOf(enabled) }
    var authInProgress by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    val activity = context as? FragmentActivity
    val biometricPrompt = remember(activity) {
        activity?.let { fragmentActivity ->
            BiometricPrompt(
                fragmentActivity,
                ContextCompat.getMainExecutor(fragmentActivity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        authInProgress = false
                        authError = null
                        locked = false
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        authInProgress = false
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        ) {
                            authError = errString.toString()
                        }
                    }
                },
            )
        }
    }
    fun requestUnlock() {
        if (biometricPrompt == null) {
            authError = "当前设备无法使用系统身份验证"
        } else {
            authError = null
            authInProgress = true
            biometricPrompt.authenticate(
                BiometricPrompt.PromptInfo.Builder()
                    .setTitle("解锁 OmniFlow")
                    .setSubtitle("请验证生物识别或设备凭据")
                    .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                    )
                    .build(),
            )
        }
    }
    DisposableEffect(lifecycleOwner, enabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && enabled && !authInProgress) locked = true
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(enabled) { locked = enabled }
    LaunchedEffect(enabled, locked) {
        if (enabled && locked && !authInProgress) requestUnlock()
    }
    if (enabled && locked) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("OmniFlow 已锁定", style = MaterialTheme.typography.headlineSmall)
                authError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = ::requestUnlock) { Text("解锁") }
            }
        }
    } else {
        content()
    }
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onLedgerSelected: (LedgerScope) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onMonthSelected: (LocalDate) -> Unit,
    onSummary: (TransactionType?) -> Unit,
    onToggleDisplayMode: () -> Unit,
    onEdit: (String) -> Unit,
    onManageLedgers: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val home = state.home
    Box(modifier.fillMaxSize()) {
        when {
            state.isLoading && home == null -> LoadingState()
            state.error != null && home == null -> ErrorState(state.error)
            home != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 1040.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                // 账本单独占一行，和统计页一致；月份切换两个箭头顶在两端
                LedgerScopePill(home.scope, state.ledgers, onLedgerSelected)
                RangeStepper(
                    label = home.month.startInclusive.toLocalDateTime(ChinaTimeZone).date.yearMonthText(),
                    onPrevious = onPreviousMonth,
                    onNext = onNextMonth,
                    onLabel = { onMonthSelected(Clock.System.now().toLocalDateTime(ChinaTimeZone).date) },
                )
                SummaryCardRow(
                    expense = home.summary.expenseTotal,
                    income = home.summary.incomeTotal,
                    onExpense = { onSummary(TransactionType.EXPENSE) },
                    onIncome = { onSummary(TransactionType.INCOME) },
                    onNet = { onSummary(null) },
                )
                Surface(shape = RoundedCornerShape(OmniRadius.medium), color = surfaceCard()) {
                    CalendarMonth(
                        month = home.month.startInclusive.toLocalDateTime(ChinaTimeZone).date,
                        summaries = home.calendar,
                        onDateSelected = onDateSelected,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("明细", style = OmniText.titleRow)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onToggleDisplayMode) {
                        val icon = if (state.displayMode == TransactionDetailDisplayMode.LIST) {
                            Icons.Default.ViewModule
                        } else {
                            Icons.AutoMirrored.Filled.List
                        }
                        Icon(icon, contentDescription = "切换明细展示方式", tint = mutedContent())
                    }
                }
                if (state.ledgers.isEmpty()) {
                    HomeEmptyState(
                        title = "先创建一个账本",
                        message = "账本准备好后，就可以开始记录每一笔交易。",
                        actionLabel = "管理账本",
                        onAction = onManageLedgers,
                    )
                } else if (home.groups.isEmpty()) {
                    // 本月没有账单时只留一句状态说明，右下角本来就有记账按钮，不必再放一个
                    Text(
                        "本月还没有账单",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        textAlign = TextAlign.Center,
                        style = OmniText.caption,
                        color = mutedContent(),
                    )
                } else {
                    TransactionGroups(home.groups, state.displayMode, onEdit)
                }
                // 给右下角的悬浮记账按钮让位，避免压住最后一条明细
                Spacer(Modifier.height(88.dp))
            }
        }
    }
}

@Composable
private fun SummaryAmount(label: String, amount: Money, color: Color) {
    Column {
        Text(label, style = OmniText.caption, color = mutedContent())
        Text(amount.asRmb(), color = color, style = OmniText.amountPrimary, maxLines = 1)
    }
}

@Composable
private fun HomeEmptyState(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun CalendarMonth(
    month: LocalDate,
    summaries: List<CalendarDaySummary>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val totals = remember(summaries) { summaries.associateBy(CalendarDaySummary::date) }
    val today = remember { Clock.System.now().toLocalDateTime(ChinaTimeZone).date }
    val first = JavaLocalDate.of(month.year, month.monthNumber, 1)
    val leading = first.dayOfWeek.value % 7
    val cells = List(leading) { null } + (1..first.lengthOfMonth()).map { day ->
        LocalDate(month.year, month.monthNumber, day)
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { weekday ->
                Text(
                    weekday,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                week.forEach { date ->
                    CalendarCell(date, date == today, date?.let(totals::get), onDateSelected, Modifier.weight(1f))
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f).height(CalendarCellHeight)) }
            }
        }
    }
}

private val CalendarCellHeight = 56.dp

@Composable
private fun CalendarCell(
    date: LocalDate?,
    isToday: Boolean,
    summary: CalendarDaySummary?,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    if (date == null) {
        Spacer(modifier.height(CalendarCellHeight))
        return
    }
    Column(
        modifier = modifier
            .height(CalendarCellHeight)
            .clip(RoundedCornerShape(6.dp))
            .clickable { onDateSelected(date) }
            .padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(
                    if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        // 收入在上、支出在下，只显示整数，超过 1000 走 x.xk；金额为 0 的那一行不占位
        summary?.incomeTotal?.takeIf { it.minor > 0 }?.let {
            CalendarAmount(it.calendarAmountText(), incomeColor())
        }
        summary?.expenseTotal?.takeIf { it.minor > 0 }?.let {
            CalendarAmount(it.calendarAmountText(), expenseColor())
        }
    }
}

@Composable
private fun CalendarAmount(text: String, color: Color) {
    Text(
        text,
        color = color,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
    )
}

@Composable
private fun TransactionGroups(
    groups: List<DayTransactionGroup>,
    displayMode: TransactionDetailDisplayMode,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (groups.isEmpty()) {
        Text(
            "暂无账单",
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            textAlign = TextAlign.Center,
            style = OmniText.caption,
            color = mutedContent(),
        )
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        groups.forEach { group ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(group.date.displayName(), style = OmniText.caption, color = mutedContent())
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 收支语义色和日历、汇总卡同一套，不跟随主题色漂移
                        if (group.incomeTotal != Money.Zero) {
                            Text("收 ${group.incomeTotal.asCompactRmb()}", color = incomeColor(), style = OmniText.caption)
                        }
                        if (group.expenseTotal != Money.Zero) {
                            Text("支 ${group.expenseTotal.asCompactRmb()}", color = mutedContent(), style = OmniText.caption)
                        }
                    }
                }
                TransactionItems(group.items, displayMode, onEdit)
            }
        }
    }
}

@Composable
private fun TransactionItems(
    items: List<TransactionListItem>,
    displayMode: TransactionDetailDisplayMode,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (displayMode) {
        TransactionDetailDisplayMode.LIST -> Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(OmniRadius.medium),
            color = surfaceCard(),
        ) {
            Column(Modifier.padding(horizontal = 12.dp)) {
                items.forEachIndexed { index, item ->
                    TransactionListRow(item, onEdit)
                    if (index != items.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }

        TransactionDetailDisplayMode.CARD -> Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { item -> TransactionCard(item, Modifier.weight(1f), onEdit) }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun TransactionListRow(item: TransactionListItem, onEdit: (String) -> Unit) {
    TransactionRow(
        iconKey = item.categoryIconKey,
        title = item.categoryDisplayName,
        subtitle = item.note,
        amount = item.amount,
        type = item.type,
        timeText = item.occurredAt.hourMinuteText(),
        onClick = { onEdit(item.id) },
    )
}

@Composable
private fun TransactionCard(item: TransactionListItem, modifier: Modifier = Modifier, onEdit: (String) -> Unit) {
    TransactionTile(
        iconKey = item.categoryIconKey,
        title = item.categoryDisplayName,
        subtitle = item.note,
        amount = item.amount,
        type = item.type,
        timeText = item.occurredAt.hourMinuteText(),
        modifier = modifier,
        onClick = { onEdit(item.id) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDetailSheet(
    state: TransactionDetailState?,
    isLoading: Boolean,
    error: String?,
    sheetHeight: Dp,
    onDismiss: () -> Unit,
    onEdit: (String) -> Unit,
) {
    var displayMode by remember(state?.date?.startInclusive, state?.type) {
        mutableStateOf(TransactionDetailDisplayMode.CARD)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        contentWindowInsets = { WindowInsets(0) },
    ) {
        // 高度和记账弹层一致：上方只留一个状态栏，不再一个太满一个太空
        Column(Modifier.fillMaxWidth().height(sheetHeight)) {
            when {
                isLoading -> LoadingState()
                error != null -> ErrorState(error)
                state != null -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                state.date.detailLabel(),
                                modifier = Modifier.weight(1f),
                                style = OmniText.titleRow,
                                maxLines = 1,
                            )
                            IconButton(onClick = {
                                displayMode = when (displayMode) {
                                    TransactionDetailDisplayMode.CARD -> TransactionDetailDisplayMode.LIST
                                    TransactionDetailDisplayMode.LIST -> TransactionDetailDisplayMode.CARD
                                }
                            }) {
                                Icon(
                                    if (displayMode == TransactionDetailDisplayMode.CARD) {
                                        Icons.AutoMirrored.Filled.List
                                    } else {
                                        Icons.Default.ViewModule
                                    },
                                    contentDescription = "切换卡片和列表",
                                    tint = mutedContent(),
                                )
                            }
                        }
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            if (state.type != TransactionType.INCOME) {
                                SummaryAmount("支出", state.summary.expenseTotal, MaterialTheme.colorScheme.onSurface)
                            }
                            if (state.type != TransactionType.EXPENSE) {
                                SummaryAmount("收入", state.summary.incomeTotal, incomeColor())
                            }
                        }
                    }
                    if (state.items.isEmpty()) {
                        item {
                            Text(
                                "当前范围暂无交易",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                                textAlign = TextAlign.Center,
                                style = OmniText.caption,
                                color = mutedContent(),
                            )
                        }
                    } else {
                        item { TransactionItems(state.items, displayMode, onEdit) }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String) {
    Text(
        message,
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}
