package com.esaulpaugh.headlong.rlp;

public class SequenceIterator {

    private RLPDecoder decoder;
    private byte[] rlp;
    private int index;
    private int end;

    public SequenceIterator(byte[] rlp, int start, int end, RLPDecoder decoder) {
        this.rlp = rlp;
        this.index = start;
        this.end = end;
        this.decoder = decoder;
    }

    public boolean hasNext() {
        return index < end;
    }

    public RLPItem next() throws DecodeException {
        RLPItem item = decoder.wrap(rlp, index);
        this.index = item.endIndex;
        return item;
    }
}
