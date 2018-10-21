package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.util.Integers;

import java.util.Arrays;

import static com.esaulpaugh.headlong.rlp.DataType.LIST_LONG_OFFSET;
import static com.esaulpaugh.headlong.rlp.DataType.LIST_SHORT_OFFSET;
import static com.esaulpaugh.headlong.rlp.DataType.MIN_LONG_DATA_LEN;
import static com.esaulpaugh.headlong.rlp.DataType.STRING_LONG_OFFSET;
import static com.esaulpaugh.headlong.rlp.DataType.STRING_SHORT_OFFSET;

/**
 * Encodes data to RLP format.
 */
public class RLPEncoder {

    private static boolean isLong(long dataLen) {
        return dataLen >= MIN_LONG_DATA_LEN;
    }

    private static int prefixLength(long dataLen) {
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
        destIndex = encodeListPrefix(dataLen, dest, destIndex);
        return encodeSequentially(elements, dest, destIndex);
    }

    private static int encodeListPrefix(long dataLen, byte[] dest, int destIndex) {
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

    // -----------------------------------------------------------------------------------------------------------------

    public static byte[] encodeAsString(byte b) {
        return encodeAsString(new byte[] { b });
    }

    public static byte[] encodeAsString(byte[] byteString) {
        byte[] dest = new byte[stringEncodedLen(byteString)];
        encodeString(byteString, dest, 0);
        return dest;
    }

    public static byte[] encodeSequentially(Object... objects) {
        byte[] dest = new byte[(int) totalEncodedLen(Arrays.asList(objects))];
        encodeSequentially(objects, dest, 0);
        return dest;
    }

    public static byte[] encodeSequentially(Iterable<?> objects) {
        byte[] dest = new byte[(int) totalEncodedLen(objects)];
        encodeSequentially(objects, dest, 0);
        return dest;
    }

    /**
     * <p>Concatenates the encodings of n objects. The array containing the objects is <i>not</i> encoded.
     *
     * @param objects
     * @param dest
     * @param destIndex
     * @return
     */
    public static int encodeSequentially(Object[] objects, byte[] dest, int destIndex) {
        for (Object item : objects) {
            destIndex = encodeItem(item, dest, destIndex);
        }
        return destIndex;
    }

    /**
     * <p>Concatenates the encodings of n objects. The {@code Iterable} containing the objects is <i>not</i> encoded.
     *
     * @param objects
     * @param dest
     * @param destIndex
     * @return
     */
    public static int encodeSequentially(Iterable<?> objects, byte[] dest, int destIndex) {
        for (Object obj : objects) {
            destIndex = encodeItem(obj, dest, destIndex);
        }
        return destIndex;
    }

    // ---------------------------------------------------------------------

    public static byte[] encodeAsList(Object... elements) {
        return encodeAsList(Arrays.asList(elements));
    }

    /**
     * Encodes an {@code Iterable} as a list item, with all its elements.
     *
     * @param elements
     * @return
     */
    public static byte[] encodeAsList(Iterable<?> elements) {
        long listDataLen = totalEncodedLen(elements);
        byte[] dest = new byte[prefixLength(listDataLen) + (int) listDataLen];
        encodeList(listDataLen, elements, dest, 0);
        return dest;
    }

    public static void encodeAsList(Object[] elements, byte[] dest, int destIndex) {
        encodeAsList(Arrays.asList(elements), dest, destIndex);
    }

    /**
     * Encodes an {@code Iterable} as a list item, with all its elements.
     *
     * @param elements
     * @param dest
     * @param destIndex
     */
    public static void encodeAsList(Iterable<?> elements, byte[] dest, int destIndex) {
        long listDataLen = totalEncodedLen(elements);
        encodeList(listDataLen, elements, dest, destIndex);
    }

    public static RLPList toList(RLPItem... encodings) {
        return toList(Arrays.asList(encodings));
    }

    /**
     * Wraps n encodings in a list item.
     *
     * @param encodings
     * @return
     */
    public static RLPList toList(Iterable<RLPItem> encodings) {
        return RLPList.withElements(encodings);
    }
}
