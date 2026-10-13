#!/usr/bin/env python3
import pathlib
import sys
import xml.etree.ElementTree as ET
reports = list(pathlib.Path(sys.argv[1]).rglob("TEST-*.xml"))
counts = {key: 0 for key in ("tests", "failures", "errors", "skipped")}
for report in reports:
    suite = ET.parse(report).getroot()
    for key in counts:
        counts[key] += int(suite.get(key, "0"))
if not reports or not counts["tests"] or counts["failures"] or counts["errors"]:
    raise SystemExit("Missing or failing test results: " + str(counts))
print("::notice::Test results: " + str(counts))
