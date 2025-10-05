/*
   Copyright 2020 Evan Saulpaugh

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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * An {@link OutputStream} that encodes data to RLP format before writing to the underlying {@link OutputStream}.
 * Each call to {@link #write(int)}, {@link #write(byte[])}, or {@link #write(byte[], int, int)} will write one RLP string item.
 * Buffered or otherwise unpredictably-sized writes to an {@link RLPOutputStream} will result in an unpredictable RLP structure.
 * Not thread-safe.
 */
public final class RLPOutputStream extends OutputStream {

    private static final int MIN_BUFFER_LEN = 128;
    private static final int MAX_BUFFER_LEN = 131_072;

    private final OutputStream out;
    private final byte[] internalBuf;
    private final ByteBuffer bb;
    private final int bufferedItemLimit;

    public RLPOutputStream(OutputStream out) {
        this(out, 1_280);
    }

    /**
     * Creates an RLPOutputStream with a custom buffer size.
     *
     * @param out   the OutputStream to wrap
     * @param bufferLen the size of this instance's internal buffer
     */
    public RLPOutputStream(OutputStream out, int bufferLen) {
        if (bufferLen < MIN_BUFFER_LEN) {
            throw new IllegalArgumentException("bufferLen too small: " + bufferLen + " < " + MIN_BUFFER_LEN);
        }
        if (bufferLen > MAX_BUFFER_LEN) {
            throw new IllegalArgumentException("bufferLen too large: " + bufferLen + " > " + MAX_BUFFER_LEN);
        }
        this.out = Objects.requireNonNull(out);
        this.internalBuf = new byte[bufferLen];
        this.bb = ByteBuffer.wrap(internalBuf);
        this.bufferedItemLimit = bufferLen - (1 + Long.BYTES); // allow room for RLP item header
    }

    /**
     * Writes the RLP encoding of the given integer to the underlying {@link OutputStream}.
     * <p>
     * NOTE: This violates the general method contract in that it may write <em>multiple</em> bytes to the underlying stream,
     * depending on the integer being encoded.
     *
     * @param b the integer to write as RLP bytes
     */
    @Override
    public void write(int b) throws IOException {
        if (b >= 0 && b < 128) {
            out.write(b);
        } else {
            write(Integers.toBytes(b));
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] buffer, int offset, int len) throws IOException {
        if (len <= bufferedItemLimit) {
            bb.rewind();
            RLPEncoder.putString(buffer, offset, len, bb);
            out.write(internalBuf, 0, bb.position());
        } else {
            final ByteBuffer temp = ByteBuffer.allocate(RLPEncoder.itemLen(len));
            RLPEncoder.putString(buffer, offset, len, temp);
            out.write(temp.array(), 0, temp.position()); // itemLen(len) could be oversized by 1 for single-byte items, so use position()
        }
    }

    public void writeSequence(Object... rawObjects) throws IOException {
        writeSequence(Arrays.asList(rawObjects));
    }

    public void writeSequence(Iterable<?> rawObjects) throws IOException {
        final int encodedLen = RLPEncoder.sumEncodedLen(rawObjects);
        if (encodedLen <= bufferedItemLimit) {
            bb.rewind();
            RLPEncoder.putSequence(rawObjects, bb);
            out.write(internalBuf, 0, bb.position());
        } else {
            writeOut(RLPEncoder.sequence(rawObjects));
        }
    }

    public void writeList(Object... rawElements) throws IOException {
        writeList(Arrays.asList(rawElements));
    }

    public void writeList(Iterable<?> rawElements) throws IOException {
        final int dataLen = RLPEncoder.sumEncodedLen(rawElements);
        if (dataLen <= bufferedItemLimit) {
            bb.rewind();
            RLPEncoder.encodeList(dataLen, rawElements, bb);
            out.write(internalBuf, 0, bb.position());
        } else {
            writeOut(RLPEncoder.list(rawElements));
        }
    }

    private void writeOut(byte[] rlp) throws IOException {
        out.write(rlp, 0, rlp.length);
    }

    @Override
    public String toString() {
        return out.toString();
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }
}
