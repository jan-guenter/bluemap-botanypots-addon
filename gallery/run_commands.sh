#!/usr/bin/env bash
# SPDX-License-Identifier: LGPL-2.1-only
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "usage: $0 <build|verify|release|clear> <rcon-command> [args ...]" >&2
  exit 2
fi

mode="$1"
shift
case "$mode" in
  build|verify|release|clear) ;;
  *) echo "unknown command stream: $mode" >&2; exit 2 ;;
esac

gallery_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
python3 "$gallery_root/generate.py" --check

while IFS= read -r command || [[ -n "$command" ]]; do
  "$@" "$command"
done < "$gallery_root/commands/$mode.txt"
