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
        return o == this || (o instanceof KVP && Arrays.equals(((KVP) o).k, this.k));
    }

    @Override
    public String toString() {
        return key().asString(Strings.UTF_8) + " --> " + value().asString(Strings.HEX);
    }

    @Override
    public int compareTo(KVP other) {
        int result = compare(this, other);
        if (result == 0) {
            throw duplicateKeyErr();
        }
        return result;
    }

    private static int compare(KVP pa, KVP pb) {
        byte[] a = pa.k;
        byte[] b = pb.k;
        int aOff = pa.keyDataIdx;
        int bOff = pb.keyDataIdx;
        final int aLen = a.length - aOff;
        final int bLen = b.length - bOff;
        final int end = aOff + Math.min(aLen, bLen);
        while(aOff < end) {
            int av = a[aOff++];
            int bv = b[bOff++];
            if (av != bv) {
                return av - bv;
            }
        }
        return aLen - bLen;
    }

    IllegalArgumentException duplicateKeyErr() {
        return new IllegalArgumentException("duplicate key: " + key().asString(Strings.UTF_8));
    }
}
