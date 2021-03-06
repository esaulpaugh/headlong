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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/** An incoming stream of RLP-encoded data. */
public final class RLPStream implements Iterable<RLPItem>, AutoCloseable {

    private final InputStream is;
    private final RLPDecoder decoder;

    public RLPStream(InputStream is) {
        this(is, RLPDecoder.RLP_STRICT);
    }

    public RLPStream(InputStream is, RLPDecoder decoder) {
        this.is = is;
        this.decoder = decoder;
    }

    public List<RLPItem> collect() {
        return collect(new ArrayList<>());
    }

    public <T extends Collection<RLPItem>> T collect(T collection) {
        for (RLPItem item : this) {
            collection.add(item);
        }
        return collection;
    }

    @Override
    public Iterator<RLPItem> iterator() {
        return new RLPStreamIterator(is, decoder);
    }

    @Override
    public void close() throws IOException {
        is.close();
    }
}
