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

import com.esaulpaugh.headlong.util.Strings;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * An {@link OutputStream} in which the data is encoded to RLP format before writing to the underlying {@link OutputStream}.
 */
public class RLPOutputStream extends OutputStream {

    private final OutputStream os;

    public RLPOutputStream() {
        this.os = new ByteArrayOutputStream() {
            @Override
            public String toString() {
                return Strings.encode(buf, 0, count, Strings.HEX);
            }
        };
    }

    public RLPOutputStream(OutputStream os) {
        this.os = os;
    }

    public ByteArrayOutputStream getByteArrayOutputStream() {
        return (ByteArrayOutputStream) os;
    }

    public OutputStream getOutputStream() {
        return os;
    }

    @Override
    public void write(int b) throws IOException {
        write(RLPEncoder.encode((byte) b));
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] buffer, int off, int len) throws IOException {
        writeInternal(RLPEncoder.encode(Arrays.copyOfRange(buffer, off, off + len)));
    }

    public void writeAll(Object... rawObjects) throws IOException {
        writeInternal(RLPEncoder.encodeSequentially(rawObjects));
    }

    public void writeAll(Iterable<Object> rawObjects) throws IOException {
        writeInternal(RLPEncoder.encodeSequentially(rawObjects));
    }

    public void writeList(Object... rawElements) throws IOException {
        writeInternal(RLPEncoder.encodeAsList(rawElements));
    }

    public void writeList(Iterable<Object> rawElements) throws IOException {
        writeInternal(RLPEncoder.encodeAsList(rawElements));
    }

    private void writeInternal(byte[] rlp) throws IOException {
        os.write(rlp, 0, rlp.length);
    }
}
