import SwiftUI
import WebKit

struct SVGIconView: View {
    let key: String
    var size: CGFloat = 24

    var body: some View {
        SVGWebView(key: key)
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}

#if os(iOS)
private struct SVGWebView: UIViewRepresentable {
    let key: String

    func makeUIView(context: Context) -> WKWebView {
        let view = WKWebView(frame: .zero)
        view.isOpaque = false
        view.backgroundColor = .clear
        view.scrollView.isScrollEnabled = false
        return view
    }

    func updateUIView(_ view: WKWebView, context: Context) { load(key, in: view) }
}
#else
private struct SVGWebView: NSViewRepresentable {
    let key: String

    func makeNSView(context: Context) -> WKWebView {
        let view = WKWebView(frame: .zero)
        view.setValue(false, forKey: "drawsBackground")
        return view
    }

    func updateNSView(_ view: WKWebView, context: Context) { load(key, in: view) }
}
#endif

private func load(_ key: String, in view: WKWebView) {
    let url = Bundle.main.url(forResource: key, withExtension: "svg", subdirectory: "icons")
        ?? Bundle.main.url(forResource: "category", withExtension: "svg", subdirectory: "icons")
    guard let url, let svg = try? String(contentsOf: url) else { return }
    let html = """
    <html><head><meta name="viewport" content="width=device-width"></head>
    <body style="margin:0;color:currentColor;background:transparent;width:100%;height:100%;display:flex">\(svg)</body></html>
    """
    view.loadHTMLString(html, baseURL: url.deletingLastPathComponent())
}
