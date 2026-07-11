import Foundation

struct LedgerUI: Identifiable, Hashable {
    let id: String
    var name: String
    var coverKey: String?
}

struct AccountUI: Identifiable, Hashable {
    let id: String
    var name: String
    var balanceMinor: Int64
    var type: String
    var iconKey: String
    var cardNumber: String?
    var note: String?
    var includeInTotalAssets: Bool
}

let bundledIconKeys = [
    "banknote", "wallet-cards", "wallet", "landmark", "shopping-bag", "utensils",
    "bus", "wrench", "film", "heart-pulse", "plane", "car", "house", "smartphone",
    "shirt", "chart-line", "briefcase-business", "trophy", "gift", "play", "category",
]

struct CategoryUI: Identifiable, Hashable {
    let id: String
    var name: String
    var type: EntryType
    var parentID: String?
    var iconKey: String?
}

struct TagUI: Identifiable, Hashable { let id: String; var name: String }
struct RuleUI: Identifiable, Hashable {
    let id: String
    var name: String
    var conditionType: String
    var conditionValue: String
    var actionType: String
    var actionValue: String
    var priority: Int
}
struct ReminderUI: Identifiable, Hashable {
    let id: String
    var name: String
    var type: String
    var amountMinor: Int64?
    var scheduleKind: String
    var paused: Bool
    var dayOfMonth: Int?
    var daysAfter: Int?
    var dayOfWeek: Int?
    var month: Int?
}
struct ImportItemUI: Identifiable, Hashable {
    let id: String
    var note: String
    var amountMinor: Int64
    var date: Date
    var source: String
    var type: EntryType?
    var categoryID: String?
    var accountID: String?
    var tags: [String]
    var excluded: Bool
    var skipped: Bool
    var duplicate: String
}
struct CalendarDayUI: Identifiable, Hashable {
    let id: String
    var date: Date
    var expenseMinor: Int64
    var incomeMinor: Int64
}
struct AnalyticsPointUI: Identifiable, Hashable { let id = UUID(); var label: String; var expense: Int64; var income: Int64 }
struct CategoryShareUI: Identifiable, Hashable { let id: String; var name: String; var amount: Int64 }
struct TagSummaryUI: Identifiable, Hashable { let id: String; var name: String; var expense: Int64; var income: Int64 }
struct AccountAssetUI: Identifiable, Hashable { let id: String; var name: String; var balance: Int64 }
struct BackupUI: Identifiable, Hashable { let id: String; var createdAt: String }
struct StatementMonthUI: Identifiable, Hashable {
    var month: Int
    var expenseMinor: Int64
    var incomeMinor: Int64
    var id: Int { month }
}
struct StatementTableUI: Identifiable, Hashable {
    var year: Int
    var months: [StatementMonthUI]
    var expenseMinor: Int64
    var incomeMinor: Int64
    var id: Int { year }
}

struct TransactionUI: Identifiable, Hashable {
    let id: String
    var ledgerID: String
    var ledgerName: String
    var accountID: String
    var accountName: String
    var categoryID: String
    var categoryName: String
    var categoryIconKey: String?
    var amountMinor: Int64
    var type: EntryType
    var date: Date
    var note: String
    var excluded: Bool
}

enum TransactionDisplayMode: String, CaseIterable, Identifiable {
    case list = "LIST"
    case card = "CARD"

    var id: String { rawValue }
    var label: String { self == .list ? "列表" : "卡片" }
}

enum AppleImportFormat: String, CaseIterable, Identifiable {
    case alipay = "ALIPAY"
    case wechat = "WECHAT"
    case jd = "JD"
    case meituan = "MEITUAN"
    case ccb = "CCB"
    case qingzi = "QINGZI"

    var id: String { rawValue }
    var label: String {
        switch self {
        case .alipay: return "支付宝"
        case .wechat: return "微信"
        case .jd: return "京东"
        case .meituan: return "美团"
        case .ccb: return "建设银行"
        case .qingzi: return "青子记账"
        }
    }
}

enum AppleCategoryGranularity: String, CaseIterable, Identifiable {
    case primary = "PRIMARY"
    case secondary = "SECONDARY"
    var id: String { rawValue }
    var label: String { self == .primary ? "一级分类" : "二级分类" }
}

enum EntryType: String, CaseIterable, Identifiable {
    case expense = "EXPENSE"
    case income = "INCOME"

    var id: String { rawValue }
    var label: String { self == .expense ? "支出" : "收入" }
}

enum MainDestination: String, CaseIterable, Identifiable {
    case home, analytics, transaction, search, more

    var id: String { rawValue }
    var title: String {
        switch self {
        case .home: return "首页"
        case .analytics: return "统计"
        case .transaction: return "记账"
        case .search: return "搜索"
        case .more: return "更多"
        }
    }
    var systemImage: String {
        switch self {
        case .home: return "house"
        case .analytics: return "chart.bar"
        case .transaction: return "plus.circle"
        case .search: return "magnifyingglass"
        case .more: return "ellipsis.circle"
        }
    }
}

extension Int64 {
    var rmb: String {
        let value = Decimal(self) / 100
        return value.formatted(.currency(code: "CNY"))
    }
}
