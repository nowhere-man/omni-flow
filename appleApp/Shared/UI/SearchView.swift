import SwiftUI

struct SearchView: View {
    @EnvironmentObject private var store: AppStore
    @State private var advanced = true

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                TextField("备注、分类、账户或标签", text: $store.searchText)
                    .textFieldStyle(.roundedBorder)
                    .onSubmit(store.search)
                Button("搜索", action: store.search)
                Button("清除") {
                    store.searchText = ""
                    store.searchResults = []
                    store.searchExpenseMinor = 0
                    store.searchIncomeMinor = 0
                }
            }
            .padding()
            DisclosureGroup("高级筛选", isExpanded: $advanced) {
                Form {
                    Picker("账本", selection: Binding(get: { store.searchLedgerID }, set: store.setSearchLedger)) {
                        Text("所有账本").tag(String?.none)
                        ForEach(store.ledgers) { Text($0.name).tag(Optional($0.id)) }
                    }
                    Picker("类型", selection: $store.searchType) {
                        Text("不限").tag(EntryType?.none)
                        ForEach(EntryType.allCases) { Text($0.label).tag(Optional($0)) }
                    }
                    Picker("一级分类", selection: $store.searchPrimaryCategoryID) {
                        Text("不限").tag(String?.none)
                        ForEach(store.searchCategories.filter { $0.parentID == nil }) { Text($0.name).tag(Optional($0.id)) }
                    }
                    Picker("二级分类", selection: $store.searchSecondaryCategoryID) {
                        Text("不限").tag(String?.none)
                        ForEach(store.searchCategories.filter { $0.parentID == store.searchPrimaryCategoryID }) { Text($0.name).tag(Optional($0.id)) }
                    }
                    Picker("标签", selection: $store.searchTagID) {
                        Text("不限").tag(String?.none)
                        ForEach(store.searchTags) { Text($0.name).tag(Optional($0.id)) }
                    }
                    Picker("账户", selection: $store.searchAccountID) {
                        Text("不限").tag(String?.none)
                        ForEach(store.accounts) { Text($0.name).tag(Optional($0.id)) }
                    }
                    HStack {
                        TextField("精确金额", text: $store.searchExact)
                        TextField("最小金额", text: $store.searchMinimum)
                        TextField("最大金额", text: $store.searchMaximum)
                    }
                    Toggle("限定日期范围", isOn: $store.searchDateEnabled)
                    if store.searchDateEnabled {
                        HStack {
                            DatePicker("开始", selection: $store.searchStartDate, displayedComponents: .date)
                            DatePicker("结束", selection: $store.searchEndDate, displayedComponents: .date)
                        }
                    }
                    HStack {
                        Button("应用筛选", action: store.search)
                        Button("清除") {
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
                        }
                    }
                }
                .formStyle(.grouped)
            }
            .padding(.horizontal)
            if store.searchResults.isEmpty {
                EmptyStateView(title: "没有搜索结果", systemImage: "magnifyingglass", detail: "输入关键词或调整筛选条件")
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
        .navigationTitle("搜索")
    }
}
