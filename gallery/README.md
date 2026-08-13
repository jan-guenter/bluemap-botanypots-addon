# Representative staging gallery

The generated gallery contains exactly 192 isolated cells on a 16-column grid:

- 183 empty stock shells, one for every owned Botany Pots block ID;
- dirt + wheat seeds and sand + cactus on one basic, hopper, and waxed shell
  each (six representative content cells); and
- three shell-only fallback controls: empty inventory, unknown seed, and a
  crossed unsupported soil/crop pair.

The shell rows vary `facing`, `level`, and `waterlogged` while the unit census
validates all 23,424 legal shell states. This is intentionally not a crop or
soil catalog gallery. `cells.tsv`, `summary.json`, and `SHA256SUMS` bind every
generated file; no third-party model, texture, recipe, JAR, or world is stored.

Regenerate and validate with:

```text
python3 gallery/generate.py
python3 gallery/generate.py --check
python3 -m unittest discover -s tools/tests -p 'test_*.py'
```

## Disposable-server use

Run each generated command through an operator-supplied RCON executable:

```text
gallery/run_commands.sh build rcon-cli
gallery/run_commands.sh verify rcon-cli
gallery/run_commands.sh release rcon-cli
```

Require `#shells=183`, `#representatives=6`, `#fallbacks=3`, `#checked=192`,
and `#failures=0`. The optional deterministic datapack ZIP can be built with
`gallery/package.sh <output.zip>`.

Use only the disposable staging server and its established performance
settings. Install the candidate JAR, perform a full JVM restart, render the
gallery, and verify removal plus another full restart restores stock shells.
Before presenting a BlueMap link, open that exact view and perform the required
lightweight blank/black/missing/gross-breakage sanity check.
