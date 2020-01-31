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

import com.esaulpaugh.headlong.exception.DecodeException;
import com.esaulpaugh.headlong.exception.UnrecoverableDecodeException;
import com.esaulpaugh.headlong.rlp.DataType;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * An object notation for RLP, not unlike JSON. Call {@link #parse()} to parse the notation into the original list of objects.
 */
public class Notation {

    private static final boolean LENIENT = true; // keep lenient so RLPItem.toString() doesn't throw, and to help with debugging

    private static final String BEGIN_NOTATION = "(";
    private static final String END_NOTATION = "\n)";

    static final String BEGIN_LIST = "{";
    static final String END_LIST = "}";
    static final String BEGIN_STRING = "'";
    static final String END_STRING = "'";

    private static final String BEGIN_LIST_SHORT = BEGIN_LIST + " ";

    private static final String DELIMITER = ", ";
    private static final String LIST_LONG_END_PLUS_DELIMITER = END_LIST + DELIMITER;
    private static final String LIST_SHORT_END_PLUS_DELIMITER = " " + LIST_LONG_END_PLUS_DELIMITER;
    private static final String STRING_END_PLUS_DELIMITER = END_STRING + DELIMITER;

    private static final String[] INDENTATION_CACHE;

    private static final String ELEMENT_INDENTATION = newIndentation(1);

    static {
        INDENTATION_CACHE = new String[8];
        for (int i = 0; i < INDENTATION_CACHE.length; i++) {
            INDENTATION_CACHE[i] = newIndentation(i);
        }
    }

    private final String value;

    private Notation(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public List<Object> parse() {
        return NotationParser.parse(value);
    }

    public static Notation forEncoding(byte[] encoding) throws DecodeException {
        return forEncoding(encoding, 0, encoding.length);
    }

    public static Notation forEncoding(final byte[] buffer, final int index, int end) throws DecodeException {
        if(index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        end = Math.min(buffer.length, end);
        if(index > end) {
            throw new UnrecoverableDecodeException("index > end: " + index + " > " + end);
        }
        StringBuilder sb = new StringBuilder(BEGIN_NOTATION);
        buildList(sb, buffer, index, end, 0, true);
        return new Notation(sb.append(END_NOTATION).toString());
    }

    public static Notation forObjects(List<Object> objects) throws DecodeException {
        return forEncoding(RLPEncoder.encodeSequentially(objects));
    }

    private static DecodeException exceedsContainer(int index, long end, int containerEnd) {
        return new UnrecoverableDecodeException("element @ index " + index + " exceeds its container: " + end + " > " + containerEnd);
    }

    private static int getShortElementEnd(int elementDataIndex, final int elementDataLen, final int containerEnd) throws DecodeException {
        final int end = elementDataIndex + elementDataLen;
        if (end > containerEnd) {
            throw exceedsContainer(elementDataIndex - 1, end, containerEnd);
        }
        return end;
    }

    private static int getLongElementEnd(byte[] data, final int leadByteIndex, final int dataIndex, final int containerEnd) throws DecodeException {
        if (dataIndex > containerEnd) {
            throw exceedsContainer(leadByteIndex, dataIndex, containerEnd);
        }
        final int lengthIndex = leadByteIndex + 1;
        final int lengthLen = dataIndex - lengthIndex;
        final long dataLenLong = Integers.getLong(data, leadByteIndex + 1, lengthLen);
        final long end = lengthIndex + lengthLen + dataLenLong;
        if (end > containerEnd) {
            throw exceedsContainer(leadByteIndex, end, containerEnd);
        }
        final int dataLen = (int) dataLenLong;
        if (dataLen < DataType.MIN_LONG_DATA_LEN) {
            throw new UnrecoverableDecodeException("long element data length must be " + DataType.MIN_LONG_DATA_LEN
                    + " or greater; found: " + dataLen + " for element @ " + leadByteIndex);
        }
        return (int) end;
    }

    private static int buildString(StringBuilder sb, byte[] data, int from, int to) throws DecodeException {
        final int len = to - from;
        if(!LENIENT && len == 1 && data[from] >= 0x00) { // same as (data[from] & 0xFF) < 0x80
            throw new UnrecoverableDecodeException("invalid rlp for single byte @ " + (from - 1));
        }
        sb.append(BEGIN_STRING).append(Strings.encode(data, from, len, Strings.HEX)).append(STRING_END_PLUS_DELIMITER);
        return to;
    }

    private static int buildList(final StringBuilder sb, final byte[] data, final int dataIndex, int end, final int depth, final boolean _long) throws DecodeException {
        if(!_long) {
            sb.append(BEGIN_LIST_SHORT);
        } else if(depth != 0) {
            sb.append(BEGIN_LIST);
        }
        final String baseIndentation = _long ? getIndentation(depth) : null;
        for (int i = dataIndex; i < end; ) {
            if(_long) {
                sb.append('\n').append(baseIndentation).append(ELEMENT_INDENTATION);
            }
            final byte lead = data[i];
            final DataType type = DataType.type(lead);
            if(type != DataType.SINGLE_BYTE) {
                int elementDataIdx = i + 1;
                if(type.isLong) {
                    if(!_long) {
                        throw new UnrecoverableDecodeException("long element found in short list");
                    }
                    elementDataIdx += lead - type.offset; // lengthOfLength
                    i = type.isString
                            ? buildString(sb, data, elementDataIdx, getLongElementEnd(data, i, elementDataIdx, end))
                            : buildList(sb, data, elementDataIdx, getLongElementEnd(data, i, elementDataIdx, end), depth + 1, true);
                } else {
                    i = type.isString
                            ? buildString(sb, data, elementDataIdx, getShortElementEnd(elementDataIdx, lead - type.offset, end))
                            : buildList(sb, data, elementDataIdx, getShortElementEnd(elementDataIdx, lead - type.offset, end), depth + 1, false);
                }
            } else {
                i = buildString(sb, data, i, i + 1);
            }
        }
        if (/* hasElement */ dataIndex != end) {
            stripFinalDelimiter(sb);
        }
        if(!_long) {
            sb.append(LIST_SHORT_END_PLUS_DELIMITER);
        } else if(depth != 0) {
            sb.append('\n')
                    .append(baseIndentation).append(LIST_LONG_END_PLUS_DELIMITER);
        }
        return end;
    }

    private static String newIndentation(int n) {
        char[] spaces = new char[n << 1]; // 2 spaces per
        Arrays.fill(spaces, ' ');
        return String.valueOf(spaces);
    }

    private static String getIndentation(int n) {
        return n < INDENTATION_CACHE.length ? INDENTATION_CACHE[n] : newIndentation(n);
    }

    private static void stripFinalDelimiter(StringBuilder sb) {
        final int n = sb.length();
        sb.replace(n - DELIMITER.length(), n, "");
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Notation && value.equals(((Notation) other).value);
    }

    @Override
    public String toString() {
        return value;
    }
}
