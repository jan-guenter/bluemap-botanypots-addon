#!/usr/bin/env python3
"""Verify exact local artifact authorities supplied to the explicit audit task."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path


EXPECTED = {
    "botany_pots_jar": (1068816, "45b23ac195511f724f62ab5f0c2d7a1c2c2403ff324a7403a1142e28a7d65edd"),
}


def main() -> int:
    parser = argparse.ArgumentParser()
    for key in EXPECTED:
        parser.add_argument("--" + key.replace("_", "-"), dest=key, type=Path, required=True)
    args = parser.parse_args()
    for key, (size, sha256) in EXPECTED.items():
        path = getattr(args, key)
        if not path.is_file() or path.is_symlink() or path.stat().st_size != size:
            raise SystemExit(f"artifact identity mismatch: {key}")
        if hashlib.sha256(path.read_bytes()).hexdigest() != sha256:
            raise SystemExit(f"artifact digest mismatch: {key}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
