package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.util.RLPIterator;

import java.io.IOException;
import java.io.InputStream;

public class RLPSequenceStreamIterator implements RLPIterator {

    private final RLPDecoder decoder;
    private final InputStream rlpStream;

    private transient byte[] buffer;
    private transient int index;

    RLPSequenceStreamIterator(RLPDecoder decoder, InputStream rlpStream) {
        this.decoder = decoder;
        this.rlpStream = rlpStream;
    }

    @Override
    public boolean hasNext() {
        if(buffer != null) {
            try {
                decoder.wrap(buffer, index);
                return true;
            } catch (DecodeException e) {
                /* do nothing */
            }
        }
        try {
            index = 0;
            final int available = rlpStream.available();
            buffer = new byte[available];
            return available > 0 && rlpStream.read(buffer) == available;
        } catch (IOException e) {
            buffer = null;
            throw new RuntimeException(e);
        }
    }

    @Override
    public RLPItem next() throws DecodeException {
        RLPItem item = decoder.wrap(buffer, index);
        index = item.endIndex;
        return item;
    }
}
