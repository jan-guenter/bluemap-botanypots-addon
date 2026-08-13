# SPDX-License-Identifier: LGPL-2.1-only
"""Regression coverage for the bounded representative Botany Pots gallery."""

from __future__ import annotations

import collections
import importlib.util
from pathlib import Path
import sys
import unittest


ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "botanypots_gallery_generator", ROOT / "gallery/generate.py"
)
assert SPEC is not None and SPEC.loader is not None
gallery = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = gallery
SPEC.loader.exec_module(gallery)


class GalleryGeneratorTest(unittest.TestCase):

    def setUp(self) -> None:
        self.files = gallery.rendered_files()
        self.cells = gallery.cells()

    def test_exact_census_and_unique_positions(self) -> None:
        self.assertEqual(192, len(self.cells))
        self.assertEqual(192, len({(cell.x, cell.y, cell.z) for cell in self.cells}))
        self.assertEqual(
            {"shell": 183, "representative": 6, "fallback": 3},
            dict(collections.Counter(cell.kind for cell in self.cells)),
        )

    def test_shell_gallery_owns_every_route(self) -> None:
        routes = gallery.read_routes()
        shells = [cell for cell in self.cells if cell.kind == "shell"]
        self.assertEqual(
            {route.block_id for route in routes},
            {cell.entry_id for cell in shells},
        )

    def test_both_pairs_cover_basic_hopper_and_waxed_shells(self) -> None:
        representatives = [cell for cell in self.cells if cell.kind == "representative"]
        self.assertEqual(
            {
                (soil, seed, shell_kind)
                for _, soil, seed in gallery.PAIRS
                for shell_kind in gallery.REPRESENTATIVE_ROUTES
            },
            {
                (cell.soil, cell.seed, cell.entry_id.rsplit("-", 1)[1])
                for cell in representatives
            },
        )
        self.assertEqual(
            {"empty-inventory", "unknown-seed", "crossed-pair"},
            {cell.entry_id for cell in self.cells if cell.kind == "fallback"},
        )

    def test_functions_are_bounded_and_verify_every_cell(self) -> None:
        build_batches = sorted(
            path for path in self.files
            if path.name.startswith("build_") and path.suffix == ".mcfunction"
        )
        verify_batches = sorted(
            path for path in self.files
            if path.name.startswith("verify_") and path.suffix == ".mcfunction"
        )
        self.assertEqual(4, len(build_batches))
        self.assertEqual(4, len(verify_batches))
        for path in [*build_batches, *verify_batches]:
            self.assertLess(len(self.files[path]), 65_536, path)
        verify = self.files[
            Path("datapack/data/botanypots_gallery/function/verify.mcfunction")
        ].decode("ascii")
        self.assertIn("score #representatives botany_gallery matches 6", verify)
        self.assertIn("score #checked botany_gallery matches 192", verify)

    def test_waterlogged_cells_are_contained_before_placement(self) -> None:
        build = b"\n".join(
            self.files[path]
            for path in sorted(self.files)
            if path.name.startswith("build_") and path.suffix == ".mcfunction"
        ).decode("utf-8")
        for cell in self.cells:
            if "waterlogged=true" not in cell.block_state:
                continue
            pot = f"setblock {cell.position()} {cell.block_state}"
            pot_index = build.index(pot)
            for x, z in ((cell.x - 1, cell.z), (cell.x + 1, cell.z),
                         (cell.x, cell.z - 1), (cell.x, cell.z + 1)):
                barrier = f"setblock {x} {cell.y} {z} minecraft:barrier"
                self.assertLess(build.rindex(barrier, 0, pot_index), pot_index)


if __name__ == "__main__":
    unittest.main()
