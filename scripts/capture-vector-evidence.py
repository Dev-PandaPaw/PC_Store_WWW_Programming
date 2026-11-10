"""Create a local evidence report from real JUnit XML/Maven output, optionally screenshot it.

Run after the documented section 3.3 suite. Does not read .env or copy JUnit properties.
Only Python standard library and a local Chrome/Chromium executable are needed.
"""
import argparse
from collections import Counter
import html
import json
import os
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET


def read_text(path):
    raw = Path(path).read_bytes()
    return raw.decode("utf-16" if raw.startswith((b"\xff\xfe", b"\xfe\xff")) else "utf-8-sig")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--reports", default="target/surefire-reports")
    parser.add_argument("--log", default="tmp/vector-verification.log")
    parser.add_argument("--simulation-log", default="tmp/vector-simulation.ndjson")
    parser.add_argument("--regression-log", default="tmp/vector-regression.log")
    parser.add_argument("--output", default="docs/testing/evidence/spring-ai-3.3")
    parser.add_argument("--chrome", help="Optional Chrome/Chromium executable for a genuine browser screenshot")
    args = parser.parse_args()
    destination = Path(args.output).resolve()
    destination.mkdir(parents=True, exist_ok=True)
    suites = []
    for path in sorted(Path(args.reports).glob("TEST-iuh.fit.nhom16.pcstore.ai.vector.*.xml")):
        root = ET.parse(path).getroot()
        cases = []
        for case in root.findall("testcase"):
            state = "FAIL" if case.find("failure") is not None or case.find("error") is not None else "SKIP" if case.find("skipped") is not None else "PASS"
            cases.append({"name": case.get("name"), "status": state})
        suites.append({"name": root.get("name").split(".")[-1], "tests": int(root.get("tests")),
                       "failures": int(root.get("failures")), "errors": int(root.get("errors")),
                       "skipped": int(root.get("skipped")), "seconds": root.get("time"), "cases": cases})
    if not suites:
        raise SystemExit("No section 3.3 JUnit reports found")
    totals = {key: sum(suite[key] for suite in suites) for key in ("tests", "failures", "errors", "skipped")}
    maven_log = read_text(args.log)
    # Keep actual test/build summaries; omit system properties, request headers and credentials.
    summary = "\n".join(line for line in maven_log.splitlines() if any(marker in line for marker in
                        ("Tests run:", "BUILD SUCCESS", "BUILD FAILURE", "Finished at:", "Total time:", "LIVE GEMINI PASS:")))
    (destination / "maven-summary.txt").write_text(summary + "\n", encoding="utf-8")
    simulation_counts = Counter()
    for line in read_text(args.simulation_log).splitlines():
        try:
            event = json.loads(line)
        except json.JSONDecodeError:
            continue
        if event.get("event") == "request" and ":batchEmbedContents" in event.get("path", ""):
            simulation_counts[str(event["statusCode"])] += 1
    regression_summary = ""
    if Path(args.regression_log).exists():
        regression_summary = "\n".join(line for line in read_text(args.regression_log).splitlines()
                                       if any(marker in line for marker in ("Tests run:", "BUILD SUCCESS", "BUILD FAILURE", "Finished at:"))
                                       and " -- in " not in line)
        (destination / "regression-summary.txt").write_text(regression_summary + "\n", encoding="utf-8")
    result = {"source": "Actual JUnit XML; no JUnit environment/system properties copied", "totals": totals,
              "suites": suites, "simulationHttpStatusCounts": dict(simulation_counts), "regressionSummary": regression_summary,
              "scope": "Fixture ProductRepository; real Spring AI + SimpleVectorStore; provider simulation plus opt-in real Gemini test. UI and full database catalog integration are pending sections 3.4/3.5."}
    (destination / "results.json").write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    passed = totals["tests"] - totals["failures"] - totals["errors"] - totals["skipped"]
    status = "PASS" if totals["failures"] + totals["errors"] == 0 else "FAIL"
    rows = "".join(f'<tr><td>{html.escape(s["name"])}</td><td>{s["tests"]}</td><td>{s["failures"]}</td><td>{s["errors"]}</td><td>{s["skipped"]}</td></tr>' for s in suites)
    details = "".join(f'<section><h3>{html.escape(s["name"])}</h3><ul>' + "".join(f'<li><b>{c["status"]}</b> {html.escape(c["name"])}</li>' for c in s["cases"]) + '</ul></section>' for s in suites)
    page = '''<!doctype html><html lang="vi"><meta charset="utf-8"><title>Minh chứng Spring AI 3.3</title>
<style>body{margin:0;background:#edf2f7;color:#14243a;font:17px/1.5 'Segoe UI',sans-serif}main{max-width:1280px;margin:32px auto;background:white;padding:36px 44px;border-radius:14px}h1{font-size:32px;margin:0 0 8px}h2{font-size:23px;margin:24px 0 12px}p{margin:8px 0}.badge{display:inline-block;background:#d8f7e8;color:#075c39;padding:8px 20px;font-weight:700;border-radius:8px;margin:10px 0}table{width:100%;border-collapse:collapse}th,td{text-align:left;padding:10px;border-bottom:1px solid #dce4eb}th{background:#eaf1f8}pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#111d30;color:#bce9cf;padding:20px;font:15px/1.55 Consolas,monospace;border-radius:10px}aside{background:#fff5df;border-left:4px solid #d69518;padding:16px;margin:20px 0}details{margin-top:25px}li{font-size:14px;overflow-wrap:anywhere}b{color:#086142}</style><main>'''
    page += f'''<p>PC STORE · NHÓM 16 · VĂN NHỰT</p><h1>Mục 3.3 — Vector Store &amp; Ingestion Pipeline</h1>
<p>Ảnh chụp báo cáo kiểm thử, tạo từ kết quả JUnit/Maven thực tế.</p><div class="badge">{status} · {passed}/{totals['tests']} tests · failures {totals['failures']} · errors {totals['errors']} · skipped {totals['skipped']}</div>
<h2>Phạm vi đã kiểm tra</h2><p>Product → Chunking → Spring AI EmbeddingModel → Gemini → SimpleVectorStore.</p>
<p>API Admin + CSRF; cập nhật/xóa dữ liệu cũ; giữ snapshot khi lỗi; từ chối reindex đồng thời.</p>
<table><tr><th>JUnit suite</th><th>Tests</th><th>Failures</th><th>Errors</th><th>Skipped</th></tr>{rows}</table>
<h2>Log Maven thực tế</h2><pre>{html.escape(summary)}</pre>
<aside><strong>Giới hạn minh chứng:</strong> Repository sản phẩm dùng fixture trong suite này. HTTP mock kiểm tra giao thức và lỗi, dùng vector tổng hợp. Live test gọi Gemini thật cho một sản phẩm và truy vấn cùng nội dung (2 requests). Chưa xác nhận chất lượng gợi ý, toàn bộ catalog DB hoặc UI của mục 3.4–3.5.</aside>
<h2>Test hồi quy toàn dự án</h2><pre>{html.escape(regression_summary or 'Chưa cung cấp log hồi quy')}</pre>
<details><summary>Tên từng test lấy từ XML</summary>{details}</details></main></html>'''
    report = destination / "report.html"
    report.write_text(page, encoding="utf-8")
    if args.chrome:
        subprocess.run([args.chrome, "--headless", "--disable-gpu", "--no-sandbox", "--hide-scrollbars",
                        "--allow-file-access-from-files", "--window-size=1500,1700", "--force-device-scale-factor=1",
                        f"--screenshot={destination / 'pass.png'}", report.as_uri()], check=True, capture_output=True,
                       creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
    print(f"{status}: {passed}/{totals['tests']}; report: {report}")


if __name__ == "__main__":
    main()
