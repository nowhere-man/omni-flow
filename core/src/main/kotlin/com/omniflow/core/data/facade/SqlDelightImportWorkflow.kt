package com.omniflow.core.data.facade

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
            val state = enrichPreview(
                previewFactory.fromRaw(sessionId, request.ledgerId, format, rawItems),
            )
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
            sessions.updateCategories(sessionId, edit.itemIds, edit.categoryId)
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
            .filter { it.categoryOrigin == ImportCategoryOrigin.USER && it.categoryId != null }
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
        const val TWO_HOURS_MILLISECONDS = 2 * 60 * 60 * 1000L
        const val DAY_MILLISECONDS = 24 * 60 * 60 * 1000L
        val CHINA_TIME_ZONE: TimeZone = TimeZone.of("Asia/Shanghai")
    }
}
