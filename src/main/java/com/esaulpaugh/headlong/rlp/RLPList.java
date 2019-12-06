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

import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.util.Integers;

import java.util.*;

import static com.esaulpaugh.headlong.rlp.DataType.LIST_LONG_OFFSET;

/**
 * Created by Evo on 1/19/2017.
 */
public final class RLPList extends RLPItem implements Iterable<RLPItem> {

    RLPList(byte lead, DataType type, byte[] buffer, int index, int containerEnd, boolean lenient) throws DecodeException {
        super(lead, type, buffer, index, containerEnd, lenient);
    }

    @Override
    public boolean isList() {
        return true;
    }

    /**
     * @param elements pre-encoded top-level elements of the list
     */
    static RLPList withElements(Iterable<RLPItem> elements) {
        int dataLen = 0;
        for (RLPItem e : elements) {
            dataLen += e.encodingLength();
        }
        try {
            return RLPDecoder.RLP_STRICT.wrapList(
                    dataLen < DataType.MIN_LONG_DATA_LEN
                            ? encodeListShort(dataLen, elements)
                            : encodeListLong(dataLen, elements)
            );
        } catch (DecodeException de) {
            throw new RuntimeException(de);
        }
    }

    private static byte[] encodeListShort(final int dataLen, final Iterable<RLPItem> elements) {
        byte[] dest = new byte[1 + dataLen];
        dest[0] = (byte) (DataType.LIST_SHORT_OFFSET + dataLen);
        copyElements(elements, dest, 1);
        return dest;
    }

    private static byte[] encodeListLong(final int dataLen, final Iterable<RLPItem> elements) {
        byte[] length = Integers.toBytes(dataLen);
        int destHeaderLen = 1 + length.length;
        byte[] dest = new byte[destHeaderLen + dataLen];
        dest[0] = (byte) (LIST_LONG_OFFSET + length.length);
        System.arraycopy(length, 0, dest, 1, length.length);
        copyElements(elements, dest, destHeaderLen);
        return dest;
    }

    private static void copyElements(Iterable<RLPItem> elements, byte[] dest, int destIndex) {
        for (RLPItem e : elements) {
            destIndex = e.export(dest, destIndex);
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
    
    @Override
    public RLPList duplicate(RLPDecoder decoder) throws DecodeException {
        return decoder.wrapList(encoding(), 0);
    }

    public RLPListIterator iterator(RLPDecoder decoder) {
        return new RLPListIterator(this, decoder);
    }

    @Override
    public Iterator<RLPItem> iterator() {
        return new Iterator<RLPItem>() {

            private final RLPListIterator iter = iterator(RLPDecoder.RLP_STRICT);

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public RLPItem next() {
                try {
                    return iter.next();
                } catch (DecodeException e) {
                    throw new NoSuchElementException(e.getMessage()); // *** beware of RuntimeException ***
                }
            }
        };
    }
}
