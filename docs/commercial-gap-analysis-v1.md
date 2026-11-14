# OmniFlow 商业化对标与全维度差距分析报告 (v1.0)

> **对标标杆**：iOS「青子记账」及主流成熟 Android 原生记账 App  
> **被审代码库**：`OmniFlow`（Android 原生记账，Kotlin 2.1 + Jetpack Compose + Material 3 + SQLDelight 2.0）  
> **审计基准**：当前主分支代码（最新 Tag 经查为 `v0.15.0`，包含 `v0.9.0` 以来引入的月度预算、招商银行流水、AI 建议分类、纯色图标库与现代 M3 输入层次等全部演进）  
> **审计角色**：资深产品分析师 + 资深 Android 架构工程师  
> **原则声明**：本报告所有结论均基于源码权威证据（附文件路径、类名、函数名与行号），不凭猜测，严格遵循模块边界与设计规范。

---

## 1. 软件定位、架构与功能页面清单

### 1.1 这个软件是什么
`OmniFlow` 是一款定位为「本地优先（Local-First）、隐私至上、无广告干扰、聚焦多渠道账单自动流入」的 Android 原生现代记账应用。其核心价值主张在于解决传统个人记账「手动逐笔记账繁琐难以坚持」的痛点，主打多渠道（支付宝、微信、京东、美团、各大商业银行流水）账单批量导入、智能格式识别、记忆与规则分类、云端 WebDAV 私有快照同步，并辅以符合 Google Material Design 3 现代规范的精致视觉体验。

### 1.2 模块结构与边界
工程严格划分为两个职责清晰的模块：
- **`:core`（Android Library）**：
  - **定位**：UI 无关的纯业务领域、持久化与数据基础设施。
  - **核心组件**：
    - 数据库与存储：SQLDelight 驱动的 SQLite 数据库（`OmniFlowDatabase`）、`SqlDelightBackupStore`、Migrations（`1.sqm` 至 `4.sqm`）。
    - 仓储层（`repository`）：`SqlDelightTransactionRepository`、`SqlDelightAccountRepository`、`SqlDelightCategoryRepository`、`SqlDelightCategoryMemoryRepository`、`SqlDelightRuleRepository`、`SqlDelightReminderRepository` 等。
    - 领域外观（`facade`）：`HomeFacade`、`AnalyticsFacade`、`ManagementFacade`、`BudgetFacade`、`ReminderFacade`、`QingziInteropFacade`、`SyncFacade`。
    - 解析与导入引擎（`parser` & `workflow`）：`BillFormatDetector`、`CsvBillParser`、`SpreadsheetBillParser`、`BocPdfBillParser`、`CmbPdfBillParser`、`QingziBillParser`、`SqlDelightImportWorkflow`。
    - 领域模型与用例（`model` & `usecase`）：纯 Kotlin 数据类、`Money` 内联值类、各类增删改查及规则执行用例。
- **`:app`（Android Application）**：
  - **定位**：负责 Jetpack Compose 响应式渲染、导航拓扑、平台特有服务集成与硬件交互。
  - **核心组件**：
    - 应用生命周期与配置：`OmniFlowApplication`（初始化单例 `SharedApp`）、`MainActivity`。
    - 统一视图状态中枢：`OmniFlowViewModel`（汇聚暴露 8 组 `StateFlow`，管理所有协程调度与 UI 状态）。
    - 导航系统：基于 AndroidX Navigation 3（`androidx.navigation3`）的单 Activity 拓扑（`AppNavigation.kt`、`OmniNavigationState`）。
    - 系统服务适配器：`ReminderScheduler`（AlarmManager + 状态栏通知）、`WebDavSyncAdapter`（HTTP WebDAV 客户端）、`AiCategoryAdapter`（OpenAI 兼容协议客户端与 Keystore 加密凭据存储）。
    - 视觉系统：`OmniTokens.kt`（全局设计令牌）、`OmniSegmented.kt`、`SvgIcon.kt`、`CategoryIconCatalog.kt`。

### 1.3 核心数据流架构
系统遵循响应式单向数据流（UDF / MVI）与数据库作为单一真实可信源（Single Source of Truth）：
```
[ 用户操作 / 系统事件 ]
       │
       ▼
[ OmniFlowViewModel ] ──(调度/校验)──► [ UseCases / Facade ]
       │                                     │
       │                                     ▼
       │                             [ SQLDelight Queries ]
       │                                     │ (写入 SQLite)
       ▼                                     ▼
[ Flow<Query.Result> ] ◄───(响应式推送)─────── [ 本地 SQLite DB ]
       │
       ▼
[ StateFlow<UiState> ] ──(collectWithLifecycle)──► [ Jetpack Compose UI ]
```

### 1.4 现有全部功能清单
1. **多账本管理**：支持创建多个独立账本、设置默认账本、自定义账本封面图标，支持单账本或多账本联合查看。
2. **多账户与净资产看板**：支持现金、微信、支付宝、银行卡等账户；记录账户余额变动历史；区分资产与负债，统计总净资产，支持将特定账户排除在总资产之外。
3. **两级分类体系**：一级收支分类 + 二级细分分类；支持自定义名称、图标与拖拽排序。
4. **多渠道账单导入**：
   - 格式：支付宝（CSV）、微信（XLSX）、京东（CSV）、美团（CSV）、建设银行（XLS）、中国银行（PDF）、招商银行（PDF）、青子记账（JSON 备份）。
   - 能力：智能去重检测（确权/疑似重复）、自动跳过中性转账交易、自动应用用户规则、历史分类记忆自动归类、可选大语言模型（OpenAI 兼容接口）建议分类、导入前日期范围过滤、全量/按组批量指定分类与剔除。
5. **月度预算控制**：支持账本月度总预算与各一级分类独立预算设置；实时计算本月支出进度、剩余可用金额与超支标记。
6. **统计报表分析**：
   - 周期切换：周、月、年、自定义日期区间。
   - 视图：年度趋势双柱图（收/支）、月度柱状对比（上下镜像/左右并排）、分类环形图（Donut Chart）与二级分类展开、大额收支排行、标签消费占比、年度汇总对账单下钻。
7. **多维明细搜索**：支持按关键词、账本、账户、收支类型、一级分类、二级分类、标签、备注、金额区间、发生日期范围联合筛选。
8. **提醒与定时通知**：支持信用卡固定还款日、账单日后 N 天还款，以及周期订阅（日/周/月/年）本地通知提醒。
9. **数据安全与备份**：
   - 应用生物识别/锁屏密码锁（BiometricPrompt）。
   - WebDAV 远程私有云全量加密快照定时备份与恢复。
   - 敏感凭据（WebDAV 密码、AI API Key）走 Android Keystore AES-GCM 硬件加密存储。
10. **数据互导与互通**：支持向青子记账标准 JSON 格式全量导出、通用 CSV 表格导出；支持解析青子记账备份 JSON 无缝迁移。
11. **视觉与交互定制**：深色/浅色/跟随系统模式、Android 12+ 动态壁纸取色（Material You）、6 套内置高品质品牌主题色。

### 1.5 现有全部页面清单
| 页面 / 路由名称 | 对应路由（OmniRoute） | 承载组件 / 文件位置 | 核心职责 |
| :--- | :--- | :--- | :--- |
| **首页** | `OmniRoute.Home` | `HomeScreen` (`OmniFlowApp.kt#L841`) | 月份切换、账本选择、收支汇总、月历热力图、日交易分组列表、记账 FAB |
| **记账面板** | `OmniRoute.TransactionEditor` | `TransactionEditorScreen` (`TransactionEditorScreen.kt#L102`) | 底部弹层（ModalBottomSheet），收支模式、分类网格翻页、账户/标签选择、内置计算键盘、日期与备注录入 |
| **明细详情** | `OmniRoute.TransactionDetail` | `TransactionRecordDetailSheet` (`TransactionRecordDetailScreen.kt#L59`) | 底部弹层，显示单笔交易完整元数据（账本、账户、分类、标签、来源、统计标志），提供编辑与删除入口 |
| **日明细 / 汇总下钻** | `OmniRoute.DateDetail` / `SummaryDetail` | `DateDetailSheet` (`OmniFlowApp.kt#L1183`) | 底部弹层，查看单日或某收支类型的全量交易卡片/列表，含当日收支汇总 |
| **统计分析** | `OmniRoute.Analytics` | `AnalyticsScreen` (`AnalyticsScreen.kt#L78`) | 周/月/年/范围切换，年度趋势图，分类饼图，排行卡片，标签统计，年度对账单入口 |
| **年度账单表格** | 模态弹层 (`statementTable != null`) | `StatementTableSheet` (`AnalyticsScreen.kt#L577`) | 底部弹层，12 个月逐月收入、支出、结余柱状分布与网格明细对账表 |
| **多维搜索** | `OmniRoute.Search` | `SearchScreen` (`SearchScreen.kt#L66`) | 关键词检索，展开式多字段高级筛选面板，汇总结果统计与流水列表 |
| **更多主页** | `OmniRoute.More` | `MoreHome` (`MoreScreen.kt#L189`) | 净资产资产负债总卡片、各管理与设置子模块导航入口矩阵、版权与版本信息 |
| **通用设置** | `OmniRoute.MoreSettings` | `SettingsPage` (`MoreScreen.kt#L374`) | 应用锁开关、深浅外观模式、全局主题色拾色器、动态取色开关、临时缓存清理 |
| **数据同步** | `OmniRoute.MoreData` | `DataManagementPage` (`MoreScreen.kt#L499`) | WebDAV 服务端配置、连通性测试、立即备份、云端历史快照恢复 |
| **AI 分类配置** | `OmniRoute.MoreAi` | `AiSettingsPage` (`AiSettingsPage.kt#L44`) | OpenAI 兼容服务地址、模型名、API Key 配置与连接测试、隐私声明 |
| **账单导入** | `OmniRoute.MoreImport` | `ImportPage` (`MoreScreen.kt#L739`) | 账本指定、8 种来源渠道选择网格、系统 SAF 文件选取 |
| **导入预览与核对** | 会话状态内嵌 (`ImportPreviewSection`) | `ImportPreviewScreen` (`ImportPreviewScreen.kt#L81`) | 日期区间二次过滤、分组折叠、分类选择器、整组批量改分类、排除切换、确认入账 |
| **分类选择弹层** | 模态弹层 (`CategoryPickerSheet`) | `CategoryPickerSheet` (`CategoryPickerSheet.kt#L38`) | 导入条目或整组指定分类的两级挑选网格 |
| **数据导出** | `OmniRoute.MoreExport` | `ExportPage` (`MoreScreen.kt#L874`) | CSV 表格 / JSON 备份格式选择、账本与日期范围筛选、导出数量预估、SAF 保存 |
| **预算管理** | `OmniRoute.MoreBudgets` | `BudgetPage` (`BudgetPage.kt#L45`) | 账本月度总预算与分类预算列表、进度条、预算新建与编辑弹层 |
| **账本管理** | `OmniRoute.MoreLedgers` | `LedgerManagement` (`ManagementScreens.kt#L92`) | 账本列表、新建/重命名账本、封面选择、默认账本设定 |
| **账户管理** | `OmniRoute.MoreAccounts` | `AccountManagement` (`ManagementScreens.kt#L236`) | 账户卡片列表、新建/编辑账户、余额校准、不计入总资产开关 |
| **资产看板** | `OmniRoute.MoreAssets` | `AssetManagement` (`ManagementScreens.kt#L378`) | 净资产卡片、资产账户占比分布、负债账户列表 |
| **分类管理** | `OmniRoute.MoreCategories` | `CategoryManagement` (`ManagementScreens.kt#L490`) | 一级/二级收支分类树形列表、图标选择器、新建与删除 |
| **标签管理** | `OmniRoute.MoreTags` | `TagManagement` (`ManagementScreens.kt#L689`) | 账本标签列表、新建与编辑 |
| **规则管理** | `OmniRoute.MoreRules` | `RuleManagement` (`ManagementScreens.kt#L764`) | 自动分类与排除规则列表、长按拖拽优先级调整、新建与编辑 |
| **提醒管理** | `OmniRoute.MoreReminders` | `ReminderManagement` (`ManagementScreens.kt#L983`) | 还款与周期订阅提醒列表、开关暂停、新建与编辑 |
| **实体聚合详情** | `OmniRoute.LedgerDetail` / `AccountDetail` / `CategoryDetail` | `EntityDetailScreen` (`EntityDetailScreen.kt#L38`) | 单一账本/账户/分类的汇总卡、最近 10 笔明细、一键跳转关联搜索 |

---

## 2. 逐页商业化深度评审 (Commercial UI/UX Audit)

### 2.1 首页 (`HomeScreen` / `OmniFlowApp.kt`)
- **信息层级与留白**：
  - 优点：账本切换、月份切换、汇总三卡（支/收/余）、月历、明细流水，自上而下逻辑通畅，中性底色沉稳。
  - **严重缺陷 1（核心指标缺失）**：**月度预算完全没有在首页透出**！商业记账软件（包括青子、随手记、钱迹）最核心的心理账户就是“本月还剩多少预算、今天还能花多少”。用户在首页完全看不到预算进度，必须深入到 `更多 -> 预算` 才能看到，严重削弱了记账对冲动消费的约束力。
  - **缺陷 2（空状态粗糙）**：当月无交易时，仅展示一句微弱的静态文字 `本月还没有账单`（`OmniFlowApp.kt#L924`），没有插画引导，也没有引导用户“点击右下角记一笔”或“导入账单”的主操作指引。
- **图表与组件质量**：
  - 月历网格（`CalendarMonth`）：数字下方显示紧凑金额，超过 1000 缩写为 `1.2k`。但在小屏幕（如 360dp 宽）设备上，当某日既有收入又有支出时，两行 10sp 的文本非常拥挤，容易与上下行日期黏连。
  - 汇总卡（`SummaryCardRow`）：只保留整数、不带货币符号。当某月支出达到百万级时，虽然做了动态降字号（`OmniText.amountCard`），但结余负数时的红色过暗，弱光环境下对比度欠佳。
- **动效与手势反馈**：
  - 点击日历日期弹出 `DateDetailSheet`，弹层动画收放干脆；但日历单元格缺少波纹点击反馈（Ripple），点按瞬间感知迟钝。

### 2.2 记账弹层 (`TransactionEditorScreen.kt`)
- **信息层级与布局承载**：
  - 优点：高度做到了屏幕减去状态栏自适应，矮屏降为 2 行分类，防止键盘被挤压出屏幕；一级分类网格分页清晰。
  - **严重缺陷 1（键盘计算器缺失等号键）**：键盘内置了 `+` 与 `-`，但**没有 `=`（等号求值键）**！当用户输入 `25+18` 时，必须直接按主动作按钮「完成」；而一旦按下「完成」，系统会立即执行计算并**直接提交保存并关闭页面**！用户如果只是想先算出一顿饭的总额再核对一下备注，根本没有中间确认机会，极易造成误入账。
  - **严重缺陷 2（备注输入阻断流程）**：当用户点击「备注」框时，为了防止系统软键盘与自定义数字键盘打架，代码设计了 `NoteInputBar`（`TransactionEditorScreen.kt#L707`），强制把数字键盘顶掉，并显示“输入备注后点完成回到数字键盘”。用户输完备注必须先在提示条上点一次“完成”，然后再在数字键盘上点一次“完成”才能入账。一个简单的记账被割裂成了两次“完成”确认。
  - **缺陷 3（跨页拖拽缺失）**：`PagedCategoryGrid` 支持长按拖动分类排序，但源码明确标注 `// 只在当前页内重排，跨页拖动不处理`（`TransactionEditorScreen.kt#L463`），当分类较多翻到第 2 页时，无法将分类拖到第 1 页。

### 2.3 交易记录详情 (`TransactionRecordDetailSheet.kt`)
- **视觉一致性与操作闭环**：
  - 优点：大图标、醒目金额、结构清晰的属性列表（账户、账本、标签、备注、导入来源），整体排版符合现代审美。
  - **缺陷 1（删除与编辑操作权重失衡）**：底部提供了大型「删除」（红色线框）与「编辑」（主色实心填充）两个并排按钮，但顶部右上角又放了一个红色的垃圾桶图标（`TransactionRecordDetailScreen.kt#L91`），同一个弹层内存在两个功能完全相同但样式不同的删除触发器，属于多余设计。
  - **缺陷 2（缺乏上一笔/下一笔快速切换）**：核对账单时，用户进入详情通常希望左右滑动快速查看前后的交易，目前必须关闭当前 Sheet 回到列表再点开下一条，浏览效率低。

### 2.4 统计分析页 (`AnalyticsScreen.kt`)
- **图表质量与深度问题**：
  - **严重缺陷 1（周/月周期缺少趋势柱状图）**：页面在「年」维度下有精致的 12 个月双柱趋势图（`TrendCard`），但当用户切换到使用频次最高的**「周」或「月」维度时，页面竟然完全没有任何趋势图表**！直接从汇总卡跳到了分类饼图。用户根本无法在周视图中看出“周几花得最多”，也无法在月视图中看出“上旬还是下旬花超了”。
  - **缺陷 2（环形饼图交互断层）**：中间的支出分类环形图（`CategoryDonut`）为静态 Canvas 绘制，不支持点击扇区高亮联动高亮下方分类明细；点击下方的分类行，直接强行跳转到了 `EntityDetailScreen`，打断了统计页面的整体浏览心流。
  - **缺陷 3（对账单表格导出受限）**：`StatementTableSheet` 汇总了全年 12 个月的收支结余表格，非常具备财务对账价值，但底部没有任何导出为 Excel / CSV 或分享图片的商业化出口。

### 2.5 搜索页 (`SearchScreen.kt`)
- **信息层级与输入摩擦**：
  - **严重缺陷 1（筛选器控件选型失误）**：展开高级筛选面板时，一级分类、二级分类、标签、备注全部为原始文本输入框（`FilterField`，`SearchScreen.kt#L255-L261`）。用户想要筛选“餐饮”分类，必须调起输入法手动打出“餐饮”两个字！而在商业化成熟 App 中，分类和标签必须是点选式的 Chip 标签或滑动选择器，强迫用户手打文字筛选体验极差。
  - **缺陷 2（无实时搜索与搜索历史）**：关键词输入后不会防抖（Debounce）自动刷新结果，必须按下软键盘搜索或点击“搜索”按钮；同时缺少“最近搜索关键词”和“热门标签”快速点选。
  - **缺陷 3（过滤条件无法固化）**：组合了复杂的账本、分类、金额区间后，一旦离开或清理，下次必须重新配置，缺乏类似青子记账“数据过滤箱”的预设保存机制。

### 2.6 账单导入源与预览页 (`ImportPage` & `ImportPreviewScreen.kt`)
- **商业化流程亮点与硬伤**：
  - 亮点：导入预览页（`ImportPreviewScreen`）是整个项目中设计深度极高的模块。具备日期区间过滤、整组批量改分类、疑似重复标黄、不计收支智能分离，且支持大语言模型智能归类，商业完成度惊艳。
  - **严重缺陷 1（格式嗅探逻辑脆弱，极易崩溃）**：
    源码 `SqlDelightImportWorkflow.kt#L354-L355` 中：
    ```kotlin
    if (fileName.endsWith(".xlsx")) return ImportFormat.WECHAT
    if (fileName.endsWith(".xls")) return ImportFormat.CCB
    ```
    只要文件名以 `.xlsx` 结尾，无论文件实际内容为何，一律被当成微信账单交给 `SpreadsheetBillParser.parseWechat` 解析；只要以 `.xls` 结尾一律被当成建行账单！如果用户导出了其他网银的 xlsx 账单或通用 Excel，解析会因找不到特定表头直接抛出 `error(...)` 并导致流程阻断。
  - **缺陷 2（返回退出缺乏会话保护）**：在预览核对界面中，如果用户不小心触发了系统返回手势（或点击顶栏返回），页面会直接出栈回到更多菜单，虽然后台会话未销毁，但缺乏“是否放弃本次导入？”的确认弹窗拦截，极易误触退出。

### 2.7 预算管理页 (`BudgetPage.kt`)
- **层级与能力缺口**：
  - 优点：支持账本总预算和各一级分类预算，进度条在超支时自动翻红（`isOverspent`），金额对比清晰。
  - **严重缺陷 1（无周期维度可选项）**：源码注释明确写明 `// 只做月度：周预算和年预算在记账场景里几乎没人用，先不引入周期字段增加复杂度`（`Budget.kt#L9`）。但实际上商业用户对“年度总预算控制”以及“大促周预算”有刚性诉求，缺少周期扩展。
  - **严重缺陷 2（缺乏动态可花额度推算）**：预算页面只计算了已用和剩余，没有结合当前月剩余天数计算“今日建议可花：¥XX”。这是青子记账非常吸睛的特色功能，在当前代码中完全未实现。

### 2.8 资产管理页 (`ManagementScreens.kt#AssetManagement`)
- **资产负债与流转断层**：
  - 优点：净资产大卡片质感出色，正负余额资产负债分类归集，直观展示资产占比条。
  - **致命业务缺陷（缺少转账/还款交易类型）**：
    整个系统的交易类型枚举 `TransactionType` 只有 `EXPENSE` 和 `INCOME`（`Model.kt#L12`），**完全没有 `TRANSFER`（转账/内部划转）**！
    当用户用银行卡给微信零钱充值 1000 元，或者信用卡还款 3000 元时，在当前系统中无法记录为转账，只能记为一笔支出，导致当月支出虚增、结余严重失真！这是记账软件最基本的财务逻辑硬伤。

---

## 3. 逐流程可用性评审 (Workflows & Usability Audit)

### 3.1 记一笔全流程 (Transaction Creation)
- **标准操作步数**：
  1. 首页右下角点击 FAB 悬浮按钮 `+`（打开记账弹层）—— **第 1 步**
  2. 在分类网格中点击目标分类（如“餐饮”）—— **第 2 步**
  3. 在数字键盘上输入金额数字（如 `2`、`5`）—— **第 3-4 步**
  4. 点击键盘右下角「完成」入账并退出—— **第 5 步**
  *(最少需要 5 次按键/点击)*
- **流程阻塞点评估**：
  - **缺少初始金额提示**：打开弹层时金额显示为 `0`，如果用户先输金额再选分类，体验尚可；但如果用户需要连续记账，点击「再记」虽保留了账户与日期，但无触觉震动反馈，容易怀疑是否保存成功。
  - **复合计算无法预览**：如买水果 `12+8.5`，按完数字后只能盲点「完成」，无中间等号结果呈现。
  - **添加二级分类阻断**：若想选的二级分类不存在，点击小圆圈 `+` 会弹出一个阻断式 `AlertDialog`，输入名称后点击“创建”，然后再回到列表选择，打断记账心流。

### 3.2 账单导入全流程 (Bill Import Flow)
- **标准操作步数**：
  1. 底部导航切换到「更多」Tab—— **第 1 步**
  2. 点击「导入账单」列表项进入导入源页—— **第 2 步**
  3. 在 9 宫格中点击来源（如“微信”）或“自动识别”—— **第 3 步**
  4. 在系统文件选择器（SAF）中选中文档—— **第 4 步**
  5. 自动进入加载解析、规则匹配与 AI 推断（等待 1~3 秒）—— **第 5 步**
  6. 进入 `ImportPreviewSection` 预览核对页，滑动查看分组与条目—— **第 6 步**
  7. （可选）点击组头或条目调整未识别的分类—— **第 7 步**
  8. 点击底部固底按钮「确认入账 (N 笔)」—— **第 8 步**
  *(全流程共 8 个关键节点)*
- **流程阻塞点评估**：
  - 导入前的前置条件要求：如果当前账本无任何有效账户，系统在步骤 3 直接报错“请先在「账户」里创建至少一个账户”，但没有提供快捷“新建账户”按钮，用户被迫原路退出。
  - 智能分类记忆生效极好：首次导入并归类后，下次导入相同商户会自动记忆，第 7 步在后续导入中可逐步免除。

### 3.3 搜索与筛选全流程 (Search & Filter Flow)
- **标准操作步数**：
  1. 底部导航切换到「搜索」Tab—— **第 1 步**
  2. 点击顶栏搜索框输入关键词，或点击「展开筛选」展开面板—— **第 2 步**
  3. （展开筛选下）手动键盘输入分类名、选择账户、选择日期区间—— **第 3-6 步**
  4. 必须手动点击外置的「搜索」按钮或键盘上的 IME Search—— **第 7 步**
- **流程阻塞点评估**：
  - 筛选与搜索必须按“搜索”才生效，不能在修改筛选条件后即时响应结果。
  - 搜索结果无法二次筛选或按金额升降序排序。

### 3.4 查看、编辑与删除全流程 (View, Edit & Delete)
- **查看与删除步数**：
  1. 首页或搜索页列表点击任意交易行—— **第 1 步**（呼出 `TransactionRecordDetailSheet`）
  2. 点击底部「删除」按钮—— **第 2 步**（呼出二次确认弹窗）
  3. 点击确认弹窗中的「删除」—— **第 3 步**（执行删除并返回）
  *(单笔删除需要 3 步)*
- **编辑步数**：
  1. 列表点击交易行—— **第 1 步**
  2. 详情中点击「编辑」按钮—— **第 2 步**（关闭详情，推入 `TransactionEditorScreen` 并恢复已有字段）
  3. 修改金额或分类—— **第 3 步**
  4. 点击键盘「完成」—— **第 4 步**（保存并返回）
- **流程阻塞点评估**：
  - **严重缺乏列表手势操作**：列表行不支持左滑删除（Swipe to Delete），也不支持长按呼出快捷菜单。对于记账软件来说，高频的“误记快速删除”被强制拉长为 3 步弹层交互。

### 3.5 批量操作能力评审 (Batch Operations)
- **现状代码证据**：
  - 账单导入模块（`ImportPreviewScreen.kt`）具有出色的批量能力：`ImportGroupHeader` 支持整组一键统一指定分类（`onPickCategory`）、整组一键切换排除（`onIncludeAll`）。
  - **但日常记账流水列表完全没有批量操作**：无论是首页日流水、月度流水，还是搜索结果页、实体详情页，**均不存在多选模式（Multi-Selection Mode）**。当用户想批量把 5 笔账单归入某个账本、或批量删除导入重复的数笔交易时，必须逐条点进详情 -> 点击删除 -> 确认删除，重复循环。

### 3.6 导航、返回与状态持久化评审 (Navigation & Back Stack)
- **导航与手势架构**：
  - 底层基于 `androidx.navigation3`，主界面 4 个 Tab 采用 `HorizontalPager`，支持左右跟手平滑滑动手势切换，体验极佳。
  - 返回栈机制：当处于非首页 Tab（如统计、搜索）且栈深度为 1 时，按物理返回键会优先切回首页（`AppNavigation.kt#L114-L117`），首页再次返回提示“再返回一次退出应用”，符合商业 Android 体验惯例。
- **状态丢失风险与系统陷阱**：
  - **草稿保护机制完善**：在 `TransactionEditorScreen` 中，若用户已输入金额、备注或选择了分类，下滑退出或按返回键会拦截并弹出 `AlertDialog("放弃未保存的交易？")`，选择继续编辑时能平滑恢复弹层高度（`OmniFlowApp.kt#L223-L228`）。
  - **应用锁的生命周期缺陷**：
    源码 `OmniFlowApp.kt#L818`：
    ```kotlin
    if (event == Lifecycle.Event.ON_STOP && enabled && !authInProgress) locked = true
    ```
    当用户在应用内调用系统相册、调用 SAF 选择文件、或弹出生物识别框时，Activity 会经历 `ON_STOP`。若外部页面跳转回来，容易误触发 `locked = true`，导致用户选完文件刚回到应用，立刻被生物识别锁挡住，甚至破坏正在进行的 ActivityResult 回调。

---

## 4. 功能差距分析：青子记账清单核对与商业必备补遗

### 4.1 对标青子记账 15 项清单核查表
以下为针对用户列出的 iOS「青子记账」15 项 App Store 特性的逐项核查结论及权威代码证据：

| 序号 | 青子记账功能特性 | OmniFlow 实现状态 | 权威代码证据（路径 / 类名 / 行号） | 详细差距与分析说明 |
| :---: | :--- | :---: | :--- | :--- |
| **1** | **分类预算管理**<br>(按分类设月/年预算，智能规划每日可花额度) | **部分实现**<br>(仅支持月度，无年预算，无日均推算) | `core/.../domain/model/Budget.kt#L8-L19`<br>`core/.../db/Budget.sq`<br>`app/.../ui/BudgetPage.kt#L45` | 支持月度账本总预算和各一级分类预算，并能按月计算支出进度。但数据结构无周期类型字段，源码注释声明“只做月度，周和年暂不引入”；完全无“结合当月剩余天数智能规划每日可花额度”的算法与展示。 |
| **2** | **定时/周期记账**<br>(提前设定，按时自动生成账目) | **未实现**<br>(仅为本地到期推送，不自动入账) | `core/.../domain/model/Reminder.kt`<br>`app/.../ReminderScheduler.kt#L107-L145`<br>`ManagementScreens.kt#L1010` | 现存模块为“还款与订阅提醒”。AlarmManager 到期后仅在通知栏弹出系统 Notification 提醒用户，代码中未调用任何创建交易逻辑，根本不会在到期时自动往数据库插入账目流水。 |
| **3** | **桌面小组件**<br>(多种账单数据实时展示) | **未实现** | `app/src/main/AndroidManifest.xml` | 清单文件中仅注册了 Activity 与两个广播接收器，完全没有 `AppWidgetProvider`、Glance 或 RemoteViews 声明，桌面无法添加任何微件。 |
| **4** | **存钱计划**<br>(设定目标并执行) | **未实现** | `core/src/main/sqldelight/.../*.sq` | 整个数据库 schema 中无任何 saving_goal/target 表，UI 层亦无任何储蓄存钱计划、心愿进度条或定期存入计划入口。 |
| **5** | **账单导入**<br>(微信/支付宝等自动生成，含京东/美团/银行质量核实) | **已实现并大幅超越竞品**<br>(但存在格式探测细节缺陷) | `core/.../parser/ImportFormat.kt`<br>`core/.../facade/SqlDelightImportWorkflow.kt`<br>`app/.../ui/ImportPreviewScreen.kt` | **覆盖度极高**：不仅支持微信、支付宝，还支持京东、美团、建行、中行、招行流水及青子备份。具备两小时/单日时间窗疑似重复检测、外部单号去重、规则自动分类、分类记忆沉淀和大模型 AI 归类。**缺陷**：后缀 `.xlsx` / `.xls` 格式探测硬编码。 |
| **6** | **模板记账**<br>(常用支出存为模板，一键生成) | **未实现** | `core/.../domain/model/SystemDefaults.kt#L12` | 源码中的“Template”仅为初始化新账本时预设的系统分类/账户模版；用户端完全没有将某一笔自定义支出（如“常喝的生椰拿铁 18元 餐饮 微信支付”）存为记账模板并在键盘上一键调用的能力。 |
| **7** | **账单日历**<br>(每日收支以日历展现) | **已实现** | `app/.../ui/OmniFlowApp.kt#L888-L1008`<br>`OmniFlowApp.kt#L484-L500`<br>`CalendarDaySummary.kt` | 首页内嵌完整月历视图，日单元格分别以绿色展示收入、深色展示支出，高亮当天；点击日历中任意日期平滑调出当日明细 Sheet，支持卡片/列表切换。 |
| **8** | **非理性消费标记**<br>(反思消费习惯类标签/复盘功能) | **未实现** | `core/.../db/Transaction.sq`<br>`AnalyticsScreen.kt` | 仅有常规自定义标签 `Tag` 和 `isExcluded`（不计入统计）。无专门针对“冲动消费 / 必需消费 / 反思复盘”的三维标记体系，统计页也无任何冲动消费占比分析报告。 |
| **9** | **多账本** | **已实现** | `core/.../db/Ledger.sq`<br>`ManagementScreens.kt#L91`<br>`SharedControls.kt#L59` | 深度支持多账本创建、封面选择、默认账本设定；首页、统计页、记账页均可通过统一的 `LedgerScopePill` 无缝切换单账本或“所有账本”跨账本统揽。 |
| **10** | **数据过滤箱**<br>(保存多个过滤条件，一键查看报表) | **未实现** | `app/.../ui/SearchScreen.kt#L66-L196` | 搜索页支持极度详尽的多维筛选，但条件仅停留在当前界面的临时内存，无法将组合条件“命名保存为过滤器”，更无法在报表页面一键调用已存过滤器。 |
| **11** | **资产/负债账户**<br>(多账户，账目与余额联动) | **已实现** | `core/.../db/Account.sq`<br>`SqlDelightTransactionRepository.kt#L229`<br>`ManagementScreens.kt#L378` | 支持多账户（现金/借记卡/信用卡等），记账/删账/改账严格触发 `adjustBalance` 实时联动计算账户余额与变动历史流水；具备资产负债与净资产大卡片分析。 |
| **12** | **多币种与汇率换算** | **未实现** | `core/.../domain/model/Money.kt#L6`<br>`Transaction.sq` | 数据库表结构无币种字段，领域值类 `Money` 直接以分（Long）存储，展示格式化硬编码为人民币符号 `¥`，不支持多币种记录或实时汇率换算。 |
| **13** | **账目地图**<br>(记录消费地点，看哪里花钱最多) | **未实现** | `Transaction.sq` | 交易数据表中无经纬度（lat/lng）或位置名称字段，工程中未引入任何地图定位 SDK，无地理空间消费分析能力。 |
| **14** | **AI / 语音快速记一笔** | **未实现**<br>(AI 仅用于账单导入，无快速记账) | `app/.../AiCategoryAdapter.kt`<br>`core/.../ai/OpenAiChatProtocol.kt` | 工程接入了 OpenAI 兼容大模型协议，但**仅封装为 CategorySuggester 专供账单导入时推断分类**。日常记账不支持对话式自然语言记账（如“今天午饭吃了麻辣烫花了30”），也无任何语音识别（ASR）模块与录音权限。 |
| **15** | **产品气质与设计**<br>(极简/无广告/设计/引导与留存) | **部分实现**<br>(极简无广告优秀，但缺引导留存) | `OmniTokens.kt`<br>`SqlDelightInitialDataRepository.kt` | **优势**：纯粹无广告、本地优先、严格落实 Material 3 规范与低饱和度配色，设计质感出众。**硬伤**：冷启动无任何新手引导；无记账打卡天数（Streak）；无报表主动提醒；缺少促留存设计。 |

---

### 4.2 青子未列、但商用原生记账软件应有的关键差距补遗

1. **内部转账（Transfer）能力缺失（致命商业硬伤）**：
   - 商业记账应用（网易有钱、钱迹、随手记、Moze 等）的核心标配能力之一就是“账户间划转”（如：招行卡转账 5000 到支付宝零钱、储蓄卡向信用卡还款）。
   - OmniFlow 目前没有转账模型。用户资金划转时，若记为支出则虚增消费，若记为“不计入统计”则操作繁琐且资产负债变动不清晰。
2. **新手冷启动引导（Onboarding Experience）全无**：
   - 用户首次打开 App 时，后台静默写入了默认账本后直接呈现空荡荡的日历页。
   - 缺乏向用户展示“支持从微信/支付宝一键导入账单”的核心卖点引导页（Welcome Carousel），导致 OmniFlow 最强大的护城河能力被深藏在二级菜单中。
3. **本地数据库全量备份与离线导入**：
   - 目前全量数据备份严重绑定在 WebDAV（坚果云等）。虽然提供了青子 JSON 和 CSV 导出，但如果用户没有 WebDAV 账号，在更换手机时，无法通过直接导出一个本地 `.db` 或全量 `.omniflow` 离线加密文件在另一台设备上一键恢复。
4. **多任务与后台隐私防护（FLAG_SECURE）**：
   - 商业金融类应用标准要求：在用户切换多任务卡片时，应支持模糊化或隐藏账单金额（通过系统 `WindowManager.LayoutParams.FLAG_SECURE` 或 Compose 覆盖层实现），防止多任务截屏泄露资产隐私。
5. **月度预算首页透出与超支预警机制**：
   - 预算不应该只是一张静态报表，成熟软件会在每日打开时提示“本月预算还剩 XX%，今日建议支出上限 XX 元”；当支出超过 80% 或超支时，应主动触发系统提醒或界面醒目 Banner。

---

## 5. 优先级与工作量矩阵 (Priority & Effort Matrix)

> **评级标准说明**：
> - **优先级**：
>   - **P0（致命阻塞 / 核心体验短板）**：严重违背记账基本规律、造成用户数据误解、或阻碍版本商业化发布的问题。
>   - **P1（重要体验缺口 / 差异化竞争特性）**：显著提升记账效率、对标标杆产品体验的关键功能。
>   - **P2（后续规划 / 进阶特性）**：需要引入较重依赖（如地图 SDK、语音 SDK）或边缘长尾特性。
> - **预估工作量**：
>   - **S（Small）**：0.5 ~ 1 个工作日以内，轻量修复或局部 UI 调整。
>   - **M（Medium）**：2 ~ 3 个工作日，涉及前后端数据流打通或完整交互设计。
>   - **L（Large）**：1 周以上，涉及复杂架构改造、外部 SDK 接入或全新算法。

| 问题 ID | 问题名称与对应模块 | 优先级 | 工作量 | 对用户核心价值（一句话） |
| :---: | :--- | :---: | :---: | :--- |
| **GAP-01** | **首页透出月度预算进度与每日建议可花额度** (`HomeScreen` & `BudgetFacade`) | **P0** | **M** | 让用户一打开 App 就能瞬间看清财务水位，直接指导今日可消费上限。 |
| **GAP-02** | **数字键盘交互重构：新增 `=` 键与计算过程预览** (`TransactionEditorScreen`) | **P0** | **S** | 彻底消除复合记账必须先盲点“完成”的误入账恐慌，保障记账数据准确。 |
| **GAP-03** | **内部转账（Transfer）基础能力与双账户联动** (`core/model` & `TransactionEditor`) | **P0** | **M** | 解决银行充值与信用卡还款导致收支虚增的致命财务硬伤，保证账目真实。 |
| **GAP-04** | **账单导入格式嗅探容错强化与兜底选择** (`SqlDelightImportWorkflow`) | **P0** | **S** | 终结 `.xlsx` / `.xls` 粗暴后缀判定导致的解析崩溃，大幅提升账单导入成功率。 |
| **GAP-05** | **列表便捷手势：明细行左滑快速删除** (`TransactionItemRow` & `TransactionListRow`) | **P0** | **S** | 将高频的误记账删除从繁琐的 3 步弹层缩减为 1 次自然滑动手势。 |
| **GAP-06** | **冷启动新手引导与功能核心亮点透出** (`AppNavigation` & 新手引导页) | **P1** | **M** | 消除新用户首次进入面对空白页面的茫然，显著提升次日留存与导入功能渗透率。 |
| **GAP-07** | **搜索页筛选条件控件现代化（Chip 拾取取代手打文字）** (`SearchScreen`) | **P1** | **S** | 彻底解放用户双手，无需在键盘上手打分类与标签名称，大幅提升检索效率。 |
| **GAP-08** | **统计页「周」与「月」维度补齐趋势柱状图** (`AnalyticsScreen`) | **P1** | **M** | 让用户清楚看出周内各天或月内各旬的消费波动节奏，填补报表断层。 |
| **GAP-09** | **常用支出记账模板（Template）快速入口** (`TransactionEditorScreen`) | **P1** | **M** | 针对早餐、咖啡、通勤等高频重复支出，实现“一键生成”，记账耗时降至 1 秒。 |
| **GAP-10** | **生命周期与应用锁逻辑修复，防止 SAF 误锁** (`OmniFlowApp#AppLockGate`) | **P1** | **S** | 避免用户选完文件返回时被应用锁阻断，修复核心流程的卡顿与打断感。 |
| **GAP-11** | **交易列表多选与批量删除/修改** (`HomeScreen` & `SearchScreen`) | **P1** | **M** | 支持多笔流水批量清理或批量变更账本，告别逐笔记账管理的低效循环。 |
| **GAP-12** | **定时记账与周期事项自动入账** (`ReminderScheduler` & `CreateTransactionUseCase`) | **P1** | **M** | 房租、房贷、固定订阅按时自动写入数据库，无需用户每次手动重复补录。 |
| **GAP-13** | **数据过滤箱（保存常用筛选条件并命名）** (`SearchScreen`) | **P1** | **S** | 一键调出“本月外卖”、“年度大件”等高频筛选报表，无需每次重选条件。 |
| **GAP-14** | **非理性消费与反思标记标签体系** (`Model.kt` & `AnalyticsScreen`) | **P2** | **M** | 帮助用户在记账时沉淀消费情绪，生成冲动消费复盘报告，契合年轻化记账趋势。 |
| **GAP-15** | **桌面小组件（Android AppWidget / Glance）** (`app/widget`) | **P2** | **L** | 桌面直接查看今日支出与剩余预算，提供桌面一键直达记账面板快捷入口。 |
| **GAP-16** | **AI 自然语言快速记一笔（NLP 智能记账）** (`AiCategoryAdapter` 扩展) | **P2** | **L** | 输入或复制一句话自动解析时间、金额、分类并生成账单草稿。 |
| **GAP-17** | **账目地图与消费地点标记** (`core/model` & 地图 SDK) | **P2** | **L** | 满足商旅与旅行用户的足迹记账需求，直观查看地理消费热力分布。 |
| **GAP-18** | **多币种与实时汇率折算** (`Money.kt` 扩展) | **P2** | **L** | 支持外币账本与多币种账户，自动拉取基准汇率折算本位币。 |

---

## 6. 第一轮可落地迭代建议 (Target: Next Release Milestone)

为了确保在总开发工作量高度可控的前提下，迅速消除致命财务与交互硬伤，显著提升商业质感，并顺利发布一个具备强说服力的全新版本（基于当前代码库演进发布新版本），建议首批落地以下 **4 项高价值、低风险改动**：

---

### 改动 1：首页透出月度预算进度与每日建议可花额度
- **涉及模块与文件**：
  - `core/src/main/kotlin/com/omniflow/core/domain/model/Budget.kt`
  - `core/src/main/kotlin/com/omniflow/core/data/facade/SqlDelightHomeFacade.kt`
  - `app/src/main/kotlin/com/omniflow/android/ui/OmniFlowApp.kt` (`HomeScreen`)
- **改动方案与预期效果**：
  1. 在 `Budget.kt` 中增加日均可用额度计算逻辑：
     $$\text{每日建议可用} = \frac{\max(0, \text{月预算} - \text{已支出})}{\text{当月剩余天数（含今天）}}$$
  2. `HomeState` 增加 `budgetProgress: BudgetProgress?` 与 `dailyAllowance: Money?` 字段。
  3. 在 `HomeScreen` 的汇总卡（`SummaryCardRow`）与月历之间，插入一张紧凑的 Material 3 预算进度卡片：
     - 展示：本月预算进度条、剩余可用额度；若已超支，显示红色的已超支警示。
     - 副标题清晰展示：“今日建议可花：¥XX / 剩余 N 天”。
     - 点击卡片直接平滑跳转至 `OmniRoute.MoreBudgets` 详情页。
- **可观察验收标准（Acceptance Criteria）**：
  - [ ] 当账本设置了总预算时，首页在汇总卡下方正确定位并显示预算卡片，进度条百分比与实际支出精确同步。
  - [ ] 当已用金额超出预算时，卡片文本与进度条颜色自动切换为 `expenseColor()`，文本明确显示“本月已超支 ¥XX”。
  - [ ] “今日建议可用”金额计算准确，在每月最后一天与第一天边界切换无除零或越界异常。
  - [ ] 点击预算卡片准确导航至预算管理页；当账本未设置总预算时，优雅回退为“点此设置本月预算”轻量引导条。

---

### 改动 2：记账数字键盘交互重构：集成 `=` 计算求值与备注闭环
- **涉及模块与文件**：
  - `app/src/main/kotlin/com/omniflow/android/ui/TransactionEditorScreen.kt` (`Keypad`, `TransactionEntryPanel`)
  - `app/src/main/kotlin/com/omniflow/android/ui/OmniFlowViewModel.kt` (`pressAmountKey`, `evaluateAmount`)
- **改动方案与预期效果**：
  1. 改造 4x4 键盘布局第 3 行第 4 列：将原单纯的“再记”与计算器逻辑解耦，当输入表达式中包含 `+` 或 `-` 时，右下角的按键文案从「完成」动态变为「=」；按下「=」时仅执行求值，将结果更新在金额屏上，不关闭弹层。
  2. 当表达式为纯数字时，该键恢复为「完成」实心高亮主按钮，点击执行保存入账。
  3. 优化备注输入栏：备注输入框常驻显示于面板中，软键盘弹起时利用 `imePadding()` 整体上移，不再通过 `NoteInputBar` 彻底遮盖键盘，用户输完备注后键盘平滑回落，支持一步点击「完成」保存。
- **可观察验收标准（Acceptance Criteria）**：
  - [ ] 输入 `15+28`，右下角按键实时变为「=」；点击「=」，金额显示为 `43`，按键重新变回「完成」，弹层保持打开。
  - [ ] 输入除法或减法负数时，友好提示“金额必须大于零”，不出现闪退或崩溃。
  - [ ] 点击备注框输入文字时，软键盘优雅弹起，不破坏输入面板各元素可见性；在软键盘上点击键盘自带的 IME 完成即可收起软键盘并保留已选参数。

---

### 改动 3：引入内部转账（Transfer）基础交易类型与双账户余额联动
- **涉及模块与文件**：
  - `core/src/main/sqldelight/com/omniflow/core/db/Transaction.sq` & Migration `5.sqm`
  - `core/src/main/kotlin/com/omniflow/core/domain/model/Model.kt` (`TransactionType`, `Transaction`)
  - `core/src/main/kotlin/com/omniflow/core/data/repository/SqlDelightTransactionRepository.kt`
  - `app/src/main/kotlin/com/omniflow/android/ui/TransactionEditorScreen.kt`
- **改动方案与预期效果**：
  1. `TransactionType` 枚举增加 `TRANSFER`；`Transaction` 表补充 `target_account_id TEXT` 字段。
  2. 记账页顶部切换控件从「支出 / 收入」扩展为「支出 / 收入 / 转账」三段选择。
  3. 选择「转账」时，分类网格隐藏，表单转为“转出账户 -> 转入账户”的双账户拾取器。
  4. 记账提交时，`SqlDelightTransactionRepository` 同一事务中执行：
     - 转出账户扣减余额（`-amount`）；
     - 转入账户增加余额（`+amount`）；
     - 转账交易自动标记 `is_excluded = 1`（默认不计入收支统计，防止虚增月度消费）。
- **可观察验收标准（Acceptance Criteria）**：
  - [ ] 记一笔转账：从“建设银行”转入“微信零钱”1000 元，保存后首页总支出与总收入均不发生变化。
  - [ ] 查看资产页面，“建设银行”余额精确减少 1000 元，“微信零钱”余额精确增加 1000 元，总净资产保持平衡。
  - [ ] 编辑或删除该笔转账记录时，双向账户余额回滚完全对称，无数据残留。

---

### 改动 4：账单导入格式嗅探健壮化与列表左滑删除手势
- **涉及模块与文件**：
  - `core/src/main/kotlin/com/omniflow/core/data/facade/SqlDelightImportWorkflow.kt` (`detectFormat`)
  - `core/src/main/kotlin/com/omniflow/core/parser/spreadsheet/SpreadsheetBillParser.kt`
  - `app/src/main/kotlin/com/omniflow/android/ui/TransactionItemRow.kt` (`TransactionListRow`)
- **改动方案与预期效果**：
  1. **移除按文件扩展名一刀切的脆弱判定**：读取 Excel 文件的前几行表头特征字（如微信必须包含“微信支付账单明细”、建行必须包含“中国建设银行”或“记账日”）；若表头无法明确识别，弹出底部 Sheet 让用户手动点选来源，杜绝直接 throw Error。
  2. **列表左滑删除手势**：使用 Compose 原生 `SwipeToDismissBox` 包装交易明细行（`TransactionListRow`），向左滑动露出红色垃圾桶背景，完全划过时触发单笔删除确认或直接删除带 Snackbar 撤销（Undo）。
- **可观察验收标准（Acceptance Criteria）**：
  - [ ] 选取一个非微信导出的任意 `.xlsx` 格式文件，系统不会直接抛出解析崩溃，而是优雅提示“未能自动识别，请在下方手动指定账单来源”。
  - [ ] 在首页或搜索结果列表中，向左轻滑任意交易行，露出红色删除区域；向左滑满直接触发删除，操作耗时从 3 步降至 1 步。
  - [ ] 删除后底部弹出带“撤销”操作的轻量 Snackbar，点击撤销可瞬间恢复账目与账户余额。

---

## 7. 总结与展望

通过以上系统的对标审计可见：
`OmniFlow` 在**现代原生架构设计、Material 3 视觉完成度、本地数据安全体系（WebDAV + Keystore）、以及多渠道账单导入（含去重与 AI 推断）**等维度，已经展现出远超一般开源作品的强大工业级底座，甚至在银行流水解析和微信/支付宝多渠道整合上超越了标杆产品「青子记账」。

然而，在**日常记账核心动线（计算器求值/备注流转）、财务业务完备性（内部转账）、预算约束透出（首页预算与每日建议）、以及列表基础交互（滑动删除/批量多选）**等商业化细节上，仍存在明显的“工程师视角”断层。

只要在接下来的里程碑中坚定落实第 6 节规划的 4 项核心改动，`OmniFlow` 将迅速填平与青子记账的核心差距，蜕变为一款真正兼具极客底座与商业级丝滑体验的 Android 原生记账标杆应用。


---

## 8. 青子记账 (v8.8.32) 官方真机截图深度视觉与交互对标

> **本节对标依据**：青子记账 iOS 最新官方 App Store 高清真机截图（v8.8.32，2026-11-11 更新，存放于 `/home/hatch/workspace/goals/omni-flow/hidden_files/qingzi-screenshots/`）。  
> **审计方法论**：逐张打开真机图片，逆向拆解其界面骨架、视觉权重、色彩与图标体系、手势与快捷动线；并检索 OmniFlow 现有代码库，以具体文件、类名与函数行号为权威证据，逐项开展全维度商业化差距审计。  
> **范围边界**：严格排除语音记账、账目地图、多币种汇率、存钱计划四项延后功能，仅提炼吸收通用设计语言与工效学手法。

---

### 8.1 十张官方截图逐张深度解读与 OmniFlow 源码对标

#### 截图 1：`01-03_一句话记账.png.jpg` — 一句话 / AI 记账与高频直达入口
- **青子真机界面解读**：
  - **页面架构与信息层级**：顶栏居中为带下拉箭头的「所有账本」作用域切换器，两侧对称布置全局搜索与快速记账「+」；紧随其后是带植物插画的月份选择器（「2026年 10月 v」与翻月箭头）；核心看板为浅底悬浮的三列月度汇总卡（总支出、总收入、净收入，净收入以专属浅绿高亮）；三卡正下方是吸睛的「一句话记账 AI+」输入框卡片（提示文案「晚餐 48 元，水果 26 元」并附带语音快捷按钮）；再下方是横向滑动的常用记账模板胶囊（「早餐 ¥12」、「咖啡 ¥15」、「地铁 ¥6」、「午餐 ¥28」）；接着是二级视图导航切页（「预算 x」、「趋势」、「建议」、「日历」）；卡片最下方预先透出了「十月预算」进度仪表盘。
  - **弹层呼出交互**：点击一句话记账时，自底向上滑出轻量 BottomSheet，顶部可就地切换账本（「日常生活 v」）与扣款账户（「微信零钱 v」），右侧带实心「记账」动作胶囊；中央为手绘便签纸插画，输入框下方标明「自动识别时间、账户、分类和二级分类」，底部调起专用自绘数字与符号键盘。
  - **配色与图标风格**：暖灰纸质底色搭配森林绿主题色，全套手绘彩色插画图标，传达温暖治愈的心理感受。
- **OmniFlow 对应页面与源码证据**：
  - 对应页面：首页 `HomeScreen`（[OmniFlowApp.kt#L841-L945](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/OmniFlowApp.kt#L841-L945)）；
  - AI 基础设施：[AiSettingsPage.kt#L44](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/AiSettingsPage.kt#L44) 与 [CategorySuggester.kt](file:///home/hatch/Repos/omni-flow/core/src/main/kotlin/com/omniflow/core/ai/CategorySuggester.kt)。
- **差距对比（OmniFlow 差在哪）**：
  - **视觉层级与入口缺失**：OmniFlow 首页在汇总卡（`SummaryCardRow`）之后直接展示日历网格（`CalendarMonth`）与交易流水，首屏完全没有高频快捷记账入口。用户想要记账，唯一的触发途径是屏幕右下角的常规 FAB 悬浮按钮。
  - **AI 场景严重受限**：OmniFlow 的 OpenAI 协议客户端目前仅被封装用于账单导入时的批量分类建议（`suggestCategories`），首页和日常记账流程中**完全没有任何自然语言解析一句话记账的功能**。
  - **常用模板（Quick Templates）全无**：青子允许用户将早餐、咖啡、地铁等固定高频支出做成首页一键直达的胶囊 Chip（耗时 1 秒）；OmniFlow 每次记录一杯咖啡都需要完整走完“点 FAB -> 翻页找餐饮 -> 输数字 -> 点完成”的 5 步循环。

---

#### 截图 2：`02-17_记账日历.png.jpg` — 账单日历与同屏明细联动
- **青子真机界面解读**：
  - **页面架构与信息层级**：月历与明细高度集成。顶栏为账本选择与月度步进器；日历主体以清晰的网格呈现 31 天，周表头紧凑居中；**有账目的单元格内均带有圆角气泡胶囊**：支出为淡橘色底黑字（如 `-2247.5`、`-39.5`），大额收入为醒目青绿色胶囊（`+9800`），选中日期（24日）带圆角深色高亮描边框。
  - **下半屏即时联动**：日历下方就地展示「记账明细」分组列表，右侧带「双列卡片 / 单列列表」视图切换开关及更多操作；分组头显示「今日 10/24 周六 v」，右侧显示当日总支出「支出 ¥266.8」；下方直接采用**双列票据卡片流**呈现流水，底部配备悬浮式胶囊 Dock 导航栏。
  - **关键交互元素**：点击日历任意日期，下方明细列表平滑刷新，无需跳转，无需弹层遮挡，浏览心流丝滑连贯。
- **OmniFlow 对应页面与源码证据**：
  - 日历组件：`CalendarMonth`（[OmniFlowApp.kt#L946-L1008](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/OmniFlowApp.kt#L946-L1008)）；
  - 单日明细弹层：`DateDetailSheet`（[OmniFlowApp.kt#L1183-L1262](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/OmniFlowApp.kt#L1183-L1262)）。
- **差距对比（OmniFlow 差在哪）**：
  - **交互动线割裂**：OmniFlow 点击日历中的某一天时，会强行弹出一个几乎全屏的模态弹层 `DateDetailSheet`；用户查看完想看另一天时，必须先下滑关闭弹层，回到首页再点另一天，无法实现青子这种“点日历即时联动下半屏流水”的同屏巡检体验。
  - **日历单元格表现力单薄**：OmniFlow 的日历单元格内仅使用 10sp 的纯文本显示数字（若金额大于 1000 缩写为 `1.2k`），没有底色胶囊气泡衬托；当一日既有收入又有支出时，两行干瘪文本黏连拥挤，远不如青子的彩色气泡清晰美观。
  - **Dock 导航与沉浸感**：青子拥有现代 Floating Dock 导航栏；OmniFlow 采用传统的固定式底栏。

---

#### 截图 3：`03-02_快速记账.png.jpg` — 快速记账面板全要素整合
- **青子真机界面解读**：
  - **页面架构与信息层级**：
    1. **顶部操作行**：关闭「X」、三段式模式选择（「支出 | 收入 | 转账」）、右上角偏好设置齿轮；
    2. **场景化账本横滑栏**：直接放置在收支模式下方（「日常生活」、「家庭开销」、「周末出游」、「养猫日记」），当前账本以黄底胶囊高亮；
    3. **分类宫格区**：4 行 5 列共 20 个大图标网格，全部采用多色手绘插画；当前选中的「日用」带有手绘便签底纸质感；每个分类右下角均有 `...` 徽标提示存在二级子分类；
    4. **关键属性胶囊行**：账户直接外显当前余额（「微信零钱 ¥386.4」）、标签选择器（「标签」）、**计入预算勾选开关**（「计入预算」带绿色对勾圆圈）、更多选项（`...`）；
    5. **录入面板区**：显示组合名称「日用 - 洗衣液」及清除叉号，金额以大号珊瑚红字体（「¥ 28」）居右；下方横滑二级分类 Chip；备注框集成语音与日期快捷入口（「今天」）；
    6. **自绘数字键盘**：4x4 紧凑网格，第四列集成 `+`、`-`、`保存再记`、`完成`。
- **OmniFlow 对应页面与源码证据**：
  - 记账面板：`TransactionEditorScreen`（[TransactionEditorScreen.kt#L102-L211](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/TransactionEditorScreen.kt#L102-L211)）；
  - 属性行：`EditorAttributeRow`（[TransactionEditorScreen.kt#L232-L250](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/TransactionEditorScreen.kt#L232-L250)）；
  - 数字键盘：`Keypad`（[TransactionEditorScreen.kt#L779-L827](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/TransactionEditorScreen.kt#L779-L827)）。
- **差距对比（OmniFlow 差在哪）**：
  - **核心交易类型缺失**：OmniFlow 的 `TransactionModeSwitch`（[TransactionEditorScreen.kt#L222](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/TransactionEditorScreen.kt#L222)）仅支持「支出」与「收入」，**完全没有「转账」（Transfer）**！
  - **账户余额盲区**：OmniFlow 的账户选择按钮仅显示账户名称，不展示该账户当前实时可用余额。用户记账时根本不知道该账户余额够不够扣。
  - **预算控制入口深藏**：OmniFlow 的“不计入统计”被藏在 `moreExpanded` 下拉菜单里（[TransactionEditorScreen.kt#L236](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/TransactionEditorScreen.kt#L236)）；而青子将「计入预算」做成一级常驻的绿色开关胶囊。
  - **账本切换笨重**：OmniFlow 在记账面板最上方使用独立的 `LedgerScopePill` 下拉菜单，独占一整行；青子为紧凑的水平滑动 FilterChip，1 次点击即可跨账本记录。
  - **键盘工效断层**：青子第四列直接提供 `保存再记` 键与 `+`/`-` 键；OmniFlow 键盘的「再记」在第三列，且遇到复合运算（如 `15+28`）时缺少计算中间求值状态。

---

#### 截图 4：`04-04_记账明细.png.jpg` — 票据卡片流与多维元数据高密度展示
- **青子真机界面解读**：
  - **页面架构与信息层级**：顶部为账本选择与月份步进器；页面主体为按天分组的流水列表，日分组头部注明日期、星期与当日支出合计；核心在于采用了**双列票据卡片（Ticket/Receipt Cards）**布局。
  - **卡片微观设计**：
    - 边缘具备拟物小票撕纸打孔剪裁视觉；
    - 左上角：大尺寸彩色手绘分类图标；
    - 右上角：**环形支付渠道品牌徽标（微信支付绿色图标、支付宝蓝色图标、美团黄色图标）** + 粗体负数金额（如 `-¥68`、`-¥45`、`-¥56.6`）；
    - 主标题：清晰表达为「一级分类 · 具体事项或商户名」（如「餐饮 · 和朋友吃火锅」）；
    - 副标题：展示备注小字（如「晚餐 AA，聊聊这一周」）；
    - 属性标签：展示圆角浅绿标签胶囊（如 `#周末放松`、`#家庭生活`、`#毛孩子`）；
    - 底部行：左下角账本小图标，右下角展示精准记账时间（如 `21:05`、`19:50`、`17:20`）。
- **OmniFlow 对应页面与源码证据**：
  - 首页流水：`TransactionGroupCard`（[OmniFlowApp.kt#L916](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/OmniFlowApp.kt#L916)）；
  - 行级渲染：`TransactionRow`（[TransactionItemRow.kt#L38-L98](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/TransactionItemRow.kt#L38-L98)）；
  - 卡片原型：`TransactionTile`（[TransactionItemRow.kt#L102-L161](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/TransactionItemRow.kt#L102-L161)）。
- **差距对比（OmniFlow 差在哪）**：
  - **流水表现形式单一**：OmniFlow 首页仅支持单列列表，不支持切换为双列卡片流；
  - **元数据维度缺失严重**：OmniFlow 的单条交易行只展示了分类图标、分类名、备注和金额，**完全没有展示交易归属的扣款账户（缺少微信/支付宝/银行卡标识），也完全没有在卡片上展示标签（Tags）**！用户在列表中根本无法看出这笔钱是用微信付的还是支付宝付的，也看不出被打上了什么场景标签；
  - **商业温度与生活气息**：青子的卡片像一本充满生活细节的消费手帐；OmniFlow 更像程序员使用的数据库流水 dump，缺乏视觉吸引力。

---

#### 截图 5：`05-09_资产账户.png.jpg` — 净资产大卡与场景化账户分组
- **青子真机界面解读**：
  - **页面架构与信息层级**：
    - **净资产 Hero 卡片**：左上角绿叶与「净资产」大标题，附带「眼睛」开关（点击可隐藏所有敏感数字，进入隐私安全模式），右上角萌系文书插画；中间超大字号「¥43,929.3」；下方双列对照展示「资产 ¥45,578」与「负债 ¥1,648.7」；底部提供三枚快捷胶囊：「置顶」、「资产负债图」、「资产分组 >」；
    - **分组与视图切换控制器**：支持「账户性质」与「自定义分组」双视角切换，右侧提供单列列表与双列（2x2）网格切换开关；
    - **分组折叠卡片**：分组头部包含组名（如「日常钱包」）、包含账户数（`4`）、本组资金小计（「共 ¥9,297.4」）以及折叠展开箭头；
    - **账户卡片元素**：圆角卡片内展示真实银行/渠道品牌图标（支付宝、微信钱包、现金硬币、余额宝、中国银行、建设银行）、账户名、用途说明文字（“日常扫码支付”、“备用现金”、“灵活存取，攒一点零花钱”）、大号余额及进入详情的右箭头。
- **OmniFlow 对应页面与源码证据**：
  - 资产页面：`AssetManagement`（[ManagementScreens.kt#L378-L450](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/ManagementScreens.kt#L378-L450)）；
  - 账户设置：`AccountManagement`（[ManagementScreens.kt#L236-L375](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/ManagementScreens.kt#L236-L375)）。
- **差距对比（OmniFlow 差在哪）**：
  - **导航拓扑层级低**：OmniFlow 将资产深埋在「更多 -> 资产」二级路由下，未作为核心一级财务看板；
  - **隐私防护机制缺失**：净资产卡片没有提供快速隐藏金额的「眼睛」图标，在公共场所极易泄露个人财务隐私；
  - **缺乏场景化自定义分组与小计**：OmniFlow 仅按照余额大于 0 或小于 0，生硬地在内存中过滤为“资产构成”和“负债”（[ManagementScreens.kt#L380-L381](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/ManagementScreens.kt#L380-L381)），不支持用户将账户自定义分组（如“日常零钱”、“投资储蓄”），也没有各分组金额小计与折叠收起能力；
  - **图标与用途描述单调**：OmniFlow 采用统一的单色矢量图标，无国内各大商业银行及第三方支付的专属品牌视觉，也缺少账户备注/用途副标题展示。

---

#### 截图 6：`06-10_资产负债图.png.jpg` — 资产负债桑基流动图
- **青子真机解读**：
  - **页面架构与核心图表**：页面顶部为财务摘要卡片；主体是一张精美的**资产负债桑基流动图（Sankey / Alluvial Diagram）**：
    - **左侧（资金来源与权益）**：总负债（占比 3.62%，¥1,648.7，珊瑚粉色流带）与净资产（占比 96.38%，¥43,929.3，青蓝色流带）；
    - **中间（汇聚节点）**：总资产流动中枢；
    - **右侧（资产形态与分布）**：分流至各账户性质——借记卡（67.31%，¥30,680.6，绿色流带）、投资账户（29.4%，¥13,400，浅蓝流带）、虚拟账户（2.7%，¥1,228.6，黄绿流带）、现金账户（0.59%，¥268.8，浅粉流带）；
    - 图表底部注明财务恒等式：「总资产 - 总负债 = 净资产」；
    - 页面下方紧跟按金额降序排列的资产构成进度条清单。
- **OmniFlow 对应页面与源码证据**：
  - **完全未实现（NOT IMPLEMENTED）**：OmniFlow 在整个代码库中**没有任何资产负债流向图、桑基图或类似的可视化报表**。
- **差距对比（OmniFlow 差在哪）**：
  - **专业财务认知的降维表达**：普通用户很难理解“为什么负债也是资产的一部分”这一会计概念。青子通过一根流动的桑基图，左侧是权益与负债，右侧是资产分布，极为优雅生动地完成了财务科普与数据透视；OmniFlow 仅有单调的数字列表，在图表高级感与深度上存在代差。

---

#### 截图 7：`07-13_分类支出增减.png.jpg` — 分类支出环比增减双向发散图
- **青子真机界面解读**：
  - **页面架构与信息层级**：周、月、年、范围切换；对齐区间标题清晰标明「2026年10月 / 10/1 - 10/24 · 对比 9/1 - 9/24」（自动按照当月已过天数精准对齐上月同期）；
  - **智能文字洞察卡片**：直观提炼结论「比上月同期多花 ¥1,246.7」，并自动生成动因归因「社交和旅行是主要增加来源」，右侧配钱包长出绿芽与上升箭头的温馨插画；
  - **核心图表——双向增减条形图（Divergent Bar Chart）**：
    - 纵轴居中为 0 刻度线，上方标注「少花 <- 0 -> 多花」；
    - 每行对应一个分类：左侧为分类图标、名称、本期金额、上期金额；右侧绘制横向柱形图，若多花向右延伸（珊瑚色，如社交 `+¥500 >`、旅行 `+¥297 >`、餐饮 `+¥176.3 >`），若少花向左延伸（绿色）；右侧箭头支持一键下钻到分类明细；
    - 卡片底部小计：展示本期总支出（¥5,456.4）、上期总支出（¥4,209.7）、较上期变化（+¥1,246.7）。
- **OmniFlow 对应页面与源码证据**：
  - 统计分析主页：`AnalyticsScreen`（[AnalyticsScreen.kt#L78-L171](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/AnalyticsScreen.kt#L78-L171)）；
  - 分析领域模型：`AnalyticsDashboardState`（[Analytics.kt#L86-L95](file:///home/hatch/Repos/omni-flow/core/src/main/kotlin/com/omniflow/core/domain/model/Analytics.kt#L86-L95)）。
- **差距对比（OmniFlow 差在哪）**：
  - **环比（MoM）增减分析完全空白**：OmniFlow 的统计模块仅分析选定时间段内的绝对金额和静态占比，`core` 层虽然有 `previousSummary`，但完全没有按分类计算上期同天数差额的逻辑，更没有“比上期多花或少花”的可视化表达；
  - **缺乏可落地的智能财务归因**：用户记账最想知道的是“这个月为什么花超了”。青子直接用一句话“社交和旅行是主要增加来源”点破原因；OmniFlow 没有任何归因分析，只能靠用户自己逐条翻看流水比对。

---

#### 截图 8：`08-15_存钱计划.png.jpg` — 目标进度条与情感化隐喻（设计手法借鉴）
- **青子真机设计手法解读**：
  - **页面架构**：顶栏「存钱计划」与右上角「心愿单 >」；总进度卡片（存钱罐插画、已存金额、目标总额、双色圆角大进度条与百分比 `42.23%`）；新建计划卡片（自由存钱 / 定期定额）；多计划卡片展示目标物品手绘插画、标签、已存/目标双数值、以及与物品色彩呼应的进度条（如飞机行李箱 64%、电脑 43.08%、应急备用金 41%）。
- **范围排除与设计提炼**：
  - **功能排除**：存钱计划功能本身已明确在本次范围中**严格排除，绝不作为待做功能列入建议**；
  - **可借鉴的通用设计手法**：
    1. **目标型双数值指示（Current / Target）**与进度百分比胶囊的排版手法；
    2. **情感化视觉隐喻**（如安全盾牌、绿芽存钱罐）；
    3. 上述设计手法可直接迁移并大幅优化 OmniFlow 现有的**月度预算管理（Budget）**模块。

---

#### 截图 9：`09-05_分类占比.png.jpg` — 外引线标注饼图与精确层级切换
- **青子真机界面解读**：
  - **页面架构与核心图表**：分类占比卡片右上角内嵌「支出 / 收入」快速切换；
  - **带折线引出线标注的环形图（Donut Chart with Leader Lines）**：
    - 中心区域展示「总支出」文本标题与醒目金额「¥5,456.4」；
    - 莫兰迪低饱和度扇区，扇区之间有微小呼吸缝隙；
    - **最大亮点**：每个关键扇区均有精致的折线引导线延伸至外侧，直接显示「分类名 百分比」（如「住房 44.11%」、「餐饮 28.25%」、「社交 9.16%」、「旅行 5.44%」、「其它 9.58%」），无需用户在图表与图例之间来回比对颜色；
  - **分类明细精细化控制**：
    - 分类明细标题右侧提供「仅一级 / 含二级」快速切换分段开关；
    - 列表项：分类手绘图标、分类名与金额、下方跟随该分类对应色彩的进度条，右侧为**精确到两位小数的百分比（如 `44.11%`）**，底部提供「查看更多 >」。
- **OmniFlow 对应页面与源码证据**：
  - 环形图与列表：`CategoryCard` 与 `CategoryDonut`（[AnalyticsScreen.kt#L454-L535](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/AnalyticsScreen.kt#L454-L535)）。
- **差距对比（OmniFlow 差在哪）**：
  - **饼图可读性差（图例对齐成本高）**：OmniFlow 的 `CategoryDonut` 仅在 Canvas 上画了一个光秃秃的色块圆环，没有任何引出线与文字标注；圆环内部只有数字，没有注明是“总支出”还是“总收入”；
  - **百分比精度不足**：OmniFlow 的分类占比粗暴取整（`(fraction * 100).toInt()}%`，如 44%），缺乏专业财务工具的精密感；
  - **二级分类切换粗糙**：OmniFlow 切换二级分类是在列表底部放了一个「展开二级分类」文本按钮，展开后仅使用两个空格粗暴缩进（`"  ${secondary.categoryName}"`，[AnalyticsScreen.kt#L468](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/AnalyticsScreen.kt#L468)），缺少像青子这样规范的顶部 Segmented Switch。

---

#### 截图 10：`10-07_预算管理.png.jpg` — 环形仪表盘微图表与口径透明度
- **青子真机界面解读**：
  - **页面架构与信息层级**：顶栏标题「预算本」，右上角设置按钮；「预算追踪」支持「10月 | 2026年」跨年月快捷选择；
  - **月度总预算 Hero 卡片**：
    - 左侧：大号圆环进度仪表盘（Donut Gauge），展示剩余百分比「80.24%」；
    - 右侧：清晰的三层数值结构——最醒目的加粗大字「还剩余: ¥1,343.6」、次级「月预算: ¥6,800」、以及**极其关键的口径声明「月支出: ¥5,456.4 (计入预算)」**；
  - **分类预算卡片流（微图表创新）**：
    - 每个分类独占一张独立圆角卡片，右侧带跳转箭头；
    - **分类图标外圈直接套嵌环形进度指示器（Circular Progress Ring）**：指示器圆环填充弧度与色彩（青绿、暖黄、深橙）实时反应预算消耗水位（如学习 32.67% 为绿环，通讯 81.67% 为黄环，餐饮 96.34% 为深橙环）；
    - 卡片右侧内容：分类名紧跟百分比胶囊（「94.35%」），中行显示「10月支出 ¥188.7 / ¥200」，底行给出明确财务结论「剩余预算 ¥11.3」。
- **OmniFlow 对应页面与源码证据**：
  - 预算管理页：`BudgetPage`（[BudgetPage.kt#L45-L88](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/BudgetPage.kt#L45-L88)）；
  - 总预算卡片：`BudgetHeroCard`（[BudgetPage.kt#L119-L150](file:///home/hatch/Repos/omni-flow/app/src/main/kotlin/com/omniflow/android/ui/BudgetPage.kt#L119-L150)）。
- **差距对比（OmniFlow 差在哪）**：
  - **微图表视觉表达落后**：OmniFlow 在总预算与各分类项中，全量使用标准的水平直线进度条（`LinearProgressIndicator`），形式机械单调；青子创造性地将“环形进度圈与分类图标”合二为一，视觉生动且一目了然；
  - **计算口径未声明导致对账困惑**：商业用户经常遇到“某笔转账或大额消费勾选了不计入预算，导致本月实际支出与预算消耗金额不一致”的情况。青子在界面上明确标注 `(计入预算)`，彻底打消了用户对系统算错账的疑虑；OmniFlow 没有任何口径解释；
  - **历史预算回溯缺失**：OmniFlow 无法切换年月查看历史月份的预算执行情况。

---

### 8.2 青子记账核心设计语言提炼与 Android Material 3 落地适配判定

#### 8.2.1 青子设计语言的核心要点提炼
1. **情感化与温度感（Emotional & Welcoming）**：
   - 采用纸质柔和底色、低饱和度莫兰迪色系；
   - 深度融入萌系生活插画（植物幼苗、存钱罐、便签纸、猫咪、咖啡杯），降低财务记账的枯燥感和记账超支的心理压力。
2. **高频场景一步直达（Zero-Friction Entry）**：
   - 首页提供一句话记账与常用消费模板胶囊；
   - 快速记账面板内支持横滑快速切换场景账本（日常生活、家庭开销）；
   - 属性行直接透出扣款账户实时余额，常驻「计入预算」勾选开关；
   - 4x4 数字键盘右下侧直接集成 `保存再记` 与连续计算。
3. **高可读性与自解释图表（Self-Explanatory Visualizations）**：
   - 饼图配备外围引导折线与文字标注，杜绝看色块猜图例；
   - 分类增减采用以 0 为中轴的双向发散条形图，自动生成文字归因；
   - 资产负债采用桑基流动图，直观普及会计恒等式；
   - 分类预算将图标与环形进度指示器结合，打造极具趣味性的微图表。

#### 8.2.2 Android Material 3 落地适配与克制判定（哪些适合、哪些不适合照搬）

| 设计语言要素 | 青子记账表现形式 | Android Material 3 落地判定 | 适配建议与工程/规范理由 |
| :--- | :--- | :---: | :--- |
| **微图表：分类图标套嵌环形进度指示圈** | 分类图标外围绘制自定义 Circular Progress，颜色随进度翻转 | **高度适合，强烈推荐** | 完美契合 M3 的组件组合规范。在 Compose 中通过 `Box` 将 `CategoryIcon` 与自定义 Canvas `drawArc` 叠合，使用 M3 `colorScheme.primary`、`tertiary` 与 `error` 语义色，既精致又完全无第三方库依赖。 |
| **高可读图表：饼图外引折线与文字标注** | 环形图扇区引出折线标注分类名与占比 | **高度适合，强烈推荐** | 解决移动端小屏幕饼图必须依赖图例的行业痛点。Canvas 自绘计算引导线锚点与文字排版，配合 M3 动态色彩，显著提升报表专业度。 |
| **对比图表：双向发散条形图** | 左右发散柱状图展现节约与超支 | **高度适合，强烈推荐** | 契合 M3 数据可视化准则。左侧节约用绿色语义色，右侧多花用支出语义色，结构对称清晰。 |
| **高效键盘：集成计算求值与「保存再记」** | 自绘 4x4 键盘带 `+`/`-`/`保存再记` | **高度适合，强烈推荐** | 纯 Android 原生 Compose 状态驱动，消除多轮打开记账页的摩擦，完全符合工效学。 |
| **账户余额透出与计入预算开关** | FilterChip 内展示实时可用余额与对勾 | **高度适合，强烈推荐** | 直接使用 Compose 原生 `FilterChip` / `InputChip`，消灭二级弹窗，提高信息透明度。 |
| **拟物化小票锯齿边缘与打孔纸质纹理** | 账单卡片采用真实的打孔撕纸边缘与纸张肌理 | **不适合照搬（坚决摒弃）** | **违背 M3 核心规范**。Material 3 强调平面容器（Containers）、表面色调（Surface Tonal Elevation）与几何圆角（Shapes: 12dp/16dp）。拟物撕纸质感属于过时的 iOS 拟物风格，在高分屏 OLED 下极易产生锯齿杂点，且在深色模式（Dark Theme）下极度违和。应采用标准的 M3 `ElevatedCard` 或 `OutlinedCard` 搭配圆角与投影。 |
| **浮动式悬空 Dock 底部导航栏** | 独立于底边的浮动胶囊导航条 | **不适合照搬（需改造）** | 青子的悬空 Dock 侵占纵向展示空间，且在 Android 全屏手势导航下极易与系统返回手势冲突。必须保留 Android 原生 M3 `NavigationBar`，并严格遵守系统导航栏 Insets 避让。 |
| **硬编码固定的莫兰迪色板** | 写死固定的柔和马卡龙/莫兰迪颜色 | **不适合照搬（需改造）** | OmniFlow 深度支持 Material You 动态壁纸取色与 6 套高质感品牌色，且必须支持浅色/深色模式无缝切换。硬编码低饱和度颜色在 Android 深色模式下无法达到 WCAG 2.1 AA 的 4.5:1 对比度标准。所有图表色必须从 M3 调色板语义令牌派生。 |

---

### 8.3 视觉与交互差距改进项矩阵

> **范围排除声明**：依据产品原则，语音记账、账目地图、多币种汇率、存钱计划 4 项功能在此**完全排除**，不列入任何建议。

| 序号 | 对应截图 | 对应 OmniFlow 页面与文件 | 优先级 | 工作量 | 核心改进内容与用户价值 |
| :---: | :--- | :--- | :---: | :---: | :--- |
| **GAP-V01** | `03-02_快速记账` | `TransactionEditorScreen.kt` | **P0** | **M** | **快速记账引入「转账」模式与双账户联动**：顶部切换升级为「支出 / 收入 / 转账」三段，解决资金划转虚增支出的财务硬伤。 |
| **GAP-V02** | `03-02_快速记账` | `TransactionEditorScreen.kt` | **P0** | **S** | **自绘数字键盘集成求值与「保存再记」**：右下角提供 `=` 运算与「保存再记」，支持连续记账与实时计算，消灭误入账。 |
| **GAP-V03** | `03-02_快速记账` | `TransactionEditorScreen.kt` | **P0** | **S** | **记账属性行透出账户余额与计入预算开关**：账户 Chip 直接显示余额（如「微信 ¥386.4」），属性行常驻「计入预算」直选开关。 |
| **GAP-V04** | `01-03`, `10-07` | `HomeScreen.kt` & `BudgetFacade` | **P0** | **M** | **首页透出月度预算进度、口径声明与每日建议可用**：汇总卡下方常驻预算进度条与今日可用额度，标明 `(计入预算)`。 |
| **GAP-V05** | `04-04`, `02-17` | `TransactionItemRow.kt` | **P0** | **S** | **明细流水呈现支付账户徽标与标签，支持左滑删除**：列表中清晰展示微信/支付宝徽标与 `#标签`，明细行支持左滑直接删除。 |
| **GAP-V06** | `10-07_预算管理` | `BudgetPage.kt` | **P1** | **M** | **分类预算引入图标外嵌环形进度仪表盘与百分比徽标**：为分类预算卡片绘制图标环形进度微图表，超支自动变色，直观展示进度。 |
| **GAP-V07** | `09-05_分类占比` | `AnalyticsScreen.kt` | **P1** | **M** | **分类环形饼图增加外引折线标注与两位小数占比**：扇区引出折线标明分类名与占比，中心注明「总支出」，摆脱色块图例查找。 |
| **GAP-V08** | `07-13_分类支出增减` | `AnalyticsScreen.kt` & `AnalyticsFacade` | **P1** | **M** | **统计页新增分类支出环比增减双向发散条形图**：计算当月已过天数与上月同期的差额，以 0 为中轴展现节约与超支柱状图。 |
| **GAP-V09** | `05-09_资产账户` | `ManagementScreens.kt` | **P1** | **M** | **资产看板增加隐私眼睛开关与自定义分组折叠卡片**：净资产支持一键隐藏金额，支持“日常零钱/储蓄”自定义分组与组内小计折叠。 |
| **GAP-V10** | `01-03_一句话记账` | `HomeScreen.kt` | **P1** | **S** | **首页增加常用消费模板横滑胶囊条**：首页在汇总卡下方提供早餐、咖啡等常用记账胶囊，一键生成记录，记账耗时降至 1 秒。 |
| **GAP-V11** | `06-10_资产负债图` | `ManagementScreens.kt` | **P2** | **M** | **资产看板引入资产-负债-净资产流动构成图**：利用 M3 堆叠流带图直观展现“总负债 + 净资产 = 资产构成”的会计恒等式。 |
| **GAP-V12** | `01-03_一句话记账` | `HomeScreen.kt` & `AiCategoryAdapter` | **P2** | **L** | **自然语言一句话快速记多笔（NLP 文本解析）**：利用内置 AI 或规则引擎解析“午餐28元，咖啡15元”，自动拆分为多笔账单草稿。 |

---

### 8.4 第一轮可落地迭代建议修订 (Target: Next Release Milestone)

依据本次青子真机截图在**快速记账工效学、明细卡片信息密度、以及预算视觉透出**等维度的 authoritative 证据，对上一版报告（第 6 节）的第一轮落地建议进行针对性修订：

#### 修订对比与原因说明
1. **原「改动 2（键盘计算器）」升级修订为「快速记账面板全要素提效（GAP-V01, GAP-V02, GAP-V03）」**：
   - *原方案*：仅关注键盘右下角 `=` 求值与备注栏遮挡问题。
   - *修订原因*：青子真机截图 `03-02` 证明，记账录入的灵魂在于“零跳转与信息全透出”。单纯改键盘等号无法根本解决效率问题；必须将**「账户实时可用余额透出」**、**「计入预算直选开关」**以及**「保存再记连续入账」**与键盘求值打包作为一体化改造，才能彻底达到商业记账软件的丝滑体验。
2. **原「改动 4（格式嗅探 + 左滑删除）」解耦修订为「交易明细流水信息密度升级与左滑删除（GAP-V05）」**：
   - *原方案*：将导入格式嗅探 Bug 修复与列表手势删除杂糅在一个改动中。
   - *修订原因*：Excel 扩展名嗅探是纯后台 Parser 容错 Bug，应纳入常规技术维护；而截图 `04-04` 揭示了 OmniFlow 界面上最刺眼的商业短板——**账单流水行里没有支付渠道标识（微信/支付宝）、也没有展示标签**，导致列表干瘪且无法辨识支付来源。因此改动 4 重新聚焦于“交易列表信息丰富度（增加账户图标与标签）与左滑快速删除手势”。
3. **原「改动 1」与「改动 3」保持高优先级，细节按截图对标强化**：
   - 改动 1（首页预算透出）强化增加 `(计入预算)` 统计口径说明；
   - 改动 3（内部转账）严格对齐青子截图 `03-02` 的顶部三段切换规格。

---

#### 修订后第一轮 4 项核心改动完整规格说明

#### 改动 1：首页透出月度预算进度卡、统计口径声明与每日建议可用 (GAP-V04)
- **涉及模块与文件**：
  - `core/src/main/kotlin/com/omniflow/core/domain/model/Budget.kt`
  - `core/src/main/kotlin/com/omniflow/core/data/facade/SqlDelightHomeFacade.kt`
  - `app/src/main/kotlin/com/omniflow/android/ui/OmniFlowApp.kt` (`HomeScreen`)
- **实施方案**：
  1. `core` 层计算：$$\text{每日建议可用} = \frac{\max(0, \text{当月总预算} - \text{计入预算之已支出})}{\text{当月剩余天数（含今天）}}$$
  2. 在 `HomeScreen` 的汇总三卡与月历之间，插入一张紧凑的 Material 3 预算进度卡片：
     - 左侧或顶行展示本月预算消耗进度条，超支时进度条和剩余金额自动转为 `expenseColor()` 并在超支时显示警示；
     - 明确标注统计口径文本：`月支出: ¥XX (计入预算)`，彻底打消对账疑惑；
     - 醒目展示：「今日建议可花: ¥XX / 剩余 N 天」；点击卡片直接跳转至 `OmniRoute.MoreBudgets`。
- **验收标准**：
  - [ ] 账本设置总预算时，首页在汇总卡下方准确定位预算卡，进度百分比与支出实时同步；
  - [ ] 卡片上清晰标明 `(计入预算)`，不计入统计的交易不会虚增预算消耗；
  - [ ] 剩余天数与每日额度在跨月最后一天/第一天无越界或除零异常；
  - [ ] 超支时卡片自动变红并提示已超支额度，未设预算时显示轻量“设置预算”引导条。

---

#### 改动 2：快速记账面板全要素工效重构（键盘求值、保存再记、账户余额与预算直选）(GAP-V02, GAP-V03)
- **涉及模块与文件**：
  - `app/src/main/kotlin/com/omniflow/android/ui/TransactionEditorScreen.kt` (`Keypad`, `EditorAttributeRow`, `TransactionEntryPanel`)
  - `app/src/main/kotlin/com/omniflow/android/ui/OmniFlowViewModel.kt` (`pressAmountKey`, `saveAndRecordAnother`)
- **实施方案**：
  1. **自绘数字键盘升级**：
     - 4x4 键盘第 3 行第 4 列放置「保存再记」，点击后将当前交易异步写入数据库，清空金额与备注，保留已选账本、账户与分类，并触发系统轻量触觉震动反馈；
     - 当金额输入框中包含 `+` 或 `-` 时，右下角「完成」键动态变更为「=」求值键，点击仅计算结果并刷新金额屏，不关闭弹层；当为纯数字时恢复为高亮「完成」保存键。
  2. **账户 Chip 实时透出可用余额**：
     - 在 `EditorAttributeRow` 中，账户 Chip 文本由单一名称升级为 `"${account.name} ${account.balance.asRmb()}"`，用户点选时一目了然。
  3. **常驻「计入预算」直选开关**：
     - 从折叠的“更多”菜单中解放，直接在属性行放置一个带对勾的 `FilterChip`（「计入预算」），默认勾选；取消勾选即标记为 `is_excluded = 1`。
- **验收标准**：
  - [ ] 输入 `25+18`，右下角按键显示为「=」，点击后金额变为 `43`，按键恢复为「完成」；
  - [ ] 点击「保存再记」，数据成功保存入库，金额归零，分类与账户保持不变，可直接输入下一笔；
  - [ ] 账户按钮直接显示账户可用余额；
  - [ ] 点击「计入预算」能即时切换选中态，入账后该交易的 `is_excluded` 字段与勾选态严格一致。

---

#### 改动 3：引入内部转账（Transfer）基础交易类型与双账户余额联动 (GAP-V01)
- **涉及模块与文件**：
  - `core/src/main/sqldelight/com/omniflow/core/db/Transaction.sq` & Migration `5.sqm`
  - `core/src/main/kotlin/com/omniflow/core/domain/model/Model.kt` (`TransactionType.TRANSFER`, `targetAccountId`)
  - `core/src/main/kotlin/com/omniflow/core/data/repository/SqlDelightTransactionRepository.kt`
  - `app/src/main/kotlin/com/omniflow/android/ui/TransactionEditorScreen.kt`
- **实施方案**：
  1. `TransactionType` 枚举扩充 `TRANSFER`；`Transaction` 表增补 `target_account_id TEXT` 字段；
  2. 记账弹层顶部切换控件升级为「支出 | 收入 | 转账」三段选择；
  3. 选择「转账」时，分类宫格自动隐藏，录入区动态变为“转出账户 -> 转入账户”的双账户选择器；
  4. 提交转账时，Repository 在单一事务中执行：转出账户扣减余额（`-amount`），转入账户增加余额（`+amount`）；转账记录默认标记 `is_excluded = 1`，绝不计入当月的收支与预算统计。
- **验收标准**：
  - [ ] 记一笔从“招商银行”转账 2000 元至“微信零钱”，保存后首页当月总支出与总收入均不变；
  - [ ] 资产看板中“招商银行”余额准确减少 2000 元，“微信零钱”准确增加 2000 元，净资产绝对平衡；
  - [ ] 编辑或删除该笔转账记录时，双向账户余额回滚完全对称。

---

#### 改动 4：交易流水信息密度升级（渠道标识/标签）与列表左滑删除手势 (GAP-V05)
- **涉及模块与文件**：
  - `app/src/main/kotlin/com/omniflow/android/ui/TransactionItemRow.kt` (`TransactionRow`, `TransactionTile`)
  - `app/src/main/kotlin/com/omniflow/android/ui/OmniFlowApp.kt` (`TransactionListRow`)
  - `core/src/main/kotlin/com/omniflow/core/domain/model/TransactionListItem`
- **实施方案**：
  1. **元数据展示补全**：
     - `TransactionListItem` 携带账户信息（`accountName`、`accountType`）；
     - 交易行（`TransactionRow`）与卡片（`TransactionTile`）在分类名称右侧或金额左侧增加支付账户渠道标签（如「微信」、「支付宝」、「招行」等轻量徽标或彩色文字），并在备注下方以小号圆角 Chip 渲染关联的 `#标签`；
  2. **列表左滑手势删除**：
     - 使用 Compose 原生 `SwipeToDismissBox` 包装流水行（`TransactionListRow`）；
     - 向左滑动拉出带有删除图标的红色背景，滑满阈值时触发单笔删除，底部弹出带「撤销（Undo）」动作的 Snackbar，点撤销可毫秒级恢复流水与账户余额。
- **验收标准**：
  - [ ] 首页流水列表中，每笔交易能清晰看出扣款账户（如“微信零钱”）及关联的标签；
  - [ ] 列表向左轻滑露出删除警示，滑满自动触发删除，操作步数从 3 步（点详情->点删除->点确认）降至 1 步；
  - [ ] 删除后点击 Snackbar 的“撤销”按钮，数据与账户余额完全恢复。
