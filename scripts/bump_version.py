from pathlib import Path
import re

PROPERTIES = Path("gradle.properties")
VERSION_PATTERN = re.compile(r"(?m)^mod_version=(.+)$")
TIERWORKS_PATTERN = re.compile(r"^(\d+)\.(\d{2})$")

text = PROPERTIES.read_text(encoding="utf-8")
match = VERSION_PATTERN.search(text)
if not match:
    raise SystemExit("mod_version is missing from gradle.properties")

current_version = match.group(1).strip()

# Bootstrap/migrate the pre-automation development version.
if current_version == "0.1.0-SNAPSHOT":
    next_version = "0.01"
else:
    tierworks_match = TIERWORKS_PATTERN.fullmatch(current_version)
    if not tierworks_match:
        raise SystemExit(
            f"Unsupported mod_version '{current_version}'. "
            "Expected x.xx, for example 0.01."
        )

    major = int(tierworks_match.group(1))
    minor = int(tierworks_match.group(2))
    next_value = major * 100 + minor + 1
    next_version = f"{next_value // 100}.{next_value % 100:02d}"

updated = VERSION_PATTERN.sub(f"mod_version={next_version}", text, count=1)
PROPERTIES.write_text(updated, encoding="utf-8")

print(next_version)
