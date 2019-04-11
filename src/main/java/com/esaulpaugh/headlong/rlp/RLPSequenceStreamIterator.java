package com.esaulpaugh.headlong.rlp;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

public class RLPSequenceStreamIterator {

    private final RLPDecoder decoder;
    private final InputStream rlpStream;

    private transient byte[] buffer;
    private transient int index;

    private transient RLPItem rlpItem;

    RLPSequenceStreamIterator(RLPDecoder decoder, InputStream rlpStream) {
        this.decoder = decoder;
        this.rlpStream = rlpStream;
    }

    public boolean hasNext() {
        if(rlpItem != null) {
            return true;
        }
        try {
            if(buffer == null) {
                index = 0;
                buffer = new byte[rlpStream.available()];
                if(rlpStream.read(buffer) <= 0) {
                    buffer = null;
                    return false;
                }
            }
            rlpItem = decoder.wrap(buffer, index);
            return true;
        } catch (DecodeException e) {
            buffer = null;
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public RLPItem next() {
        if(hasNext()) {
            index = rlpItem.endIndex;
            RLPItem item = rlpItem;
            rlpItem = null;
            return item;
        }
        throw new NoSuchElementException();
    }
}
