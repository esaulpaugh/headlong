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

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * For iterating over sequentially encoded RLP items.
 */
class RLPIterator implements Iterator<RLPItem> {

    protected final RLPDecoder decoder;
    protected byte[] buffer;
    protected int index;
    protected final int end;

    protected RLPItem next;

    RLPIterator(RLPDecoder decoder, byte[] buffer, int start, int end) {
        this.decoder = decoder;
        this.buffer = buffer;
        this.index = start;
        this.end = end;
    }

    @Override
    public boolean hasNext() {
        if(next != null) {
            return true;
        }
        if(index >= end) {
            return false;
        }
        try {
            next = decoder.wrap(buffer, index);
            this.index = next.endIndex;
            return true;
        } catch (DecodeException de) {
            throw noSuchElementException(de);
        }
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

    static NoSuchElementException noSuchElementException(Throwable cause) {
        return (NoSuchElementException) new NoSuchElementException().initCause(cause);
    }
}
