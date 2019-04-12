package com.esaulpaugh.headlong.rlp;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

public class RLPSequenceStreamIterator {

    private final RLPDecoder decoder;
    private final InputStream rlpStream;

    private transient byte[] buffer;
    private transient int index;
    private transient int readIndex;

    private transient RLPItem rlpItem;

    RLPSequenceStreamIterator(RLPDecoder decoder, InputStream rlpStream) {
        this.decoder = decoder;
        this.rlpStream = rlpStream;
        this.buffer = new byte[this.index = 0]; // make sure index == buffer.length
        this.readIndex = 0;
    }

    public boolean hasNext() {
        if(rlpItem != null) {
            return true;
        }
        try {
            final int available = rlpStream.available();
            if(available > 0) {
                byte[] newBuffer;
                if (index == buffer.length) {
                    index = 0;
                    readIndex = 0;
                    newBuffer = new byte[available];
                } else {
                    newBuffer = new byte[buffer.length + available];
                    System.arraycopy(buffer, index, newBuffer, 0, readIndex - index);
                }
                buffer = newBuffer;
                int read = rlpStream.read(buffer, readIndex, available);
                if(read <= 0) {
                    return false;
                }
                readIndex += read;
            }
            if(index == buffer.length) {
                return false;
            }
            rlpItem = decoder.wrap(buffer, index);
            return true;
        } catch (DecodeException e) {
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
