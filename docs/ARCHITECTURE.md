# Architecture

The add-on has four bounded layers:

1. An exact artifact detector activates only for Botany Pots 21.1.44.
2. A fixed 183-ID route allowlist redirects every legal pot state to one
   synthetic BlueMap renderer while preserving the original state.
3. A bounded NBT decoder retains only visual inventory slots 0 and 1, and a
   four-row catalog resolves only dirt+wheat or sand+cactus.
4. The BlueMap 5.22 adapter renders the installed stock shell, then emits the
   selected installed-resource crop and soil overlays with seed 42 and the
   exact Botany display transforms.

The block-entity DTO is registered during the add-on entrypoint. At resource
activation the adapter probes the process-wide BlueNBT instance and requires
the exact DTO with retained slots 0/1. BlueNBT exposes no cache refresh API, so
a failed hot-add remains inactive until a full JVM restart.

Crop renders before soil, matching Botany's order. Each overlay sequence is
transactional and the stock shell is outside the rollback boundary. Any other
pair, malformed NBT, missing model/texture, or unsupported renderer leaves only
the installed stock shell. The outer BlueMap block renderer remains responsible
for the normal waterlogged pass after the custom renderer returns.

There is no runtime recipe scan, exporter, optional profile, external
attestation, or client-only resource pack in this tranche.
