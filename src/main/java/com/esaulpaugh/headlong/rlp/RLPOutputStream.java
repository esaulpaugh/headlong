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

import com.esaulpaugh.headlong.util.Strings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * An {@link OutputStream} in which the data is encoded to RLP format before writing to the underlying {@link OutputStream}.
 * Each call to {@link #write(int)}, {@link #write(byte[])}, or {@link #write(byte[], int, int)} will encode one RLP item.
 * Buffered or otherwise unpredictably-sized writes to a {@link RLPOutputStream} will result in an unpredictable RLP structure.
 */
public class RLPOutputStream extends OutputStream {

    private final OutputStream out;

    public RLPOutputStream() {
        this.out = new ByteArrayOutputStream() {
            @Override
            public String toString() {
                return Strings.encode(buf, 0, count, Strings.HEX);
            }
        };
    }

    public RLPOutputStream(OutputStream out) {
        this.out = out;
    }

    public ByteArrayOutputStream getByteArrayOutputStream() {
        return (ByteArrayOutputStream) out;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    @Override
    public void write(int b) throws IOException {
        writeOut(RLPEncoder.encode((byte) b));
    }

    @Override
    public void write(byte[] b) throws IOException {
        writeOut(RLPEncoder.encode(b));
    }

    @Override
    public void write(byte[] buffer, int off, int len) throws IOException {
        writeOut(RLPEncoder.encode(Arrays.copyOfRange(buffer, off, off + len)));
    }

    public void writeAll(Object... rawObjects) throws IOException {
        writeOut(RLPEncoder.encodeSequentially(rawObjects));
    }

    public void writeAll(Iterable<?> rawObjects) throws IOException {
        writeOut(RLPEncoder.encodeSequentially(rawObjects));
    }

    public void writeList(Object... rawElements) throws IOException {
        writeOut(RLPEncoder.encodeAsList(rawElements));
    }

    public void writeList(Iterable<?> rawElements) throws IOException {
        writeOut(RLPEncoder.encodeAsList(rawElements));
    }

    private void writeOut(byte[] rlp) throws IOException {
        out.write(rlp, 0, rlp.length);
    }
}
