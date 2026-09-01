# Release procedure

1. Run `focusedGate`, freeze the tree, and obtain an independent read-only
   audit of the exact two-pair/183-shell contract.
2. Open a pull request. CI is authoritative for the clean build, tests,
   package boundaries, and publication metadata.
3. Deploy the CI candidate JAR alone to the disposable BlueMap `packs/`
   directory and perform a full JVM restart.
4. Build, verify, render, and visually inspect the 192-cell gallery. Open the
   exact BlueMap link before presenting it.
5. Remove the candidate and fully restart again; require all 183 routes to
   return to stock rendering.
6. After owner acceptance, increase `addon_version` only through a reviewed
   pull request and create annotated tag `v<addon_version>` at that commit.
   Publish only the first-party JAR, sources JAR, POM, and module metadata.

No exporter, attestation ZIP, client override pack, or third-party asset is a
release input for this bounded tranche.

## 0.1.0-alpha.2 acceptance record

On 2026-09-01, the owner accepted the Botany Pots area in the combined ATMons
1.2.0 BlueMap 5.23 integration gallery. The migration changes the host adapter
and shared-source ownership, not the bounded rendering contract. The exact
accepted production JAR is 150,791 bytes with SHA-256
`1dfa631f22eb65aa953d3f554d2b4c02e0fab000c1461903cad56b419d12d3c4`.
The local clean release build reproduced those bytes exactly.

## 0.1.0-alpha.1 acceptance record

On 2026-08-13, the owner visually accepted the bounded disposable-staging
result: the 183-shell review grid plus dirt with mature wheat and sand with
mature cactus on representative basic, hopper, and waxed shells. This is not
acceptance of an exhaustive crop/soil matrix or of every legal shell state.

The accepted runtime candidate was
`bluemap-botanypots-addon-0.1.0-alpha.1-preview.2.jar`, 148,769 bytes, with
SHA-256
`6d2af050e9fdacabbdcdf307cb9a67c2070dd1f7601fc160c301defce5a126e5`.
After acceptance, the independent release audit found and corrected a narrow
fail-closed edge case: a crop-resource emission failure could otherwise leave
a soil-only partial overlay. The correction does not change the accepted
successful dirt+wheat or sand+cactus rendering path. The release gate must
cover the correction with focused regression tests and preserve those
successful-gallery semantics; the release JAR is therefore a distinct byte
identity from the visually accepted preview.
