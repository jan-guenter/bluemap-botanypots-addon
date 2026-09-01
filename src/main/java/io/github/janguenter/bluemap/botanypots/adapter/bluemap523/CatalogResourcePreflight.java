/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import de.bluecolored.bluemap.core.map.hires.block.BlockRendererType;
import de.bluecolored.bluemap.core.resources.ResourcePath;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.ResourcePack;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.Variant;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Element;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Face;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.model.Model;
import de.bluecolored.bluemap.core.resources.pack.resourcepack.texture.Texture;
import de.bluecolored.bluemap.core.util.Key;
import de.bluecolored.bluemap.core.world.BlockState;
import io.github.janguenter.bluemap.botanypots.catalog.CatalogEntry;
import io.github.janguenter.bluemap.botanypots.catalog.DisplayNode;
import io.github.janguenter.bluemap.botanypots.catalog.NormalizedCatalog;
import io.github.janguenter.bluemap.botanypots.model.ResourceId;
import io.github.janguenter.bluemap.botanypots.profile.RouteCatalog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Post-bake validation of every effective shell, model, texture, and tint route. */
final class CatalogResourcePreflight {

    private static final List<String> FACINGS = List.of("south", "east", "north", "west");

    private CatalogResourcePreflight() {
    }

    static Result validate(
            ResourcePack resourcePack,
            RouteCatalog routes,
            NormalizedCatalog catalog
    ) {
        if (!stockShells(resourcePack, routes)) {
            return new Result(false, Set.of());
        }
        Set<String> invalidProfiles = new HashSet<>();
        for (CatalogEntry entry : catalog.entries()) {
            if (entry.display().members().stream().anyMatch(node -> !node(resourcePack, node))) {
                invalidProfiles.add(entry.profile());
            }
        }
        return new Result(true, Set.copyOf(invalidProfiles));
    }

    private static boolean stockShells(ResourcePack pack, RouteCatalog routes) {
        Set<ResourcePath<Model>> validatedModels = new HashSet<>();
        int states = 0;
        try {
            for (ResourceId route : routes.routes()) {
                de.bluecolored.bluemap.core.resources.pack.resourcepack.blockstate.BlockState
                        resource = pack.getBlockStates().get(Key.parse(route.value()));
                if (resource == null) {
                    return false;
                }
                for (boolean waterlogged : List.of(false, true)) {
                    for (int level = 0; level < 16; level++) {
                        for (String facing : FACINGS) {
                            BlockState state = BlockState.fromString(route.value()
                                    + "[facing=" + facing + ",level=" + level
                                    + ",waterlogged=" + waterlogged + "]");
                            List<Variant> selected = FixedVariantSelector.select(resource, state);
                            if (selected.isEmpty()) {
                                return false;
                            }
                            for (Variant variant : selected) {
                                if (variant.getRenderer() != BlockRendererType.DEFAULT
                                        || !model(pack, variant.getModel(), null, variant,
                                        validatedModels)) {
                                    return false;
                                }
                            }
                            states++;
                        }
                    }
                }
            }
        } catch (RuntimeException exception) {
            return false;
        }
        return states == RouteCatalog.EXPECTED_LEGAL_STATES;
    }

    private static boolean node(ResourcePack pack, DisplayNode node) {
        try {
            if (node.fluid() != null && pack.getTextures().get(
                    Key.parse(node.fluid().stillTexture().value())) == null) {
                return false;
            }
            return ordinary(pack, node);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean ordinary(ResourcePack pack, DisplayNode node) {
        BlockState displayState = BlockState.fromString(node.blockState());
        List<Variant> variants;
        if (node.modelOverride() != null) {
            variants = List.of(new Variant(new ResourcePath<Model>(
                    node.modelOverride().value())));
        } else {
            variants = FixedVariantSelector.select(
                    pack.getBlockStates().get(displayState.getId()), displayState);
        }
        if (variants.isEmpty()) {
            return false;
        }
        boolean geometry = node.fluid() != null;
        Set<ResourcePath<Model>> validated = new HashSet<>();
        for (Variant variant : variants) {
            if (variant.getRenderer() == BlockRendererType.LIQUID && node.fluid() != null) {
                continue;
            }
            if (variant.getRenderer() != BlockRendererType.DEFAULT
                    || !model(pack, variant.getModel(), node, variant, validated)) {
                return false;
            }
            geometry = true;
        }
        return geometry;
    }

    private static boolean model(
            ResourcePack pack,
            ResourcePath<Model> path,
            DisplayNode display,
            Variant variant,
            Set<ResourcePath<Model>> validated
    ) {
        if (display == null && !validated.add(path)) {
            return true;
        }
        Model model = path.getResource(pack.getModels()::get);
        if (model == null || model.getElements() == null) {
            return false;
        }
        boolean foundFace = false;
        for (Element element : model.getElements()) {
            if (element == null) {
                continue;
            }
            for (Face face : element.getFaces().values()) {
                if (face == null || display != null && !included(display, face, variant)) {
                    continue;
                }
                ResourcePath<Texture> texture = face.getTexture().getTexturePath(
                        model.getTextures()::get);
                if (texture == null || texture.getResource(pack.getTextures()::get) == null
                        || display != null && !tintCovered(display, face.getTintindex())) {
                    return false;
                }
                foundFace = true;
            }
        }
        return foundFace;
    }

    private static boolean included(DisplayNode display, Face face, Variant variant) {
        return VirtualDisplayModelEmitter.included(display.faces(), face, variant);
    }

    private static boolean tintCovered(DisplayNode display, int index) {
        if (index < 0 || display.tint() != null && display.tint().allArgb() != null) {
            return true;
        }
        return display.tint() != null && display.tint().indices().containsKey(index);
    }

    record Result(boolean stockShellsValid, Set<String> invalidProfiles) {
    }
}
