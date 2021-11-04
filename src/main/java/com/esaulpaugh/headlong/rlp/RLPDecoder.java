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

import com.esaulpaugh.headlong.util.Integers;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.esaulpaugh.headlong.rlp.DataType.MIN_LONG_DATA_LEN;
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
        RLPList list = wrap(buffer, index);
        return list.iterator(this);
    }

    /**
     * Returns an {@link RLPItem} for a length-one encoding (e.g. 0xc0)
     *
     * @param lengthOneRLP the encoding
     * @return the item
     * @throws IllegalArgumentException if the byte fails to decode
     */
    public <T extends RLPItem> T wrap(byte lengthOneRLP) {
        return wrap(new byte[] { lengthOneRLP }, 0);
    }

    public <T extends RLPItem> T wrap(byte[] encoding) {
        return wrap(encoding, 0);
    }

    public <T extends RLPItem> T wrap(byte[] buffer, int index) {
        return wrap(buffer, index, buffer.length);
    }

    @SuppressWarnings("unchecked")
    <T extends RLPItem> T wrap(byte[] buffer, int index, int containerEnd) {
        byte lead = buffer[index];
        DataType type = DataType.type(lead);
        switch (type.ordinal()) {
        case ORDINAL_SINGLE_BYTE:
            return (T) new RLPString(buffer, index, index, 1, requireInBounds(index + 1L, containerEnd, buffer, index));
        case ORDINAL_STRING_SHORT:
        case ORDINAL_LIST_SHORT:
            return newShortItem(buffer, index, index + 1, lead - type.offset, containerEnd, type.isString);
        case ORDINAL_STRING_LONG:
        case ORDINAL_LIST_LONG:
            return newLongItem(buffer, index, lead - type.offset, containerEnd, type.isString);
        default: throw new AssertionError();
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends RLPItem> T newShortItem(byte[] buffer, int index, int dataIndex, int dataLength, int containerEnd, boolean isString) {
        final int endIndex = requireInBounds((long) dataIndex + dataLength, containerEnd, buffer, index);
        if(isString) {
            if(!lenient && dataLength == 1 && DataType.isSingleByte(buffer[dataIndex])) {
                throw new IllegalArgumentException("invalid rlp for single byte @ " + index);
            }
            return (T) new RLPString(buffer, index, dataIndex, dataLength, endIndex);
        }
        return (T) new RLPList(buffer, index, dataIndex, dataLength, endIndex);
    }

    @SuppressWarnings("unchecked")
    private <T extends RLPItem> T newLongItem(final byte[] buffer, final int index, final int diff, final int containerEnd, final boolean isString) {
        final int lengthIndex = index + 1;
        final int dataIndex = requireInBounds((long) lengthIndex + diff, containerEnd, buffer, index);
        final long dataLength = Integers.getLong(buffer, lengthIndex, diff, lenient);
        if(dataLength < MIN_LONG_DATA_LEN) {
            throw new IllegalArgumentException("long element data length must be " + MIN_LONG_DATA_LEN
                    + " or greater; found: " + dataLength + " for element @ " + index);
        }
        final int endIndex = requireInBounds(dataIndex + dataLength, containerEnd, buffer, index);
        return (T) (isString
                ? new RLPString(buffer, index, dataIndex, (int) dataLength, endIndex)
                : new RLPList(buffer, index, dataIndex, (int) dataLength, endIndex)
        );
    }

    private static int requireInBounds(long val, int containerEnd, byte[] buffer, int index) {
        if (val > containerEnd) {
            String msg = "element @ index " + index + " exceeds its container: " + val + " > " + containerEnd;
            throw containerEnd == buffer.length ? new ShortInputException(msg) : new IllegalArgumentException(msg);
        }
        return (int) val;
    }
}
