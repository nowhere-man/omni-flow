package com.omniflow.core.parser.pdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class CmbPdfAnalysis {
    public static void main(String[] args) throws IOException {
        // PDF文件路径
        String pdfPath = "/Users/liushaojie/Downloads/招商银行交易流水(申请时间2026年08月31日19时27分04秒).pdf";

        // 读取PDF字节
        byte[] pdfBytes = Files.readAllBytes(Paths.get(pdfPath));
        System.out.println("PDF文件大小: " + pdfBytes.length + " bytes");

        // 使用PdfTextExtractor提取文本
        List<List<PdfTextRun>> pages = PdfTextExtractor.extract(pdfBytes);
        System.out.println("提取到 " + pages.size() + " 页");

        if (pages.isEmpty()) {
            System.out.println("无法提取PDF内容，可能文件加密或格式不支持");
            return;
        }

        // 分析第一页的内容
        List<PdfTextRun> firstPage = pages.get(0);
        System.out.println("\n第一页文本内容:");
        for (PdfTextRun run : firstPage) {
            System.out.printf("[%d, %d] %s%n", (int)run.x, (int)run.y, run.text);
        }

        // 检查是否包含招商银行特征
        String allText = pages.stream()
            .flatMap(List::stream)
            .map(PdfTextRun::getText)
            .reduce("", (a, b) -> a + b);

        System.out.println("\nPDF特征检查:");
        System.out.println("包含'招商银行': " + allText.contains("招商银行"));
        System.out.println("包含'交易日期': " + allText.contains("交易日期"));
        System.out.println("包含'摘要': " + allText.contains("摘要"));
        System.out.println("包含'发生额': " + allText.contains("发生额"));
        System.out.println("包含'余额': " + allText.contains("余额"));

        // 分析表格结构
        System.out.println("\n分析表格结构:");
        List<PdfTextRun> headerLine = null;
        for (List<PdfTextRun> page : pages) {
            // 寻找包含表头的行
            for (PdfTextRun run : page) {
                if (run.getText().contains("交易日期") || run.getText().contains("记账日期")) {
                    headerLine = page;
                    break;
                }
            }
            if (headerLine != null) break;
        }

        if (headerLine != null) {
            System.out.println("找到表头行:");
            for (PdfTextRun run : headerLine) {
                System.out.printf("  [%d, %d] %s%n", (int)run.x, (int)run.y, run.text);
            }
        }
    }
}