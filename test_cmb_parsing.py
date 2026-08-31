#!/usr/bin/env python3

import subprocess
import json
import os

def test_cmb_pdf():
    # PDF文件路径
    pdf_path = "/Users/liushaojie/Downloads/招商银行交易流水(申请时间2026年08月31日19时27分04秒).pdf"

    # 读取PDF文件内容
    with open(pdf_path, 'rb') as f:
        pdf_bytes = f.read()

    # 创建临时文件
    temp_file = "/tmp/cmb_pdf.bin"
    with open(temp_file, 'wb') as f:
        f.write(pdf_bytes)

    print("PDF文件大小:", len(pdf_bytes), "bytes")

    try:
        # 使用Gradle运行测试来解析PDF
        result = subprocess.run([
            "./gradlew", ":core:test", "--tests", "*CmbPdfBillParserTest*",
            "--info", "--stacktrace"
        ], cwd="/Users/liushaojie/Documents/Repos/omni-flow",
           capture_output=True, text=True, timeout=60)

        if result.returncode == 0:
            print("✓ 测试通过")
            print(result.stdout[-1000:] if len(result.stdout) > 1000 else result.stdout)
        else:
            print("✗ 测试失败")
            print("STDOUT:", result.stdout)
            print("STDERR:", result.stderr)

    except subprocess.TimeoutExpired:
        print("✗ 测试超时")
    except Exception as e:
        print(f"✗ 执行测试时出错: {e}")
    finally:
        # 清理临时文件
        if os.path.exists(temp_file):
            os.remove(temp_file)

if __name__ == "__main__":
    test_cmb_pdf()