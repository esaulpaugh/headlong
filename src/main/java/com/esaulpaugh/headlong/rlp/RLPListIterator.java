package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.exception.DecodeException;

import java.util.NoSuchElementException;

public class RLPListIterator {

    private final RLPList list;
    private final RLPDecoder decoder;

    private int nextElementIndex;

    public RLPListIterator(RLPList list, RLPDecoder decoder) {
        this.list = list;
        this.decoder = decoder;
        this.nextElementIndex = list.dataIndex;
    }

    public boolean hasNext() {
        return this.nextElementIndex < list.endIndex;
    }

    public RLPItem next() throws DecodeException {
        if (hasNext()) {
            RLPItem element = decoder.wrap(list.buffer, this.nextElementIndex, list.endIndex);
            this.nextElementIndex = element.endIndex;
            return element;
        }
        throw new NoSuchElementException();
    }
}
