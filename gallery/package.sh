#!/usr/bin/env bash
# SPDX-License-Identifier: LGPL-2.1-only
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 /absolute/or/relative/output.zip" >&2
  exit 2
fi

output_path="$(realpath -m -- "$1")"
output_parent="$(dirname -- "$output_path")"
if [[ ! -d "$output_parent" ]]; then
  echo "output directory does not exist: $output_parent" >&2
  exit 2
fi

gallery_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
archive_temp="$(mktemp -d /tmp/bluemap-botanypots-gallery.XXXXXX)"
cleanup() { rm -rf -- "$archive_temp"; }
trap cleanup EXIT

python3 "$gallery_root/generate.py" --check
(cd "$gallery_root" && sha256sum --check SHA256SUMS)
mkdir -p "$archive_temp/root"
cp -a "$gallery_root/datapack/." "$archive_temp/root/"
find "$archive_temp/root" -exec touch -h -t 198001010000.00 {} +
(
  cd "$archive_temp/root"
  LC_ALL=C find . -type f -printf '%P\n' | LC_ALL=C sort |
    zip -q -X -9 "$archive_temp/botanypots-gallery.zip" -@
)
unzip -tq "$archive_temp/botanypots-gallery.zip"
cp "$archive_temp/botanypots-gallery.zip" "$output_path"
sha256sum "$output_path"
