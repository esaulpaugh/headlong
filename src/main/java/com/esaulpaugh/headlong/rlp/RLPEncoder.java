package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.util.FloatingPoint;
import com.esaulpaugh.headlong.rlp.util.Integers;
import com.esaulpaugh.headlong.rlp.util.ObjectNotation;
import com.esaulpaugh.headlong.rlp.util.Parser;
import com.esaulpaugh.headlong.rlp.util.Strings;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.esaulpaugh.headlong.rlp.DataType.LIST_LONG_OFFSET;
import static com.esaulpaugh.headlong.rlp.DataType.LIST_SHORT_OFFSET;
import static com.esaulpaugh.headlong.rlp.DataType.MIN_LONG_DATA_LEN;
import static com.esaulpaugh.headlong.rlp.DataType.STRING_LONG_OFFSET;
import static com.esaulpaugh.headlong.rlp.DataType.STRING_SHORT_OFFSET;
import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.rlp.util.Strings.HEX;
import static com.esaulpaugh.headlong.rlp.util.Strings.UTF_8;

public class RLPEncoder {

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

    private static long totalEncodedLen(Iterable<Object> objects) {
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
        if (obj instanceof Iterable) {
            return listEncodedLen((Iterable<Object>) obj);
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
        final int n = Integers.putLong(dataLen, dest, lengthIndex);
        dest[destIndex] = (byte) (LIST_LONG_OFFSET + (byte) n);
        return lengthIndex + n;
    }

    private static int encodeShortListPrefix(final long dataLen, byte[] dest, final int destIndex) {
        dest[destIndex] = (byte) (LIST_SHORT_OFFSET + (byte) dataLen);
        return destIndex + 1;
    }

    // -----------------------------------------------------------------------------------------------------------------

    public static byte[] encode(byte b) {
        return encode(new byte[] { b });
    }

    public static byte[] encode(byte[] byteString) {
        byte[] dest = new byte[stringEncodedLen(byteString)];
        encodeString(byteString, dest, 0);
        return dest;
    }

    public static byte[] encodeSequentially(Object... objects) {
        byte[] dest = new byte[(int) totalEncodedLen(Arrays.asList(objects))];
        encodeSequentially(objects, dest, 0);
        return dest;
    }

    public static byte[] encodeSequentially(Iterable<Object> objects) {
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
    public static int encodeSequentially(Iterable<Object> objects, byte[] dest, int destIndex) {
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
    public static byte[] encodeAsList(Iterable<Object> elements) {
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
    public static void encodeAsList(Iterable<Object> elements, byte[] dest, int destIndex) {
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
     * @throws DecodeException
     */
    public static RLPList toList(Iterable<RLPItem> encodings) {
        return RLPList.withElements(encodings);
    }

    private static byte[][] TESTS = new byte[100_000][];

    static {
        Random r = new Random();
        r.setSeed(new SecureRandom().nextLong());
        for (int i = 0; i < TESTS.length; i++) {
            TESTS[i] = new byte[r.nextInt(56)];
            r.nextBytes(TESTS[i]);
        }
    }

    public static void main(String[] args0) throws DecodeException {

//        byte[] bytes0 = new byte[0];
//        Arrays.fill(bytes0, (byte) 0xFF);
//        byte[] dest = new byte[56];

        int di;
        int destIndex;

        // sw 5.69
        // for 9.107
        // ac 2.82


//        long start, end;


        // switch 318 w/ 3, 100_000_000
        // arraycopy 602 w/ 3, 100_000_000
        // for 545 w/ 3, 100_000_000

        // switch 3300 w/ 55
        // arraycopy 973 w/ 55
        // for 4487 w/ 55

        // switch:
        // for: 195 w/ 1, 1399 w/ 27, 2408 w/ 55
        // arraycopy: 513 w/ 1, 819 w/ 27, 980 w/ 55

//        destIndex = 1;
//        for (int j = 0; j < dataLen; j++) {
//            dest[destIndex++] = b[j];
//        }
//        System.arraycopy(b, 0, dest, 1, dataLen);
//        System.out.println(Arrays.toString(dest));
//        if(true) return;

//        for (int i = 0; i < TESTS.length; i++) {
//            byte[] bytes = TESTS[i];
//            di = 1;
//            final int len = bytes.length;
//            System.arraycopy(bytes, 0, dest, di, len); // 876
//        }
//        start = System.nanoTime();
//        for (int i = 0; i < TESTS.length; i++) {
//            byte[] bytes = TESTS[i];
//            di = 1;
//            final int len = bytes.length;
////            copyShortString2(bytes, dest, 1);
////            copyShortString(b, dest, 1);
////            for (int j = 0; j < len; j++) { // 1400 w/ 27
////                dest[di++] = bytes[j];
////            }
//            System.arraycopy(bytes, 0, dest, di, len);
//        }
//        end = System.nanoTime();
//
//        System.out.println((end - start) / 1_000_000.0);


//        byte[] data0 = new byte[] {
//                (byte) 0xf8, (byte) 148, // TODO test multiple length bytes
//                (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, (byte) 0x80, (byte) 0x81, (byte) 0x81, (byte) 0x81, (byte) '\u0093', (byte) '\u230A',
//                (byte) 0xb8, 56, 0x09,(byte)0x80,-1,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, -3, -2, 0, 0,
//                (byte) 0xf8, 0x38, 0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 36, 74, 0, 0,
//                (byte) 0x84, 'c', 'a', 't', 's',
//                (byte) 0x84, 'd', 'o', 'g', 's',
//                (byte) 0xca, (byte) 0x84, 92, '\r', '\n', '\f', (byte) 0x84, '\u0009', 'o', 'g', 's', // TODO TEST ALL 256 BYTE VALUES IN ALL ELEMENT TYPES
//        };
//
//        final byte[] invalidAf = new byte[] { (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, 0x00, (byte) 0x81, '\0', (byte) 0x81, '\u001B', (byte) '\u230A' };

//        final byte[] data0 = new byte[] { (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, (byte) 0x80, (byte) 0x81, (byte) 0x81, (byte) 0x81, (byte) '\u0080', (byte) '\u230A' };

//        final byte[] data0 = new byte[] { (byte) 0x82, 'h', 'i', (byte) 0x84, 'L', 'O', 'O', 'K', '@', (byte) 0x82, 'm', 'e'};

        // decode
        final byte[] rlp0 = new byte[] { (byte) 0xc0, (byte) 0x83, 'c', 'a', 't', 0x09, 0x09 };
        RLPItem item0 = RLP_STRICT.wrap(rlp0, 1); // wrap item at index 1
        String cat = item0.asString(UTF_8); // "cat"
        RLPItem item1 = RLP_STRICT.wrap(rlp0, item0.endIndex);
        String hex = item1.asString(HEX); // "09"

        // encode a list item with n elements
        byte[] rlp1 = RLPEncoder.encodeAsList(new byte[0], FloatingPoint.toBytes(0.5f), new Object[] {} );
        System.out.println(Strings.encode(rlp1, HEX)); // "c780843f000000c0"

        // concatenate n encodings
        byte[] rlp2 = RLPEncoder.encodeSequentially(Strings.decode(cat, UTF_8), Integers.toBytes(32L), new Object[] { new Object[] {}, new byte[] { '\t' } }, FloatingPoint.toBytes(0.0));
        System.out.println(Strings.encode(rlp2, HEX)); // "8363617420c2c00980"

        // Object notation and parser for debugging
        String notation = ObjectNotation.forEncoding(rlp2).toString();
        System.out.println(notation);
    /*
        (
          "636174",
          "20",
          { {  }, "09" },
          ""
        )
    */
        List<Object> rlp2Objects = Parser.parse(notation);
        byte[] rlp3 = RLPEncoder.encodeSequentially(rlp2Objects);
        System.out.println(Strings.encode(rlp3, HEX)); // "8363617420c2c00900"


//        ObjectNotation objectNotation = ObjectNotation.fromEncoding(data0);
//
//        System.out.println(objectNotation.toString());
//        System.out.println(Hex.toHexString(data0));
//        System.out.println("data0.length = " + data0.length);
//
//        List<Object> objects = objectNotation.parse();
//
//        final byte[] rlp = encodeSequentially(objects);
//
//        System.out.println("BEFORE:" + Hex.toHexString(data0));
//        System.out.println("AFTER: " + Hex.toHexString(rlp));
//        System.out.println(Arrays.equals(data0, rlp) ? "SUCCESS" : "FAIL");
    }
}
