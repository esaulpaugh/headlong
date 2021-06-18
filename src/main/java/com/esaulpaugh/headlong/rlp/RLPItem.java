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

import com.esaulpaugh.headlong.rlp.util.FloatingPoint;
import com.esaulpaugh.headlong.rlp.util.Notation;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;

import static com.esaulpaugh.headlong.rlp.DataType.MIN_LONG_DATA_LEN;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_LIST_LONG;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_LIST_SHORT;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_SINGLE_BYTE;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_STRING_LONG;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_STRING_SHORT;
import static com.esaulpaugh.headlong.rlp.DataType.STRING_SHORT;

/**
 * An immutable view of a portion of a (possibly mutable) byte array containing RLP-encoded data, starting at {@code index}
 * (inclusive) and ending at {@code endIndex} (exclusive), representing a single item (either a string or list). Useful
 * when decoding or otherwise manipulating RLP items.
 *
 * Created by Evo on 1/19/2017.
 */
public abstract class RLPItem {

    public static final RLPItem[] EMPTY_ARRAY = new RLPItem[0];

    protected final byte[] buffer;
    protected final int index;

    public final transient int dataIndex;
    public final transient int dataLength;
    public final transient int endIndex;

    RLPItem(final byte lead, final DataType type, final byte[] buffer, final int index, int containerEnd, final boolean lenient) {
        containerEnd = Math.min(buffer.length, containerEnd);

        final int _dataIndex;
        final long _dataLength;
        
        final int diff = lead - type.offset;
        switch (type.ordinal()) {
        case ORDINAL_SINGLE_BYTE:
            _dataIndex = index;
            _dataLength = 1;
            break;
        case ORDINAL_STRING_SHORT:
        case ORDINAL_LIST_SHORT:
            _dataIndex = index + 1;
            _dataLength = diff;
            break;
        case ORDINAL_STRING_LONG:
        case ORDINAL_LIST_LONG:
            int lengthIndex = index + 1;
            _dataIndex = lengthIndex + diff; // type dictates that diff guaranteed to be in [1,8]
            if (_dataIndex > containerEnd) {
                throw exceedsContainer(index, _dataIndex, containerEnd, containerEnd == buffer.length);
            }
            _dataLength = Integers.getLong(buffer, lengthIndex, diff, lenient);
            if(_dataLength < MIN_LONG_DATA_LEN) {
                throw new IllegalArgumentException("long element data length must be " + MIN_LONG_DATA_LEN + " or greater; found: " + _dataLength + " for element @ " + index);
            }
            break;
        default: throw new Error();
        }

        final long _endIndex = _dataIndex + _dataLength;

        if(_endIndex > containerEnd) {
            throw exceedsContainer(index, _endIndex, containerEnd, containerEnd == buffer.length);
        }
        if(!lenient && _dataLength == 1 && type == STRING_SHORT && DataType.isSingleByte(buffer[_dataIndex])) {
            throw new IllegalArgumentException("invalid rlp for single byte @ " + index);
        }

        this.buffer = buffer;
        this.index = index;
        this.dataIndex = _dataIndex;
        this.dataLength = (int) _dataLength;
        this.endIndex = (int) _endIndex;
    }

    RLPItem(RLPItem it) {
        this.buffer = it.encoding();
        this.index = 0;
        this.dataIndex = buffer.length - it.dataLength;
        this.dataLength = it.dataLength;
        this.endIndex = buffer.length;
    }

    static IllegalArgumentException exceedsContainer(int index, long end, int containerEnd, boolean shortInput) {
        String msg = "element @ index " + index + " exceeds its container: " + end + " > " + containerEnd;
        return shortInput ? new ShortInputException(msg) : new IllegalArgumentException(msg);
    }

    public final DataType type() {
        return DataType.type(buffer[index]);
    }

    public abstract boolean isString();

    public abstract boolean isList();

    public abstract RLPString asRLPString();

    public abstract RLPList asRLPList();

    /**
     * Clones this object.
     *
     * @return an independent and exact copy
     * @throws IllegalArgumentException if a problem in re-decoding the item occurs
     */
    public abstract RLPItem duplicate();

    public final int encodingLength() {
        return endIndex - index;
    }

    public final byte[] encoding() {
        return copyOfRange(index, endIndex);
    }

    public final byte[] data() {
        return copyOfRange(dataIndex, endIndex);
    }

    public final byte[] copyOfRange(int from, int to) {
        byte[] range = new byte[to - from];
        exportRange(from, to, range, 0);
        return range;
    }

    public final int export(byte[] dest, int destIndex) {
        return exportRange(index, endIndex, dest, destIndex);
    }

    public final int exportData(byte[] dest, int destIndex) {
        return exportRange(dataIndex, endIndex, dest, destIndex);
    }

    public final void exportData(OutputStream os) throws IOException {
        os.write(buffer, dataIndex, dataLength);
    }

    /**
     * Copies the specified range of bytes from this item's underlying buffer into the specified destination array.
     *
     * @param from      the initial index of the range to be copied, inclusive
     * @param to        the final index of the range to be copied, exclusive
     * @param dest      the destination array into which the bytes will be copied
     * @param destIndex the index into the destination array at which to place the bytes
     * @return the next index into {@code dest}
     * @throws IndexOutOfBoundsException if {@code from < index} or {@code to > endIndex}
     */
    public final int exportRange(int from, int to, byte[] dest, int destIndex) {
        if(from >= index) {
            if(to <= endIndex) {
                int len = to - from;
                System.arraycopy(buffer, from, dest, destIndex, len);
                return destIndex + len;
            }
            throw new IndexOutOfBoundsException(to + " > " + endIndex);
        }
        throw new IndexOutOfBoundsException(from + " < " + index);
    }

    /**
     * @return the byte array representation of this item's data
     * @see RLPItem#data()
     */
    public final byte[] asBytes() {
        return data();
    }

    /**
     * Returns the {@link String}, of the given encoding, representing this item.
     *
     * @param encoding one of { {@link Strings#BASE_64_URL_SAFE}, {@link Strings#UTF_8}, {@link Strings#HEX} }.
     * @return  this item's payload (data) bytes, encoded to your liking
     */
    public final String asString(int encoding) {
        return Strings.encode(buffer, dataIndex, dataLength, encoding);
    }

    /**
     * Returns the {@code boolean} representation for this item. False for {@code 0xc0}, {@code 0x80}, and {@code 0x00};
     * true for everything else.
     *
     * @return the {@code boolean}
     */
    public final boolean asBoolean() {
        return dataLength != 0 && buffer[index] != 0x00;
    }

    /**
     * Returns the {@code char} representation for this item.
     *
     * @param lenient whether to allow leading zeroes in the raw data
     * @return the {@code char}
     * @throws IllegalArgumentException if this item is not interpretable as a char
     * @see #asShort(boolean)
     */
    public final char asChar(boolean lenient) {
        return (char) asShort(lenient);
    }

    public final byte asByte(boolean lenient) {
        return Integers.getByte(buffer, dataIndex, dataLength, lenient);
    }

    public final short asShort(boolean lenient) {
        return Integers.getShort(buffer, dataIndex, dataLength, lenient);
    }

    public final int asInt(boolean lenient) {
        return Integers.getInt(buffer, dataIndex, dataLength, lenient);
    }

    public final long asLong(boolean lenient) {
        return Integers.getLong(buffer, dataIndex, dataLength, lenient);
    }

    public final BigInteger asBigInt(boolean lenient) {
        return Integers.getBigInt(buffer, dataIndex, dataLength, lenient);
    }

    public final float asFloat(boolean lenient) {
        return FloatingPoint.getFloat(buffer, dataIndex, dataLength, lenient);
    }

    public final double asDouble(boolean lenient) {
        return FloatingPoint.getDouble(buffer, dataIndex, dataLength, lenient);
    }

    public final byte asByte() {
        return asByte(false);
    }

    public final int asInt() {
        return asInt(false);
    }

    public final long asLong() {
        return asLong(false);
    }

    public final BigInteger asBigInt() {
        return asBigInt(false);
    }

    public final BigInteger asBigIntSigned() {
        return new BigInteger(data());
    }

    /**
     * @see Arrays#hashCode(byte[])
     */
    @Override
    public final int hashCode() {
        int result = 1;
        for (int i = index; i < endIndex; i++) {
            result = 31 * result + buffer[i];
        }
        return result;
    }

    @Override
    public final boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof RLPItem)) return false;
        RLPItem other = (RLPItem) o;
//        return Arrays.equals( // Java 9+ vectorizedMismatch
//                this.buffer, this.index, this.endIndex,
//                other.buffer, other.index, other.endIndex
//        );
        return equals(other.buffer, other.index, other.endIndex);
    }

    private boolean equals(byte[] b, int bIdx, int bEnd) {
        final int len = this.endIndex - this.index;
        if(len != bEnd - bIdx) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (this.buffer[this.index + i] != b[bIdx + i])
                return false;
        }
        return true;
    }

    @Override
    public final String toString() {
        return Notation.encodeToString(buffer, index, endIndex);
    }

    /**
     * @param encoding one of { {@link Strings#BASE_64_URL_SAFE}, {@link Strings#UTF_8}, {@link Strings#HEX} }.
     * @return  this item's bytes, including RLP prefix, encoded to your liking
     */
    public final String encodingString(int encoding) {
        return Strings.encode(buffer, index, encodingLength(), encoding);
    }
}
