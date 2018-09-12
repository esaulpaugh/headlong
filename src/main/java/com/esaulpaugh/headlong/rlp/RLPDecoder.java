package com.esaulpaugh.headlong.rlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Decodes RLP-formatted data.
 */
public class RLPDecoder {

    public static final RLPDecoder RLP_STRICT = new RLPDecoder(false);
    public static final RLPDecoder RLP_LENIENT = new RLPDecoder(true);

    public final boolean lenient;

    private RLPDecoder(boolean lenient) {
        this.lenient = lenient;
    }

    /**
     * e.g. 0xC0
     * @param lengthOneRLP  a one-byte RLP encoding
     * @return  the item
     */
    public RLPItem wrap(byte lengthOneRLP) throws DecodeException {
        return wrap(new byte[] { lengthOneRLP }, 0);
    }

    public RLPItem wrap(byte[] encoding) throws DecodeException {
        return wrap(encoding, 0);
    }

    public RLPItem wrap(byte[] buffer, int index) throws DecodeException {
        return wrap(buffer, index, buffer.length);
    }

    RLPItem wrap(byte[] buffer, int index, int containerEnd) throws DecodeException {
        switch (DataType.type(buffer[index])) {
        case SINGLE_BYTE:
        case STRING_SHORT:
        case STRING_LONG:
            return new RLPString(buffer, index, containerEnd, lenient);
        case LIST_SHORT:
        case LIST_LONG:
            return new RLPList(buffer, index, containerEnd, lenient);
        default:
            throw new AssertionError();
        }
    }

    public List<RLPItem> collect(byte[] encodings, int index, final int endIndex) throws DecodeException {
        ArrayList<RLPItem> arrayList = new ArrayList<>();
        collect(encodings, index, endIndex, arrayList);
        return arrayList;
    }

    public void collect(byte[] encodings, int index, final int endIndex, Collection<RLPItem> dest) throws DecodeException {
        while (index < endIndex) {
            RLPItem item = wrap(encodings, index);
            dest.add(item);
            index = item.endIndex;
        }
    }

    public List<RLPItem> collect(final int n, byte[] encodings) throws DecodeException {
        ArrayList<RLPItem> arrayList = new ArrayList<>(n);
        collect(n, encodings, 0, arrayList);
        return arrayList;
    }

    public void collect(int n, byte[] encodings, int index, Collection<RLPItem> dest) throws DecodeException {
        int count = 0;
        while (count < n) {
            RLPItem item = wrap(encodings, index);
            dest.add(item);
            count++;
            index = item.endIndex;
        }
    }

    public void collect(byte[] encodings, int index, final int endIndex, RLPItem[] dest) throws DecodeException {
        int count = 0;
        while (index < endIndex) {
            RLPItem item = wrap(encodings, index);
            dest[count] = item;
            count++;
            index = item.endIndex;
        }
    }

    public void collect(int n, byte[] encodings, int index, RLPItem[] dest) throws DecodeException {
        int count = 0;
        while (count < n) {
            RLPItem item = wrap(encodings, index);
            dest[count] = item;
            count++;
            index = item.endIndex;
        }
    }

//    public List<RLPItem> collect(final int n, byte[] encodings) throws DecodeException {
//        ArrayList<RLPItem> arrayList = new ArrayList<>(n);
//        collect((count, i) -> count < n, encodings, 0, arrayList);
//        return arrayList;
//    }
//
//    public int collect(java.util.function.BiPredicate<Integer, Integer> predicate, byte[] encodings, int index, Collection<RLPItem> collection) throws DecodeException {
//        int count = 0;
//        while (predicate.test(count, index)) {
//            RLPItem item = wrap(encodings, index);
//            collection.add(item);
//            count++;
//            index = item.endIndex;
//        }
//        return count;
//    }
}
