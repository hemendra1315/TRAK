#!/usr/bin/env python3
"""Summarise JUnit XML results as GitHub annotations. Usage: test-summary.py <title> <glob> [<glob>...]"""
import glob
import sys
import xml.etree.ElementTree as ET

title, patterns = sys.argv[1], sys.argv[2:]
t = f = e = s = 0
failed = []
files = sorted({p for pat in patterns for p in glob.glob(pat, recursive=True)})
for path in files:
    root = ET.parse(path).getroot()
    suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
    for su in suites:
        t += int(su.get("tests", 0)); f += int(su.get("failures", 0))
        e += int(su.get("errors", 0)); s += int(su.get("skipped", 0))
        for c in su.findall("testcase"):
            bad = c.find("failure") if c.find("failure") is not None else c.find("error")
            if bad is not None:
                msg = (bad.get("message") or bad.text or "")[:600]
                failed.append(f"{c.get('classname')}.{c.get('name')}: {msg}")
print(f"::notice title={title}::tests={t} failures={f} errors={e} skipped={s} files={len(files)}")
for x in failed[:10]:
    print(f"::error title={title} failed::" + x.replace("%", "%25").replace("\r", "").replace("\n", "%0A"))
