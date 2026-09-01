# Provenance and clean-room boundary

The implementation is evidence-locked to All the Mons 1.2.0, the exact
BlueMap 5.23 feature backport, and Botany Pots 21.1.44. Exact identities are recorded in
`provenance/upstreams.json` and `exact-artifacts.json`.

The four normalized catalog rows contain only identifiers, transforms, and the
two owner-approved representative mappings. The production JAR contains no
upstream class, model, texture, recipe JSON, JAR, PNG, or pack override. All
geometry and textures are resolved from the operator-installed Botany Pots and
vanilla resource roots.

Resource-model mechanics adapted from BlueMap retain its MIT notice. Botany
display and persisted-inventory semantics are adapted under LGPL-2.1-only, so
the complete add-on and corresponding source are LGPL-2.1-only.

Four integration primitives compile from `bluemap-addon-adapter-api`
`0.1.0-alpha.2` at commit
`e81f08bc4bfbf02d810ec8949a019130e2e61634`, source tree
`2f974c9bb2ba13888d69682f86f30f58922d30eb`. Its MIT license is bundled; its
standalone JAR is not.
