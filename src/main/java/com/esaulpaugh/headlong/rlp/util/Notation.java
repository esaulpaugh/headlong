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
import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.exception.UnrecoverableDecodeException;
import com.esaulpaugh.headlong.util.Strings;

import java.util.Arrays;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.DataType.MIN_LONG_DATA_LEN;
import static com.esaulpaugh.headlong.util.Strings.HEX;

/**
 * An object notation for RLP, not unlike JSON. Call {@link #parse()} to parse the notation into the original list of objects.
 */
public class Notation {

    private static final boolean LENIENT = true; // keep lenient so RLPItem.toString() doesn't throw, and to help with debugging

    private static final String BEGIN_NOTATION = "(";
    private static final String END_NOTATION = "\n)";

    static final String BEGIN_LIST = "{";
    static final String END_LIST = "}";
    static final String BEGIN_STRING = "\"";
    static final String END_STRING = "\"";

    private static final String BEGIN_LIST_SHORT = BEGIN_LIST + " ";

    private static final String COMMA_SPACE = ", ";
    private static final String LIST_LONG_END_COMMA_SPACE = END_LIST + COMMA_SPACE;
    private static final String LIST_SHORT_END_COMMA_SPACE = " " + LIST_LONG_END_COMMA_SPACE;
    private static final String STRING_END_COMMA_SPACE = END_STRING + COMMA_SPACE;

    private final String value;

    private Notation(String value) {
        if(value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        this.value = value;
    }

    public List<Object> parse() {
        return NotationParser.parse(value);
    }

    private static final String[] INDENTATIONS;

    private static final int INDENTATION_CACHE_SIZE = 16;

    private static final String ELEMENT_INDENTATION = newIndentation(1);

    static {
        INDENTATIONS = new String[INDENTATION_CACHE_SIZE];
        for (int i = 0; i < INDENTATIONS.length; i++) {
            INDENTATIONS[i] = newIndentation(i);
        }
    }

    private static DecodeException exceedsContainer(int index, long end, int containerEnd) {
        String msg = "element @ index " + index + " exceeds its container: " + end + " > " + containerEnd;
        return new UnrecoverableDecodeException(msg);
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
//        if (dataLenLong > MAX_ARRAY_LENGTH) {
//            throw new DecodeException("too much data: " + dataLenLong + " > " + MAX_ARRAY_LENGTH);
//        }
        final long end = lengthIndex + lengthLen + dataLenLong;
        if (end > containerEnd) {
            throw exceedsContainer(leadByteIndex, end, containerEnd);
        }
        final int dataLen = (int) dataLenLong;
        if (dataLen < MIN_LONG_DATA_LEN) {
            throw new UnrecoverableDecodeException("long element data length must be " + MIN_LONG_DATA_LEN
                    + " or greater; found: " + dataLen + " for element @ " + leadByteIndex);
        }
        return (int) end;
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
        buildLongList(
                sb,
                buffer,
                index,
                end,
                0
        );
        return new Notation(sb.append(END_NOTATION).toString());
    }

    private static int buildLongList(final StringBuilder sb, final byte[] data, final int dataIndex, int end, final int depth) throws DecodeException {
        if(depth != 0) {
            sb.append(BEGIN_LIST);
        }

        final String baseIndentation = getIndentation(depth);

        int elementDataIndex = -1;
        int lengthLen;
        int elementDataLen;
        int elementEnd = -1;
        boolean hasELement = false;
        int i = dataIndex;
        while (i < end) {
            sb.append('\n').append(baseIndentation);
            byte current = data[i];
            final DataType type = DataType.type(current);
            switch (type) {
            case SINGLE_BYTE:
                break;
            case STRING_SHORT:
            case LIST_SHORT:
                elementDataIndex = i + 1;
                elementDataLen = current - type.offset;
                elementEnd = getShortElementEnd(elementDataIndex, elementDataLen, end);
                break;
            case STRING_LONG:
            case LIST_LONG:
                lengthLen = current - type.offset;
                elementDataIndex = i + 1 + lengthLen;
                elementEnd = getLongElementEnd(data, i, elementDataIndex, end);
            }
            hasELement = true;
            sb.append(ELEMENT_INDENTATION);
            switch (type) {
            case SINGLE_BYTE:
                i = buildByte(sb, data, i);
                break;
            case STRING_SHORT:
            case STRING_LONG:
                i = buildString(sb, data, elementDataIndex, elementEnd);
                break;
            case LIST_SHORT:
                i = buildShortList(sb, data, elementDataIndex, elementEnd);
                break;
            case LIST_LONG:
                i = buildLongList(sb, data, elementDataIndex, elementEnd, depth + 1);
            }
        }
        if (hasELement) {
            stripFinalCommaAndSpace(sb);
        }
        if(depth != 0) {
            sb.append('\n')
                    .append(baseIndentation).append(LIST_LONG_END_COMMA_SPACE);
        }
        return end;
    }

    private static int buildShortList(final StringBuilder sb, final byte[] data, final int dataIndex, final int end) throws DecodeException {
        sb.append(BEGIN_LIST_SHORT);

        boolean hasElement = false;
        int i = dataIndex;
        LOOP:
        for ( ; i < end; ) {
            byte current = data[i];
            final DataType type = DataType.type(current);
            hasElement = true;
            switch (type) {
            case SINGLE_BYTE:
                i = buildByte(sb, data, i);
                continue LOOP;
            case STRING_SHORT:
            case LIST_SHORT:
                int elementDataIndex = i + 1;
                int elementEnd = getShortElementEnd(elementDataIndex, current - type.offset, end);
                i = type == DataType.STRING_SHORT
                        ? buildString(sb, data, elementDataIndex, elementEnd)
                        : buildShortList(sb, data, elementDataIndex, elementEnd);
                break;
            case STRING_LONG:
            case LIST_LONG:
            default:
                throw new Error();
            }
        }
        if (hasElement) {
            stripFinalCommaAndSpace(sb);
        }
        sb.append(LIST_SHORT_END_COMMA_SPACE);
        return end;
    }

    private static String newIndentation(int n) {
        char[] spaces = new char[n << 1]; // 2 spaces per
        Arrays.fill(spaces, ' ');
        return String.valueOf(spaces);
    }

    private static String getIndentation(int n) {
        return n >= INDENTATIONS.length ? newIndentation(n) : INDENTATIONS[n];
    }

    private static void stripFinalCommaAndSpace(StringBuilder sb) {
        final int n = sb.length();
        sb.replace(n - 2, n, "");
    }

    private static int buildByte(StringBuilder sb, byte[] data, int i) {
        sb.append(BEGIN_STRING).append(Strings.encode(data, i, 1, HEX)).append(STRING_END_COMMA_SPACE);
        return i + 1;
    }

    private static int buildString(StringBuilder sb, byte[] data, int from, int to) throws DecodeException {
        final int len = to - from;
        if(!LENIENT && len == 1 && data[from] >= 0x00) { // same as (data[from] & 0xFF) < 0x80
            throw new UnrecoverableDecodeException("invalid rlp for single byte @ " + (from - 1));
        }
        sb.append(BEGIN_STRING).append(Strings.encode(data, from, len, HEX)).append(STRING_END_COMMA_SPACE);
        return to;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Notation
                && value.equals(((Notation) other).value);
    }

    @Override
    public String toString() {
        return value;
    }
}
