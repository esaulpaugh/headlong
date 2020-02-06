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

import com.esaulpaugh.headlong.exception.DecodeException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;

/** Decodes RLP-formatted data. */
public final class RLPDecoder {

    public static final RLPDecoder RLP_STRICT = new RLPDecoder(false);
    public static final RLPDecoder RLP_LENIENT = new RLPDecoder(true);

    public final boolean lenient;

    private RLPDecoder(boolean lenient) {
        this.lenient = lenient;
    }

    public Iterator<RLPItem> sequenceIterator(byte[] buffer) {
        return sequenceIterator(buffer, 0);
    }

    /**
     * Returns an iterator over the sequence of RLP items starting at {@code index}.
     *
     * @param buffer the array containing the sequence
     * @param index  the index of the sequence
     * @return an iterator over the items in the sequence
     */
    public Iterator<RLPItem> sequenceIterator(byte[] buffer, int index) {
        return new RLPStreamIterator(null, RLPDecoder.this, buffer, index) {

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
        };
    }

    public RLPStream stream(byte[] bytes) {
        return stream(new ByteArrayInputStream(bytes));
    }

    public RLPStream stream(InputStream is) {
        return new RLPStream(is, this);
    }

    public Iterator<RLPItem> listIterator(byte[] buffer) {
        return listIterator(buffer, 0);
    }

    /**
     * Returns an iterator over the elements in the RLP list item at {@code index}.
     *
     * @param buffer    the array containing the list item
     * @param index the index of the RLP list item
     * @return the iterator over the elements in the list
     * @throws DecodeException  if the RLP list failed to decode
     */
    public Iterator<RLPItem> listIterator(byte[] buffer, int index) {
        return wrapList(buffer, index).iterator(this);
    }
    
    public RLPString wrapString(byte lengthOneRlp) {
        return wrapString(new byte[] { lengthOneRlp }, 0);
    }
    
    public RLPString wrapString(byte[] encoding) {
        return wrapString(encoding, 0);
    }
    
    public RLPString wrapString(byte[] buffer, int index) {
        byte lead = buffer[index];
        DataType type = DataType.type(lead);
        switch (type) {
        case SINGLE_BYTE:
        case STRING_SHORT:
        case STRING_LONG: return new RLPString(lead, type, buffer, index, Integer.MAX_VALUE, lenient);
        default: throw new IllegalArgumentException("item is not a string");
        }
    }

    public RLPList wrapList(byte[] encoding) {
        return wrapList(encoding, 0);
    }

    public RLPList wrapList(byte[] buffer, int index) {
        byte lead = buffer[index];
        DataType type = DataType.type(lead);
        switch (type) {
        case LIST_SHORT:
        case LIST_LONG: return new RLPList(lead, type, buffer, index, Integer.MAX_VALUE, lenient);
        default: throw new IllegalArgumentException("item is not a list");
        }
    }

    /**
     * Returns an {@link RLPItem} for a length-one encoding (e.g. 0xc0)
     *
     * @param lengthOneRLP the encoding
     * @return the item
     * @throws DecodeException if the byte fails to decode
     */
    public RLPItem wrap(byte lengthOneRLP) {
        return wrap(new byte[] { lengthOneRLP }, 0);
    }

    public RLPItem wrap(byte[] encoding) {
        return wrap(encoding, 0);
    }

    public RLPItem wrap(byte[] buffer, int index) {
        return wrap(buffer, index, Integer.MAX_VALUE);
    }

    RLPItem wrap(byte[] buffer, int index, int containerEnd) {
        byte lead = buffer[index];
        DataType type = DataType.type(lead);
        switch (type) {
        case SINGLE_BYTE:
        case STRING_SHORT:
        case STRING_LONG: return new RLPString(lead, type, buffer, index, containerEnd, lenient);
        case LIST_SHORT:
        case LIST_LONG: return new RLPList(lead, type, buffer, index, containerEnd, lenient);
        default: throw new Error();
        }
    }

    /*
     *  Methods for gathering sequential items into a collection
     */

    public List<RLPItem> collectAll(byte[] encodings) {
        return collectAll(encodings, 0);
    }

    public List<RLPItem> collectAll(byte[] encodings, int index) {
        return collectBefore(encodings, index, encodings.length);
    }

    public List<RLPItem> collectBefore(byte[] encodings, int endIndex) {
        return collectBefore(encodings, 0, endIndex);
    }

    public List<RLPItem> collectBefore(byte[] encodings, int index, int endIndex) {
        ArrayList<RLPItem> dest = new ArrayList<>();
        collectBefore(encodings, index, endIndex, dest);
        return dest;
    }

    public List<RLPItem> collectN(byte[] encodings, int n) {
        return collectN(encodings, 0, n);
    }

    public List<RLPItem> collectN(byte[] encodings, int index, int n) {
        ArrayList<RLPItem> dest = new ArrayList<>(n);
        collect(encodings, index, (count, idx) -> count < n, dest);
        return dest;
    }
    // --------
    public int collectAll(byte[] encodings, int index, Collection<RLPItem> dest) {
        return collectBefore(encodings, index, encodings.length, dest);
    }

    public int collectBefore(byte[] encodings, int index, int endIndex, Collection<RLPItem> dest) {
        return collect(encodings, index, (count, idx) -> idx < endIndex, dest);
    }

    public void collectN(byte[] encodings, int index, int n, Collection<RLPItem> dest) {
        collect(encodings, index, (count, idx) -> count < n, dest);
    }
    // -------
    public int collect(byte[] encodings, int index, BiPredicate<Integer, Integer> predicate, Collection<RLPItem> collection) {
        int count = 0;
        while (predicate.test(count, index)) {
            RLPItem item = wrap(encodings, index);
            collection.add(item);
            count++;
            index = item.endIndex;
        }
        return count;
    }
}
