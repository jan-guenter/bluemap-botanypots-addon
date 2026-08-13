# Agent guide

This standalone repository owns the bounded All the Mons 1.2.0 Botany Pots
BlueMap tranche. Read the workspace and portfolio guides, this README, and the
three files under `docs/` before changing it.

## Boundaries

- Java 21, Minecraft 1.21.1, NeoForge 21.1.248, exact BlueMap 5.22 backport
  commit `9be321df995a1103808621d529eb72773e719d4d`, and exact Botany Pots
  21.1.44 artifact identity.
- Own exactly the 183 `botanypots:*_botany_pot` IDs and 23,424 legal states.
- Support exactly dirt+wheat seeds and sand+cactus. Unknown or other contents
  are stock-shell-only. Do not add a recipe exporter, broad catalog, optional
  integration profile, custom-loader interpreter, or external attestation.
- Read only bounded slots 0/1 for optics. Preserve stock-first rendering,
  atomic overlays, deterministic mature transforms, and capacity propagation.
- Bundle no upstream class, JAR, model, texture, recipe, or other asset.
- Every install, replacement, upgrade, and rollback requires a full JVM
  restart. Removal plus restart must restore stock behavior.
- Do not change cluster state, another repository, remotes, tags, releases, or
  production systems from this repository.

## Validation cadence

Implement one coherent tranche, then run the single `focusedGate` documented
in README. PR CI is authoritative for clean packaging/publication checks.
Freeze the tree and obtain an independent read-only audit before any commit,
remote, pull request, release, or staging deployment.
