package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.util.RLPIntegers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.DataType.LIST_LONG_OFFSET;

/**
 * Created by Evo on 1/19/2017.
 */
public class RLPList extends RLPItem {

    RLPList(byte[] buffer, int index, int containerEnd, boolean lenient) throws DecodeException {
        super(buffer, index, containerEnd, lenient);
    }

    /**
     *
     * @param srcElements pre-encoded top-level elements of the list
     * @return
     */
    static RLPList withElements(Iterable<RLPItem> srcElements) {

        byte[] dest;

        int dataLen = 0;
        for (RLPItem element : srcElements) {
            dataLen += element.encodingLength();
        }

        if (dataLen < DataType.MIN_LONG_DATA_LEN) {
            dest = new byte[1 + dataLen];
            dest[0] = (byte) (DataType.LIST_SHORT_OFFSET + dataLen);
            copyElements(srcElements, dest, 1);
        } else {
            dest = encodeListLong(dataLen, srcElements);
        }

        try {
            return new RLPList(dest, 0, dest.length, false);
        } catch (DecodeException de) {
            throw new AssertionError(de);
        }
    }

    @Override
    public boolean isList() {
        return true;
    }

    /**
     * Use {@link #elements(RLPDecoder)} or {@link #elements(RLPDecoder, Collection)}
     * @param results
     * @param decoder
     * @throws DecodeException
     */
    @Deprecated
    public void elementsRecursive(Collection<Object> results, RLPDecoder decoder) throws DecodeException {
        List<RLPItem> actualList = elements(decoder);
        for (RLPItem element : actualList) {
            if(element.isList()) {
                List<Object> subList = new ArrayList<>();
                ((RLPList) element).elementsRecursive(subList, decoder);
                results.add(subList);
            } else {
                results.add(element);
            }
        }
    }

    public List<RLPItem> elements(RLPDecoder decoder) throws DecodeException {
        ArrayList<RLPItem> arrayList = new ArrayList<>();
        elements(decoder, arrayList);
        return arrayList;
    }

    public void elements(RLPDecoder decoder, Collection<RLPItem> collection) throws DecodeException {
        int i = dataIndex;
        while (i < this.endIndex) {
            RLPItem item = decoder.wrap(buffer, i, this.endIndex);
            collection.add(item);
            i = item.endIndex;
        }
    }

    private static void copyElements(Iterable<RLPItem> srcElements, byte[] dest, int destIndex) {
        for (final RLPItem element : srcElements) {
            final int elementLen = element.encodingLength();
            System.arraycopy(element.buffer, element.index, dest, destIndex, elementLen);
            destIndex += elementLen;
        }
    }

    private static byte[] encodeListLong(final int srcDataLen, final Iterable<RLPItem> srcElements) {

        int t = srcDataLen;

        byte a = 0, b = 0, c = 0, d;

        int n = 1;
        d = (byte) (t & 0xFF);
        t = t >>> Byte.SIZE;
        if(t != 0) {
            n = 2;
            c = (byte) (t & 0xFF);
            t = t >>> Byte.SIZE;
            if(t != 0) {
                n = 3;
                b = (byte) (t & 0xFF);
                t = t >>> Byte.SIZE;
                if(t != 0) {
                    n = 4;
                    a = (byte) (t & 0xFF);
                }
            }
        }

        int destDataIndex = 1 + n;
        byte[] dest = new byte[destDataIndex + srcDataLen];
        dest[0] = (byte) (LIST_LONG_OFFSET + n);
        RLPIntegers.insertBytes(n, dest, 1, a, b, c, d);

        copyElements(srcElements, dest, destDataIndex);

        return dest;
    }
}
