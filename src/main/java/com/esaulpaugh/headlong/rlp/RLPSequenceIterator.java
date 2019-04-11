package com.esaulpaugh.headlong.rlp;

import java.util.NoSuchElementException;

/**
 * For iterating over sequentially encoded RLP items.
 */
public class RLPSequenceIterator {

    private final RLPDecoder decoder;
    private final byte[] rlp;
    private int index;
    private final int end;

    RLPSequenceIterator(RLPDecoder decoder, byte[] rlp, int start, int end) {
        this.decoder = decoder;
        this.rlp = rlp;
        this.index = start;
        this.end = end;
    }

    public boolean hasNext() {
        return index < end;
    }

    public RLPItem next() throws DecodeException {
        if(hasNext()) {
            RLPItem item = decoder.wrap(rlp, index);
            this.index = item.endIndex;
            return item;
        }
        throw new NoSuchElementException();
    }
}
