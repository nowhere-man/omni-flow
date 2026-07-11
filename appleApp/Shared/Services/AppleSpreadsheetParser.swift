import Foundation
import zlib

#if canImport(OmniFlowShared)
import OmniFlowShared

final class PlatformSpreadsheetParser: NSObject, AppleSpreadsheetParser {
    func parse(format: ImportFormat, bytes: KotlinByteArray) -> AppleSpreadsheetParseResult {
        do {
            let data = bytes.data
            let rows: [RawTransaction]
            switch format.name {
            case "WECHAT": rows = try parseWechat(data)
            case "CCB": rows = try parseCCB(data)
            default: throw SpreadsheetError.unsupported
            }
            return AppleSpreadsheetParseResult(rows: rows, error: nil)
        } catch {
            return AppleSpreadsheetParseResult(rows: [], error: error.localizedDescription)
        }
    }

    private func parseWechat(_ data: Data) throws -> [RawTransaction] {
        let archive = try ZipReader(data: data)
        let sharedStrings = archive["xl/sharedStrings.xml"].map(SharedStrings.parse) ?? []
        guard let sheetData = archive["xl/worksheets/sheet1.xml"] else { throw SpreadsheetError.missingSheet }
        let rows = Worksheet.parse(sheetData, sharedStrings: sharedStrings)
        guard let headerIndex = rows.firstIndex(where: { $0.first == "交易时间" }) else { throw SpreadsheetError.missingHeader }
        let header = rows[headerIndex]
        return try rows.dropFirst(headerIndex + 1).filter { $0.contains(where: { !$0.isEmpty }) }.map { row in
            let direction = value("收/支", row: row, header: header)
            let type = direction == "收入" ? "INCOME" : direction == "支出" ? "EXPENSE" : nil
            return AppleRawTransactionFactory.shared.create(
                formatName: "WECHAT",
                occurredAtMillis: try date(value("交易时间", row: row, header: header), pattern: "yyyy-MM-dd HH:mm:ss"),
                amountMinor: try money(value("金额(元)", row: row, header: header)),
                typeName: type,
                excluded: direction == nil || direction == "中性交易",
                accountName: value("支付方式", row: row, header: header),
                note: joined([
                    value("交易对方", row: row, header: header),
                    value("商品", row: row, header: header),
                    value("备注", row: row, header: header),
                ]),
                externalId: value("交易单号", row: row, header: header),
                sourceCategory: value("交易类型", row: row, header: header)
            )
        }
    }

    private func parseCCB(_ data: Data) throws -> [RawTransaction] {
        let workbook = try OLEReader(data: data).stream(named: ["Workbook", "Book"])
        let rows = BIFF8.rows(workbook)
        guard let headerIndex = rows.firstIndex(where: { $0.contains("交易日期") && $0.contains("交易金额") }) else {
            throw SpreadsheetError.missingHeader
        }
        let header = rows[headerIndex]
        return try rows.dropFirst(headerIndex + 1).filter { $0.contains(where: { !$0.isEmpty }) }.map { row in
            let signed = try money(value("交易金额", row: row, header: header), signed: true)
            return AppleRawTransactionFactory.shared.create(
                formatName: "CCB",
                occurredAtMillis: try date(value("交易日期", row: row, header: header), pattern: "yyyyMMdd"),
                amountMinor: abs(signed),
                typeName: signed >= 0 ? "INCOME" : "EXPENSE",
                excluded: false,
                accountName: nil,
                note: joined([
                    value("摘要", row: row, header: header),
                    value("交易地点/附言", row: row, header: header),
                    value("对方账号与户名", row: row, header: header),
                ]),
                externalId: nil,
                sourceCategory: nil
            )
        }
    }

    private func value(_ name: String, row: [String], header: [String]) -> String? {
        guard let index = header.firstIndex(of: name), index < row.count else { return nil }
        let value = row[index].trimmingCharacters(in: .whitespacesAndNewlines)
        return value.isEmpty || value == "/" ? nil : value
    }

    private func joined(_ values: [String?]) -> String? {
        let result = values.compactMap { $0 }.reduce(into: [String]()) { if !$0.contains($1) { $0.append($1) } }
        return result.isEmpty ? nil : result.joined(separator: " | ")
    }

    private func money(_ value: String?, signed: Bool = false) throws -> Int64 {
        guard let value, let decimal = Decimal(string: value.replacingOccurrences(of: "¥", with: "").replacingOccurrences(of: ",", with: "")) else {
            throw SpreadsheetError.invalidMoney
        }
        let minor = NSDecimalNumber(decimal: decimal * 100).int64Value
        return signed ? minor : abs(minor)
    }

    private func date(_ value: String?, pattern: String) throws -> Int64 {
        guard let value else { throw SpreadsheetError.invalidDate }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN_POSIX")
        formatter.timeZone = TimeZone(identifier: "Asia/Shanghai")
        formatter.dateFormat = pattern
        guard let date = formatter.date(from: value) else { throw SpreadsheetError.invalidDate }
        return Int64(date.timeIntervalSince1970 * 1000)
    }
}

private enum SpreadsheetError: LocalizedError {
    case unsupported, missingSheet, missingHeader, invalidMoney, invalidDate, invalidArchive, missingWorkbook
    var errorDescription: String? {
        switch self {
        case .unsupported: return "该文件格式不是 Apple 表格账单"
        case .missingSheet: return "未找到微信工作表"
        case .missingHeader: return "未找到账单明细表头"
        case .invalidMoney: return "账单金额格式无效"
        case .invalidDate: return "账单日期格式无效"
        case .invalidArchive: return "XLSX 压缩包损坏"
        case .missingWorkbook: return "未找到 XLS Workbook 流"
        }
    }
}

private struct ZipReader {
    private let entries: [String: Data]
    subscript(path: String) -> Data? { entries[path] }

    init(data: Data) throws {
        guard let eocd = data.lastIndex(of: 0x06054b50), data.u32(eocd + 16) < data.count else {
            throw SpreadsheetError.invalidArchive
        }
        var offset = data.u32(eocd + 16)
        let count = data.u16(eocd + 10)
        var result: [String: Data] = [:]
        for _ in 0..<count {
            guard data.u32(offset) == 0x02014b50 else { throw SpreadsheetError.invalidArchive }
            let method = data.u16(offset + 10)
            let compressedSize = data.u32(offset + 20)
            let uncompressedSize = data.u32(offset + 24)
            let nameLength = data.u16(offset + 28)
            let extraLength = data.u16(offset + 30)
            let commentLength = data.u16(offset + 32)
            let localOffset = data.u32(offset + 42)
            let nameData = data.subdata(in: offset + 46..<offset + 46 + nameLength)
            let name = String(data: nameData, encoding: .utf8) ?? ""
            let localNameLength = data.u16(localOffset + 26)
            let localExtraLength = data.u16(localOffset + 28)
            let payloadOffset = localOffset + 30 + localNameLength + localExtraLength
            let compressed = data.subdata(in: payloadOffset..<payloadOffset + compressedSize)
            result[name] = method == 0 ? compressed : try compressed.inflateRaw(expectedSize: uncompressedSize)
            offset += 46 + nameLength + extraLength + commentLength
        }
        entries = result
    }
}

private enum SharedStrings {
    static func parse(_ data: Data) -> [String] {
        let delegate = SharedStringsDelegate()
        let parser = XMLParser(data: data)
        parser.delegate = delegate
        parser.parse()
        return delegate.values
    }
}

private final class SharedStringsDelegate: NSObject, XMLParserDelegate {
    var values: [String] = []
    private var current = ""
    private var inText = false
    func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String : String] = [:]) {
        if elementName == "si" { current = "" }
        if elementName == "t" { inText = true }
    }
    func parser(_ parser: XMLParser, foundCharacters string: String) { if inText { current += string } }
    func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
        if elementName == "t" { inText = false }
        if elementName == "si" { values.append(current) }
    }
}

private enum Worksheet {
    static func parse(_ data: Data, sharedStrings: [String]) -> [[String]] {
        let delegate = WorksheetDelegate(sharedStrings: sharedStrings)
        let parser = XMLParser(data: data)
        parser.delegate = delegate
        parser.parse()
        return delegate.rows
    }
}

private final class WorksheetDelegate: NSObject, XMLParserDelegate {
    let sharedStrings: [String]
    var rows: [[String]] = []
    private var row: [Int: String] = [:]
    private var column = 0
    private var type = ""
    private var text = ""
    private var collecting = false

    init(sharedStrings: [String]) { self.sharedStrings = sharedStrings }

    func parser(_ parser: XMLParser, didStartElement elementName: String, namespaceURI: String?, qualifiedName qName: String?, attributes attributeDict: [String : String] = [:]) {
        if elementName == "row" { row = [:] }
        if elementName == "c" {
            column = Self.column(attributeDict["r"] ?? "A1")
            type = attributeDict["t"] ?? ""
            text = ""
        }
        if elementName == "v" || elementName == "t" { collecting = true }
    }
    func parser(_ parser: XMLParser, foundCharacters string: String) { if collecting { text += string } }
    func parser(_ parser: XMLParser, didEndElement elementName: String, namespaceURI: String?, qualifiedName qName: String?) {
        if elementName == "v" || elementName == "t" { collecting = false }
        if elementName == "c" {
            row[column] = type == "s" ? sharedStrings[safe: Int(text) ?? -1] ?? "" : text
        }
        if elementName == "row" {
            let maximum = row.keys.max() ?? -1
            rows.append((0...maximum).map { row[$0] ?? "" })
        }
    }
    private static func column(_ reference: String) -> Int {
        reference.prefix { $0.isLetter }.reduce(0) { $0 * 26 + Int($1.asciiValue! - 64) } - 1
    }
}

private struct OLEReader {
    let data: Data
    let sectorSize: Int
    let miniSectorSize: Int
    let miniCutoff: Int
    let fat: [UInt32]
    let miniFat: [UInt32]
    let directories: [DirectoryEntry]
    let miniStream: Data

    init(data: Data) throws {
        guard data.prefix(8) == Data([0xD0,0xCF,0x11,0xE0,0xA1,0xB1,0x1A,0xE1]) else { throw SpreadsheetError.invalidArchive }
        self.data = data
        sectorSize = 1 << data.u16(30)
        miniSectorSize = 1 << data.u16(32)
        miniCutoff = data.u32(56)
        let fatCount = data.u32(44)
        var fatSectors = (0..<109).map { UInt32(data.u32(76 + $0 * 4)) }.filter { $0 < 0xFFFFFFFA }
        fatSectors = Array(fatSectors.prefix(fatCount))
        fat = fatSectors.flatMap { sector in
            let bytes = data.sector(sector, size: sectorSize)
            return stride(from: 0, to: bytes.count, by: 4).map { UInt32(bytes.u32($0)) }
        }
        let directoryData = Self.chain(data: data, start: UInt32(data.u32(48)), table: fat, sectorSize: sectorSize)
        directories = stride(from: 0, to: directoryData.count, by: 128).compactMap { DirectoryEntry(data: directoryData, offset: $0) }
        let root = directories.first { $0.type == 5 }
        miniStream = root.map {
            let stream = Self.chain(data: data, start: $0.start, table: fat, sectorSize: sectorSize)
            return stream.subdata(in: 0..<Swift.min($0.size, stream.count))
        } ?? Data()
        let miniFatData = Self.chain(data: data, start: UInt32(data.u32(60)), table: fat, sectorSize: sectorSize)
        miniFat = stride(from: 0, to: miniFatData.count, by: 4).map { UInt32(miniFatData.u32($0)) }
    }

    func stream(named names: [String]) throws -> Data {
        guard let entry = directories.first(where: { names.contains($0.name) }) else { throw SpreadsheetError.missingWorkbook }
        if entry.size < miniCutoff {
            var output = Data()
            var sector = entry.start
            var guardCount = 0
            while sector < 0xFFFFFFFA && Int(sector) < miniFat.count && guardCount < miniFat.count {
                let start = Int(sector) * miniSectorSize
                output.append(miniStream.subdata(in: start..<min(start + miniSectorSize, miniStream.count)))
                sector = miniFat[Int(sector)]
                guardCount += 1
            }
            return Data(output.prefix(entry.size))
        }
        return Data(Self.chain(data: data, start: entry.start, table: fat, sectorSize: sectorSize).prefix(entry.size))
    }

    private static func chain(data: Data, start: UInt32, table: [UInt32], sectorSize: Int) -> Data {
        var output = Data()
        var sector = start
        var guardCount = 0
        while sector < 0xFFFFFFFA && Int(sector) < table.count && guardCount < table.count {
            output.append(data.sector(sector, size: sectorSize))
            sector = table[Int(sector)]
            guardCount += 1
        }
        return output
    }
}

private struct DirectoryEntry {
    let name: String
    let type: UInt8
    let start: UInt32
    let size: Int
    init?(data: Data, offset: Int) {
        guard offset + 128 <= data.count else { return nil }
        let nameLength = Int(data.u16(offset + 64))
        guard nameLength >= 2 else { return nil }
        name = String(data: data.subdata(in: offset..<offset + nameLength - 2), encoding: .utf16LittleEndian) ?? ""
        type = data[offset + 66]
        start = UInt32(data.u32(offset + 116))
        size = data.u64(offset + 120)
    }
}

private enum BIFF8 {
    static func rows(_ data: Data) -> [[String]] {
        var offset = 0
        var strings: [String] = []
        var cells: [Int: [Int: String]] = [:]
        while offset + 4 <= data.count {
            let id = data.u16(offset)
            let length = data.u16(offset + 2)
            let payload = offset + 4
            guard payload + length <= data.count else { break }
            if id == 0x00FC {
                var chunks = [data.subdata(in: payload..<payload + length)]
                var next = payload + length
                while next + 4 <= data.count, data.u16(next) == 0x003C {
                    let continueLength = data.u16(next + 2)
                    guard next + 4 + continueLength <= data.count else { break }
                    chunks.append(data.subdata(in: next + 4..<next + 4 + continueLength))
                    next += 4 + continueLength
                }
                strings = parseSST(chunks)
                offset = next
                continue
            }
            if id == 0x00FD && length >= 10 {
                let row = data.u16(payload)
                let column = data.u16(payload + 2)
                let index = data.u32(payload + 6)
                cells[row, default: [:]][column] = strings[safe: index] ?? ""
            }
            offset = payload + length
        }
        return cells.keys.sorted().map { row in
            let values = cells[row].orEmpty
            return (0...(values.keys.max() ?? -1)).map { values[$0] ?? "" }
        }
    }

    private static func parseSST(_ chunks: [Data]) -> [String] {
        var cursor = ChunkCursor(chunks)
        _ = cursor.u32()
        let unique = cursor.u32()
        var result: [String] = []
        for _ in 0..<unique {
            let count = cursor.u16()
            let flags = cursor.u8()
            let richRuns = flags & 0x08 != 0 ? cursor.u16() : 0
            let extensionSize = flags & 0x04 != 0 ? cursor.u32() : 0
            var highByte = flags & 0x01 != 0
            var scalars: [UInt16] = []
            for _ in 0..<count {
                if cursor.atChunkEnd {
                    cursor.advanceChunk()
                    highByte = cursor.u8() & 0x01 != 0
                }
                scalars.append(highByte ? UInt16(cursor.u8()) | UInt16(cursor.u8()) << 8 : UInt16(cursor.u8()))
            }
            result.append(String(decoding: scalars, as: UTF16.self))
            cursor.skip(richRuns * 4 + extensionSize)
        }
        return result
    }
}

private struct ChunkCursor {
    private let chunks: [Data]
    private var chunk = 0
    private var offset = 0
    init(_ chunks: [Data]) { self.chunks = chunks }
    var atChunkEnd: Bool { chunk >= chunks.count || offset >= chunks[chunk].count }
    mutating func advanceChunk() { chunk += 1; offset = 0 }
    mutating func u8() -> UInt8 {
        if atChunkEnd { advanceChunk() }
        guard chunk < chunks.count, offset < chunks[chunk].count else { return 0 }
        defer { offset += 1 }
        return chunks[chunk][offset]
    }
    mutating func u16() -> Int { Int(u8()) | Int(u8()) << 8 }
    mutating func u32() -> Int { u16() | u16() << 16 }
    mutating func skip(_ count: Int) { for _ in 0..<count { _ = u8() } }
}

private extension KotlinByteArray {
    var data: Data { Data((0..<size).map { UInt8(bitPattern: get(index: Int32($0))) }) }
}

private extension Data {
    func u16(_ offset: Int) -> Int { Int(self[offset]) | Int(self[offset + 1]) << 8 }
    func u32(_ offset: Int) -> Int { u16(offset) | u16(offset + 2) << 16 }
    func u64(_ offset: Int) -> Int { u32(offset) | u32(offset + 4) << 32 }
    func lastIndex(of signature: Int) -> Int? {
        guard count >= 4 else { return nil }
        return stride(from: count - 4, through: 0, by: -1).first { u32($0) == signature }
    }
    func sector(_ id: UInt32, size: Int) -> Data {
        let start = (Int(id) + 1) * size
        return subdata(in: start..<Swift.min(start + size, count))
    }
    func inflateRaw(expectedSize: Int) throws -> Data {
        var stream = z_stream()
        guard inflateInit2_(&stream, -MAX_WBITS, ZLIB_VERSION, Int32(MemoryLayout<z_stream>.size)) == Z_OK else {
            throw SpreadsheetError.invalidArchive
        }
        defer { inflateEnd(&stream) }
        var output = Data(count: expectedSize)
        let status = withUnsafeBytes { input in
            output.withUnsafeMutableBytes { destination in
                stream.next_in = UnsafeMutablePointer<Bytef>(mutating: input.bindMemory(to: Bytef.self).baseAddress!)
                stream.avail_in = uInt(count)
                stream.next_out = destination.bindMemory(to: Bytef.self).baseAddress!
                stream.avail_out = uInt(expectedSize)
                return inflate(&stream, Z_FINISH)
            }
        }
        guard status == Z_STREAM_END else { throw SpreadsheetError.invalidArchive }
        return output
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? { indices.contains(index) ? self[index] : nil }
}

private extension Optional where Wrapped == [Int: String] {
    var orEmpty: [Int: String] { self ?? [:] }
}
#endif
