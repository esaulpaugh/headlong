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
import java.util.Arrays;
import java.util.Comparator;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;

/**
 * As per EIP-778.
 *
 * @see Record
 */
public final class KVP implements Comparable<KVP> {

    public static final String ID = "id";
    public static final String SECP256K1 = "secp256k1";
    public static final String IP = "ip";
    public static final String TCP = "tcp";
    public static final String UDP = "udp";
    public static final String IP6 = "ip6";
    public static final String TCP6 = "tcp6";
    public static final String UDP6 = "udp6";

    private final byte[] k;
    private final byte[] v;
    private final int keyDataIdx;
    final int length;

    public KVP(String keyUtf8, String val, int valEncoding) {
        this(keyUtf8, Strings.decode(val, valEncoding));
    }

    public KVP(String keyUtf8, byte[] rawVal) {
        this(Strings.decode(keyUtf8, Strings.UTF_8), rawVal);
    }

    public KVP(byte[] key, byte[] value) {
        this.k = RLPEncoder.encodeString(key);
        this.v = RLPEncoder.encodeString(value);
        this.keyDataIdx = key().dataIndex;
        this.length = k.length + v.length;
    }

    public KVP(RLPItem key, RLPItem value) {
        this(key.encoding(), value.encoding(), key.dataIndex);
    }

    private KVP(KVP p, byte[] value) {
        this(p.k, RLPEncoder.encodeString(value), p.keyDataIdx);
    }

    private KVP(byte[] k, byte[] v, int i) {
        this.k = k;
        this.v = v;
        this.keyDataIdx = i;
        this.length = k.length + v.length;
    }

    public KVP withValue(String val, int valEncoding) {
        return withValue(Strings.decode(val, valEncoding));
    }

    public KVP withValue(byte[] value) {
        return new KVP(this, value);
    }

    public RLPString key() {
        return RLP_STRICT.wrapString(k);
    }

    public RLPString value() {
        return RLP_STRICT.wrapString(v);
    }

    void export(ByteBuffer bb) {
        bb.put(k).put(v);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(k);
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) || (o instanceof KVP && Arrays.equals(((KVP) o).k, this.k));
    }

    @Override
    public String toString() {
        return key().asString(Strings.UTF_8) + " --> " + value().asString(Strings.HEX);
    }

    @Override
    public int compareTo(KVP other) {
        return PAIR_COMPARATOR.compare(this, other);
    }

    public static final Comparator<KVP> PAIR_COMPARATOR = (pa, pb) -> {
        byte[] a = pa.k;
        byte[] b = pb.k;
        if(a != b) {
            final int aOff = pa.keyDataIdx;
            final int bOff = pb.keyDataIdx;
            final int len = Math.min(a.length - aOff, b.length - bOff);
            int i;
            for (i = 0; i < len; i++) {
                if (a[aOff + i] != b[bOff + i]) {
                    break;
                }
            }
            int result = i < len ? a[aOff + i] - b[bOff + i] : (a.length - aOff) - (b.length - bOff);
            if (result != 0) {
                return result;
            }
        }
        throw new IllegalArgumentException("duplicate key: " + pa.key().asString(Strings.UTF_8));
    };
}
