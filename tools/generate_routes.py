#!/usr/bin/env python3
# SPDX-License-Identifier: LGPL-2.1-only
"""Generate the exact Botany Pots 21.1.44 owned block-route table."""

from __future__ import annotations

import argparse
from pathlib import Path
import tempfile


MATERIALS = (
    "black_concrete",
    "black_glazed_terracotta",
    "black_terracotta",
    "blue_concrete",
    "blue_glazed_terracotta",
    "blue_terracotta",
    "bricks",
    "brown_concrete",
    "brown_glazed_terracotta",
    "brown_terracotta",
    "cyan_concrete",
    "cyan_glazed_terracotta",
    "cyan_terracotta",
    "deepslate_bricks",
    "end_stone_bricks",
    "gray_concrete",
    "gray_glazed_terracotta",
    "gray_terracotta",
    "green_concrete",
    "green_glazed_terracotta",
    "green_terracotta",
    "light_blue_concrete",
    "light_blue_glazed_terracotta",
    "light_blue_terracotta",
    "light_gray_concrete",
    "light_gray_glazed_terracotta",
    "light_gray_terracotta",
    "lime_concrete",
    "lime_glazed_terracotta",
    "lime_terracotta",
    "magenta_concrete",
    "magenta_glazed_terracotta",
    "magenta_terracotta",
    "mossy_stone_bricks",
    "mud_bricks",
    "nether_bricks",
    "orange_concrete",
    "orange_glazed_terracotta",
    "orange_terracotta",
    "pink_concrete",
    "pink_glazed_terracotta",
    "pink_terracotta",
    "polished_blackstone_bricks",
    "prismarine_bricks",
    "purple_concrete",
    "purple_glazed_terracotta",
    "purple_terracotta",
    "quartz_bricks",
    "red_concrete",
    "red_glazed_terracotta",
    "red_nether_bricks",
    "red_terracotta",
    "stone_bricks",
    "terracotta",
    "tuff_bricks",
    "white_concrete",
    "white_glazed_terracotta",
    "white_terracotta",
    "yellow_concrete",
    "yellow_glazed_terracotta",
    "yellow_terracotta",
)
KINDS = ("basic", "hopper", "waxed")
OUTPUT = Path(
    "src/main/resources/bluemap-botanypots/profiles/atmons-1.2.0/routes.tsv"
)


def content() -> str:
    rows = ["block_id\tkind\tlegal_states"]
    for material in MATERIALS:
        rows.extend(
            (
                f"botanypots:{material}_botany_pot\tbasic\t128",
                f"botanypots:{material}_hopper_botany_pot\thopper\t128",
                f"botanypots:{material}_waxed_botany_pot\twaxed\t128",
            )
        )
    return "\n".join(rows) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    generated = content()
    if args.check:
        if not OUTPUT.is_file() or OUTPUT.read_text(encoding="utf-8") != generated:
            raise SystemExit(f"generated route table is stale: {OUTPUT}")
        return 0
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(
        "w", encoding="utf-8", dir=OUTPUT.parent, delete=False
    ) as handle:
        handle.write(generated)
        temporary = Path(handle.name)
    temporary.replace(OUTPUT)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
