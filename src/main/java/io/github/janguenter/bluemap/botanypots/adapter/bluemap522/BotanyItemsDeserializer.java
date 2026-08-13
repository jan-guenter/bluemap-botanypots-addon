/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap522;

import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TagType;
import de.bluecolored.bluenbt.TypeDeserializer;
import io.github.janguenter.bluemap.botanypots.model.BotanyInventoryProjection;
import io.github.janguenter.bluemap.botanypots.model.ItemProjection;
import io.github.janguenter.bluemap.botanypots.model.ResourceId;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Bounded decoder that semantically interprets only visual slots 0 and 1. */
final class BotanyItemsDeserializer implements TypeDeserializer<BotanyInventoryProjection> {

    private static final int MAX_FIELD_NAME_CHARS = 64;
    private static final int MAX_ITEM_ID_CHARS = 256;

    @Override
    public BotanyInventoryProjection read(NBTReader reader) throws IOException {
        try {
            if (reader.peek() != TagType.LIST) {
                throw StrictNbtBudget.rejected();
            }
            int length = reader.beginList();
            if (length < 0 || length > BotanyInventoryProjection.MAXIMUM_ENTRIES) {
                throw StrictNbtBudget.rejected();
            }
            Map<Integer, ItemProjection> visual = new LinkedHashMap<>();
            Set<Integer> seenSlots = new HashSet<>();
            StrictNbtBudget budget = new StrictNbtBudget();
            for (int index = 0; index < length; index++) {
                if (reader.peek() != TagType.COMPOUND) {
                    throw StrictNbtBudget.rejected();
                }
                Entry entry = readEntry(reader, budget);
                if (entry.slot() != null && !seenSlots.add(entry.slot())
                        && entry.slot() <= 1) {
                    throw StrictNbtBudget.rejected();
                }
                if (entry.stack() != null) {
                    visual.put(entry.slot(), entry.stack());
                }
            }
            if (reader.peek() != TagType.END) {
                throw StrictNbtBudget.rejected();
            }
            reader.endList();
            return new BotanyInventoryProjection(true, visual);
        } catch (IOException | RuntimeException exception) {
            throw StrictNbtBudget.rejected(exception);
        }
    }

    private static Entry readEntry(NBTReader reader, StrictNbtBudget budget) throws IOException {
        Integer slot = null;
        String id = null;
        Integer count = null;
        boolean idSeen = false;
        boolean countSeen = false;
        boolean componentsSeen = false;
        boolean componentBearing = false;
        boolean semanticMalformed = false;
        boolean unknownSeen = false;
        reader.beginCompound();
        while (reader.peek() != TagType.END) {
            String name = reader.name();
            if (name.length() > MAX_FIELD_NAME_CHARS) {
                throw StrictNbtBudget.rejected();
            }
            switch (name) {
                case "Slot" -> {
                    if (slot != null || reader.peek() != TagType.BYTE) {
                        throw StrictNbtBudget.rejected();
                    }
                    slot = Byte.toUnsignedInt(reader.nextByte());
                }
                case "id" -> {
                    if (idSeen) {
                        semanticMalformed = true;
                        budget.discard(reader);
                        continue;
                    }
                    idSeen = true;
                    if (reader.peek() == TagType.STRING) {
                        id = reader.nextString();
                        semanticMalformed |= id.length() > MAX_ITEM_ID_CHARS;
                    } else {
                        semanticMalformed = true;
                        budget.discard(reader);
                    }
                }
                case "count" -> {
                    if (countSeen) {
                        semanticMalformed = true;
                        budget.discard(reader);
                        continue;
                    }
                    countSeen = true;
                    if (reader.peek() == TagType.INT) {
                        count = reader.nextInt();
                    } else {
                        semanticMalformed = true;
                        budget.discard(reader);
                    }
                }
                case "components" -> {
                    if (componentsSeen) {
                        semanticMalformed = true;
                        budget.discard(reader);
                        continue;
                    }
                    componentsSeen = true;
                    if (reader.peek() == TagType.COMPOUND) {
                        Components components = readComponents(reader, budget);
                        componentBearing = components.nonEmpty();
                        semanticMalformed |= !components.validIds();
                    } else {
                        semanticMalformed = true;
                        budget.discard(reader);
                    }
                }
                default -> {
                    unknownSeen = true;
                    budget.discard(reader);
                }
            }
        }
        reader.endCompound();
        if (slot == null || slot > BotanyInventoryProjection.MAXIMUM_SLOT) {
            throw StrictNbtBudget.rejected();
        }
        if (slot > 1) {
            return new Entry(slot, null);
        }
        if (semanticMalformed || unknownSeen || id == null || count == null) {
            throw StrictNbtBudget.rejected();
        }
        try {
            return new Entry(slot, new ItemProjection(
                    ResourceId.parse(id), count, componentBearing
            ));
        } catch (IllegalArgumentException exception) {
            throw StrictNbtBudget.rejected(exception);
        }
    }

    private static Components readComponents(
            NBTReader reader,
            StrictNbtBudget budget
    ) throws IOException {
        boolean nonEmpty = false;
        boolean validIds = true;
        reader.beginCompound();
        while (reader.peek() != TagType.END) {
            nonEmpty = true;
            String componentId = reader.name();
            if (componentId.length() > 256) {
                throw StrictNbtBudget.rejected();
            }
            try {
                ResourceId.parse(componentId);
            } catch (IllegalArgumentException exception) {
                validIds = false;
            }
            budget.discard(reader);
        }
        reader.endCompound();
        return new Components(nonEmpty, validIds);
    }

    private record Entry(Integer slot, ItemProjection stack) {
    }

    private record Components(boolean nonEmpty, boolean validIds) {
    }
}
