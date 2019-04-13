package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.exception.UnrecoverableDecodeException;

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
        this.buffer = new byte[this.index = 0]; // make sure index == buffer.length
    }

    public boolean hasNext() throws IOException, UnrecoverableDecodeException {
        if (rlpItem != null) {
            return true;
        }
        final int available = rlpStream.available();
        if (available > 0) {
            int keptBytes = buffer.length - index;
            byte[] newBuffer = new byte[keptBytes + available];
            System.arraycopy(buffer, index, newBuffer, 0, keptBytes);
            buffer = newBuffer;
            index = 0;
            int read = rlpStream.read(buffer, keptBytes, available);
            if (read != available) {
                throw new IOException("read failed: " + read + " != " + available);
            }
        }
        if (index == buffer.length) {
            return false;
        }
        try {
            rlpItem = decoder.wrap(buffer, index);
            return true;
        } catch (DecodeException e) {
            if (e.isRecoverable()) {
                return false;
            }
            throw (UnrecoverableDecodeException) e;
        }
    }

    public RLPItem next() throws IOException, UnrecoverableDecodeException {
        if(hasNext()) {
            index = rlpItem.endIndex;
            RLPItem item = rlpItem;
            rlpItem = null;
            return item;
        }
        throw new NoSuchElementException();
    }
}
