/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.catalog;

import java.io.IOException;
import java.io.InputStream;

/** Production boundary for the bundled representative display catalog. */
@FunctionalInterface
public interface CatalogSource {

    NormalizedCatalog read(InputStream input) throws IOException;
}
