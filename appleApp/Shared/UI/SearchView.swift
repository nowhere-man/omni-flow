import SwiftUI

struct SearchView: View {
    @EnvironmentObject private var store: AppStore
    @State private var advanced = false
    @State private var hasSearched = false

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                TextField("备注、分类、账户或标签", text: $store.searchText)
                    .textFieldStyle(.roundedBorder)
                    .onSubmit { store.search(); hasSearched = true }
                Button { store.search(); hasSearched = true } label: { Image(systemName: "magnifyingglass") }
                    .buttonStyle(.borderedProminent)
                    .accessibilityLabel("搜索")
                Button(action: clear) { Image(systemName: "xmark") }
                    .buttonStyle(.bordered)
                    .accessibilityLabel("清除")
            }
            .padding(.horizontal)
            .padding(.top, 10)
            DisclosureGroup(isExpanded: $advanced) {
                VStack(spacing: 8) {
                    HStack(spacing: 8) {
                    Picker("账本", selection: Binding(get: { store.searchLedgerID }, set: store.setSearchLedger)) {
                        Text("所有账本").tag(String?.none)
                        ForEach(store.ledgers) { Text($0.name).tag(Optional($0.id)) }
                    }
                    .frame(maxWidth: .infinity)
                    Picker("类型", selection: $store.searchType) {
                        Text("不限").tag(EntryType?.none)
                        ForEach(EntryType.allCases) { Text($0.label).tag(Optional($0)) }
                    }
                    .frame(maxWidth: .infinity)
                    }
                    HStack(spacing: 8) {
                    Picker("一级分类", selection: $store.searchPrimaryCategoryID) {
                        Text("不限").tag(String?.none)
                        ForEach(store.searchCategories.filter { $0.parentID == nil }) { Text($0.name).tag(Optional($0.id)) }
                    }
                    .frame(maxWidth: .infinity)
                    Picker("二级分类", selection: $store.searchSecondaryCategoryID) {
                        Text("不限").tag(String?.none)
                        ForEach(store.searchCategories.filter { $0.parentID == store.searchPrimaryCategoryID }) { Text($0.name).tag(Optional($0.id)) }
                    }
                    .frame(maxWidth: .infinity)
                    }
                    HStack(spacing: 8) {
                    Picker("标签", selection: $store.searchTagID) {
                        Text("不限").tag(String?.none)
                        ForEach(store.searchTags) { Text($0.name).tag(Optional($0.id)) }
                    }
                    .frame(maxWidth: .infinity)
                    Picker("账户", selection: $store.searchAccountID) {
                        Text("不限").tag(String?.none)
                        ForEach(store.accounts) { Text($0.name).tag(Optional($0.id)) }
                    }
                    .frame(maxWidth: .infinity)
                    }
                    HStack(spacing: 8) {
                        TextField("精确金额", text: $store.searchExact)
                        TextField("最小金额", text: $store.searchMinimum)
                        TextField("最大金额", text: $store.searchMaximum)
                    }
                    .textFieldStyle(.roundedBorder)
                    Toggle("日期范围", isOn: $store.searchDateEnabled)
                    if store.searchDateEnabled {
                        HStack(spacing: 8) {
                            DatePicker("开始", selection: $store.searchStartDate, displayedComponents: .date)
                            DatePicker("结束", selection: $store.searchEndDate, displayedComponents: .date)
                        }
                    }
                    HStack {
                        Spacer()
                        Button("应用筛选") { store.search(); hasSearched = true }.buttonStyle(.borderedProminent)
                        Button("重置", action: clear).buttonStyle(.bordered)
                    }
                }
                .padding(10)
                .background(.quaternary, in: RoundedRectangle(cornerRadius: 12))
            } label: {
                Label("筛选", systemImage: "slider.horizontal.3")
                    .font(.subheadline.weight(.semibold))
            }
            .padding(10)
            if store.searchResults.isEmpty {
                EmptyStateView(
                    title: hasSearched ? "没有搜索结果" : "还没有可搜索的交易",
                    systemImage: "magnifyingglass",
                    detail: hasSearched ? "调整筛选条件后再试" : "新增交易后，可按备注、分类或账户检索",
                    actionTitle: hasSearched ? "清除筛选" : "新增交易"
                ) {
                    if hasSearched { clear() } else { store.startNewTransaction() }
                }
            } else {
                HStack(spacing: 12) {
                    SummaryCard(title: "支出", value: store.searchExpenseMinor.rmb)
                    SummaryCard(title: "收入", value: store.searchIncomeMinor.rmb)
                    SummaryCard(title: "结余", value: (store.searchIncomeMinor - store.searchExpenseMinor).rmb)
                }
                .padding(.horizontal)
                List(store.searchResults) { item in
                    Button { store.editTransaction(item) } label: { HStack {
                        VStack(alignment: .leading) {
                            Text(item.categoryName)
                            Text("\(item.ledgerName) · \(item.accountName)").font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Text(item.amountMinor.rmb)
                    } }.buttonStyle(.plain)
                }
            }
        }
        .navigationTitle("")
    }

    private func clear() {
        store.searchText = ""
        store.searchType = nil
        store.searchAccountID = nil
        store.searchPrimaryCategoryID = nil
        store.searchSecondaryCategoryID = nil
        store.searchTagID = nil
        store.searchExact = ""
        store.searchMinimum = ""
        store.searchMaximum = ""
        store.searchDateEnabled = false
        store.searchResults = []
        store.searchExpenseMinor = 0
        store.searchIncomeMinor = 0
        hasSearched = false
    }
}
