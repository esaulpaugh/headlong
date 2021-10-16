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

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

/** An {@link java.util.Iterator} over consecutive serialized RLP items. */
class RLPSequenceIterator implements Iterator<RLPItem> {

    final RLPDecoder decoder;
    byte[] buffer;
    int index;

    RLPItem next;

    RLPSequenceIterator(RLPDecoder decoder, byte[] buffer, int index) {
        this.decoder = decoder;
        this.buffer = buffer;
        this.index = index;
    }

    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        }
        if (index < buffer.length) {
            next = decoder.wrap(buffer, index);
            this.index = next.endIndex;
            return true;
        }
        return false;
    }

    @Override
    public RLPItem next() {
        if(hasNext()) {
            RLPItem item = next;
            next = null;
            index = item.endIndex;
            return item;
        }
        throw new NoSuchElementException();
    }

    static final class StreamRLPSequenceIterator extends RLPSequenceIterator {

        final InputStream is;

        StreamRLPSequenceIterator(InputStream is, RLPDecoder decoder) {
            super(decoder, new byte[0], 0); // make sure index == buffer.length
            this.is = is;
        }

        @Override
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            try {
                final int available = is.available();
                if (available > 0) {
                    int keptBytes = buffer.length - index;
                    byte[] newBuffer = new byte[keptBytes + available];
                    System.arraycopy(buffer, index, newBuffer, 0, keptBytes);
                    buffer = newBuffer;
                    index = 0;
                    int read = is.read(buffer, keptBytes, available);
                    if (read != available) {
                        throw new IOException("read failed: " + read + " != " + available);
                    }
                } else if (index >= buffer.length) {
                    return false;
                }
                next = decoder.wrap(buffer, index);
                return true;
            } catch (ShortInputException e) {
                return false;
            } catch (IOException io) {
                throw new RuntimeException(io);
            }
        }
    }
}
