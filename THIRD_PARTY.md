# Third-party and provenance inventory

| Component | Exact identity / role | License | Bundled |
| --- | --- | --- | --- |
| BlueMap | Feature backport `5.22-feature.backport-5.23-stateless-java-web-server-46`, commit `7e07f4e74ec1e92a6ead9aa1e66054af3e133aac`, API commit `285c9a60eff3ac2b0cab308ce1058d1565be0971`; host ABI and adapted resource-model mechanics | MIT | License notice only |
| BlueMap Add-on Adapter API | Four exact source-compiled integration primitives; `0.1.0-alpha.2`, commit `e81f08bc4bfbf02d810ec8949a019130e2e61634`, source tree `2f974c9bb2ba13888d69682f86f30f58922d30eb` | MIT | Source only |
| Botany Pots | `21.1.44`, 1,068,816 bytes, SHA-256 `45b23ac195511f724f62ab5f0c2d7a1c2c2403ff324a7403a1142e28a7d65edd`; shell, NBT, and display authority | LGPL-2.1-only | No |
| Minecraft | `1.21.1`; vanilla dirt, sand, wheat, and cactus resources | Proprietary runtime | No |

The production and sources-JAR audits permit only project classes plus the
four exact shared Adapter API classes, and reject every other foreign class,
nested JAR, third-party asset, PNG file, and NeoForge/Minecraft class. The
canonical machine-readable record is `provenance/upstreams.json`.
