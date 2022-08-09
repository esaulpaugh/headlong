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

/** Extends {@link RLPItem}. Created by Evo on 1/19/2017. */
public final class RLPString extends RLPItem {

    RLPString(byte[] buffer, int index, int dataIndex, int dataLength, int endIndex) {
        super(buffer, index, dataIndex, dataLength, endIndex);
    }

    @Override
    public boolean isString() {
        return true;
    }

    @Override
    public RLPString asRLPString() {
        return this;
    }

    /** @see RLPItem#duplicate() */
    @Override
    public RLPString duplicate() {
        final byte[] enc = encoding();
        return new RLPString(enc, 0, enc.length - dataLength, dataLength, enc.length);
    }
}
