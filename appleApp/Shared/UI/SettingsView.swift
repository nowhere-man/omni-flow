import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var store: AppStore

    var body: some View {
        Form {
            Toggle("应用锁", isOn: Binding(get: { store.appLockEnabled }, set: store.setAppLockEnabled))
            Picker("界面外观", selection: Binding(get: { store.appearanceMode }, set: store.setAppearanceMode)) {
                Text("跟随系统").tag("SYSTEM")
                Text("浅色").tag("LIGHT")
                Text("深色").tag("DARK")
            }
            Button("数据管理") { store.destination = .more }
        }
        .formStyle(.grouped)
        #if os(macOS)
        .frame(width: 460, height: 220)
        .padding()
        #endif
    }
}
