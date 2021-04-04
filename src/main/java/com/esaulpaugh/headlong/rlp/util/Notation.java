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
package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.rlp.DataType;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
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

    private static final String BEGIN_LIST = "[";
    private static final String END_LIST = "]";
    private static final String BEGIN_STRING = "'";
    private static final String END_STRING = "'";
    private static final String DELIMITER = ",";

    private static final String SHORT_LIST_START = BEGIN_LIST + ' ';

    private static final String DELIMITER_SPACE = DELIMITER + ' ';
    private static final String STRING_DELIMIT_SPACE = END_STRING + DELIMITER_SPACE;
    private static final String STRING_DELIMIT = END_STRING + DELIMITER;

    private static final String SHORT_LIST_END = ' ' + END_LIST + DELIMITER_SPACE;
    private static final String SHORT_LIST_END_NO_SPACE = ' ' + END_LIST + DELIMITER;
    private static final String LONG_LIST_END = END_LIST + DELIMITER;

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

    public static Notation forEncoding(byte[] encoding) {
        return forEncoding(encoding, 0, encoding.length);
    }

    public static Notation forEncoding(final byte[] buffer, final int index, int end) {
        if(index >= 0) {
            end = Math.min(buffer.length, end);
            if (index <= end) {
                StringBuilder sb = new StringBuilder(BEGIN_NOTATION);
                buildLongList(sb, buffer, index, end, 0);
                return new Notation(sb.append(END_NOTATION).toString());
            }
            throw new IllegalArgumentException("index > end: " + index + " > " + end);
        }
        throw new ArrayIndexOutOfBoundsException(index);
    }

    public static Notation forObjects(Object... objects) {
        return forEncoding(RLPEncoder.encodeSequentially(objects));
    }

    public static Notation forObjects(Iterable<Object> objects) {
        return forEncoding(RLPEncoder.encodeSequentially(objects));
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
        if(!LENIENT && len == 1 && DataType.isSingleByte(data[from])) { // same as (data[from] & 0xFF) < 0x80
            throw new IllegalArgumentException("invalid rlp for single byte @ " + (from - 1)); // item prefix is 1 byte
        }
        sb.append(BEGIN_STRING).append(Strings.encode(data, from, len, Strings.HEX))
                .append(addSpace ? STRING_DELIMIT_SPACE : STRING_DELIMIT);
        return to;
    }

    private static int buildLongList(StringBuilder sb, byte[] data, int dataIndex, int end, int depth) {
        if(depth != 0) {
            sb.append(BEGIN_LIST);
        }
        buildListContent(sb, dataIndex, end, data, false, depth + 1);
        if(depth != 0) {
            sb.append(getLinePadding(depth)).append(LONG_LIST_END);
        }
        return end;
    }

    private static int buildShortList(StringBuilder sb, byte[] data, int dataIndex, int end, int depth, boolean addSpace) {
        sb.append(SHORT_LIST_START);
        buildListContent(sb, dataIndex, end, data, true, depth + 1);
        sb.append(addSpace ? SHORT_LIST_END : SHORT_LIST_END_NO_SPACE);
        return end;
    }

    private static void buildListContent(StringBuilder sb, final int dataIndex, final int end, byte[] data, boolean shortList, final int elementDepth) {
        final String elementPrefix = shortList ? null : getLinePadding(elementDepth);
        for (int i = dataIndex; i < end; ) {
            if(!shortList) {
                sb.append(elementPrefix);
            }
            final byte lead = data[i];
            final DataType type = DataType.type(lead);
            if(type == DataType.SINGLE_BYTE) {
                i = buildString(sb, data, i, i + 1, shortList);
            } else {
                if(type.isLong && shortList) {
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
            sb.replace(sb.length() - (shortList ? DELIMITER_SPACE : DELIMITER).length(), sb.length(), ""); // trim
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
    private static final int LIST = 0;
    private static final int STRING = 1;

    /**
     * Returns the object hierarchy represented by the notation.
     *
     * @param notation the notation to be parsed
     * @return the hierarchy of objects
     */
    public static List<Object> parse(String notation) {
        List<Object> topLevelObjects = new ArrayList<>(); // a sequence (as in encodeSequentially)
        parse(notation, 0, notation.length(), topLevelObjects, new int[2]);
        return topLevelObjects;
    }

    private static int parse(String notation, int i, final int end, List<Object> parent, int[] resultHolder) {

        int nextArrayEnd = -1;

        while (i < end) {
            if(!findNextObject(notation, i, resultHolder)) {
                return Integer.MAX_VALUE;
            }

            if(i > nextArrayEnd) { // only update nextArrayEnd when i has passed it
                nextArrayEnd = notation.indexOf(Notation.END_LIST, i);
                if(nextArrayEnd < 0) {
                    nextArrayEnd = Integer.MAX_VALUE;
                }
            }

            int nextObjectIndex = resultHolder[0];

            if(nextArrayEnd < nextObjectIndex) {
                return nextArrayEnd + END_LIST.length();
            }

            if(STRING == resultHolder[1] /* nextObjectType */) {
                int datumStart = nextObjectIndex + BEGIN_STRING.length();
                int datumEnd = notation.indexOf(END_STRING, datumStart);
                if(datumEnd < 0) {
                    throw new IllegalArgumentException("unterminated string @ " + datumStart);
                }
                parent.add(Strings.decode(notation.substring(datumStart, datumEnd)));
                i = datumEnd + END_STRING.length();
            } else {
                List<Object> childList = new ArrayList<>();
                i = parse(notation, nextObjectIndex + BEGIN_LIST.length(), end, childList, resultHolder);
                parent.add(childList);
            }
        }

        return end + END_LIST.length();
    }

    private static boolean findNextObject(String notation, int startIndex, int[] resultHolder) {
        final int indexString = notation.indexOf(BEGIN_STRING, startIndex);
        final int indexList = notation.indexOf(BEGIN_LIST, startIndex);
        if(indexString == -1) {
            if(indexList == -1) {
                return false;
            }
        } else if(indexString < indexList || indexList == -1) {
            resultHolder[0] = indexString;
            resultHolder[1] = STRING;
            return true;
        }
        // indexString == -1 || indexList <= indexString
        resultHolder[0] = indexList;
        resultHolder[1] = LIST;
        return true;
    }
}
