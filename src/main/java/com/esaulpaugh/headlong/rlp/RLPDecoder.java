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
import com.esaulpaugh.headlong.util.Strings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.esaulpaugh.headlong.rlp.DataType.LIST_SHORT_OFFSET;
import static com.esaulpaugh.headlong.rlp.DataType.MIN_LONG_DATA_LEN;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_LIST_LONG;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_LIST_SHORT;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_SINGLE_BYTE;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_STRING_LONG;
import static com.esaulpaugh.headlong.rlp.DataType.ORDINAL_STRING_SHORT;
import static com.esaulpaugh.headlong.rlp.DataType.STRING_SHORT_OFFSET;

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
    public Iterator<RLPItem> sequenceIterator(final InputStream is) {
        return new RLPSequenceIterator(RLPDecoder.this, Strings.EMPTY_BYTE_ARRAY, 0) { // make sure index == buffer.length
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
        };
    }

    public static Stream<RLPItem> stream(Iterator<RLPItem> iter) {
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

    public <T extends RLPItem> T wrapBits(long bits) {
        return wrap(RLPEncoder.bitsToBytes(bits), 0);
    }

    public RLPString wrapString(byte[] buffer) {
        return wrapString(buffer, 0);
    }

    public RLPList wrapList(byte[] buffer) {
        return wrapList(buffer, 0);
    }

    public RLPItem wrapItem(byte[] buffer) {
        return wrapItem(buffer, 0);
    }

    public RLPString wrapString(byte[] buffer, int index) {
        return wrap(buffer, index);
    }

    public RLPList wrapList(byte[] buffer, int index) {
        return wrap(buffer, index);
    }

    public RLPItem wrapItem(byte[] buffer, int index) {
        return wrap(buffer, index);
    }

    public <T extends RLPItem> T wrap(byte[] buffer) {
        return wrap(buffer, 0);
    }

    public <T extends RLPItem> T wrap(byte[] buffer, int index) {
        return wrap(buffer, index, buffer.length);
    }

    @SuppressWarnings("unchecked")
    <T extends RLPItem> T wrap(byte[] buffer, int index, int containerEnd) {
        byte lead = buffer[index];
        DataType type = DataType.type(lead);
        switch (type.ordinal()) {
        case ORDINAL_SINGLE_BYTE: return (T) newSingleByte(buffer, index, containerEnd);
        case ORDINAL_STRING_SHORT: return (T) newStringShort(buffer, index, lead, containerEnd, lenient);
        case ORDINAL_LIST_SHORT: return (T) newListShort(buffer, index, lead, containerEnd);
        case ORDINAL_STRING_LONG:
        case ORDINAL_LIST_LONG: return newLongItem(lead, type.offset, type.isString, buffer, index, containerEnd, lenient);
        default: throw new AssertionError();
        }
    }

    private static RLPString newSingleByte(byte[] buffer, int index, int containerEnd) {
        final int endIndex = requireInBounds(index + 1L, containerEnd, buffer, index);
        return new RLPString(buffer, index, index, 1, endIndex);
    }

    private static RLPString newStringShort(byte[] buffer, int index, byte lead, int containerEnd, boolean lenient) {
        final int dataIndex = index + 1;
        final int dataLength = lead - STRING_SHORT_OFFSET;
        final int endIndex = requireInBounds((long) dataIndex + dataLength, containerEnd, buffer, index);
        if (!lenient && dataLength == 1 && DataType.isSingleByte(buffer[dataIndex])) {
            throw new IllegalArgumentException("invalid rlp for single byte @ " + index);
        }
        return new RLPString(buffer, index, dataIndex, dataLength, endIndex);
    }

    private static RLPList newListShort(byte[] buffer, int index, byte lead, int containerEnd) {
        final int dataIndex = index + 1;
        final int dataLength = lead - LIST_SHORT_OFFSET;
        final int endIndex = requireInBounds((long) dataIndex + dataLength, containerEnd, buffer, index);
        return new RLPList(buffer, index, dataIndex, dataLength, endIndex);
    }

    @SuppressWarnings("unchecked")
    private static <T extends RLPItem> T newLongItem(byte lead, byte offset, boolean isString, byte[] buffer, int index, int containerEnd, boolean lenient) {
        final int diff = lead - offset;
        final int lengthIndex = index + 1;
        final int dataIndex = requireInBounds((long) lengthIndex + diff, containerEnd, buffer, index);
        final long dataLength = Integers.getLong(buffer, lengthIndex, diff, lenient);
        if (dataLength < MIN_LONG_DATA_LEN) {
            throw new IllegalArgumentException("long element data length must be " + MIN_LONG_DATA_LEN
                    + " or greater; found: " + dataLength + " for element @ " + index);
        }
        final int dataLen = requireInBounds(dataLength, containerEnd, buffer, index);
        final int endIndex = requireInBounds(dataIndex + dataLength, containerEnd, buffer, index);
        return (T) (isString
                ? new RLPString(buffer, index, dataIndex, dataLen, endIndex)
                : new RLPList(buffer, index, dataIndex, dataLen, endIndex)
        );
    }

    private static int requireInBounds(long val, int containerEnd, byte[] buffer, int index) {
        if (val > containerEnd) {
            String msg = "element @ index " + index + " exceeds its container: " + val + " > " + containerEnd;
            throw buffer.length == containerEnd ? new ShortInputException(msg) : new IllegalArgumentException(msg);
        }
        return (int) val;
    }
}