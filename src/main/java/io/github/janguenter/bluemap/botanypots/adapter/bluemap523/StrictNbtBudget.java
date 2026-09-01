/*
 * SPDX-License-Identifier: LGPL-2.1-only
 */
package io.github.janguenter.bluemap.botanypots.adapter.bluemap523;

import de.bluecolored.bluenbt.NBTReader;
import de.bluecolored.bluenbt.TagType;

import java.io.IOException;

/** Project-authored bounded traversal for component payloads that are intentionally opaque. */
final class StrictNbtBudget {

    static final int MAX_DEPTH = 12;
    static final int MAX_TAGS = 512;
    static final int MAX_LIST_LENGTH = 256;
    static final int MAX_ARRAY_LENGTH = 4_096;
    static final long MAX_ARRAY_BYTES = 65_536L;
    static final int MAX_STRING_CHARS = 4_096;
    static final int MAX_NAME_CHARS = 256;

    private static final byte[] NO_BYTES = new byte[0];
    private static final int[] NO_INTS = new int[0];
    private static final long[] NO_LONGS = new long[0];

    private int tags;
    private long arrayBytes;

    void discard(NBTReader reader) throws IOException {
        discard(reader, 0);
    }

    private void discard(NBTReader reader, int depth) throws IOException {
        visit();
        switch (reader.peek()) {
            case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> reader.skip();
            case STRING -> {
                if (reader.nextString().length() > MAX_STRING_CHARS) {
                    throw rejected();
                }
            }
            case BYTE_ARRAY -> account(reader.nextByteArray(NO_BYTES), Byte.BYTES);
            case INT_ARRAY -> account(reader.nextIntArray(NO_INTS), Integer.BYTES);
            case LONG_ARRAY -> account(reader.nextLongArray(NO_LONGS), Long.BYTES);
            case LIST -> discardList(reader, depth);
            case COMPOUND -> discardCompound(reader, depth);
            case END -> throw rejected();
            default -> throw rejected();
        }
    }

    private void discardList(NBTReader reader, int depth) throws IOException {
        requireDepth(depth);
        int length = reader.beginList();
        if (length < 0 || length > MAX_LIST_LENGTH) {
            throw rejected();
        }
        for (int index = 0; index < length; index++) {
            if (reader.peek() == TagType.END) {
                throw rejected();
            }
            discard(reader, depth + 1);
        }
        if (reader.peek() != TagType.END) {
            throw rejected();
        }
        reader.endList();
    }

    private void discardCompound(NBTReader reader, int depth) throws IOException {
        requireDepth(depth);
        reader.beginCompound();
        while (reader.peek() != TagType.END) {
            String name = reader.name();
            if (name.length() > MAX_NAME_CHARS) {
                throw rejected();
            }
            discard(reader, depth + 1);
        }
        reader.endCompound();
    }

    private void visit() throws IOException {
        if (++tags > MAX_TAGS) {
            throw rejected();
        }
    }

    private void account(int length, int width) throws IOException {
        if (length < 0 || length > MAX_ARRAY_LENGTH) {
            throw rejected();
        }
        arrayBytes += (long) length * width;
        if (arrayBytes > MAX_ARRAY_BYTES) {
            throw rejected();
        }
    }

    private static void requireDepth(int depth) throws IOException {
        if (depth >= MAX_DEPTH) {
            throw rejected();
        }
    }

    static IOException rejected() {
        return new IOException("Rejected malformed or oversized Botany Pots retained NBT");
    }

    static IOException rejected(Exception cause) {
        return new IOException("Rejected malformed or oversized Botany Pots retained NBT", cause);
    }
}
