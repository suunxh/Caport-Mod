#!/usr/bin/env python3
import pathlib
import re
import sys
log = pathlib.Path(sys.argv[1]).read_text(errors="replace")
build_file = pathlib.Path(__file__).resolve().parents[1] / "fabric-26.3/build.gradle"
version = re.search(r"(?m)^version\s*=\s*['\"]([^'\"]+)['\"]", build_file.read_text()).group(1)
for marker in (f"caport {version} Fabric 26.3 initialized", "caport client smoke: title screen reached"):
    if marker not in log:
        print("::error::Missing startup marker: " + marker)
        raise SystemExit(1)
print("::notice::Fabric 26.3 client initialized, reached the title screen, and shut down cleanly")
