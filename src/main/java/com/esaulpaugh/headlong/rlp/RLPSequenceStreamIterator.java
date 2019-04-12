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

    // for test only
    byte[] buffer() {
        return buffer;
    }

    public boolean hasNext() throws IOException, UnrecoverableDecodeException {
        if(rlpItem != null) {
            return true;
        }
        try {
            final int available = rlpStream.available();
            if(available > 0) {
                updateBuffer(available);
                int read = rlpStream.read(buffer, readIndex, available);
                if(read != available) {
                    throw new IOException("read failed: " + read + " != " + available);
                }
                readIndex += read;
            }
            if(index == buffer.length) {
                return false;
            }
            rlpItem = decoder.wrap(buffer, index);
            return true;
        } catch (DecodeException e) {
            if(e.isRecoverable()) {
                return false;
            }
            throw (UnrecoverableDecodeException) e;
        }
    }

    private void updateBuffer(int available) {
        int keptBytes = readIndex - index;
        byte[] newBuffer = new byte[keptBytes + available];
        System.arraycopy(buffer, index, newBuffer, 0, keptBytes);
        int droppedBytes = buffer.length - keptBytes;
        index -= droppedBytes;
        readIndex -= droppedBytes;
        buffer = newBuffer;
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
