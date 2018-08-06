package com.esaulpaugh.headlong.rlp.codec;

import com.esaulpaugh.headlong.rlp.codec.decoding.ObjectNotation;
import com.esaulpaugh.headlong.rlp.codec.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.codec.util.Integers;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.codec.DataType.LIST_LONG;
import static com.esaulpaugh.headlong.rlp.codec.DataType.LIST_SHORT;
import static com.esaulpaugh.headlong.rlp.codec.DataType.MIN_LONG_DATA_LEN;
import static com.esaulpaugh.headlong.rlp.codec.DataType.STRING_LONG;
import static com.esaulpaugh.headlong.rlp.codec.DataType.STRING_SHORT;

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

    public static long totalDataLen(Iterable<Object> list) {
        long total = 0;
        for (Object obj : list) {
            if (obj instanceof byte[]) {
                final byte[] bytes = (byte[]) obj;
                int dataLen = bytes.length;
                if (isLong(dataLen)) {
                    int n = Integers.numBytes(dataLen);
                    total += 1 + n + dataLen;
                } else {
                    if(dataLen == 1 && bytes[0] >= 0x00) { // same as (bytes[0] & 0xFF) < 0x80
                        total++;
                    } else {
                        total += 1 + dataLen;
                    }
                }
            } else if (obj instanceof List) {
                long subListLen = totalDataLen((Iterable<Object>) obj);
                if (isLong(subListLen)) {
                    total += 1 + Integers.numBytes(subListLen) + subListLen;
                } else {
                    total += 1 + subListLen;
                }
            }
        }
        return total;
    }

    private static int encodeObject(Object o, byte[] dest, int destIndex) {
        if (o instanceof byte[]) {
            final byte[] bytes = (byte[]) o;
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
        } else if (o instanceof Iterable) {
            Iterable<Object> list = (Iterable<Object>) o;
            long listLen = totalDataLen(list);
            destIndex = encodeList(list, listLen, isLong(listLen), dest, destIndex);
        }

        return destIndex;
    }

    private static int encodeList(Iterable<Object> list, final long listLen, boolean isLong, byte[] dest, int destIndex) {
        if (isLong) {
//            if(listLen > Integer.MAX_VALUE) {
//                throw new Exception("too much data: " + listLen);
//            }
            int n = Integers.put(listLen, dest, destIndex + 1);
            dest[destIndex] = (byte) (LIST_LONG.offset + (byte) n);
            destIndex += 1 + n;
            for (Object o : list) {
                destIndex = encodeObject(o, dest, destIndex);
            }
        } else {
            destIndex++;
            final int dataIndex = destIndex;
            for (Object o : list) {
                destIndex = encodeObject(o, dest, destIndex);
            }
            dest[dataIndex - 1] = (byte) (LIST_SHORT.offset + (byte) (destIndex - dataIndex));
        }
        return destIndex;
    }

    /**
     * e.g. 0xC0
     * @param lengthOneRLP
     * @return
     */
    public static RLPItem wrap(byte lengthOneRLP) throws DecodeException {
        return RLPItem.fromEncoding(new byte[] { lengthOneRLP }, 0, 1);
    }

    public static RLPItem wrap(byte[] encoding) throws DecodeException {
        return RLPItem.fromEncoding(encoding, 0, encoding.length);
    }

    public static RLPItem wrap(byte[] encoding, int index, int len) throws DecodeException {
        return RLPItem.fromEncoding(encoding, index, len);
    }

    public static byte[] encodeAsList(Iterable<Object> objects) {
        long dataLen = totalDataLen(objects);
        byte[] dest = new byte[prefixLength(dataLen) + (int) dataLen];
        encodeList(objects, dataLen, isLong(dataLen), dest, 0);
        return dest;
    }

    public static void encodeAsList(Iterable<Object> objects, byte[] dest, int destIndex) {
        long dataLen = totalDataLen(objects);
        encodeList(objects, dataLen, isLong(dataLen), dest, destIndex);
    }

    public static RLPList toList(Iterable<RLPItem> elements) throws DecodeException {
        return RLPList.withElements(elements);
    }

    public static byte[] encodeAsList(byte[]... data) {
        return encodeAsList(Arrays.asList((Object[]) data));
    }

    public static byte[] encodeAll(byte[]... data) {
        return encodeAll(Arrays.asList((Object[]) data));
    }

    public static byte[] encodeAll(Iterable<Object> objects) {
        long dataLen = totalDataLen(objects);
        byte[] dest = new byte[(int) dataLen];
        encodeAll(objects, dest, 0);
        return dest;
    }

    public static void encodeAll(Iterable<Object> objects, byte[] dest, int destIndex) {
        for (Object o : objects) {
            destIndex = encodeObject(o, dest, destIndex);
        }
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

        final byte[] rlp = encodeAll(objects);

        System.out.println("BEFORE:" + Hex.toHexString(data0));
        System.out.println("AFTER: " + Hex.toHexString(rlp));
        System.out.println(Arrays.equals(data0, rlp) ? "SUCCESS" : "FAIL");
    }
}
