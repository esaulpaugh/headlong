/*
   Copyright 2019-2026 Evan Saulpaugh

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

import com.esaulpaugh.headlong.util.FloatingPoint;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * An immutable view of a portion of a (possibly mutable) byte array containing RLP-encoded data, starting at {@code index}
 * (inclusive) and ending at {@code endIndex} (exclusive), representing a single item (either a string or list). Useful
 * when decoding or otherwise manipulating RLP items.
 * <p>
 * Created by Evo on 1/19/2017.
 */
public abstract class RLPItem implements Comparable<RLPItem> {

    public static final RLPItem[] EMPTY_ARRAY = new RLPItem[0];

    final byte[] buffer;
    public final int index;

    public final transient int dataIndex;
    public final transient int dataLength;
    public final transient int endIndex;

    {
        final Class<?> c = this.getClass();
        final boolean permitted = c == RLPString.class || c == RLPList.class;
        if (!permitted) {
            throw new IllegalStateException("unexpected subclass");
        }
    }

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

    public final boolean isString() {
        return this instanceof RLPString;
    }

    public final boolean isList() {
        return this instanceof RLPList;
    }

    public final RLPString asRLPString() {
        return (RLPString) this;
    }

    public final RLPList asRLPList() {
        return (RLPList) this;
    }

    /**
     * Clones this object.
     *
     * @return an independent and exact copy
     */
    public abstract RLPItem duplicate();

    /**
     * Returns the length of this item's RLP encoding, including prefix, in bytes.
     *
     * @return  the byte-length of the encoding
     */
    public final int encodingLength() {
        return endIndex - index;
    }

    /**
     * Returns this item's RLP encoding.
     *
     * @return  this item's bytes, including prefix
     */
    public final byte[] encoding() {
        return Arrays.copyOfRange(buffer, index, endIndex);
    }

    /**
     * Returns the payload portion of this item only, and not the prefix.
     *
     * @return  the data part of the encoding
     */
    public final byte[] data() {
        return Arrays.copyOfRange(buffer, dataIndex, endIndex);
    }

    /**
     * Inserts this item's RLP encoding into the specified buffer, starting at {@code destIndex} (inclusive) and ending
     * at {@code destIndex} plus {@link RLPItem#encodingLength()} (exclusive).
     *
     * @param dest  the destination array into which the bytes will be copied
     * @param destIndex the index into the destination array
     * @return  the index into {@code dest} immediately after the last byte of the copied encoding
     * @see RLPItem#encoding()
     */
    public final int copy(byte[] dest, int destIndex) {
        int len = encodingLength();
        System.arraycopy(buffer, index, dest, destIndex, len);
        return destIndex + len;
    }

    /**
     * Inserts this item's RLP encoding into the specified {@link ByteBuffer} at its current position.
     *
     * @param dest  the data's destination
     */
    public final void copy(ByteBuffer dest) {
        dest.put(buffer, index, encodingLength());
    }

    /**
     * Writes this item's RLP encoding to the specified {@link OutputStream}.
     *
     * @param dest  the data's destination
     * @throws IOException  if an I/O error occurs
     */
    public final void copy(OutputStream dest) throws IOException {
        dest.write(buffer, index, encodingLength());
    }

    /**
     * Inserts this item's data into the specified buffer, starting at {@code destIndex} (inclusive) and ending at
     * {@code destIndex} plus {@link RLPItem#dataLength} (exclusive).
     *
     * @param dest  the destination array into which the bytes will be copied
     * @param destIndex the index into the destination array
     * @return  the index into {@code dest} immediately after the last byte of the copied data
     * @see RLPItem#data()
     */
    public final int copyData(byte[] dest, int destIndex) {
        System.arraycopy(buffer, dataIndex, dest, destIndex, dataLength);
        return destIndex + dataLength;
    }

    /**
     * Inserts this item's data into the specified {@link ByteBuffer} at its current position.
     *
     * @param dest  the data's destination
     */
    public final void copyData(ByteBuffer dest) {
        dest.put(buffer, dataIndex, dataLength);
    }

    /**
     * Writes this item's data to the specified {@link OutputStream}.
     *
     * @param dest  the data's destination
     * @throws IOException  if an I/O error occurs
     */
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
        int hash = 1;
        for (int i = index; i < endIndex; i++) {
            hash = 31 * hash + buffer[i];
        }
        return hash;
    }

    /**
     * Indicates whether this item's RLP encoding (including prefix) is byte-for-byte identical to another item's. If
     * the argument object is not an {@link RLPItem}, this method returns {@code false}.
     *
     * @param o the object with which to compare this item
     * @return  {@code true} if the argument is an {@link RLPItem} with an identical encoding; {@code false} otherwise.
     */
    @Override
    public final boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof RLPItem)) return false;
        RLPItem other = (RLPItem) o;
//        return Arrays.equals( // Java 9+ vectorizedMismatch
//                this.buffer, this.index, this.endIndex,
//                other.buffer, other.index, other.endIndex
//        );
        return other.encodingLength() == encodingLength() && encodingEquals(other.buffer, other.index);
    }

    private boolean encodingEquals(byte[] oBuf, int oIdx) {
        for (int i = this.index; i < this.endIndex; i++) {
            if (this.buffer[i] != oBuf[oIdx++])
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

    @Override
    public final int compareTo(RLPItem othr) {
        // Arrays.compareUnsigned // Java 9+
        int thisOffset = this.dataIndex;
        int othrOffset = othr.dataIndex;
        final int end = thisOffset + Math.min(this.dataLength, othr.dataLength);
        while (thisOffset < end) {
            int t = this.buffer[thisOffset++];
            int o = othr.buffer[othrOffset++];
            if (t != o) {
                return (t & 0xFF) - (o & 0xFF); // unsigned difference (required for sorting utf-8)
            }
        }
        return this.dataLength - othr.dataLength;
    }

    @SuppressWarnings({"deprecation", "removal"})
    @Override
    protected final void finalize() throws Throwable { /* (empty) final finalize helps prevent finalizer attacks on non-final class RLPItem */ }
}
