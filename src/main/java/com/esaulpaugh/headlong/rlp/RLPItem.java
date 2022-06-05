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
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * An immutable view of a portion of a (possibly mutable) byte array containing RLP-encoded data, starting at {@code index}
 * (inclusive) and ending at {@code endIndex} (exclusive), representing a single item (either a string or list). Useful
 * when decoding or otherwise manipulating RLP items.
 *
 * Created by Evo on 1/19/2017.
 */
public abstract class RLPItem {

    public static final RLPItem[] EMPTY_ARRAY = new RLPItem[0];

    final byte[] buffer;
    public final int index;

    public final transient int dataIndex;
    public final transient int dataLength;
    public final transient int endIndex;

    RLPItem(byte[] buffer, int index, int dataIndex, int dataLength, int endIndex) {
        this.buffer = buffer;
        this.index = index;
        this.dataIndex = dataIndex;
        this.dataLength = dataLength;
        this.endIndex = endIndex;
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
        return Arrays.copyOfRange(buffer, index, endIndex);
    }

    public final byte[] data() {
        return Arrays.copyOfRange(buffer, dataIndex, endIndex);
    }

    public final int copy(byte[] dest, int destIndex) {
        int len = encodingLength();
        System.arraycopy(buffer, index, dest, destIndex, len);
        return destIndex + len;
    }

    public final int copyData(byte[] dest, int destIndex) {
        System.arraycopy(buffer, dataIndex, dest, destIndex, dataLength);
        return destIndex + dataLength;
    }

    public final void copyData(OutputStream dest) throws IOException {
        dest.write(buffer, dataIndex, dataLength);
    }

    /**
     * @return the byte array representation of this item's data
     * @see RLPItem#data()
     */
    public final byte[] asBytes() {
        return data();
    }

    /**
     * Returns the {@link String}, of the given encoding, representing this item's data.
     *
     * @param encoding one of { {@link Strings#HEX}, {@link Strings#UTF_8}, {@link Strings#BASE_64_URL_SAFE}, {@link Strings#ASCII} }.
     * @return  this item's payload (data) bytes, encoded to your liking
     */
    public final String asString(int encoding) {
        return Strings.encode(buffer, dataIndex, dataLength, encoding);
    }

    /**
     * Returns the {@code boolean} representation for this item's data. False for {@code 0xc0}, {@code 0x80}, and {@code 0x00};
     * true for everything else.
     *
     * @return the {@code boolean}
     */
    public final boolean asBoolean() {
        return dataLength != 0 && buffer[index] != 0x00;
    }

    /**
     * Returns the {@code char} representation for this item's data.
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
//        return new BigInteger(buffer, dataIndex, dataLength); // Java 9+
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
     * @param encoding one of { {@link Strings#HEX}, {@link Strings#UTF_8}, {@link Strings#BASE_64_URL_SAFE}, {@link Strings#ASCII} }.
     * @return  this item's bytes, including RLP prefix, encoded to your liking
     */
    public final String encodingString(int encoding) {
        return Strings.encode(buffer, index, encodingLength(), encoding);
    }
}
