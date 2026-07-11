import SwiftUI

struct AnalyticsView: View {
    @EnvironmentObject private var store: AppStore
    @State private var mode = RangeMode.month
    @State private var anchor = Date()
    @State private var customStart = Date()
    @State private var customEnd = Date()
    @State private var showIncome = true
    @State private var showExpense = true
    @State private var selectedPointLabel: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                Text("统计").font(.largeTitle.bold())
                HStack {
                    Picker("账本", selection: Binding(get: { store.analyticsLedgerID }, set: store.selectAnalyticsLedger)) {
                        Text("所有账本").tag(String?.none)
                        ForEach(store.ledgers) { Text($0.name).tag(Optional($0.id)) }
                    }.pickerStyle(.menu)
                    Picker("范围", selection: $mode) {
                        ForEach(RangeMode.allCases) { Text($0.label).tag($0) }
                    }.pickerStyle(.segmented)
                }
                if mode == .custom {
                    HStack {
                        DatePicker("开始", selection: $customStart, displayedComponents: .date)
                        DatePicker("结束", selection: $customEnd, displayedComponents: .date)
                        Button("应用", action: refresh)
                    }
                } else {
                    HStack {
                        Button { shift(-1) } label: { Image(systemName: "chevron.left") }
                        Spacer(); Text(range.start.formatted(date: .abbreviated, time: .omitted)); Spacer()
                        Button { shift(1) } label: { Image(systemName: "chevron.right") }
                    }
                }
                HStack(spacing: 12) {
                    SummaryCard(title: "总支出", value: store.analyticsExpenseMinor.rmb)
                    SummaryCard(title: "总收入", value: store.analyticsIncomeMinor.rmb)
                    SummaryCard(title: "总结余", value: (store.analyticsIncomeMinor - store.analyticsExpenseMinor).rmb)
                }
                Button("查看年度账单") { store.loadAnalyticsStatement(year: Calendar.current.component(.year, from: anchor)) }
                VStack(alignment: .leading, spacing: 10) {
                    Picker("排行榜", selection: $store.analyticsRankingType) {
                        ForEach(EntryType.allCases) { Text($0.label).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    HStack {
                        Picker("分类收支", selection: $store.analyticsCategoryType) {
                            ForEach(EntryType.allCases) { Text($0.label).tag($0) }
                        }
                        Picker("分类层级", selection: $store.analyticsCategoryGranularity) {
                            ForEach(AppleCategoryGranularity.allCases) { Text($0.label).tag($0) }
                        }
                    }
                }
                Group {
                    AnalyticsCard(title: "收支趋势") {
                        HStack {
                            Toggle("收入", isOn: $showIncome).toggleStyle(.button)
                            Toggle("支出", isOn: $showExpense).toggleStyle(.button)
                        }
                        let maximum = max(store.analyticsPoints.flatMap { [$0.income, $0.expense] }.max() ?? 1, 1)
                        ForEach(store.analyticsPoints) { point in
                            Button { selectedPointLabel = point.label } label: {
                                VStack(alignment: .leading, spacing: 6) {
                                    Text(point.label).font(.caption)
                                    if showIncome { AmountBar(label: "收入", value: point.income, maximum: maximum) }
                                    if showExpense { AmountBar(label: "支出", value: point.expense, maximum: maximum) }
                                }
                                .padding(8)
                                .background(selectedPointLabel == point.label ? Color.accentColor.opacity(0.15) : Color.clear, in: RoundedRectangle(cornerRadius: 10))
                            }
                            .buttonStyle(.plain)
                        }
                        if let point = store.analyticsPoints.first(where: { $0.label == selectedPointLabel }) {
                            Text("已选 \(point.label)：收入 \(point.income.rmb) · 支出 \(point.expense.rmb)")
                        }
                    }
                    AnalyticsCard(title: "同环比") {
                        Text("环比支出变化 \(store.analyticsPreviousExpenseChange.rmb)")
                        Text("环比收入变化 \(store.analyticsPreviousIncomeChange.rmb)")
                        Text("环比结余变化 \(store.analyticsPreviousNetChange.rmb)")
                        Text("同比支出变化 \(store.analyticsYearExpenseChange.rmb)")
                        Text("同比收入变化 \(store.analyticsYearIncomeChange.rmb)")
                        Text("同比结余变化 \(store.analyticsYearNetChange.rmb)")
                    }
                    AnalyticsCard(title: "收支排行榜") {
                        ForEach(store.analyticsRanking) { item in
                            Button { store.editTransaction(item) } label: {
                                HStack { Text(item.categoryName); Spacer(); Text(item.amountMinor.rmb) }
                            }
                            .buttonStyle(.plain)
                            Divider()
                        }
                    }
                    AnalyticsCard(title: "分类占比") {
                        if store.analyticsPrimaryCategoryID != nil {
                            Button("返回一级分类") {
                                store.analyticsPrimaryCategoryID = nil
                                store.analyticsCategoryGranularity = .primary
                            }
                        }
                        let total = max(store.analyticsCategories.map(\.amount).reduce(0, +), 1)
                        ForEach(store.analyticsCategories) { item in
                            if store.analyticsCategoryGranularity == .primary {
                                Button {
                                    store.analyticsPrimaryCategoryID = item.id
                                    store.analyticsCategoryGranularity = .secondary
                                } label: {
                                    AmountBar(label: item.name, value: item.amount, maximum: total)
                                }
                                .buttonStyle(.plain)
                            } else {
                                AmountBar(label: item.name, value: item.amount, maximum: total)
                            }
                        }
                    }
                }
                Group {
                    AnalyticsCard(title: "标签分析") {
                        ForEach(store.analyticsTags) { tag in
                            HStack { Text(tag.name); Spacer(); Text("支出 \(tag.expense.rmb) · 收入 \(tag.income.rmb)") }
                        }
                    }
                    AnalyticsCard(title: "账户资产") {
                        let summary = store.analyticsAccountSummary
                        Text("净资产 \((summary.assets - summary.liabilities).rmb) · 资产 \(summary.assets.rmb) · 负债 \(summary.liabilities.rmb)")
                        ForEach(store.analyticsAccountAssets) { account in
                            HStack { Text(account.name); Spacer(); Text(account.balance.rmb) }
                        }
                    }
                }
            }
            .padding()
        }
        .navigationTitle("统计")
        .onAppear(perform: refresh)
        .onChange(of: mode) { _ in refresh() }
        .onChange(of: store.analyticsLedgerID) { _ in refresh() }
        .onChange(of: store.analyticsRankingType) { _ in refresh() }
        .onChange(of: store.analyticsCategoryType) { _ in store.analyticsPrimaryCategoryID = nil; refresh() }
        .onChange(of: store.analyticsCategoryGranularity) { value in
            if value == .primary { store.analyticsPrimaryCategoryID = nil }
            refresh()
        }
        .sheet(item: $store.analyticsStatement) { StatementTableView(table: $0) }
    }

    private var range: DateInterval {
        let calendar = Calendar.current
        switch mode {
        case .week: return calendar.dateInterval(of: .weekOfYear, for: anchor) ?? DateInterval(start: anchor, duration: 7 * 86_400)
        case .month: return calendar.dateInterval(of: .month, for: anchor) ?? DateInterval(start: anchor, duration: 30 * 86_400)
        case .year: return calendar.dateInterval(of: .year, for: anchor) ?? DateInterval(start: anchor, duration: 365 * 86_400)
        case .custom:
            let start = min(customStart, customEnd)
            return DateInterval(start: start, end: Calendar.current.date(byAdding: .day, value: 1, to: max(customStart, customEnd)) ?? max(customStart, customEnd))
        }
    }

    private func shift(_ value: Int) {
        let component: Calendar.Component = mode == .week ? .weekOfYear : mode == .year ? .year : .month
        anchor = Calendar.current.date(byAdding: component, value: value, to: anchor) ?? anchor
        refresh()
    }

    private func refresh() { store.observeAnalytics(start: range.start, end: range.end, ledgerID: store.analyticsLedgerID) }
}

private struct StatementTableView: View {
    let table: StatementTableUI

    var body: some View {
        NavigationStack {
            List {
                Section("全年") {
                    statementRow("合计", income: table.incomeMinor, expense: table.expenseMinor)
                }
                Section("月份") {
                    ForEach(table.months) { month in
                        statementRow("\(month.month) 月", income: month.incomeMinor, expense: month.expenseMinor)
                    }
                }
            }
            .navigationTitle("\(table.year) 年账单")
        }
        .frame(minWidth: 420, minHeight: 520)
    }

    private func statementRow(_ label: String, income: Int64, expense: Int64) -> some View {
        HStack {
            Text(label)
            Spacer()
            Text(income.rmb).foregroundStyle(.green)
            Text(expense.rmb).foregroundStyle(.red)
            Text((income - expense).rmb)
        }
    }
}

private enum RangeMode: String, CaseIterable, Identifiable {
    case week, month, year, custom
    var id: String { rawValue }
    var label: String { switch self { case .week: return "周"; case .month: return "月"; case .year: return "年"; case .custom: return "范围" } }
}

private struct AnalyticsCard<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title).font(.title2.bold())
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 18))
    }
}

private struct AmountBar: View {
    let label: String
    let value: Int64
    let maximum: Int64

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack { Text(label); Spacer(); Text(value.rmb) }
            GeometryReader { proxy in
                Capsule().fill(.quaternary)
                    .overlay(alignment: .leading) {
                        Capsule().fill(.tint).frame(width: proxy.size.width * CGFloat(value) / CGFloat(maximum))
                    }
            }
            .frame(height: 8)
        }
    }
}
