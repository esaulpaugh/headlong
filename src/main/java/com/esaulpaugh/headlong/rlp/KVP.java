/*
   Copyright 2019-2026 Evan Saulpaugh

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

import java.nio.ByteBuffer;
import java.util.Iterator;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.ASCII;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

/**
 * A key-value pair as per EIP-778.
 *
 * @see Record
 */
public final class KVP implements Comparable<KVP> {

    public static final KVP[] EMPTY_ARRAY = new KVP[0];

    public static final String ID = "id";
    public static final String SECP256K1 = "secp256k1";
    public static final String IP = "ip";
    public static final String TCP = "tcp";
    public static final String UDP = "udp";
    public static final String IP6 = "ip6";
    public static final String TCP6 = "tcp6";
    public static final String UDP6 = "udp6";
    public static final String CLIENT = "client";

    final byte[] rlp;
    final RLPString key;

    public KVP(String keyUtf8, String val, int valEncoding) {
        this(keyUtf8, Strings.decode(val, valEncoding));
    }

    public KVP(String keyUtf8, byte[] strBytes) {
        this(RLPEncoder.sequence(Strings.decode(keyUtf8, UTF_8), strBytes));
    }

    public KVP(RLPString key, RLPItem value) {
        final int keyLen = key.encodingLength();
        this.rlp = new byte[keyLen + value.encodingLength()];
        key.copy(rlp, 0);
        value.copy(rlp, keyLen);
        this.key = RLP_STRICT.wrapString(rlp);
    }

    private KVP(byte[] rlp) {
        this.rlp = rlp;
        this.key = RLP_STRICT.wrapString(rlp);
    }

    public KVP withValue(String val, int valEncoding) {
        return withValue(Strings.decode(val, valEncoding));
    }

    public KVP withValue(byte[] value) {
//         return new KVP(key, RLP_STRICT.wrapString(RLPEncoder.string(value)));
        final int vlen = RLPEncoder.stringEncodedLen(value);
        final byte[] out = new byte[key.endIndex + vlen];
        System.arraycopy(rlp, 0, out, 0, key.endIndex);
        RLPEncoder.putString(value, ByteBuffer.wrap(out, key.endIndex, vlen));
        return new KVP(out);
    }

    public RLPString key() {
        return key;
    }

    public RLPItem value() {
        return RLP_STRICT.wrap(rlp, key.endIndex);
    }

    void export(ByteBuffer bb) {
        bb.put(rlp);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof KVP && key.equals(((KVP) o).key);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(key.asString(UTF_8)).append(" --> ");
        final RLPItem value = value();
        if (value.isString()) {
            return sb.append(value.asString(HEX)).toString();
        }
        sb.append('[');
        final Iterator<RLPItem> iter = value.asRLPList().iterator();
        if (iter.hasNext()) {
            for ( ; true; sb.append(", ")) {
                sb.append('"').append(iter.next().asString(ASCII)).append('"');
                if (!iter.hasNext()) {
                    break;
                }
            }
        }
        return sb.append(']').toString();
    }

    @Override
    public int compareTo(KVP other) {
        return this.key.compareTo(other.key);
    }
}
