/*
   Copyright 2019 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** A JSON-like notation for RLP items. Call {@link #parse()} to get the raw object hierarchy. */
public final class Notation {

    private static final RLPDecoder DECODER = RLPDecoder.RLP_LENIENT; // keep lenient so RLPItem.toString() doesn't throw, and to help with debugging

    private static final String BEGIN_NOTATION = "(";
    private static final String END_NOTATION = "\n)";
    private static final char BEGIN_LIST = '[';
    private static final char END_LIST = ']';
    private static final char BEGIN_STRING = '\'';
    private static final char END_STRING = BEGIN_STRING;
    private static final String DELIMITER = ",";
    private static final String SPACE = " ";

    private static final String[] LINE_PADDING_CACHE;

    static {
        LINE_PADDING_CACHE = new String[8]; // pick any cache size
        for (int i = 0; i < LINE_PADDING_CACHE.length; i++) {
            LINE_PADDING_CACHE[i] = newLinePadding(i);
        }
    }

    private final String value;

    private Notation(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public List<Object> parse() {
        return Notation.parse(value);
    }

    public static Notation forEncoding(byte[] rlp) {
        return new Notation(encodeToString(rlp));
    }

    public static String encodeToString(byte[] rlp) {
        return encodeToString(rlp, 0, rlp.length);
    }

    static String encodeToString(final byte[] buffer, final int index, int end) {
        StringBuilder sb = new StringBuilder(BEGIN_NOTATION);
        buildLongList(sb, buffer, index, end, 0);
        return sb.append(END_NOTATION).toString();
    }

    public static Notation forObjects(Object... objects) {
        return forEncoding(RLPEncoder.sequence(objects));
    }

    public static Notation forObjects(Iterable<Object> objects) {
        return forEncoding(RLPEncoder.sequence(objects));
    }

    private static int buildString(StringBuilder sb, byte[] data, int from, int to, boolean addSpace) {
        final int len = to - from;
        if (!DECODER.lenient && len == 1 && DataType.isSingleByte(data[from])) { // same as (data[from] & 0xFF) < 0x80
            throw new IllegalArgumentException("invalid rlp for single byte @ " + (from - 1)); // item prefix is 1 byte
        }
        sb.append(BEGIN_STRING).append(Strings.encode(data, from, len, Strings.HEX))
                .append(addSpace ? END_STRING + DELIMITER + SPACE : END_STRING + DELIMITER);
        return to;
    }

    private static int buildLongList(StringBuilder sb, byte[] data, int dataIndex, int end, int depth) {
        if (depth != 0) {
            sb.append(BEGIN_LIST);
        }
        buildListContent(sb, dataIndex, end, data, false, depth + 1);
        if (depth != 0) {
            sb.append(getLinePadding(depth)).append(END_LIST + DELIMITER);
        }
        return end;
    }

    private static int buildShortList(StringBuilder sb, byte[] data, int dataIndex, int end, int depth, boolean addSpace) {
        sb.append(BEGIN_LIST + SPACE);
        buildListContent(sb, dataIndex, end, data, true, depth + 1);
        sb.append(addSpace
                ? SPACE + END_LIST + DELIMITER + SPACE
                : SPACE + END_LIST + DELIMITER);
        return end;
    }

    private static void buildListContent(StringBuilder sb, final int dataIndex, final int end, byte[] data, boolean shortList, final int elementDepth) {
        if (end == dataIndex) return;
        final String elementPrefix = shortList ? null : getLinePadding(elementDepth);
        int i = dataIndex;
        do {
            if (!shortList) {
                sb.append(elementPrefix);
            }
            final byte lead = data[i];
            final DataType type = DataType.type(lead);
            if (type == DataType.SINGLE_BYTE) {
                i = buildString(sb, data, i, i + 1, shortList);
            } else {
                if (type.isLong && shortList) {
                    throw new IllegalArgumentException("long element found in short list");
                }
                final int elementDataIdx = i + 1 + /* lengthOfLength */ (type.isLong ? lead - type.offset : 0);
                final int elementEnd = DECODER.wrap(data, i, end).endIndex;
                i = type.isString
                        ? buildString(sb, data, elementDataIdx, elementEnd, shortList)
                        : type.isLong
                            ? buildLongList(sb, data, elementDataIdx, elementEnd, elementDepth)
                            : buildShortList(sb, data, elementDataIdx, elementEnd, elementDepth, shortList);
            }
        } while (i < end);
        sb.setLength(sb.length() - (shortList ? DELIMITER + SPACE : DELIMITER).length()); // trim
    }

    private static String getLinePadding(int depth) {
        return depth < LINE_PADDING_CACHE.length ? LINE_PADDING_CACHE[depth] : newLinePadding(depth);
    }

    @SuppressWarnings("deprecation")
    private static String newLinePadding(int depth) {
        byte[] prefix = new byte[1 + (depth * 2)]; // 2 spaces per level
        Arrays.fill(prefix, (byte) ' ');
        prefix[0] = (byte) '\n';
        return new String(prefix, 0, 0, prefix.length);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Notation && (((Notation) o).value).equals(this.value);
    }

    @Override
    public String toString() {
        return value;
    }
// ---------------------------------------------------------------------------------------------------------------------
    /**
     * Returns the object hierarchy represented by the notation.
     *
     * @param notation the notation to be parsed
     * @return the hierarchy of objects
     */
    public static List<Object> parse(String notation) {
        List<Object> topLevelObjects = new ArrayList<>(); // a sequence (as in RLPEncoder.sequence)
        if (parse(notation, 0, topLevelObjects, 0) != Integer.MAX_VALUE) {
            throw new IllegalArgumentException("syntax error");
        }
        return topLevelObjects;
    }

    private static final int MAX_DEPTH = 768;

    private static int parse(final String notation, int i, final List<Object> parent, final int depth) {
        do {
            switch (notation.charAt(i++)) {
            case BEGIN_STRING:
                final int datumEnd = notation.indexOf(END_STRING, i);
                if (datumEnd < 0) {
                    throw new IllegalArgumentException("unterminated string @ " + i);
                }
                parent.add(FastHex.decode(notation, i, datumEnd - i));
                i = datumEnd + 1;
                continue;
            case BEGIN_LIST:
                if (depth >= MAX_DEPTH) {
                    throw new IllegalArgumentException("exceeds max depth of " + MAX_DEPTH);
                }
                List<Object> childList = new ArrayList<>();
                i = parse(notation, i, childList, depth + 1);
                parent.add(childList);
                continue;
            case END_LIST: return i;
            }
        } while (i < notation.length());
        return Integer.MAX_VALUE;
    }
}
