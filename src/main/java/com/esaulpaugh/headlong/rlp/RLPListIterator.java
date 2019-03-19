package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.util.RLPIterator;

import java.util.NoSuchElementException;

public class RLPListIterator implements RLPIterator {

    private final RLPList list;
    private final RLPDecoder decoder;

    private int nextElementIndex;

    public RLPListIterator(RLPList list, RLPDecoder decoder) {
        this.list = list;
        this.decoder = decoder;
        this.nextElementIndex = list.dataIndex;
    }

    @Override
    public boolean hasNext() {
        return this.nextElementIndex < list.endIndex;
    }

    @Override
    public RLPItem next() throws DecodeException {
        if (hasNext()) {
            RLPItem element = decoder.wrap(list.buffer, this.nextElementIndex, list.endIndex);
            this.nextElementIndex = element.endIndex;
            return element;
        }
        throw new NoSuchElementException();
    }
}
