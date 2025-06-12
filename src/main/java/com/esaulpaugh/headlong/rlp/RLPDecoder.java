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
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.concurrent.locks.LockSupport;
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
import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.NONNULL;
import static java.util.Spliterator.ORDERED;

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
     * Returns an iterator over the sequence of RLP items starting at {@code index}. Not thread-safe.
     *
     * @param buffer the array containing the sequence
     * @param index  the index of the sequence
     * @return an iterator over the items in the sequence
     */
    public Iterator<RLPItem> sequenceIterator(byte[] buffer, int index) {
        return new RLPSequenceIterator(RLPDecoder.this, buffer, index);
    }

    /**
     * Returns a non-blocking iterator which relies on {@link InputStream#available()} and buffers available bytes eagerly. Some
     * InputStreams always report zero bytes available and thus cannot be read this way. {@link Iterator#hasNext} indicates only
     * whether a complete item is immediately available. It is the responsibility of the caller to close the stream; the
     * returned iterator does not itself ever call {@link InputStream#close()}. Not thread-safe.
     * <p>
     *     For blocking iteration, use {@code sequenceIterator(Channels.newChannel(inputStream))}.
     * </p>
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
                        final int keptBytes = buffer.length - index;
                        final byte[] newBuffer = new byte[keptBytes + available];
                        System.arraycopy(buffer, index, newBuffer, 0, keptBytes);
                        buffer = newBuffer;
                        index = 0;
                        int totalRead = 0;
                        do {
                            final int read = is.read(newBuffer, keptBytes + totalRead, available - totalRead);
                            if (read <= 0) {
                                buffer = Arrays.copyOf(newBuffer, keptBytes + totalRead);
                                break;
                            }
                            totalRead += read;
                        } while (totalRead < available);
                    } else if (index >= buffer.length) {
                        return false;
                    }
                    next = decoder.wrap(buffer, index);
                    return true;
                } catch (ShortInputException e) {
                    return false;
                } catch (IOException io) {
                    throw new UncheckedIOException(io);
                }
            }
        };
    }

    public Iterator<RLPItem> sequenceIterator(ReadableByteChannel channel) {
        return sequenceIterator(channel, Strings.EMPTY_BYTE_ARRAY, 65536, 640_000L);
    }

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Returns a blocking iterator that buffers semi-lazily. {@link Iterator#hasNext()} may return false if additional bytes are
     * needed to complete the current item when {@link ReadableByteChannel#read(ByteBuffer)} returns 0 or -1 or if the channel
     * has been closed. It is the responsibility of the caller to close the channel; the returned iterator itself never
     * calls {@link java.nio.channels.Channel#close()}. Consider iterating within a virtual thread to avoid
     * blocking a platform thread. Not thread-safe.
     *
     * @param channel   input channel containing the RLP sequence data
     * @param initialBuffer  initial buffer to use; if null, a default-sized buffer is allocated
     * @param maxBufferResize   iterator throws if a partial item would exceed this length in bytes
     * @param maxDelayNanos highest delay interval before hasNext ends read retries and returns false
     * @return  an iterator over the items in the stream
     */
    public Iterator<RLPItem> sequenceIterator(final ReadableByteChannel channel, byte[] initialBuffer, final int maxBufferResize, final long maxDelayNanos) {
        return new RLPSequenceIterator(RLPDecoder.this, initialBuffer == null ? new byte[DEFAULT_BUFFER_SIZE] : initialBuffer, 0) {
            private static final long INITIAL_DELAY_NANOS = 5_000L;
            private ByteBuffer bb = ByteBuffer.wrap(buffer);
            private long delayNanos = INITIAL_DELAY_NANOS;
            private boolean channelClosed = false;

            @Override
            public boolean hasNext() {
                if (next != null) {
                    return true;
                }
                try {
                    while (true) {
                        final int capacity = bb.capacity();
                        if (index == capacity) {
                            resize(capacity > DEFAULT_BUFFER_SIZE && capacity < DEFAULT_BUFFER_SIZE * 6 ? capacity : DEFAULT_BUFFER_SIZE);
                        }
                        final int bytesRead = channelClosed || !bb.hasRemaining() ? Integer.MAX_VALUE : channel.read(bb);
                        final int end = bb.position();
                        if (index < end) {
                            try {
                                next = decoder.wrap(buffer, index, end);
                                delayNanos = INITIAL_DELAY_NANOS;
                                return true;
                            } catch (ShortInputException sie) {
                                if (!channelClosed && bytesRead > 0) {
                                    delayNanos = INITIAL_DELAY_NANOS;
                                    if (bytesRead == Integer.MAX_VALUE) {
                                        final long resize = Math.max(DEFAULT_BUFFER_SIZE, sie.encodingLen);
                                        if (resize > maxBufferResize) {
                                            throw new IOException("resize would exceed limit: " + resize + " > " + maxBufferResize);
                                        }
                                        resize((int) resize);
                                    }
                                    continue;
                                }
                            }
                        }
                        if (channelClosed || bytesRead == -1 || delayNanos > maxDelayNanos) {
                            break;
                        }
                        delayNanos = Math.min(delayNanos * 2, maxDelayNanos + 1);
                        LockSupport.parkNanos(delayNanos);
                    }
                } catch (ClosedChannelException ignored) {
                    channelClosed = true;
                    // don't immediately return false, try to parse a buffered item
                    return hasNext(); // recurse once
                } catch (IOException io) {
                    throw new UncheckedIOException(io);
                }
                return false;
            }

            private void resize(int len) {
                final int keptBytes = bb.position() - index; // pos == limit, pos == capacity
                if (len == bb.capacity()) {
                    System.arraycopy(buffer, index, buffer, 0, keptBytes);
                    bb.position(keptBytes);
                } else {
                    bb = ByteBuffer.allocate(len);
                    bb.put(buffer, index, keptBytes);
                    buffer = bb.array();
                }
                index = 0;
            }
        };
    }

    /** Iterator-based stream for single-threaded use only. */
    public static Stream<RLPItem> stream(Iterator<RLPItem> iter) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, ORDERED | NONNULL | IMMUTABLE), false);
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

    static final byte[] RLP_ZERO_BYTE = new byte[1];

    public <T extends RLPItem> T wrapBits(long bits) {
        return wrap(bits == 0L ? RLP_ZERO_BYTE : Integers.toBytes(bits), 0);
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
        return new RLPString(buffer, index, index, 1, requireInBounds(index + 1L, containerEnd, index, 0L));
    }

    private static RLPString newStringShort(byte[] buffer, int index, byte lead, int containerEnd, boolean lenient) {
        final int dataLength = lead - STRING_SHORT_OFFSET;
        final long dataIndexLong = index + 1L;
        final int endIndex = requireInBounds(dataIndexLong + dataLength, containerEnd, index, 0L); // implicitly validates dataIndexLong since dataLength is in [0,55]
        if (!lenient && dataLength == 1 && DataType.isSingleByte(buffer[(int)dataIndexLong])) {
            throw new IllegalArgumentException("invalid rlp for single byte @ " + index);
        }
        return new RLPString(buffer, index, (int)dataIndexLong, dataLength, endIndex);
    }

    private static RLPList newListShort(byte[] buffer, int index, byte lead, int containerEnd) {
        final int dataLength = lead - LIST_SHORT_OFFSET;
        final long dataIndex = index + 1L;
        return new RLPList(buffer, index, (int)dataIndex, dataLength, requireInBounds(dataIndex + dataLength, containerEnd, index, 0L));
    }

    @SuppressWarnings("unchecked")
    private static <T extends RLPItem> T newLongItem(byte lead, byte offset, boolean isString, byte[] buffer, int index, int containerEnd, boolean lenient) {
        final int lengthOfLength = lead - offset;
        final long dataIndexLong = index + 1L + lengthOfLength;
        requireInBounds(dataIndexLong, containerEnd, index, dataIndexLong + MIN_LONG_DATA_LEN);
        final long dataLengthLong = Integers.getLong(buffer, index + 1, lengthOfLength, lenient);
        if (dataLengthLong < MIN_LONG_DATA_LEN) {
            throw new IllegalArgumentException("long element data length must be " + MIN_LONG_DATA_LEN
                    + " or greater; found: " + dataLengthLong + " for element @ " + index);
        }
        if (dataLengthLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("length is too great: " + dataLengthLong);
        }
        final int endIndex = requireInBounds(dataIndexLong + dataLengthLong, containerEnd, index, dataIndexLong + dataLengthLong - index);
        return (T) (isString
                ? new RLPString(buffer, index, (int) dataIndexLong, (int) dataLengthLong, endIndex)
                : new RLPList(buffer, index, (int) dataIndexLong, (int) dataLengthLong, endIndex)
        );
    }

    private static int requireInBounds(long val, int containerEnd, int index, long encodingLen) {
        if (val > containerEnd) {
            throw new ShortInputException("element @ index " + index + " exceeds its container: " + val + " > " + containerEnd, encodingLen);
        }
        return (int) val;
    }
}