import SwiftUI

struct TransactionEditorView: View {
    @EnvironmentObject private var store: AppStore
    @State private var type: EntryType = .expense
    @State private var ledgerID = ""
    @State private var accountID = ""
    @State private var categoryID = ""
    @State private var amount = ""
    @State private var note = ""
    @State private var date = Date()
    @State private var excluded = false
    @State private var saving = false
    @State private var message: String?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                Group {
                    Picker("类型", selection: $type) {
                        ForEach(EntryType.allCases) { Text($0.label).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    Picker("账本", selection: $ledgerID) {
                        Text("请选择").tag("")
                        ForEach(store.ledgers) { Text($0.name).tag($0.id) }
                    }
                    Picker("账户", selection: $accountID) {
                        Text("请选择").tag("")
                        ForEach(store.accounts) { Text($0.name).tag($0.id) }
                    }
                    TransactionCategoryPicker(categories: store.categories, type: type, selectedID: $categoryID)
                }
                Group {
                    TransactionTagPicker(
                        tags: store.tags,
                        selectedIDs: Binding(
                            get: { store.editingTagIDs },
                            set: { store.editingTagIDs = $0 }
                        )
                    )
                    TransactionAmountKeypad(amount: $amount)
                    TextField("备注", text: $note, axis: .vertical)
                    DatePicker("日期", selection: $date)
                    Toggle("不计入收支", isOn: $excluded)
                    if let message { Text(message).foregroundStyle(.red) }
                }
                HStack {
                    if store.editingTransaction == nil {
                        Button("保存再记") { save(again: true) }.disabled(saving)
                    } else {
                        Button("删除", role: .destructive, action: delete).disabled(saving)
                    }
                    Button("完成") { save(again: false) }.disabled(saving).buttonStyle(.borderedProminent)
                }
            }
            .padding()
        }
        .navigationTitle("记账")
        .onAppear {
            if let item = store.editingTransaction {
                type = item.type
                ledgerID = item.ledgerID
                accountID = item.accountID
                categoryID = item.categoryID
                amount = String(Double(item.amountMinor) / 100)
                note = item.note
                date = item.date
                excluded = item.excluded
                store.selectResourceLedger(item.ledgerID)
                return
            }
            if ledgerID.isEmpty { ledgerID = store.defaultLedgerID ?? store.selectedLedgerID ?? store.resourceLedgerID ?? store.ledgers.first?.id ?? "" }
            if accountID.isEmpty { accountID = store.accounts.first(where: { $0.name == "现金" })?.id ?? store.accounts.first?.id ?? "" }
            if let draftDate = store.draftTransactionDate { date = draftDate }
        }
        .onChange(of: ledgerID) { store.selectResourceLedger($0.isEmpty ? nil : $0) }
        .onChange(of: type) { value in
            if let category = store.categories.first(where: { $0.id == categoryID }), category.type != value { categoryID = "" }
        }
    }

    private func save(again: Bool) {
        let normalized = amount.replacingOccurrences(of: ",", with: "")
        guard let decimal = evaluateAmount(normalized), ledgerID.isEmpty == false,
              accountID.isEmpty == false, categoryID.isEmpty == false else {
            message = "请选择账本、账户、分类并输入有效金额"
            return
        }
        saving = true
        let minor = NSDecimalNumber(decimal: decimal * 100).int64Value
        store.saveTransaction(
            id: store.editingTransaction?.id,
            ledgerID: ledgerID,
            accountID: accountID,
            categoryID: categoryID,
            amountMinor: minor,
            type: type,
            date: date,
            note: note,
            excluded: excluded,
            tagIDs: store.editingTagIDs
        ) { error in
            saving = false
            message = error
            guard error == nil else { return }
            amount = ""
            categoryID = ""
            note = ""
            excluded = false
            store.editingTagIDs = []
            if !again { store.editingTransaction = nil; store.destination = .home }
        }
    }

    private func delete() {
        guard let id = store.editingTransaction?.id else { return }
        saving = true
        store.deleteTransaction(id) { error in
            saving = false
            message = error
            if error == nil { store.editingTransaction = nil; store.destination = .home }
        }
    }
}

private struct TransactionCategoryPicker: View {
    let categories: [CategoryUI]
    let type: EntryType
    @Binding var selectedID: String
    private let columns = Array(repeating: GridItem(.flexible(), spacing: 8), count: 4)

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("一级分类").font(.headline)
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(primaryCategories) { category in
                    Button { selectedID = category.id } label: {
                        VStack(spacing: 4) {
                            SVGIconView(key: category.iconKey ?? "category", size: 26)
                            Text(category.name).font(.caption).lineLimit(1)
                        }
                        .frame(maxWidth: .infinity, minHeight: 54)
                    }
                    .buttonStyle(.bordered)
                    .tint(primaryID == category.id ? .accentColor : .secondary)
                }
            }
            if !secondaryCategories.isEmpty {
                Text("二级分类").font(.headline)
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 90), spacing: 8)], spacing: 8) {
                    ForEach(secondaryCategories) { category in
                        Button(category.name) { selectedID = category.id }
                            .buttonStyle(.bordered)
                            .tint(selectedID == category.id ? .accentColor : .secondary)
                    }
                }
            }
        }
    }

    private var primaryCategories: [CategoryUI] { categories.filter { $0.type == type && $0.parentID == nil } }
    private var selectedCategory: CategoryUI? { categories.first { $0.id == selectedID } }
    private var primaryID: String? { selectedCategory?.parentID ?? selectedCategory?.id }
    private var secondaryCategories: [CategoryUI] {
        guard let primaryID else { return [] }
        return categories.filter { $0.type == type && $0.parentID == primaryID }
    }
}

private struct TransactionAmountKeypad: View {
    @Binding var amount: String
    private let rows = [["1", "2", "3", "+"], ["4", "5", "6", "-"], ["7", "8", "9", "⌫"], [".", "0"]]

    var body: some View {
        VStack(spacing: 8) {
            Text(amount.isEmpty ? "0" : amount)
                .font(.largeTitle.monospacedDigit())
                .frame(maxWidth: .infinity, alignment: .trailing)
            ForEach(rows, id: \.self) { row in
                HStack(spacing: 8) {
                    ForEach(row, id: \.self) { key in
                        Button(key) { press(key) }.frame(maxWidth: .infinity)
                    }
                    ForEach(0..<(4 - row.count), id: \.self) { _ in Spacer().frame(maxWidth: .infinity) }
                }
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
    }

    private func press(_ key: String) {
        if key == "⌫" { amount = String(amount.dropLast()); return }
        if key == ".", amount.split(whereSeparator: { $0 == "+" || $0 == "-" }).last?.contains(".") == true { return }
        if (key == "+" || key == "-") && (amount.isEmpty || amount.last == "+" || amount.last == "-") { return }
        amount += key
    }
}

private func evaluateAmount(_ expression: String) -> Decimal? {
    var total = Decimal.zero
    var current = ""
    var sign = Decimal(1)
    for character in expression {
        if character == "+" || character == "-" {
            guard !current.isEmpty, let value = Decimal(string: current) else { return nil }
            total += sign * value
            current = ""
            sign = character == "+" ? 1 : -1
        } else {
            current.append(character)
        }
    }
    guard !current.isEmpty, let value = Decimal(string: current) else { return nil }
    return total + sign * value
}

private struct TransactionTagPicker: View {
    let tags: [TagUI]
    @Binding var selectedIDs: Set<String>

    var body: some View {
        if !tags.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("标签").font(.headline)
                ForEach(tags) { tag in
                    Toggle(
                        tag.name,
                        isOn: Binding(
                            get: { selectedIDs.contains(tag.id) },
                            set: { selected in
                                if selected { _ = selectedIDs.insert(tag.id) } else { _ = selectedIDs.remove(tag.id) }
                            }
                        )
                    )
                }
            }
        }
    }
}
