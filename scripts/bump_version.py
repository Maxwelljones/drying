from pathlib import Path
import re

PROPERTIES = Path("gradle.properties")
PATTERN = re.compile(r"(?m)^mod_version=(\d+)\.(\d{2})$")

text = PROPERTIES.read_text(encoding="utf-8")
match = PATTERN.search(text)
if not match:
    raise SystemExit("mod_version must use x.xx format, for example 0.01")

major = int(match.group(1))
minor = int(match.group(2))
next_value = major * 100 + minor + 1
next_version = f"{next_value // 100}.{next_value % 100:02d}"

updated = PATTERN.sub(f"mod_version={next_version}", text, count=1)
PROPERTIES.write_text(updated, encoding="utf-8")
print(next_version)
