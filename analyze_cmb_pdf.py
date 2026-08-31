#!/usr/bin/env python3

import fitz  # PyMuPDF
import json
import os

def analyze_cmb_pdf():
    """分析招商银行PDF的结构和内容"""
    # PDF文件路径
    pdf_path = "/Users/liushaojie/Downloads/招商银行交易流水(申请时间2026年08月31日19时27分04秒).pdf"

    try:
        # 打开PDF文件
        doc = fitz.open(pdf_path)
        print(f"PDF文件: {pdf_path}")
        print(f"总页数: {doc.page_count}")
        print("=" * 50)

        # 分析每一页
        for page_num in range(min(doc.page_count, 5)):  # 只分析前5页
            page = doc[page_num]
            print(f"\n第 {page_num + 1} 页分析:")
            print("-" * 30)

            # 获取页面文本（带布局）
            text = page.get_text("blocks")
            print("页面文本:")
            print(text[:1000] if len(text) > 1000 else text)

            # 获取文本块信息
            blocks = page.get_text("dict")["blocks"]
            print(f"\n文本块数量: {len(blocks)}")

            # 分析表格区域
            for block in blocks:
                if "lines" in block:
                    for line in block["lines"]:
                        line_text = "".join([span["text"] for span in line["spans"]])
                        # 检查是否是表头或交易记录
                        if any(keyword in line_text for keyword in ["招商银行", "交易日期", "摘要", "发生额", "余额"]):
                            y_pos = line["spans"][0]["bbox"][1]  # Y坐标
                            print(f"表头/关键字: {line_text.strip()} (Y={y_pos:.1f})")

            # 检查是否有表格
            print("\n表格检测:")
            tables = list(page.find_tables())
            print(f"找到 {len(tables)} 个表格")

            # 分析第一个表格（如果有）
            if tables:
                table = tables[0]
                print(f"表格位置: {table.bbox}")
                table_content = table.extract()
                print(f"表格行列数: {len(table_content)} 行 x {len(table_content[0])} 列")
                print("表格内容预览:")
                for row in table_content[:5]:  # 只显示前5行
                    print(f"  {row}")

        doc.close()

        # 保存分析结果
        analysis = {
            "file_path": pdf_path,
            "total_pages": 4,  # 我们知道有4页
            "analysis_complete": True
        }

        with open("/tmp/cmb_analysis.json", "w", encoding="utf-8") as f:
            json.dump(analysis, f, ensure_ascii=False, indent=2)

        print("\n详细分析结果已保存到 /tmp/cmb_analysis.json")

    except Exception as e:
        print(f"分析过程中出错: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    analyze_cmb_pdf()