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
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** A JSON-like notation for RLP items. Call {@link #parse()} get back the raw object hierarchy. */
public final class Notation {

    private static final boolean LENIENT = true; // keep lenient so RLPItem.toString() doesn't throw, and to help with debugging

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

    private static IllegalArgumentException exceedsContainer(int index, long end, int containerEnd) {
        return new IllegalArgumentException("element @ index " + index + " exceeds its container: " + end + " > " + containerEnd);
    }

    private static int getShortElementEnd(int elementDataIndex, final int elementDataLen, final int containerEnd) {
        final int end = elementDataIndex + elementDataLen;
        if (end <= containerEnd) {
            return end;
        }
        throw exceedsContainer(elementDataIndex - 1, end, containerEnd);
    }

    private static int getLongElementEnd(byte[] data, final int leadByteIndex, final int dataIndex, final int containerEnd) {
        if (dataIndex <= containerEnd) {
            final int lengthIndex = leadByteIndex + 1;
            final int lengthLen = dataIndex - lengthIndex;
            final long dataLenLong = Integers.getLong(data, leadByteIndex + 1, lengthLen, LENIENT);
            final long end = lengthIndex + lengthLen + dataLenLong;
            if (end > containerEnd) {
                throw exceedsContainer(leadByteIndex, end, containerEnd);
            }
            final int dataLen = (int) dataLenLong;
            if (dataLen >= DataType.MIN_LONG_DATA_LEN) {
                return (int) end;
            }
            throw new IllegalArgumentException("long element data length must be " + DataType.MIN_LONG_DATA_LEN
                    + " or greater; found: " + dataLen + " for element @ " + leadByteIndex);
        }
        throw exceedsContainer(leadByteIndex, dataIndex, containerEnd);
    }

    private static int buildString(StringBuilder sb, byte[] data, int from, int to, boolean addSpace) {
        final int len = to - from;
        if (!LENIENT && len == 1 && DataType.isSingleByte(data[from])) { // same as (data[from] & 0xFF) < 0x80
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
        final String elementPrefix = shortList ? null : getLinePadding(elementDepth);
        int i = dataIndex;
        while (i < end) {
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
                final int diff = lead - type.offset;
                final int elementDataIdx = i + 1 + /* lengthOfLength */ (type.isLong ? diff : 0);
                final int elementEnd = type.isLong
                        ? getLongElementEnd(data, i, elementDataIdx, end)
                        : getShortElementEnd(elementDataIdx, diff, end);
                i = type.isString
                        ? buildString(sb, data, elementDataIdx, elementEnd, shortList)
                        : type.isLong
                            ? buildLongList(sb, data, elementDataIdx, elementEnd, elementDepth)
                            : buildShortList(sb, data, elementDataIdx, elementEnd, elementDepth, shortList);
            }
        }
        if (/* hasElement */ dataIndex != end) {
            sb.setLength(sb.length() - (shortList ? DELIMITER + SPACE : DELIMITER).length()); // trim
        }
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
        parse(notation, 0, notation.length(), topLevelObjects, 0);
        return topLevelObjects;
    }

    private static final int MAX_DEPTH = 768;

    private static int parse(final String notation, int i, final int end, final List<Object> parent, int depth) {
        while (i < end) {
            switch (notation.charAt(i)) {
            case BEGIN_STRING:
                final int datumStart = i + 1;
                final int datumEnd = notation.indexOf(END_STRING, datumStart);
                if (datumEnd < 0) {
                    throw new IllegalArgumentException("unterminated string @ " + datumStart);
                }
                parent.add(FastHex.decode(notation, datumStart, datumEnd - datumStart));
                i = datumEnd + 1;
                continue;
            case BEGIN_LIST:
                if (depth >= MAX_DEPTH) {
                    throw new IllegalArgumentException("exceeds max depth of " + MAX_DEPTH);
                }
                List<Object> childList = new ArrayList<>();
                i = parse(notation, i + 1, end, childList, depth + 1);
                parent.add(childList);
                continue;
            case END_LIST:
                return i + 1;
            default:
                i++;
            }
        }
        return Integer.MAX_VALUE;
    }
}
