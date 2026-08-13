#!/usr/bin/env python3
# SPDX-License-Identifier: LGPL-2.1-only
"""Generate the bounded 192-cell representative Botany Pots gallery."""

from __future__ import annotations

import argparse
import csv
from dataclasses import dataclass
import hashlib
import json
from pathlib import Path
import tempfile


ROOT = Path(__file__).resolve().parent
PROFILE = ROOT.parent / "src/main/resources/bluemap-botanypots/profiles/atmons-1.2.0"
ORIGIN = (2048, 100, 2048)
COLUMNS = 16
SPACING = 4
BATCH_SIZE = 48
SHELL_COUNT = 183
REPRESENTATIVE_COUNT = 6
FALLBACK_COUNT = 3
CELL_COUNT = SHELL_COUNT + REPRESENTATIVE_COUNT + FALLBACK_COUNT
REPRESENTATIVE_ROUTES = {
    "basic": "botanypots:terracotta_botany_pot",
    "hopper": "botanypots:terracotta_hopper_botany_pot",
    "waxed": "botanypots:terracotta_waxed_botany_pot",
}
PAIRS = (
    ("wheat-over-dirt", "minecraft:dirt", "minecraft:wheat_seeds"),
    ("cactus-over-sand", "minecraft:sand", "minecraft:cactus"),
)


@dataclass(frozen=True)
class Route:
    block_id: str
    kind: str


@dataclass(frozen=True)
class Cell:
    index: int
    kind: str
    entry_id: str
    block_state: str
    x: int
    y: int
    z: int
    soil: str = ""
    seed: str = ""
    fixture: str = ""

    def position(self) -> str:
        return f"{self.x} {self.y} {self.z}"

    def items(self) -> str:
        stacks: list[str] = []
        if self.soil:
            stacks.append(f'{{Slot:0b,id:"{self.soil}",count:1}}')
        if self.seed:
            stacks.append(f'{{Slot:1b,id:"{self.seed}",count:1}}')
        return "[" + ",".join(stacks) + "]"


def read_routes() -> list[Route]:
    with (PROFILE / "routes.tsv").open(encoding="ascii", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if len(rows) != SHELL_COUNT or len({row["block_id"] for row in rows}) != SHELL_COUNT:
        raise ValueError("exact route census changed")
    if any(row["legal_states"] != "128" for row in rows):
        raise ValueError("route state census changed")
    routes = [Route(row["block_id"], row["kind"]) for row in rows]
    by_id = {route.block_id: route.kind for route in routes}
    if any(by_id.get(block_id) != kind for kind, block_id in REPRESENTATIVE_ROUTES.items()):
        raise ValueError("representative shell roster changed")
    return routes


def validate_catalog() -> None:
    with (PROFILE / "catalog.tsv").open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if len(rows) != 4 or {row["profile"] for row in rows} != {"core"}:
        raise ValueError("representative catalog census changed")
    soils = {item for row in rows if row["kind"] == "soil"
             for item in row["input_items"].split(",")}
    crops = {(row["accepted_soils"], row["input_items"])
             for row in rows if row["kind"] == "crop"}
    expected = {(soil, seed) for _, soil, seed in PAIRS}
    if soils != {soil for _, soil, _ in PAIRS} or crops != expected:
        raise ValueError("representative catalog pair identity changed")


def position(index: int) -> tuple[int, int, int]:
    x0, y, z0 = ORIGIN
    return x0 + SPACING * (index % COLUMNS), y, z0 + SPACING * (index // COLUMNS)


def block_state(block_id: str, index: int) -> str:
    facing = ("south", "east", "north", "west")[index % 4]
    waterlogged = "true" if index // 4 % 2 else "false"
    level = index // 8 % 16
    return f"{block_id}[facing={facing},level={level},waterlogged={waterlogged}]"


def cells() -> list[Cell]:
    routes = read_routes()
    validate_catalog()
    result = [
        Cell(index, "shell", route.block_id, block_state(route.block_id, index),
             *position(index), fixture=f"empty-{route.kind}-shell")
        for index, route in enumerate(routes)
    ]
    for pair_id, soil, seed in PAIRS:
        for shell_kind, route in REPRESENTATIVE_ROUTES.items():
            index = len(result)
            result.append(Cell(
                index, "representative", f"{pair_id}-{shell_kind}",
                block_state(route, index), *position(index), soil, seed,
                f"{pair_id};shell={shell_kind}",
            ))
    fallbacks = (
        ("empty-inventory", "basic", "", ""),
        ("unknown-seed", "hopper", "minecraft:dirt", "minecraft:stick"),
        ("crossed-pair", "waxed", "minecraft:dirt", "minecraft:cactus"),
    )
    for fallback_id, shell_kind, soil, seed in fallbacks:
        index = len(result)
        route = REPRESENTATIVE_ROUTES[shell_kind]
        result.append(Cell(
            index, "fallback", fallback_id, block_state(route, index),
            *position(index), soil, seed, f"shell-only;reason={fallback_id}",
        ))
    if len(result) != CELL_COUNT or [cell.index for cell in result] != list(range(CELL_COUNT)):
        raise ValueError("representative gallery census changed")
    return result


def cell_tsv(all_cells: list[Cell]) -> bytes:
    rows = ["kind\tentry_id\tx\ty\tz\tblock_state\tsoil_item\tseed_item\tfixture"]
    rows.extend("\t".join((
        cell.kind, cell.entry_id, str(cell.x), str(cell.y), str(cell.z),
        cell.block_state, cell.soil, cell.seed, cell.fixture,
    )) for cell in all_cells)
    return ("\n".join(rows) + "\n").encode("utf-8")


def batches(all_cells: list[Cell]) -> list[list[Cell]]:
    return [all_cells[index:index + BATCH_SIZE]
            for index in range(0, len(all_cells), BATCH_SIZE)]


def build_batch(batch: list[Cell]) -> bytes:
    lines = ["# Generated by gallery/generate.py; do not edit."]
    for cell in batch:
        if "waterlogged=true" in cell.block_state:
            for x, z in ((cell.x - 1, cell.z), (cell.x + 1, cell.z),
                         (cell.x, cell.z - 1), (cell.x, cell.z + 1)):
                lines.append(f"setblock {x} {cell.y} {z} minecraft:barrier")
        lines.append(f"setblock {cell.position()} {cell.block_state}")
        lines.append(f"data merge block {cell.position()} {{Items:{cell.items()}}}")
        counter = {"shell": "#shells", "representative": "#representatives",
                   "fallback": "#fallbacks"}[cell.kind]
        lines.append(f"scoreboard players add {counter} botany_gallery 1")
    return ("\n".join(lines) + "\n").encode("utf-8")


def verify_batch(batch: list[Cell]) -> bytes:
    lines = ["# Generated by gallery/generate.py; do not edit."]
    for cell in batch:
        lines.append(
            f"execute unless block {cell.position()} {cell.block_state} run "
            "scoreboard players add #failures botany_gallery 1"
        )
        if cell.soil:
            lines.append(
                f"execute unless data block {cell.position()} "
                f"Items[{{Slot:0b,id:\"{cell.soil}\"}}] run "
                "scoreboard players add #failures botany_gallery 1"
            )
        if cell.seed:
            lines.append(
                f"execute unless data block {cell.position()} "
                f"Items[{{Slot:1b,id:\"{cell.seed}\"}}] run "
                "scoreboard players add #failures botany_gallery 1"
            )
        lines.append("scoreboard players add #checked botany_gallery 1")
    return ("\n".join(lines) + "\n").encode("utf-8")


def rendered_files() -> dict[Path, bytes]:
    all_cells = cells()
    groups = batches(all_cells)
    cell_bytes = cell_tsv(all_cells)
    minimum_x = min(cell.x for cell in all_cells)
    maximum_x = max(cell.x for cell in all_cells)
    minimum_z = min(cell.z for cell in all_cells)
    maximum_z = max(cell.z for cell in all_cells)
    files: dict[Path, bytes] = {
        Path("cells.tsv"): cell_bytes,
        Path("summary.json"): (json.dumps({
            "schema": 3,
            "cell_count": CELL_COUNT,
            "columns": COLUMNS,
            "rows": CELL_COUNT // COLUMNS,
            "shell_count": SHELL_COUNT,
            "representative_count": REPRESENTATIVE_COUNT,
            "fallback_count": FALLBACK_COUNT,
            "cells_sha256": hashlib.sha256(cell_bytes).hexdigest(),
        }, indent=2, sort_keys=True) + "\n").encode("utf-8"),
        Path("datapack/pack.mcmeta"): (
            '{\n  "pack": {\n    "description": '
            '"ATM 1.2.0 Botany Pots representative BlueMap gallery",\n'
            '    "pack_format": 48\n  }\n}\n'
        ).encode("ascii"),
        Path("datapack/data/minecraft/tags/function/load.json"): (
            '{\n  "values": [\n    "botanypots_gallery:load"\n  ]\n}\n'
        ).encode("ascii"),
        Path("datapack/data/botanypots_gallery/function/load.mcfunction"): (
            "scoreboard objectives add botany_gallery dummy\n"
        ).encode("ascii"),
        Path("datapack/data/botanypots_gallery/function/clear.mcfunction"): (
            f"fill {minimum_x - 2} 100 {minimum_z - 2} "
            f"{maximum_x + 2} 102 {maximum_z + 2} minecraft:air\n"
        ).encode("ascii"),
        Path("datapack/data/botanypots_gallery/function/pose.mcfunction"): (
            f"tp @s {(minimum_x + maximum_x) // 2} 160 "
            f"{(minimum_z + maximum_z) // 2} -35 65\n"
            "gamemode spectator @s\n"
        ).encode("ascii"),
        Path("datapack/data/botanypots_gallery/function/release.mcfunction"): (
            f"forceload remove {minimum_x} {minimum_z} {maximum_x} {maximum_z}\n"
            "save-all flush\n"
        ).encode("ascii"),
    }
    build = [
        f"forceload add {minimum_x} {minimum_z} {maximum_x} {maximum_z}",
        "function botanypots_gallery:clear",
        f"fill {minimum_x - 2} 99 {minimum_z - 2} "
        f"{maximum_x + 2} 99 {maximum_z + 2} minecraft:smooth_stone",
        "scoreboard players set #shells botany_gallery 0",
        "scoreboard players set #representatives botany_gallery 0",
        "scoreboard players set #fallbacks botany_gallery 0",
        *[f"function botanypots_gallery:build_{index:02d}"
          for index in range(len(groups))],
    ]
    verify = [
        "scoreboard players set #checked botany_gallery 0",
        "scoreboard players set #failures botany_gallery 0",
        *[f"function botanypots_gallery:verify_{index:02d}"
          for index in range(len(groups))],
        "execute unless score #shells botany_gallery matches 183 run "
        "scoreboard players add #failures botany_gallery 1",
        "execute unless score #representatives botany_gallery matches 6 run "
        "scoreboard players add #failures botany_gallery 1",
        "execute unless score #fallbacks botany_gallery matches 3 run "
        "scoreboard players add #failures botany_gallery 1",
        "execute unless score #checked botany_gallery matches 192 run "
        "scoreboard players add #failures botany_gallery 1",
    ]
    files[Path("datapack/data/botanypots_gallery/function/build.mcfunction")] = (
        "\n".join(build) + "\n").encode("ascii")
    files[Path("datapack/data/botanypots_gallery/function/verify.mcfunction")] = (
        "\n".join(verify) + "\n").encode("ascii")
    for index, group in enumerate(groups):
        files[Path(f"datapack/data/botanypots_gallery/function/build_{index:02d}.mcfunction")] = build_batch(group)
        files[Path(f"datapack/data/botanypots_gallery/function/verify_{index:02d}.mcfunction")] = verify_batch(group)
    command_build = ["scoreboard objectives add botany_gallery dummy", *build[:6]]
    for group in groups:
        command_build.extend(build_batch(group).decode("utf-8").splitlines()[1:])
    command_verify = verify[:2]
    for group in groups:
        command_verify.extend(verify_batch(group).decode("utf-8").splitlines()[1:])
    command_verify.extend(verify[-4:])
    files[Path("commands/build.txt")] = ("\n".join(command_build) + "\n").encode("utf-8")
    files[Path("commands/verify.txt")] = ("\n".join(command_verify) + "\n").encode("utf-8")
    files[Path("commands/clear.txt")] = files[Path("datapack/data/botanypots_gallery/function/clear.mcfunction")]
    files[Path("commands/release.txt")] = files[Path("datapack/data/botanypots_gallery/function/release.mcfunction")]
    return files


def checksum_file(files: dict[Path, bytes]) -> bytes:
    return ("\n".join(
        f"{hashlib.sha256(value).hexdigest()}  {path.as_posix()}"
        for path, value in sorted(files.items(), key=lambda item: item[0].as_posix())
    ) + "\n").encode("ascii")


def write_atomic(path: Path, value: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=path.parent, delete=False) as handle:
        handle.write(value)
        temporary = Path(handle.name)
    temporary.replace(path)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files = rendered_files()
    files[Path("SHA256SUMS")] = checksum_file(files)
    if args.check:
        for path, expected in files.items():
            actual = ROOT / path
            if not actual.is_file() or actual.read_bytes() != expected:
                raise SystemExit(f"stale gallery output: {path}")
        return 0
    for path, value in files.items():
        write_atomic(ROOT / path, value)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
