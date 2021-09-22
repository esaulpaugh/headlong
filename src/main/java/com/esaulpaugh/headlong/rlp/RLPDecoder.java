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

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_LIST_LONG;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_LIST_SHORT;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_SINGLE_BYTE;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_STRING_LONG;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_STRING_SHORT;

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
        return new RLPSequenceIterator(RLPDecoder.this, buffer, index);
    }

    /**
     * Returns an iterator over the sequence of RLPItems in the given {@link InputStream}.
     *
     * @param is    the stream of RLP data
     * @return  an iterator over the items in the stream
     */
    public Iterator<RLPItem> sequenceIterator(InputStream is) {
        return new RLPSequenceIterator.StreamRLPSequenceIterator(is, RLPDecoder.this);
    }

    public Stream<RLPItem> stream(byte[] bytes) {
        return stream(sequenceIterator(bytes));
    }

    public Stream<RLPItem> stream(byte[] buffer, int index) {
        return stream(sequenceIterator(buffer, index));
    }

    public Stream<RLPItem> stream(InputStream is) {
        return stream(sequenceIterator(is));
    }

    private static Stream<RLPItem> stream(Iterator<RLPItem> iter) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED), false);
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
     * @throws IllegalArgumentException  if the RLP list failed to decode
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
        switch (type.ordinal()) {
        case ORDINAL_SINGLE_BYTE:
        case ORDINAL_STRING_SHORT:
        case ORDINAL_STRING_LONG: return new RLPString(lead, type, buffer, index, Integer.MAX_VALUE, lenient);
        default: throw new IllegalArgumentException("item is not a string");
        }
    }

    public RLPList wrapList(byte[] encoding) {
        return wrapList(encoding, 0);
    }

    public RLPList wrapList(byte[] buffer, int index) {
        byte lead = buffer[index];
        DataType type = DataType.type(lead);
        switch (type.ordinal()) {
        case ORDINAL_LIST_SHORT:
        case ORDINAL_LIST_LONG: return new RLPList(lead, type, buffer, index, Integer.MAX_VALUE, lenient);
        default: throw new IllegalArgumentException("item is not a list");
        }
    }

    /**
     * Returns an {@link RLPItem} for a length-one encoding (e.g. 0xc0)
     *
     * @param lengthOneRLP the encoding
     * @return the item
     * @throws IllegalArgumentException if the byte fails to decode
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
        switch (type.ordinal()) {
        case ORDINAL_SINGLE_BYTE:
        case ORDINAL_STRING_SHORT:
        case ORDINAL_STRING_LONG: return new RLPString(lead, type, buffer, index, containerEnd, lenient);
        case ORDINAL_LIST_SHORT:
        case ORDINAL_LIST_LONG: return new RLPList(lead, type, buffer, index, containerEnd, lenient);
        default: throw new AssertionError();
        }
    }
    
    @FunctionalInterface
    public interface BiIntPredicate {
        boolean test(int count, int index);
    }

    /**
     * For gathering sequential items into a collection.
     *
     * @param buffer the buffer containing the encodings
     * @param index the index into the buffer of the first encoding
     * @param predicate the condition under which an item is to be added to the collection
     * @param collection    the collection to which the items will be added
     * @return  the number of items added
     */
    public int collect(byte[] buffer, int index, BiIntPredicate predicate, Collection<? super RLPItem> collection) {
        int count = 0;
        while (predicate.test(count, index)) {
            RLPItem item = wrap(buffer, index);
            collection.add(item);
            count++;
            index = item.endIndex;
        }
        return count;
    }
}
