# Third-party and provenance inventory

| Component | Exact identity / role | License | Bundled |
| --- | --- | --- | --- |
| BlueMap | Backport 5.22, commit `9be321df995a1103808621d529eb72773e719d4d`; internal ABI and adapted resource-model mechanics | MIT | License notice only |
| Botany Pots | `21.1.44`, 1,068,816 bytes, SHA-256 `45b23ac195511f724f62ab5f0c2d7a1c2c2403ff324a7403a1142e28a7d65edd`; shell, NBT, and display authority | LGPL-2.1-only | No |
| Minecraft | `1.21.1`; vanilla dirt, sand, wheat, and cactus resources | Proprietary runtime | No |

The production and sources-JAR audits reject foreign classes, nested JARs,
third-party assets, PNG files, and NeoForge/Minecraft classes. The canonical
machine-readable record is `provenance/upstreams.json`.
