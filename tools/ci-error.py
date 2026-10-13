#!/usr/bin/env python3
"""Expose a bounded build-error tail in GitHub annotations for API-accessible diagnostics."""
import pathlib
import sys
text = pathlib.Path(sys.argv[1]).read_text(errors="replace")[-12000:]
message = text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
print("::error::" + message)
