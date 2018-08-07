package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.util.Integers;
import com.esaulpaugh.headlong.rlp.util.ObjectNotation;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;

import static com.esaulpaugh.headlong.rlp.DataType.LIST_LONG;
import static com.esaulpaugh.headlong.rlp.DataType.LIST_SHORT;
import static com.esaulpaugh.headlong.rlp.DataType.MIN_LONG_DATA_LEN;
import static com.esaulpaugh.headlong.rlp.DataType.STRING_LONG;
import static com.esaulpaugh.headlong.rlp.DataType.STRING_SHORT;

/**
 * Stateless, though not designed to be thread-safe.
 */
public class RLPCodec {

    /* ***************************************************************/
    // Todo: streaming
    /* ***************************************************************/

    private static boolean isLong(long dataLen) {
        return dataLen >= MIN_LONG_DATA_LEN;
    }

    private static int prefixLength(long dataLen) {
        if (isLong(dataLen)) {
            return 1 + Integers.numBytes(dataLen);
        } else {
            return 1;
        }
    }

//    public static long totalEncodedLen(Object[] items) {
//        long total = 0;
//        final int len = items.length;
//        for (int i = 0; i < len; i++) {
//            total += itemEncodedLen(items[i]);
//        }
//        return total;
//    }

    private static long totalEncodedLen(Iterable<Object> items) {
        long total = 0;
        for (Object item : items) {
            total += itemEncodedLen(item);
        }
        return total;
    }

    private static long itemEncodedLen(Object item) {
        if (item instanceof byte[]) {
            return stringEncodedLen((byte[]) item);
        }
        if (item instanceof Iterable) {
            return listEncodedLen((Iterable<Object>) item);
        }
        if(item instanceof Object[]) {
            return listEncodedLen(Arrays.asList((Object[]) item));
        }
        if(item == null) {
            throw new NullPointerException();
        }
        throw new IllegalArgumentException("unsupported object type: " + item.getClass().getName());
    }

    private static int stringEncodedLen(byte[] byteString) {
        final int dataLen = byteString.length;
        if (isLong(dataLen)) {
            return 1 + Integers.numBytes(dataLen) + dataLen;
        }
        if (dataLen == 1 && byteString[0] >= 0x00) { // same as (bytes[0] & 0xFF) < 0x80
            return 1;
        }
        return 1 + dataLen;
    }

    private static long listEncodedLen(Iterable<Object> elements) {
        final long listDataLen = totalEncodedLen(elements);
        if (isLong(listDataLen)) {
            return 1 + Integers.numBytes(listDataLen) + listDataLen;
        }
        return 1 + listDataLen;
    }

    private static int encodeItem(Object item, byte[] dest, int destIndex) {
        if (item instanceof byte[]) {
            return encodeString((byte[]) item, dest, destIndex);
        }
        if (item instanceof Iterable) {
            Iterable<Object> elements = (Iterable<Object>) item;
            return encodeList(totalEncodedLen(elements), elements, dest, destIndex);
        }
        if(item instanceof Object[]) {
            Iterable<Object> elements = Arrays.asList((Object[]) item);
            return encodeList(totalEncodedLen(elements), elements, dest, destIndex);
        }
        if(item == null) {
            throw new NullPointerException();
        }
        throw new IllegalArgumentException("unsupported object type: " + item.getClass().getName());
    }

    private static int encodeString(byte[] bytes, byte[] dest, int destIndex) {
        final int dataLen = bytes.length;
        if (isLong(dataLen)) { // long string
            int n = Integers.put(dataLen, dest, destIndex + 1);
            dest[destIndex] = (byte) (STRING_LONG.offset + (byte) n);
            destIndex += 1 + n;
            System.arraycopy(bytes, 0, dest, destIndex, dataLen);
            destIndex += dataLen;
        } else { // short strings
            if(dataLen == 1) {
                byte first = bytes[0];
                if(first >= 0x00) { // same as (first & 0xFF) < 0x80
                    dest[destIndex++] = first;
                } else {
                    dest[destIndex++] = (byte) (STRING_SHORT.offset + (byte) dataLen);
                    dest[destIndex++] = first;
                }
            } else {
                dest[destIndex++] = (byte) (STRING_SHORT.offset + (byte) dataLen);
                for (int i = 0; i < dataLen; i++) {
                    dest[destIndex++] = bytes[i];
                }
            }
        }
        return destIndex;
    }

//    private static int encodeList(long dataLen, Object[] elements, byte[] dest, int destIndex) {
//        destIndex = encodeListPrefix(dataLen, dest, destIndex);
//        return encodeSequentially(elements, dest, destIndex);
//    }

    private static int encodeList(long dataLen, Iterable<Object> elements, byte[] dest, int destIndex) {
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
        final int n = Integers.put(dataLen, dest, lengthIndex);
        dest[destIndex] = (byte) (LIST_LONG.offset + (byte) n);
        return lengthIndex + n;
    }

    private static int encodeShortListPrefix(final long dataLen, byte[] dest, final int destIndex) {
        dest[destIndex] = (byte) (LIST_SHORT.offset + (byte) dataLen);
        return destIndex + 1;
    }

    /**
     * e.g. 0xC0
     * @param lengthOneRLP
     * @return
     */
    public static RLPItem wrap(byte lengthOneRLP) throws DecodeException {
        return wrap(new byte[] { lengthOneRLP }, 0);
    }

    public static RLPItem wrap(byte[] encoding) throws DecodeException {
        return wrap(encoding, 0);
    }

    public static RLPItem wrap(byte[] buffer, int index) throws DecodeException {
        return wrap(buffer, index, buffer.length);
    }

    static RLPItem wrap(byte[] buffer, int index, int containerEnd) throws DecodeException {
        switch (DataType.type(buffer[index])) {
        case SINGLE_BYTE:
        case STRING_SHORT:
        case STRING_LONG:
            return new RLPString(buffer, index, containerEnd);
        case LIST_SHORT:
        case LIST_LONG:
            return new RLPList(buffer, index, containerEnd);
        default:
            throw new RuntimeException("???");
        }
    }

    public static byte[] encode(byte b) {
        return encode(new byte[] { b });
    }

    public static byte[] encode(byte[] byteString) {
        byte[] dest = new byte[stringEncodedLen(byteString)];
        encodeString(byteString, dest, 0);
        return dest;
    }

    public static byte[] encodeSequentially(Object... items) {
        Iterable<Object> iterable = Arrays.asList(items);
        byte[] dest = new byte[(int) totalEncodedLen(iterable)];
        encodeSequentially(iterable, dest, 0);
        return dest;
    }

    public static byte[] encodeSequentially(Iterable<Object> items) {
        byte[] dest = new byte[(int) totalEncodedLen(items)];
        encodeSequentially(items, dest, 0);
        return dest;
    }

    public static int encodeSequentially(Object[] items, byte[] dest, int destIndex) {
        for (Object item : items) {
            destIndex = encodeItem(item, dest, destIndex);
        }
        return destIndex;
    }

    public static int encodeSequentially(Iterable<Object> items, byte[] dest, int destIndex) {
        for (Object item : items) {
            destIndex = encodeItem(item, dest, destIndex);
        }
        return destIndex;
    }

    public static byte[] encodeAsList(Object... elements) {
        return encodeAsList(Arrays.asList(elements));
    }

    public static byte[] encodeAsList(Iterable<Object> elements) {
        long listDataLen = totalEncodedLen(elements);
        byte[] dest = new byte[prefixLength(listDataLen) + (int) listDataLen];
        encodeList(listDataLen, elements, dest, 0);
        return dest;
    }

    public static void encodeAsList(Object[] elements, byte[] dest, int destIndex) {
        encodeAsList(Arrays.asList(elements), dest, destIndex);
    }

    public static void encodeAsList(Iterable<Object> elements, byte[] dest, int destIndex) {
        long listDataLen = totalEncodedLen(elements);
        encodeList(listDataLen, elements, dest, destIndex);
    }

    public static RLPList toList(Iterable<RLPItem> encodings) throws DecodeException {
        return RLPList.withElements(encodings);
    }

    public static List<RLPItem> collect(final int n, byte[] encodings) throws DecodeException {
        ArrayList<RLPItem> arrayList = new ArrayList<>();
        collect((count, i) -> count < n, encodings, 0, arrayList);
        return arrayList;
    }

    public static void collect(int n, byte[] encodings, int index, RLPItem[] dest) throws DecodeException {
        int count = 0;
        while (count < n) {
            RLPItem item = RLPCodec.wrap(encodings, index);
            dest[count] = item;
            count++;
            index = item.endIndex;
        }
    }

    public static int collect(BiPredicate<Integer, Integer> predicate, byte[] encodings, int index, Collection<RLPItem> collection) throws DecodeException {
        int count = 0;
        while (predicate.test(count, index)) {
            RLPItem item = RLPCodec.wrap(encodings, index);
            collection.add(item);
            count++;
            index = item.endIndex;
        }
        return count;
    }

    public static void main(String[] args0) throws DecodeException {

        byte[] data0 = new byte[] {
                (byte) 0xf8, (byte) 148, // TODO test multiple length bytes
                (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, (byte) 0x80, (byte) 0x81, (byte) 0x81, (byte) 0x81, (byte) '\u0093', (byte) '\u230A',
                (byte) 0xb8, 56, 0x09,(byte)0x80,-1,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, -3, -2, 0, 0,
                (byte) 0xf8, 0x38, 0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 36, 74, 0, 0,
                (byte) 0x84, 'c', 'a', 't', 's',
                (byte) 0x84, 'd', 'o', 'g', 's',
                (byte) 0xca, (byte) 0x84, 92, '\r', '\n', '\f', (byte) 0x84, '\u0009', 'o', 'g', 's', // TODO TEST ALL 256 BYTE VALUES IN ALL ELEMENT TYPES
        };

        final byte[] invalidAf = new byte[] { (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, 0x00, (byte) 0x81, '\0', (byte) 0x81, '\u001B', (byte) '\u230A' };

//        final byte[] data0 = new byte[] { (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, (byte) 0x80, (byte) 0x81, (byte) 0x81, (byte) 0x81, (byte) '\u0080', (byte) '\u230A' };

//        final byte[] data0 = new byte[] { (byte) 0x83, 'c', 'a', 't' };
//        final byte[] data0 = new byte[] { (byte) 0x82, 'h', 'i', (byte) 0x84, 'L', 'O', 'O', 'K', '@', (byte) 0x82, 'm', 'e'};

        ObjectNotation objectNotation = ObjectNotation.fromEncoding(data0);

        System.out.println(objectNotation.toString());
        System.out.println(Hex.toHexString(data0));
        System.out.println("data0.length = " + data0.length);

        List<Object> objects = objectNotation.parse();

        final byte[] rlp = encodeSequentially(objects);

        System.out.println("BEFORE:" + Hex.toHexString(data0));
        System.out.println("AFTER: " + Hex.toHexString(rlp));
        System.out.println(Arrays.equals(data0, rlp) ? "SUCCESS" : "FAIL");
    }
}
