#!/usr/bin/env python3
"""Verify checked-in exact-profile outputs without regenerating server evidence."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import subprocess


ROOT = Path(__file__).resolve().parents[1]
PROFILE_ROOT = ROOT / "src/main/resources/bluemap-botanypots/profiles/atmons-1.2.0"
CATALOG = PROFILE_ROOT / "catalog.tsv"
CATALOG_SHA = PROFILE_ROOT / "catalog.sha256"
PROFILE = PROFILE_ROOT / "profile.json"


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def catalog_census() -> dict[str, int]:
    lines = CATALOG.read_text(encoding="utf-8").splitlines()
    if not lines or not lines[0].startswith("kind\trecipe_id\t"):
        raise SystemExit("catalog header mismatch")
    counts = {"crop": 0, "soil": 0, "default_soil": 0}
    nodes = 0
    blocks: set[str] = set()
    previous: tuple[str, str] | None = None
    for line in lines[1:]:
        fields = line.split("\t")
        if len(fields) != 9 or fields[0] not in counts:
            raise SystemExit("catalog row mismatch")
        order = (fields[0], fields[1])
        if previous is not None and previous >= order:
            raise SystemExit("catalog ordering/identity mismatch")
        previous = order
        counts[fields[0]] += 1
        displays = fields[8].split("||")
        nodes += len(displays)
        for display in displays:
            state = display.split("~", 1)[0]
            blocks.add(state.split("[", 1)[0])
    counts["entries"] = sum(counts.values())
    counts["nodes"] = nodes
    counts["blocks"] = len(blocks)
    return counts


def main() -> int:
    subprocess.run(
        ["python3", str(ROOT / "tools/generate_routes.py"), "--check"],
        cwd=ROOT,
        check=True,
    )
    profile = json.loads(PROFILE.read_text(encoding="utf-8"))
    census = catalog_census()
    expected = {
        "crop": profile["crop_entry_count"],
        "soil": profile["soil_entry_count"],
        "default_soil": 0,
        "entries": profile["catalog_entry_count"],
        "nodes": profile["display_node_count"],
        "blocks": profile["display_node_count"],
    }
    if census != expected:
        raise SystemExit(f"catalog/profile census mismatch: {census} != {expected}")
    expected_sha_line = f"{digest(CATALOG)}  catalog.tsv\n"
    if CATALOG_SHA.read_text(encoding="ascii") != expected_sha_line:
        raise SystemExit("catalog digest file is stale")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
