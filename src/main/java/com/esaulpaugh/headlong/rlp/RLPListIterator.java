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

import java.util.NoSuchElementException;

public class RLPListIterator {

    private final RLPList list;
    private final RLPDecoder decoder;

    private int nextElementIndex;

    public RLPListIterator(RLPList list, RLPDecoder decoder) {
        this.list = list;
        this.decoder = decoder;
        this.nextElementIndex = list.dataIndex;
    }

    public boolean hasNext() {
        return this.nextElementIndex < list.endIndex;
    }

    public RLPItem next() throws DecodeException {
        if (hasNext()) {
            RLPItem element = decoder.wrap(list.buffer, this.nextElementIndex, list.endIndex);
            this.nextElementIndex = element.endIndex;
            return element;
        }
        throw new NoSuchElementException();
    }
}
