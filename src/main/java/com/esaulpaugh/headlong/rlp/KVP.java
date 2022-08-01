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

import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;

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

    final byte[] rlp;
    final RLPString key;

    public KVP(String keyUtf8, String val, int valEncoding) {
        this(keyUtf8, Strings.decode(val, valEncoding));
    }

    public KVP(String keyUtf8, byte[] rawVal) {
        this(Strings.decode(keyUtf8, Strings.UTF_8), rawVal);
    }

    public KVP(byte[] key, byte[] value) {
        this.rlp = RLPEncoder.sequence(key, value);
        this.key = RLP_STRICT.wrapString(rlp);
    }

    public KVP(RLPString key, RLPString value) {
        final int keyLen = key.encodingLength();
        this.rlp = new byte[keyLen + value.encodingLength()];
        key.copy(rlp, 0);
        value.copy(rlp, keyLen);
        this.key = RLP_STRICT.wrapString(rlp);
    }

    public KVP withValue(String val, int valEncoding) {
        return withValue(Strings.decode(val, valEncoding));
    }

    public KVP withValue(byte[] value) {
        return new KVP(key, RLP_STRICT.wrapString(RLPEncoder.encodeString(value)));
    }

    public RLPString key() {
        return key;
    }

    public RLPString value() {
        return RLP_STRICT.wrapString(rlp, key.endIndex);
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
        return key.asString(Strings.UTF_8) + " --> " + value().asString(Strings.HEX);
    }

    @Override
    public int compareTo(KVP other) {
        int result = this.key.compareTo(other.key);
        if (result == 0) {
            throw duplicateKeyErr(key);
        }
        return result;
    }

    static IllegalArgumentException duplicateKeyErr(RLPString key) {
        return new IllegalArgumentException("duplicate key: " + key.asString(Strings.UTF_8));
    }
}
