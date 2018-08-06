package com.esaulpaugh.headlong.rlp.codec;

import com.esaulpaugh.headlong.rlp.codec.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.codec.util.Integers;

import java.util.ArrayList;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.codec.DataType.LIST_LONG;

/**
 * Created by Evo on 1/19/2017.
 */
public class RLPList extends RLPItem {

    RLPList(byte[] buffer, int index, int containerLimit) throws DecodeException {
        super(buffer, index, containerLimit);
    }

    /**
     *
     * @param srcElements pre-encoded top-level elements of the list
     * @return
     */
    static RLPList withElements(Iterable<RLPItem> srcElements) throws DecodeException {

        byte[] dest;

        int dataLen = 0;
        for (RLPItem element : srcElements) {
            dataLen += element.encodingLength();
        }

        if (dataLen < DataType.MIN_LONG_DATA_LEN) {
            dest = new byte[1 + dataLen];
            dest[0] = (byte) (DataType.LIST_SHORT.offset + dataLen);
            copyElements(srcElements, dest, 1);
        } else {
            dest = encodeListLong(dataLen, srcElements);
        }

        return new RLPList(dest, 0, dest.length);
    }

    @Override
    public boolean isList() {
        return true;
    }

    public List<RLPItem> elements() throws DecodeException {

        ArrayList<RLPItem> arrayList = new ArrayList<>();

        final int end = dataIndex + dataLength;
        int i = dataIndex;
        while (i < end) {
            RLPItem newElement = RLPItem.fromEncoding(buffer, i, endIndex());

            arrayList.add(newElement);
            i += newElement.encodingLength();
        }

        return arrayList;
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
        dest[0] = (byte) (LIST_LONG.offset + n);
        Integers.insertBytes(n, dest, 1, a, b, c, d);

        copyElements(srcElements, dest, destDataIndex);

        return dest;
    }
}
