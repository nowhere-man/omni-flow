package com.omniflow.core.data.facade

import com.omniflow.core.domain.ai.CategoryOption
import com.omniflow.core.domain.ai.CategorySuggester
import com.omniflow.core.domain.ai.CategorySuggestionEntry
import com.omniflow.core.domain.ai.CategorySuggestionRequest
import com.omniflow.core.domain.facade.ImportWorkflow
import com.omniflow.core.domain.model.Account
import com.omniflow.core.domain.model.AccountType
import com.omniflow.core.domain.model.ImportCategoryBatchEdit
import com.omniflow.core.domain.model.ImportCategoryDefaults
import com.omniflow.core.domain.model.ImportCategoryOrigin
import com.omniflow.core.domain.model.ImportCommitResult
import com.omniflow.core.domain.model.ImportDuplicateStatus
import com.omniflow.core.domain.model.ImportExcludeBatchEdit
import com.omniflow.core.domain.model.ImportPreviewEdit
import com.omniflow.core.domain.model.ImportPreviewItem
import com.omniflow.core.domain.model.ImportPreviewState
import com.omniflow.core.domain.model.ImportPreviewPhase
import com.omniflow.core.domain.model.ImportRequest
import com.omniflow.core.domain.model.ImportSessionId
import com.omniflow.core.domain.model.LedgerId
import com.omniflow.core.domain.model.Rule
import com.omniflow.core.domain.model.RuleActionType
import com.omniflow.core.domain.model.Transaction
import com.omniflow.core.domain.model.TransactionType
import com.omniflow.core.domain.repository.AccountRepository
import com.omniflow.core.domain.repository.CategoryMemoryEntry
import com.omniflow.core.domain.repository.CategoryMemoryRepository
import com.omniflow.core.domain.repository.CategoryRepository
import com.omniflow.core.domain.repository.ImportCommitRepository
import com.omniflow.core.domain.repository.ImportCommitTransaction
import com.omniflow.core.domain.repository.ImportPreviewSession
import com.omniflow.core.domain.repository.ImportSessionRepository
import com.omniflow.core.domain.repository.RuleRepository
import com.omniflow.core.domain.repository.TransactionDedupeRepository
import com.omniflow.core.domain.util.UuidGenerator
import com.omniflow.core.domain.usecase.CreateImportPreviewUseCase
import com.omniflow.core.parser.BillFormatDetector
import com.omniflow.core.parser.ImportFormat
import com.omniflow.core.parser.RawTransaction
import com.omniflow.core.parser.csv.CsvCharset
import com.omniflow.core.parser.csv.CsvDecoder
import com.omniflow.core.parser.csv.CsvBillParser
import com.omniflow.core.parser.qingzi.QingziBillParser
import com.omniflow.core.parser.pdf.BocPdfBillParser
import com.omniflow.core.parser.spreadsheet.SpreadsheetBillParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max

class SqlDelightImportWorkflow(
    private val sessions: ImportSessionRepository,
    private val commits: ImportCommitRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val rules: RuleRepository,
    private val categoryMemory: CategoryMemoryRepository,
    private val dedupe: TransactionDedupeRepository,
    private val suggester: CategorySuggester? = null,
    private val previewFactory: CreateImportPreviewUseCase = CreateImportPreviewUseCase(),
    private val formatDetector: BillFormatDetector = BillFormatDetector(),
    private val csvParser: CsvBillParser = CsvBillParser(),
    private val qingziParser: QingziBillParser = QingziBillParser(),
    private val ids: UuidGenerator = UuidGenerator(),
) : ImportWorkflow {
    override fun preview(request: ImportRequest): Flow<Result<ImportPreviewState>> = flow {
        emit(Result.success(progressState(request, ImportPreviewPhase.DETECTING, 0.1f)))
        emit(runCatching {
            val format = detectFormat(request)
            emit(Result.success(progressState(request, ImportPreviewPhase.PARSING, 0.3f, format)))
            val rawItems = parse(format, request.bytes)
            val sessionId = ids.next()
            emit(Result.success(progressState(request, ImportPreviewPhase.ENRICHING, 0.7f, format)))
            val enriched = enrichPreview(
                previewFactory.fromRaw(sessionId, request.ledgerId, format, rawItems),
            )
            // 只有真的有空缺、且用户配了 AI 时才多走一相；否则整段跳过，不产生网络请求
            val state = if (suggester?.isConfigured() == true && enriched.gaps().isNotEmpty()) {
                emit(Result.success(progressState(request, ImportPreviewPhase.SUGGESTING, 0.85f, format)))
                suggestCategories(enriched)
            } else {
                enriched
            }
            sessions.create(sessionId, request.ledgerId, format, state.items)
            state
        })
    }.flowOn(Dispatchers.Default)

    override fun observe(sessionId: ImportSessionId): Flow<Result<ImportPreviewState>> = flow {
        emit(runCatching { toState(sessions.state(sessionId) ?: error("导入会话不存在或已结束")) })
    }

    override suspend fun editItem(edit: ImportPreviewEdit): Result<ImportPreviewState> = withContext(Dispatchers.Default) {
        runCatching {
            sessions.updateItem(edit.sessionId, edit)
            toState(sessions.state(edit.sessionId) ?: error("导入会话不存在或已结束"))
        }
    }

    override suspend fun editCategories(
        sessionId: ImportSessionId,
        edit: ImportCategoryBatchEdit,
    ): Result<ImportPreviewState> = withContext(Dispatchers.Default) {
        runCatching {
            sessions.updateCategories(sessionId, edit.itemIds, edit.categoryId, edit.type)
            toState(sessions.state(sessionId) ?: error("导入会话不存在或已结束"))
        }
    }

    override suspend fun editSkipped(
        sessionId: ImportSessionId,
        edit: ImportExcludeBatchEdit,
    ): Result<ImportPreviewState> = withContext(Dispatchers.Default) {
        runCatching {
            sessions.updateSkipped(sessionId, edit.itemIds, edit.isSkipped)
            toState(sessions.state(sessionId) ?: error("导入会话不存在或已结束"))
        }
    }

    /** AI 建议失败后重试；没配置 AI 时原样返回当前会话。 */
    override suspend fun resuggest(sessionId: ImportSessionId): Result<ImportPreviewState> =
        withContext(Dispatchers.Default) {
            runCatching {
                val current = toState(sessions.state(sessionId) ?: error("导入会话不存在或已结束"))
                if (suggester?.isConfigured() != true || current.gaps().isEmpty()) return@runCatching current
                val suggested = suggestCategories(current)
                suggested.items
                    .filter { item -> item.categoryOrigin == ImportCategoryOrigin.AI }
                    .groupBy(ImportPreviewItem::categoryId)
                    .forEach { (categoryId, items) ->
                        sessions.updateCategories(sessionId, items.mapTo(mutableSetOf()) { it.id }, categoryId)
                    }
                toState(sessions.state(sessionId) ?: error("导入会话不存在或已结束"))
                    .copy(suggestionError = suggested.suggestionError)
            }
        }

    override suspend fun cancel(sessionId: ImportSessionId): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching { sessions.delete(sessionId) }
    }

    override suspend fun commit(sessionId: ImportSessionId): Result<ImportCommitResult> = withContext(Dispatchers.Default) {
        runCatching {
            val session = sessions.state(sessionId) ?: error("导入会话不存在或已结束")
            val state = toState(session)
            require(state.isReadyToCommit) { "仍有未完成的导入明细" }
            val transactionsToCreate = state.importableItems.map { item ->
            ImportCommitTransaction(
                transaction = Transaction(
                    id = ids.next(),
                    ledgerId = session.ledgerId,
                    accountId = item.accountId ?: error("导入明细缺少账户"),
                    categoryId = item.categoryId ?: error("导入明细缺少分类"),
                    amount = item.raw.amount,
                    type = item.type ?: error("导入明细缺少收支类型"),
                    occurredAt = item.raw.occurredAt,
                    note = item.note,
                    isExcluded = item.isExcluded,
                    source = session.format.transactionSource,
                    externalId = item.raw.externalId,
                ),
                tagNames = item.tags,
            )
        }
            val categoryMemories = state.importableItems
            // AI 判定和用户手改一视同仁：下次同一商户直接走记忆，不再请求模型。
            // 用户在预览页改过的条目 origin 已经是 USER，所以「AI 猜错 → 用户改」记住的是改后的。
            .filter { it.categoryOrigin in REMEMBERED_ORIGINS && it.categoryId != null }
            .map { item ->
                CategoryMemoryEntry(
                    ledgerId = session.ledgerId,
                    memoryKey = memoryKey(item.raw),
                    categoryId = item.categoryId!!,
                )
            }
            commits.commit(sessionId, transactionsToCreate, categoryMemories)
            ImportCommitResult(
                importedCount = transactionsToCreate.size,
                excludedCount = session.items.count(ImportPreviewItem::isSkipped),
            )
        }
    }

    private fun progressState(
        request: ImportRequest,
        phase: ImportPreviewPhase,
        progress: Float,
        format: ImportFormat? = request.selectedFormat,
    ) = ImportPreviewState(
        sessionId = "",
        ledgerId = request.ledgerId,
        format = format,
        items = emptyList(),
        phase = phase,
        progress = progress,
    )

    private suspend fun enrichPreview(initial: ImportPreviewState): ImportPreviewState {
        val activeAccounts = accounts.activeAccounts()
        val activeCategories = categories.activeCategories(initial.ledgerId)
        val activeRules = rules.activeRules(initial.ledgerId)
        val enrichedItems = mutableListOf<ImportPreviewItem>()
        for (item in initial.items) {
            val duplicateStatus = duplicateStatus(initial.ledgerId, item.raw)
            val matchedRule = activeRules.firstOrNull { rule -> ruleMatches(rule, item.raw) }
            val ruleCategory = matchedRule?.takeIf { it.actionType == RuleActionType.SET_CATEGORY }?.actionValue
            val memoryCategory = if (matchedRule == null) {
                categoryMemory.categoryId(initial.ledgerId, memoryKey(item.raw))
            } else {
                null
            }
            // 「不计收支 / 中性交易」默认不导入：余额宝转出、信用卡还款、零钱提现这类占比很高
            // （支付宝样例 107 条里 37 条），逐条要求选收支类型只会把「确认入账」堵死。
            // 兜底给一个收支类型，用户从「不计收支」分组里加回来时才不会又卡在 requiresTypeSelection。
            val isNeutral = item.raw.type == null && item.raw.isExcluded
            val resolvedType = item.type ?: if (isNeutral) TransactionType.EXPENSE else null
            val selectedCategory = (ruleCategory ?: memoryCategory)
                ?.takeIf { candidate -> activeCategories.any { it.id == candidate && it.type == resolvedType } }
                ?: ImportCategoryDefaults.defaultCategoryId(item.raw, resolvedType, activeCategories)
            enrichedItems += item.copy(
                type = resolvedType,
                accountId = resolveAccount(item.raw, activeAccounts),
                categoryId = selectedCategory,
                categoryOrigin = when {
                    selectedCategory == null -> ImportCategoryOrigin.NONE
                    ruleCategory != null -> ImportCategoryOrigin.RULE
                    memoryCategory != null -> ImportCategoryOrigin.MEMORY
                    else -> ImportCategoryOrigin.NONE
                },
                isExcluded = item.isExcluded || matchedRule?.actionType == RuleActionType.SET_EXCLUDED,
                isSkipped = matchedRule?.actionType == RuleActionType.EXCLUDE ||
                    duplicateStatus != ImportDuplicateStatus.NONE ||
                    isNeutral,
                duplicateStatus = duplicateStatus,
            )
        }
        return initial.copy(items = enrichedItems)
    }

    /** 规则、记忆、内置映射都没填上分类的条目——只有这些才需要问 AI。 */
    private fun ImportPreviewState.gaps(): List<ImportPreviewItem> = items.filter {
        !it.isSkipped && it.categoryId == null && it.type != null
    }

    /**
     * 按 [memoryKey] 去重后问 AI：一个商户消费十笔也只问一次，
     * 请求量和「确认导入时写入分类记忆」的粒度天然一致。
     * 任何失败都只落到 [ImportPreviewState.suggestionError]，条目保持待分类，导入照常进行。
     */
    private suspend fun suggestCategories(state: ImportPreviewState): ImportPreviewState {
        val suggester = this.suggester ?: return state
        val gaps = state.gaps()
        val byKey = gaps.groupBy { memoryKey(it.raw) }
        val truncated = (byKey.size - MAX_SUGGESTION_KEYS).coerceAtLeast(0)
        val keys = byKey.keys.take(MAX_SUGGESTION_KEYS)
        val activeCategories = categories.activeCategories(state.ledgerId)
        val optionsOf = { type: TransactionType ->
            activeCategories.filter { it.parentId == null && it.type == type }
                .map { CategoryOption(it.id, it.name) }
        }
        val expenseOptions = optionsOf(TransactionType.EXPENSE)
        val incomeOptions = optionsOf(TransactionType.INCOME)
        if (expenseOptions.isEmpty() && incomeOptions.isEmpty()) return state

        val entries = keys.mapNotNull { key ->
            val sample = byKey.getValue(key).first()
            val type = sample.type ?: return@mapNotNull null
            CategorySuggestionEntry(
                key = key,
                type = type,
                counterparty = sample.raw.counterparty,
                description = sample.note ?: sample.raw.note,
                sourceCategory = sample.raw.sourceCategory,
                amount = sample.raw.amount,
            )
        }
        val result = suggester.suggest(
            CategorySuggestionRequest(entries, expenseOptions, incomeOptions),
        )
        val suggestions = result.getOrElse { error ->
            return state.copy(suggestionError = "AI 建议失败：${error.message ?: "未知错误"}")
        }
        if (suggestions.isEmpty() && truncated == 0) return state

        val updated = state.items.map { item ->
            if (item !in gaps) return@map item
            val categoryId = suggestions[memoryKey(item.raw)] ?: return@map item
            // 分类必须属于本账本且收支方向对得上，模型给的名字已经过白名单，这里再挡一次 id
            val valid = activeCategories.any { it.id == categoryId && it.type == item.type }
            if (valid) item.copy(categoryId = categoryId, categoryOrigin = ImportCategoryOrigin.AI) else item
        }
        return state.copy(
            items = updated,
            suggestionError = if (truncated > 0) "条目过多，有 $truncated 类未请求 AI，请手动归类" else null,
        )
    }

    private suspend fun duplicateStatus(ledgerId: LedgerId, raw: RawTransaction): ImportDuplicateStatus {
        val source = raw.format.transactionSource?.name
        if (source != null && raw.externalId != null && dedupe.hasExternalId(source, raw.externalId)) {
            return ImportDuplicateStatus.CONFIRMED
        }
        val instant = raw.occurredAt.toEpochMilliseconds()
        val (start, end) = if (raw.format == ImportFormat.CCB) {
            val startOfDay = raw.occurredAt.toLocalDateTime(CHINA_TIME_ZONE).date
                .atStartOfDayIn(CHINA_TIME_ZONE)
                .toEpochMilliseconds()
            startOfDay to startOfDay + DAY_MILLISECONDS
        } else {
            max(0L, instant - TWO_HOURS_MILLISECONDS) to instant + TWO_HOURS_MILLISECONDS + 1
        }
        return if (dedupe.likelyDuplicate(
                ledgerId = ledgerId,
                amount = raw.amount,
                occurredAtStart = start,
                occurredAtEnd = end,
                note = raw.note,
            )
        ) {
            ImportDuplicateStatus.SUSPECTED
        } else {
            ImportDuplicateStatus.NONE
        }
    }

    private fun resolveAccount(raw: RawTransaction, activeAccounts: List<Account>): String? = when {
        raw.format == ImportFormat.QINGZI -> activeAccounts.firstOrNull { it.type == AccountType.CASH }?.id
        else -> activeAccounts.firstOrNull { it.name == raw.accountName?.trim() }?.id
            ?: activeAccounts.firstOrNull { it.type == AccountType.CASH }?.id
    }

    private fun detectFormat(request: ImportRequest): ImportFormat {
        request.selectedFormat?.let { return it }
        val fileName = request.fileName.lowercase()
        if (fileName.endsWith(".xlsx")) return ImportFormat.WECHAT
        if (fileName.endsWith(".xls")) return ImportFormat.CCB
        if (fileName.endsWith(".pdf")) {
            require(BocPdfBillParser.matches(request.bytes)) { "暂不支持这个 PDF 账单，目前只识别中国银行交易流水明细清单" }
            return ImportFormat.BOC
        }

        val candidates = listOf(CsvCharset.UTF8, CsvCharset.GB18030)
            .flatMap { charset -> formatDetector.detect(request.fileName, CsvDecoder.decode(request.bytes, charset)) }
            .distinct()
        return candidates.singleOrNull()
            ?: error("无法唯一识别账单来源，请选择导入来源")
    }

    private fun parse(format: ImportFormat, bytes: ByteArray): List<RawTransaction> = when (format) {
        ImportFormat.ALIPAY -> csvParser.parse(format, CsvDecoder.decode(bytes, CsvCharset.GB18030)).getOrThrow()
        ImportFormat.JD, ImportFormat.MEITUAN -> csvParser.parse(format, CsvDecoder.decode(bytes, CsvCharset.UTF8)).getOrThrow()
        ImportFormat.QINGZI -> qingziParser.parse(CsvDecoder.decode(bytes, CsvCharset.UTF8)).getOrThrow().transactions
        ImportFormat.WECHAT, ImportFormat.CCB -> SpreadsheetBillParser.parse(format, bytes).getOrThrow()
        ImportFormat.BOC -> BocPdfBillParser.parse(bytes).getOrThrow()
    }

    private fun toState(session: ImportPreviewSession): ImportPreviewState = ImportPreviewState(
        sessionId = session.id,
        ledgerId = session.ledgerId,
        format = session.format,
        items = session.items,
    )

    private fun ruleMatches(rule: Rule, raw: RawTransaction): Boolean = when (rule.conditionType.name) {
        "NOTE_CONTAINS" -> raw.note?.contains(rule.conditionValue, ignoreCase = true) == true
        "TRANSACTION_TYPE" -> raw.type?.name == rule.conditionValue
        "TRANSACTION_SOURCE" -> raw.format.transactionSource?.name == rule.conditionValue
        else -> false
    }

    /**
     * 分类记忆的键。以前用完整备注，而备注是「交易对方 | 商品说明 | 备注」拼出来的，
     * 几乎条条唯一，记忆基本命中不了。改成优先用来源分类（支付宝/京东的「交易分类」），
     * 其次用交易对方（微信/美团/建行），都没有才退回备注。
     */
    private fun memoryKey(raw: RawTransaction): String {
        val source = raw.format.transactionSource?.name.orEmpty()
        raw.sourceCategory?.trim()?.takeIf(String::isNotEmpty)?.let { return "$source:cat:${normalize(it)}" }
        raw.counterparty?.trim()?.takeIf(String::isNotEmpty)?.let { return "$source:party:${normalize(it)}" }
        return "$source:note:${normalize(raw.note)}"
    }

    private fun normalize(value: String?): String = value.orEmpty().lowercase().filterNot(Char::isWhitespace)

    private companion object {
        /** 一次导入最多问这么多个来源键，挡住超大账单把请求量和费用打上去。 */
        const val MAX_SUGGESTION_KEYS = 200
        val REMEMBERED_ORIGINS = setOf(ImportCategoryOrigin.USER, ImportCategoryOrigin.AI)
        const val TWO_HOURS_MILLISECONDS = 2 * 60 * 60 * 1000L
        const val DAY_MILLISECONDS = 24 * 60 * 60 * 1000L
        val CHINA_TIME_ZONE: TimeZone = TimeZone.of("Asia/Shanghai")
    }
}
