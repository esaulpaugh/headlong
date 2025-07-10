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

import com.esaulpaugh.headlong.util.Integers;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.esaulpaugh.headlong.rlp.DataType.LIST_LONG_OFFSET;
import static com.esaulpaugh.headlong.rlp.DataType.LIST_SHORT_OFFSET;
import static com.esaulpaugh.headlong.rlp.DataType.MIN_LONG_DATA_LEN;
import static com.esaulpaugh.headlong.rlp.DataType.STRING_LONG_OFFSET;
import static com.esaulpaugh.headlong.rlp.DataType.STRING_SHORT_OFFSET;

/** For encoding data to Recursive Length Prefix format. */
public final class RLPEncoder {

    private RLPEncoder() {}

// -------------- made visibile to Record -------------------------------------------------------------------------------
    static int payloadLen(byte[] seqBytes, Iterable<KVP> sorted) {
        long sum = stringEncodedLen(seqBytes);
        for (KVP pair : sorted) {
            sum += pair.rlp.length;
        }
        return requireNoOverflow(sum);
    }

    static byte[] encodeRecordContent(byte[] seqBytes, Iterable<KVP> sorted, int dataLen) {
        ByteBuffer bb = ByteBuffer.allocate(itemLen(dataLen));
        insertListPrefix(dataLen, bb);
        putString(seqBytes, bb);
        for (KVP pair : sorted) {
            pair.export(bb);
        }
        return bb.array();
    }

    /** Doesn't work for single-byte items. */
    static int itemLen(int dataLen) {
        return (isShort(dataLen) ? 1 : 1 + Integers.len(dataLen))
                + dataLen;
    }

    static void insertListPrefix(int dataLen, ByteBuffer bb) {
        if (isShort(dataLen)) {
            bb.put((byte) (LIST_SHORT_OFFSET + dataLen));
        } else {
            final int len = Integers.len(dataLen);
            bb.put((byte) (LIST_LONG_OFFSET + len));
            Integers.putLong(dataLen, len, bb);
        }
    }
// ---------------------------------------------------------------------------------------------------------------------
    private static int requireNoOverflow(long length) {
        if (length >= 0L && length <= Integer.MAX_VALUE) {
            return (int) length;
        }
        throw new ArithmeticException("integer overflow");
    }

    private static boolean isShort(int dataLen) {
        return dataLen < MIN_LONG_DATA_LEN;
    }

    private static int sumEncodedLen(Iterable<?> rawItems) {
        long sum = 0L;
        for (Object raw : rawItems) {
            sum += encodedLen(raw);
        }
        return requireNoOverflow(sum);
    }

    private static int encodedLen(Object raw) {
        if (raw instanceof byte[]) {
            return stringEncodedLen((byte[]) raw);
        }
        if (raw instanceof Iterable<?>) {
            return listEncodedLen((Iterable<?>) raw);
        }
        if (raw instanceof Object[]) {
            return listEncodedLen(Arrays.asList((Object[]) raw));
        }
        throw new IllegalArgumentException("unsupported object type. expected instanceof byte[], Iterable, or Object[]");
    }

    private static int stringEncodedLen(byte[] byteString) {
        return itemLen(byteString.length == 1 && DataType.isSingleByte(byteString[0]) ? 0 : byteString.length);
    }

    private static int listEncodedLen(Iterable<?> items) {
        return itemLen(sumEncodedLen(items));
    }

    private static void encodeItem(Object raw, ByteBuffer bb) {
        if (raw instanceof byte[]) {
            putString((byte[]) raw, bb);
        } else if (raw instanceof Iterable<?>) {
            Iterable<?> elements = (Iterable<?>) raw;
            encodeList(sumEncodedLen(elements), elements, bb);
        } else if (raw instanceof Object[]) {
            Iterable<?> elements = Arrays.asList((Object[]) raw);
            encodeList(sumEncodedLen(elements), elements, bb);
        } else {
            throw new IllegalArgumentException("unsupported object type. expected instanceof byte[], Iterable, or Object[]");
        }
    }

    private static void encodeList(int dataLen, Iterable<?> elements, ByteBuffer bb) {
        insertListPrefix(dataLen, bb);
        putSequence(elements, bb);
    }
// ---------------------------------------------------------------------------------------------------------------------
    /**
     * Returns the RLP encoding of the given byte string.
     *
     * @param byteString the byte string to be encoded
     * @return the encoding
     */
    public static byte[] string(byte[] byteString) {
        ByteBuffer bb = ByteBuffer.allocate(stringEncodedLen(byteString));
        putString(byteString, bb);
        return bb.array();
    }

    /**
     * Puts into the destination buffer at its current position the RLP encoding of the given byte string.
     *
     * @param byteString the byte string to be encoded
     * @param dest    the destination for the sequence of RLP encodings
     */
    public static void putString(byte[] byteString, ByteBuffer dest) {
        if (byteString.length == 1) {
            if (!DataType.isSingleByte(byteString[0])) {
                dest.put((byte) (STRING_SHORT_OFFSET + 1));
            }
        } else if (isShort(byteString.length)) {
            dest.put((byte) (STRING_SHORT_OFFSET + byteString.length)); // dataLen is 0 or 2-55
        } else { // long string
            final int len = Integers.len(byteString.length);
            dest.put((byte) (STRING_LONG_OFFSET + len));
            Integers.putLong(byteString.length, len, dest);
        }
        dest.put(byteString);
    }

    public static byte[] sequence(Object... objects) {
        return sequence(Arrays.asList(objects));
    }
//----------------------------------------------------------------------------------------------------------------------
    /**
     * Returns the concatenation of the encodings of the given objects in the given order. The {@link Iterable} containing
     * the objects is not itself an item.
     *
     * @param objects the raw objects to be encoded
     * @return the encoded sequence
     */
    public static byte[] sequence(Iterable<?> objects) {
        byte[] dest = new byte[sumEncodedLen(objects)];
        putSequence(objects, dest, 0);
        return dest;
    }

    /**
     * Inserts into the destination array at the given index the concatenation of the encodings of the given objects in the given order.
     * The {@link Iterable} containing the objects is not itself an item.
     *
     * @param objects   the raw objects to be encoded
     * @param dest      the destination for the sequence of RLP encodings
     * @param destIndex the index into the destination for the sequence
     * @return the index into {@code dest} immediately after the last byte of the sequence
     */
    public static int putSequence(Iterable<?> objects, byte[] dest, int destIndex) {
        ByteBuffer bb = ByteBuffer.wrap(dest, destIndex, dest.length - destIndex);
        putSequence(objects, bb);
        return bb.position();
    }

    /**
     * Puts into the destination buffer at its current position the concatenation of the encodings of the given objects
     * in the given order. The {@link Iterable} containing the objects is not itself an item.
     *
     * @param objects the raw objects to be encoded
     * @param dest    the destination for the sequence of RLP encodings
     */
    public static void putSequence(Iterable<?> objects, ByteBuffer dest) {
        for (Object raw : objects) {
            encodeItem(raw, dest);
        }
    }
//----------------------------------------------------------------------------------------------------------------------
    /**
     * Returns the encoding of an RLP list item containing the given elements encoded in the given order.
     *
     * @param elements the raw elements to be encoded as an RLP list item
     * @return the encoded RLP list item
     */
    public static byte[] list(Object... elements) {
        return list(Arrays.asList(elements));
    }
//----------------------------------------------------------------------------------------------------------------------
    /**
     * Returns the encoding of an RLP list item containing the given elements encoded in the given order.
     *
     * @param elements the raw elements to be encoded as an RLP list item
     * @return the encoded RLP list item
     */
    public static byte[] list(Iterable<?> elements) {
        int dataLen = sumEncodedLen(elements);
        ByteBuffer bb = ByteBuffer.allocate(itemLen(dataLen));
        encodeList(dataLen, elements, bb);
        return bb.array();
    }

    /**
     * Inserts into the destination array at the given index the encoding of an RLP list item containing the given
     * elements encoded in the given order.
     *
     * @param elements  the raw elements to be encoded as an RLP list item
     * @param dest      the destination for the encoded RLP list
     * @param destIndex the index into the destination for the list
     * @return the index into {@code dest} immediately after the last byte of the list
     */
    public static int putList(Iterable<?> elements, byte[] dest, int destIndex) {
        ByteBuffer bb = ByteBuffer.wrap(dest, destIndex, dest.length - destIndex);
        putList(elements, bb);
        return bb.position();
    }

    /**
     * Puts into the destination buffer at its current position the encoding of an RLP list item containing the given
     * elements encoded in the given order.
     *
     * @param elements the raw elements to be encoded as an RLP list item
     * @param dest     the destination for the encoded RLP list
     */
    public static void putList(Iterable<?> elements, ByteBuffer dest) {
        encodeList(sumEncodedLen(elements), elements, dest);
    }
}
