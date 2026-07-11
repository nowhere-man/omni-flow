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
        editorLayout
        .navigationTitle(store.editingTransaction == nil ? "记一笔" : "编辑交易")
        .onAppear(perform: loadDraft)
        .onChange(of: store.transactionDraftRevision) { _ in loadDraft() }
        .onChange(of: ledgerID) { store.selectResourceLedger($0.isEmpty ? nil : $0) }
        .onChange(of: type) { value in
            if let category = store.categories.first(where: { $0.id == categoryID }), category.type != value { categoryID = "" }
        }
    }

    @ViewBuilder
    private var editorLayout: some View {
        #if os(macOS)
        HStack(spacing: 0) {
            ScrollView { editorFields.padding() }
                .frame(maxWidth: 720, maxHeight: .infinity, alignment: .topLeading)
            Divider()
            amountPanel
                .frame(width: 360)
                .frame(maxHeight: .infinity, alignment: .bottom)
        }
        #else
        ScrollView { editorFields.padding() }
            .safeAreaInset(edge: .bottom, spacing: 0) { amountPanel }
        #endif
    }

    private var editorFields: some View {
        VStack(alignment: .leading, spacing: 16) {
            Picker("类型", selection: $type) {
                ForEach(EntryType.allCases) { Text($0.label).tag($0) }
            }
            .pickerStyle(.segmented)

            HStack(spacing: 0) {
                CompactMenu("账本", selection: $ledgerID, placeholder: "选择账本", values: store.ledgers)
                Divider().frame(height: 38)
                CompactMenu("账户", selection: $accountID, placeholder: "选择账户", values: store.accounts)
            }
            .padding(.vertical, 4)
            .background(.quaternary, in: RoundedRectangle(cornerRadius: 8))

            TransactionCategoryPicker(categories: store.categories, type: type, selectedID: $categoryID)
            TransactionTagPicker(tags: store.tags, selectedIDs: Binding(
                get: { store.editingTagIDs },
                set: { store.editingTagIDs = $0 }
            ))
            TextField("备注", text: $note)
                .textFieldStyle(.roundedBorder)
            HStack {
                DatePicker("日期", selection: $date, displayedComponents: .date)
                Spacer()
                Toggle("不计入收支", isOn: $excluded).labelsHidden()
            }
        }
    }

    private var amountPanel: some View {
        TransactionAmountPanel(
            amount: $amount,
            saving: saving,
            secondaryTitle: store.editingTransaction == nil ? "再记" : "删除",
            message: message,
            onSecondary: { store.editingTransaction == nil ? save(again: true) : delete() },
            onDone: { save(again: false) }
        )
    }

    private func loadDraft() {
        message = nil
        saving = false
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
        ledgerID = store.defaultLedgerID ?? store.selectedLedgerID ?? store.resourceLedgerID ?? store.ledgers.first?.id ?? ""
        accountID = store.accounts.first(where: { $0.name == "现金" })?.id ?? store.accounts.first?.id ?? ""
        categoryID = ""
        amount = ""
        note = ""
        excluded = false
        date = store.draftTransactionDate ?? Date()
        store.editingTagIDs = []
    }

    private func save(again: Bool) {
        let normalized = amount.replacingOccurrences(of: ",", with: "")
        guard let decimal = evaluateAmount(normalized), !ledgerID.isEmpty, !accountID.isEmpty, !categoryID.isEmpty else {
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
            if again {
                amount = ""
                categoryID = ""
                note = ""
                excluded = false
                store.editingTagIDs = []
            } else {
                store.editingTransaction = nil
                store.destination = .home
            }
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

private struct CompactMenu<Value: Identifiable & Hashable>: View where Value.ID == String {
    let label: String
    @Binding var selection: String
    let placeholder: String
    let values: [Value]

    init(_ label: String, selection: Binding<String>, placeholder: String, values: [Value]) {
        self.label = label
        _selection = selection
        self.placeholder = placeholder
        self.values = values
    }

    var body: some View {
        Menu {
            ForEach(values) { value in Button(valueName(value)) { selection = value.id } }
        } label: {
            VStack(alignment: .leading, spacing: 2) {
                Text(label).font(.caption).foregroundStyle(.secondary)
                Text(values.first(where: { $0.id == selection }).map(valueName) ?? placeholder)
                    .fontWeight(.medium)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
        }
        .buttonStyle(.plain)
    }

    private func valueName(_ value: Value) -> String {
        switch value {
        case let ledger as LedgerUI: return ledger.name
        case let account as AccountUI: return account.name
        default: return placeholder
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
            Text("分类").font(.headline)
            if primaryCategories.isEmpty {
                Text("选择账本后加载分类").font(.subheadline).foregroundStyle(.secondary)
            } else {
                LazyVGrid(columns: columns, spacing: 8) {
                    ForEach(primaryCategories) { category in
                        CategoryTile(category: category, selected: primaryID == category.id) { selectedID = category.id }
                    }
                }
            }
            if !secondaryCategories.isEmpty {
                Text("二级分类").font(.subheadline.weight(.semibold))
                LazyVGrid(columns: columns, spacing: 8) {
                    ForEach(secondaryCategories) { category in
                        CategoryTile(category: category, selected: selectedID == category.id) { selectedID = category.id }
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

private struct CategoryTile: View {
    @Environment(\.colorScheme) private var colorScheme
    let category: CategoryUI
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                SVGIconView(key: category.iconKey ?? "category", size: 22)
                Text(category.name).font(.caption).lineLimit(1)
            }
            .frame(maxWidth: .infinity, minHeight: 64)
            .foregroundStyle(selected ? (colorScheme == .dark ? .black : .white) : .primary)
            .background(selected ? (colorScheme == .dark ? Color.white : Color.black) : Color.secondary.opacity(0.12), in: RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
    }
}

private struct TransactionAmountPanel: View {
    @Binding var amount: String
    let saving: Bool
    let secondaryTitle: String
    let message: String?
    let onSecondary: () -> Void
    let onDone: () -> Void
    private let rows = [["1", "2", "3", "+"], ["4", "5", "6", "-"], ["7", "8", "9", "⌫"], [".", "0"]]

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Text("金额").foregroundStyle(.secondary)
                Spacer()
                Text("¥\(amount.isEmpty ? "0" : amount)").font(.title2.monospacedDigit().weight(.semibold))
            }
            if let message { Text(message).font(.caption).foregroundStyle(.red).frame(maxWidth: .infinity, alignment: .leading) }
            ForEach(rows, id: \.self) { row in
                HStack(spacing: 8) {
                    ForEach(row, id: \.self) { key in keypadButton(key) }
                    if row.count == 2 {
                        Button(secondaryTitle, action: onSecondary)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .buttonStyle(.bordered)
                            .disabled(saving)
                        Button(saving ? "保存中" : "完成", action: onDone)
                            .frame(maxWidth: .infinity, minHeight: 44)
                            .buttonStyle(.borderedProminent)
                            .disabled(saving)
                    }
                }
            }
        }
        .padding(12)
        .background(.regularMaterial)
    }

    private func keypadButton(_ key: String) -> some View {
        Button(key) { press(key) }
            .frame(maxWidth: .infinity, minHeight: 44)
            .buttonStyle(.bordered)
            .disabled(saving)
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
                Text("标签").font(.subheadline.weight(.semibold))
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 76), spacing: 8)], spacing: 8) {
                    ForEach(tags) { tag in
                        Button(tag.name) {
                            if !selectedIDs.insert(tag.id).inserted { selectedIDs.remove(tag.id) }
                        }
                        .buttonStyle(.bordered)
                        .tint(selectedIDs.contains(tag.id) ? .primary : .secondary)
                    }
                }
            }
        }
    }
}
