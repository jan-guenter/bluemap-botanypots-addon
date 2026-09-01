# BlueMap Botany Pots Add-on

Server-side BlueMap rendering for the exact All the Mons 1.2.0 Botany Pots
shell family, plus two deliberately representative static-mature contents.

Version `0.1.0-alpha.2` is the owner-accepted BlueMap 5.23 migration candidate.
Its reviewed production JAR is 150,791 bytes with SHA-256
`1dfa631f22eb65aa953d3f554d2b4c02e0fab000c1461903cad56b419d12d3c4`.

The add-on owns all 183 `botanypots:*_botany_pot` block IDs and their 23,424
legal `facing`/`level`/`waterlogged` states. It always renders the installed
Botany Pots model first, preserving the basic, hopper, waxed, material, level,
orientation, and BlueMap-owned waterlogged appearance. It adds overlays only
for these exact persisted slot-0/slot-1 pairs:

| Soil | Seed/crop item | Static display |
| --- | --- | --- |
| `minecraft:dirt` | `minecraft:wheat_seeds` | dirt with mature `minecraft:wheat[age=7]` |
| `minecraft:sand` | `minecraft:cactus` | sand with mature `minecraft:cactus[age=15]` |

The two pairs work on every owned shell; the gallery demonstrates each on a
basic, hopper, and waxed shell. Empty, unknown, crossed, malformed, ambiguous,
or component-bearing visual contents remain the stock shell only. This release
does not claim support for any other crop, soil, recipe family, integration, or
client callback.

## Exact target

- All the Mons `1.2.0`, pack commit `c7bb230f21d14d26859d0b92548f089b3a493ad9`
- Minecraft `1.21.1`, NeoForge `21.1.248`, Java `21`
- BlueMap feature backport `5.22-feature.backport-5.23-stateless-java-web-server-46`,
  commit `7e07f4e74ec1e92a6ead9aa1e66054af3e133aac`, API commit
  `285c9a60eff3ac2b0cab308ce1058d1565be0971`
- Botany Pots `21.1.44`, 1,068,816-byte JAR, SHA-256
  `45b23ac195511f724f62ab5f0c2d7a1c2c2403ff324a7403a1142e28a7d65edd`

Runtime activation requires the exact BlueMap version/commit and exact
Botany Pots mod ID, size, and SHA-256. Missing, duplicate, mismatched, or
unreadable core input leaves BlueMap's ordinary stock renderer untouched.

## Safety contract

- Only inventory slots 0 (soil) and 1 (seed/crop) affect the overlay. The NBT
  decoder bounds the 15-slot inventory structure and ignores unusual but
  structurally bounded data in optically irrelevant slots 2–14.
- The stock shell is emitted first. Crop and soil overlays are transactional;
  invalid resources or rendering failures retain the shell and never leave
  partial overlay geometry. BlueMap capacity failures propagate.
- Rendering is deterministic and static-mature. Growth, timers, outputs,
  tools, automation, particles, and animation phase are outside this tranche.
- The installed Botany Pots and vanilla resource packs supply all models and
  textures. The add-on bundles no upstream assets, recipes, classes, or JARs.
- Removing the add-on and performing a full JVM restart restores stock
  rendering without changing world data or requiring a client mod.

## Installation

Place the release JAR in BlueMap's `packs/` directory and fully restart the
Minecraft/BlueMap JVM. Do not install it as a NeoForge mod. A BlueMap-only
reload is insufficient for initial installation, upgrade, replacement, or
rollback because BlueMap caches same-ID add-on classloaders and BlueNBT caches
block-entity delegates.

No exporter, attestation ZIP, client override pack, or integration mod is
required by this bounded tranche.

## Development

Clone with submodules, or initialize the toolkit and adapter API gitlinks in an
existing checkout:

```bash
git submodule update --init --recursive -- \
  tooling/bluemap-addon-toolkit modules/bluemap-addon-adapter-api
```

Use Java 21 and the exact workspace BlueMap checkout. The end-of-tranche local gate is:

```bash
gradle --no-daemon -PbluemapSourcePath=../bluemap-backport focusedGate
```

The explicit artifact audit accepts only the exact Botany Pots JAR:

```bash
gradle --no-daemon -PbluemapSourcePath=../bluemap-backport \
  -PbotanyPotsJar=/absolute/path/to/botanypots-neoforge-1.21.1-21.1.44.jar \
  verifyPinnedArtifacts
```

The deterministic gallery contains 192 cells: 183 shells, six supported-pair
cells, and three shell-only fallback controls. See `gallery/README.md`.

## License

Copyright © 2026 Jan Günter and contributors. This project is licensed under
LGPL-2.1-only. See `LICENSE`, `NOTICE.md`, `THIRD_PARTY.md`, and
`provenance/upstreams.json`.
