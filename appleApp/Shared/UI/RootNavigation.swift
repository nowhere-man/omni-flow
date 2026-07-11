import SwiftUI

#if os(iOS)
struct PhoneRootView: View {
    @EnvironmentObject private var store: AppStore

    var body: some View {
        TabView(selection: $store.destination) {
            HomeView().tabItem { Label("首页", systemImage: "house") }.tag(MainDestination.home)
            AnalyticsView().tabItem { Label("统计", systemImage: "chart.bar") }.tag(MainDestination.analytics)
            TransactionEditorView().tabItem { Label("记账", systemImage: "plus.circle") }.tag(MainDestination.transaction)
            SearchView().tabItem { Label("搜索", systemImage: "magnifyingglass") }.tag(MainDestination.search)
            MoreView().tabItem { Label("更多", systemImage: "ellipsis.circle") }.tag(MainDestination.more)
        }
    }
}
#endif

#if os(macOS)
struct MacRootView: View {
    @EnvironmentObject private var store: AppStore

    var body: some View {
        NavigationSplitView {
            List(MainDestination.allCases, selection: $store.destination) { destination in
                Label(destination.title, systemImage: destination.systemImage)
                    .tag(destination)
            }
            .listStyle(.sidebar)
            .navigationTitle("OmniFlow")
        } detail: {
            Group {
                switch store.destination {
                case .home: HomeView()
                case .analytics: AnalyticsView()
                case .transaction: TransactionEditorView()
                case .search: SearchView()
                case .more: MoreView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}
#endif
