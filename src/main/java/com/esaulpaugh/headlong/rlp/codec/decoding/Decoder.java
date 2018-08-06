//package com.esaulpaugh.headlong.rlp.codec.decoding;
//
//import com.esaulpaugh.headlong.rlp.codec.DataType;
//import com.esaulpaugh.headlong.rlp.codec.exception.DecodeException;
//import com.esaulpaugh.headlong.rlp.codec.util.Integers;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static com.esaulpaugh.headlong.rlp.codec.DataType.MIN_LONG_DATA_LEN;
//
//public class Decoder {
//
//    private static final boolean ONLINE = false;
//
//    private static int getShortElementEnd(int elementDataIndex, final int elementDataLen, final int containerLimit) throws DecodeException {
//        final int end = elementDataIndex + elementDataLen;
//        if(end > containerLimit) {
//            throw new DecodeException("element @ index " + (elementDataIndex - 1) + " exceeds its container: " + end + " > " + containerLimit);
//        }
//        return end;
//    }
//
//    private static int getLongElementEnd(byte[] data, final int leadByteIndex, final int lengthLen, final int containerLimit) throws DecodeException {
//
//        int lengthIndex = leadByteIndex + 1;
//        int dataIndex = lengthIndex + lengthLen;
//        if(dataIndex > containerLimit) {
//            throw new DecodeException("end of input reached; element @ " + leadByteIndex + " cannot be decoded: " + (lengthIndex + lengthLen) + " > " + containerLimit);
//        }
//        final long dataLenLong = Integers.get(data, leadByteIndex + 1, lengthLen);
//        if(dataLenLong > Integer.MAX_VALUE) {
//            throw new DecodeException("too much data: " + dataLenLong + " > " + Integer.MAX_VALUE);
//        }
//        final int dataLen = (int) dataLenLong;
//        if(dataLen < MIN_LONG_DATA_LEN) {
//            throw new DecodeException("long element data length must be " + MIN_LONG_DATA_LEN + " or greater; found: " + dataLen + " for element @ " + leadByteIndex);
//        }
//        final int end = dataIndex + dataLen;
//        if(end < 0) {
//            throw new DecodeException("overflow");
//        }
//        if(end > containerLimit) {
//            throw new DecodeException("element @ index " + leadByteIndex + " exceeds its container; indices: " + end + " > " + containerLimit);
//        }
//
//        return end;
//    }
//
//    public static List<Object> decodeRLP(byte[] encoding) throws DecodeException {
//        return decodeRLP(encoding, 0, encoding.length);
//    }
//
//    private static List<Object> decodeRLP(byte[] encoding, int index, int len) throws DecodeException {
//        List<Object> top = new ArrayList<>();
//        buildLongList(
//                encoding,
//                index,
//                ONLINE ? Integer.MAX_VALUE : index + len,
//                top
//        );
//        return top;
//    }
//
//    private static int buildLongList(final byte[] data, int i, int end, final List<Object> parent) throws DecodeException {
//
//        int elementDataIndex = -1;
//        int lengthLen;
//        int elementDataLen;
//        int elementEnd = -1;
//        List<Object> childList;
//        while(i < end) {
//            byte current = data[i];
//            final DataType type = DataType.type(current);
//            switch (type) {
//            case SINGLE_BYTE:
//                break;
//            case STRING_SHORT:
//            case LIST_SHORT:
//                elementDataIndex = i + 1;
//                elementDataLen = current - type.offset;
//                elementEnd = getShortElementEnd(elementDataIndex, elementDataLen, end);
//                break;
//            case STRING_LONG:
//            case LIST_LONG:
//                lengthLen = current - type.offset;
//                elementDataIndex = i + 1 + lengthLen;
//                elementEnd = getLongElementEnd(data, i, lengthLen, end);
//                break;
//            default:
//                throw new RuntimeException();
//            }
//            switch (type) {
//            case SINGLE_BYTE:
//                i = buildByte(parent, data, i);
//                break;
//            case STRING_SHORT:
//                i = buildString(parent, data, elementDataIndex, elementEnd);
//                break;
//            case LIST_SHORT:
//                childList = new ArrayList<>();
//                i = buildShortList(childList, data, elementDataIndex, elementEnd);
//                parent.add(childList);
//                break;
//            case STRING_LONG:
//                i = buildString(parent, data, elementDataIndex, elementEnd);
//                break;
//            case LIST_LONG:
//                childList = new ArrayList<>();
//                i = buildLongList(data, elementDataIndex, elementEnd, childList);
//                parent.add(childList);
////            default:
//            }
//        }
//
//        return end;
//    }
//
//    private static int buildShortList(List<Object> parent, final byte[] data, final int dataIndex, final int end) throws DecodeException {
//
//        int elementDataIndex = -1;
//        int elementDataLen;
//        int elementEnd = -1;
//        List<Object> childList;
//        int i = dataIndex;
//        while(i < end) {
//            byte current = data[i];
//            final DataType type = DataType.type(current);
//            switch (type) {
//            case SINGLE_BYTE:
//                break;
//            case STRING_SHORT:
//            case LIST_SHORT:
//                elementDataIndex = i + 1;
//                elementDataLen = current - type.offset;
//                elementEnd = getShortElementEnd(elementDataIndex, elementDataLen, end);
//                break;
//            case STRING_LONG:
//            case LIST_LONG:
//                throw new IllegalStateException("surely, it cannot possibly fit. index: " + i);
//            default:
//                throw new RuntimeException();
//            }
//            switch (type) {
//            case SINGLE_BYTE: i = buildByte(parent, data, i); break;
//            case STRING_SHORT: i = buildString(parent, data, elementDataIndex, elementEnd); break;
//            case LIST_SHORT:
//                childList = new ArrayList<>();
//                i = buildShortList(childList, data, elementDataIndex, elementEnd);
//                parent.add(childList);
//                break;
//            case STRING_LONG: break;
//            case LIST_LONG: break;
//            default:
//            }
//        }
//
//        return end;
//    }
//
//    private static int buildByte(List<Object> objects, byte[] data, int dataIndex) {
//
//        objects.add(new byte[] { data[dataIndex] }); // autoboxed
//
//        return dataIndex + 1;
//    }
//
//    private static int buildString(List<Object> objects, byte[] data, int from, int to) throws DecodeException {
//
//        final int len = to - from;
//        if(len == 1 && data[from] >= 0x00) { // same as (data[from] & 0xFF) < 0x80
//            throw new DecodeException("invalid rlp for single byte @ " + (from - 1));
//        }
//        byte[] range = new byte[len];
//        System.arraycopy(data, from, range, 0, len);
//        objects.add(range);
//
//        return to;
//    }
//}
