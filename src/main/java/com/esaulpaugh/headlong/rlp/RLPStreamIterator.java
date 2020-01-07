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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class RLPStreamIterator implements Iterator<RLPItem>, AutoCloseable {

    private final RLPDecoder decoder;
    private final InputStream rlpStream;

    private byte[] buffer;
    private int index;

    private RLPItem rlpItem;

    RLPStreamIterator(RLPDecoder decoder, InputStream rlpStream) {
        this.decoder = decoder;
        this.rlpStream = rlpStream;
        this.buffer = new byte[this.index = 0]; // make sure index == buffer.length
    }

    @Override
    public boolean hasNext() {
        if (rlpItem != null) {
            return true;
        }
        try {
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
        } catch (IOException io) {
            throw RLPIterator.noSuchElementException(io);
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
            throw RLPIterator.noSuchElementException(e);
        }
    }

    @Override
    public RLPItem next() {
        if(hasNext()) {
            try {
                index = rlpItem.endIndex;
                RLPItem item = rlpItem;
                rlpItem = null;
                return item;
            } catch (NullPointerException npe) {
                throw RLPIterator.noSuchElementException(npe);
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    public void close() throws IOException {
        rlpStream.close();
    }
}
