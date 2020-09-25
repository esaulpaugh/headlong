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

import java.util.Arrays;
import java.util.Comparator;

import static com.esaulpaugh.headlong.util.Strings.UTF_8;

/**
 * As per EIP-778.
 *
 * @see Record
 */
public final class KeyValuePair implements Comparable<KeyValuePair> {

    public static final Comparator<KeyValuePair> PAIR_COMPARATOR = (pa, pb) -> {
        byte[] a = pa.key;
        byte[] b = pb.key;
        if(a != b) {
            int i = 0;
            final int len = Math.min(a.length, b.length);
            boolean mismatch = false;
            for ( ; i < len; i++) {
                if (a[i] != b[i]) {
                    mismatch = true;
                    break;
                }
            }
            int result = mismatch ? a[i] - b[i] : a.length - b.length;
            if(result != 0) {
                return result;
            }
        }
        throw new IllegalArgumentException("duplicate key: " + Strings.encode(a, UTF_8));
    };

    public static final String ID = "id";
    public static final String SECP256K1 = "secp256k1";
    public static final String IP = "ip";
    public static final String TCP = "tcp";
    public static final String UDP = "udp";
    public static final String IP6 = "ip6";
    public static final String TCP6 = "tcp6";
    public static final String UDP6 = "udp6";

    private final byte[] key;
    private final byte[] value;

    public KeyValuePair(String key, String value, int valueEncoding) {
        this.key = Strings.decode(key, UTF_8);
        this.value = Strings.decode(value, valueEncoding);
    }

    public KeyValuePair(byte[] key, byte[] value) {
        this.key = Arrays.copyOf(key, key.length);
        this.value = Arrays.copyOf(value, value.length);
    }

    public byte[] getKey() {
        return Arrays.copyOf(key, key.length);
    }

    public byte[] getValue() {
        return Arrays.copyOf(value, value.length);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) || (o instanceof KeyValuePair && Arrays.equals(((KeyValuePair) o).key, this.key));
    }

    @Override
    public String toString() {
        return Strings.encode(key, UTF_8) + " --> " + Strings.encode(value, Strings.HEX);
    }

    @Override
    public int compareTo(KeyValuePair other) {
        return PAIR_COMPARATOR.compare(this, other);
    }
}
