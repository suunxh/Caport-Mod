#!/usr/bin/env python3
import pathlib
import sys
log = pathlib.Path(sys.argv[1]).read_text(errors="replace")
for marker in ("caport 1.1.0 Fabric 26.3 initialized", "caport client smoke: title screen reached"):
    if marker not in log:
        raise SystemExit("Missing startup marker: " + marker)
print("::notice::Fabric 26.3 client initialized, reached the title screen, and shut down cleanly")
