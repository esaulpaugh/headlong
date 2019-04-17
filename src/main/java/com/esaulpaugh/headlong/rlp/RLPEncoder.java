package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.eip778.KeyValuePair;
import com.esaulpaugh.headlong.rlp.util.Integers;

import java.util.Arrays;

import static com.esaulpaugh.headlong.rlp.DataType.*;

/**
 * Encodes data to RLP format.
 */
public class RLPEncoder {

    private static boolean isLong(long dataLen) {
        return dataLen >= MIN_LONG_DATA_LEN;
    }

    public static int prefixLength(long dataLen) {
        if (isLong(dataLen)) {
            return 1 + Integers.len(dataLen);
        } else {
            return 1;
        }
    }

    private static long totalEncodedLen(Iterable<?> objects) {
        long total = 0;
        for (Object obj : objects) {
            total += itemEncodedLen(obj);
        }
        return total;
    }

    public static long dataLen(KeyValuePair[] pairs) {
        long total = 0;
        for (KeyValuePair kvp : pairs) {
            total += itemEncodedLen(kvp.getKey()) + itemEncodedLen(kvp.getValue());
        }
        return total;
    }

    private static long itemEncodedLen(Object obj) {
        if (obj instanceof byte[]) {
            return stringEncodedLen((byte[]) obj);
        }
        if (obj instanceof Iterable<?>) {
            return listEncodedLen((Iterable<?>) obj);
        }
        if(obj instanceof Object[]) {
            return listEncodedLen(Arrays.asList((Object[]) obj));
        }
        if(obj == null) {
            throw new NullPointerException();
        }
        throw new IllegalArgumentException("unsupported object type: " + obj.getClass().getName());
    }

    private static int stringEncodedLen(byte[] byteString) {
        final int dataLen = byteString.length;
        if (isLong(dataLen)) {
            return 1 + Integers.len(dataLen) + dataLen;
        }
        if (dataLen == 1 && byteString[0] >= 0x00) { // same as (bytes[0] & 0xFF) < 0x80
            return 1;
        }
        return 1 + dataLen;
    }

    private static long listEncodedLen(Iterable<?> elements) {
        final long listDataLen = totalEncodedLen(elements);
        if (isLong(listDataLen)) {
            return 1 + Integers.len(listDataLen) + listDataLen;
        }
        return 1 + listDataLen;
    }

    private static int encodeKeyValuePair(KeyValuePair pair, byte[] dest, int destIndex) {
        destIndex = encodeString(pair.getKey(), dest, destIndex);
        return encodeString(pair.getValue(), dest, destIndex);
    }

    private static int encodeItem(Object item, byte[] dest, int destIndex) {
        if (item instanceof byte[]) {
            return encodeString((byte[]) item, dest, destIndex);
        }
        if (item instanceof Iterable<?>) {
            Iterable<?> elements = (Iterable<?>) item;
            return encodeList(totalEncodedLen(elements), elements, dest, destIndex);
        }
        if(item instanceof Object[]) {
            Iterable<Object> elements = Arrays.asList((Object[]) item);
            return encodeList(totalEncodedLen(elements), elements, dest, destIndex);
        }
        if(item == null) {
            throw new NullPointerException(); // TODO correct behavior?
        }
        throw new IllegalArgumentException("unsupported object type: " + item.getClass().getName());
    }

    private static int encodeString(byte[] data, byte[] dest, int destIndex) {
        final int dataLen = data.length;
        if (isLong(dataLen)) { // long string
            int n = Integers.putLong(dataLen, dest, destIndex + 1);
            dest[destIndex] = (byte) (STRING_LONG_OFFSET + (byte) n);
            destIndex += 1 + n;
            System.arraycopy(data, 0, dest, destIndex, dataLen);
            return destIndex + dataLen;
        }
        // short string
        if (dataLen == 1) {
            byte first = data[0];
            if (first >= 0x00) { // same as (first & 0xFF) < 0x80
                dest[destIndex++] = first;
                return destIndex;
            } else {
                dest[destIndex++] = (byte) (STRING_SHORT_OFFSET + (byte) dataLen);
                dest[destIndex++] = first;
                return destIndex;
            }
        }
        // dataLen is 0 or 2-55
        dest[destIndex++] = (byte) (STRING_SHORT_OFFSET + (byte) dataLen);
        // 56-case switch statement is faster than for loop, but arraycopy is faster than both
        System.arraycopy(data, 0, dest, destIndex, dataLen);
        return destIndex + dataLen;
    }

    private static int encodeList(long dataLen, Iterable<?> elements, byte[] dest, int destIndex) {
        destIndex = insertListPrefix(dataLen, dest, destIndex);
        return encodeSequentially(elements, dest, destIndex);
    }

    public static int insertListPrefix(long dataLen, byte[] dest, int destIndex) {
        return isLong(dataLen)
                ? encodeLongListPrefix(dataLen, dest, destIndex)
                : encodeShortListPrefix(dataLen, dest, destIndex);
    }

    private static int encodeLongListPrefix(final long dataLen, byte[] dest, final int destIndex) {
        final int lengthIndex = destIndex + 1;
        final int n = Integers.putLong(dataLen, dest, lengthIndex);
        dest[destIndex] = (byte) (LIST_LONG_OFFSET + (byte) n);
        return lengthIndex + n;
    }

    private static int encodeShortListPrefix(final long dataLen, byte[] dest, final int destIndex) {
        dest[destIndex] = (byte) (LIST_SHORT_OFFSET + (byte) dataLen);
        return destIndex + 1;
    }

    private static int encodeList(long dataLen, long seq, KeyValuePair[] pairs, byte[] dest, int destIndex) {
        destIndex = insertListPrefix(dataLen, dest, destIndex);
        return encodeSequentially(seq, pairs, dest, destIndex);
    }

    private static int encodeSequentially(long seq, KeyValuePair[] pairs, byte[] dest, int destIndex) {
        byte[] seqBytes = Integers.toBytes(seq);
        destIndex = encodeItem(seqBytes, dest, destIndex);
        for (KeyValuePair kvp : pairs) {
            destIndex = encodeKeyValuePair(kvp, dest, destIndex);
        }
        return destIndex;
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Returns the RLP encoding of the given byte.
     *
     * @param b the byte to be encoded
     * @return  the encoding
     */
    public static byte[] encode(byte b) {
        return encode(new byte[] { b });
    }

    /**
     * Returns the RLP encoding of the given byte string.
     *
     * @param byteString    the byte string to be encoded
     * @return  the encoding
     */
    public static byte[] encode(byte[] byteString) {
        byte[] dest = new byte[stringEncodedLen(byteString)];
        encodeString(byteString, dest, 0);
        return dest;
    }

    /**
     * Returns the concatenation of the encodings of the given objects in the given order.
     *
     * @param objects   the raw objects to be encoded in sequence
     * @return  the encoded sequence
     */
    public static byte[] encodeSequentially(Object... objects) {
        byte[] dest = new byte[(int) totalEncodedLen(Arrays.asList(objects))];
        encodeSequentially(objects, dest, 0);
        return dest;
    }

    /**
     * Returns the concatenation of the encodings of the given objects in the given order. The {@link Iterable}
     * containing the objects is <i>not</i> encoded.
     *
     * @param objects   the raw objects to be encoded
     * @return  the encoded sequence
     */
    public static byte[] encodeSequentially(Iterable<?> objects) {
        byte[] dest = new byte[(int) totalEncodedLen(objects)];
        encodeSequentially(objects, dest, 0);
        return dest;
    }

    /**
     * Inserts the concatenation of the encodings of the given objects in the given order into the destination array.
     * The array containing the objects is <i>not</i> encoded.
     *
     * @param objects   the raw objects to be encoded
     * @param dest  the destination for the sequence of RLP encodings
     * @param destIndex the index into {@code dest} for the sequence
     * @return  the index into {@code dest} marking the end of the sequence
     */
    public static int encodeSequentially(Object[] objects, byte[] dest, int destIndex) {
        if(objects instanceof KeyValuePair[]) {
            for (KeyValuePair kvp : (KeyValuePair[]) objects) {
                destIndex = encodeKeyValuePair(kvp, dest, destIndex);
            }
        } else {
            for (Object item : objects) {
                destIndex = encodeItem(item, dest, destIndex);
            }
        }
        return destIndex;
    }

    /**
     * Inserts the concatenation of the encodings of the given objects in the given order into the destination array.
     * The {@code Iterable} containing the objects is <i>not</i> encoded.
     *
     * @param objects   the raw objects to be encoded
     * @param dest  the destination for the sequence of RLP encodings
     * @param destIndex the index into the destination for the sequence
     * @return  the index marking the end of the sequence
     */
    public static int encodeSequentially(Iterable<?> objects, byte[] dest, int destIndex) {
        for (Object obj : objects) {
            destIndex = encodeItem(obj, dest, destIndex);
        }
        return destIndex;
    }

    // ---------------------------------------------------------------------

    /**
     * Returns the encoding of an RLP list item containing the given objects, encoded.
     *
     * @param elements  the raw elements to be encoded as an RLP list item
     * @return  the encoded RLP list item
     */
    public static byte[] encodeAsList(Object... elements) {
        return encodeAsList(Arrays.asList(elements));
    }

    /**
     * Returns the encoding of an RLP list item containing the encoded elements of the given {@link Iterable} in the
     * given order.
     *
     * @param elements  the raw elements to be encoded as an RLP list item
     * @return  the encoded RLP list item
     */
    public static byte[] encodeAsList(Iterable<?> elements) {
        long listDataLen = totalEncodedLen(elements);
        byte[] dest = new byte[prefixLength(listDataLen) + (int) listDataLen];
        encodeList(listDataLen, elements, dest, 0);
        return dest;
    }

    /**
     * Inserts the encoding of an RLP list item, containing the encoded elements of the array in the given order, into
     * the destination array.
     *
     * @param elements  the raw elements to be encoded as an RLP list item
     * @param dest  the destination for the RLP encoding of the list
     * @param destIndex the index into the destination for the list
     */
    public static void encodeAsList(Object[] elements, byte[] dest, int destIndex) {
        encodeAsList(Arrays.asList(elements), dest, destIndex);
    }

    /**
     * Inserts the encoding of an RLP list item, containing the encoded elements of the {@link Iterable} in the given
     * order, into the destination array.
     *
     * @param elements  the raw elements to be encoded as an RLP list item
     * @param dest  the destination for the RLP encoding of the list
     * @param destIndex the index into the destination for the list
     */
    public static void encodeAsList(Iterable<?> elements, byte[] dest, int destIndex) {
        long listDataLen = totalEncodedLen(elements);
        encodeList(listDataLen, elements, dest, destIndex);
    }

    /**
     * Wraps n encodings in an RLPList.
     *
     * @param encodings the RLP-encoded elements of the new RLPList
     * @return  the RLPList containing the given elements
     */
    public static RLPList toList(RLPItem... encodings) {
        return toList(Arrays.asList(encodings));
    }

    /**
     * Wraps n encodings in an RLPList.
     *
     * @param encodings the RLP-encoded elements of the new RLPList
     * @return  the RLPList containing the given elements
     */
    public static RLPList toList(Iterable<RLPItem> encodings) {
        return RLPList.withElements(encodings);
    }

    public static void insertRecordContentList(int dataLen, long seq, KeyValuePair[] pairs, byte[] record, int offset) {
        Arrays.sort(pairs);
        encodeList(dataLen, seq, pairs, record, offset);
    }

    public static int insertRecordSignature(byte[] signature, byte[] record, int offset) {
        return encodeItem(signature, record, offset);
    }
}
