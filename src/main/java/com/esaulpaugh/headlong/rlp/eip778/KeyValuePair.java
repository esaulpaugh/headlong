package com.esaulpaugh.headlong.rlp.eip778;

import com.esaulpaugh.headlong.util.Strings;

import java.util.Arrays;

import static com.esaulpaugh.headlong.util.Strings.*;

public class KeyValuePair implements Comparable<KeyValuePair> {

    public static final String ID = "id";
    public static final String SECP256K1 = "secp256k1";
    public static final String IP = "ip";
    public static final String TCP = "tcp";
    public static final String UDP = "udp";

//    public static final KeyValuePair[] EMPTY_ARRAY = new KeyValuePair[0];

    final byte[] key;
    final byte[] value;

    public KeyValuePair(String key, String value, int valueEncoding) {
        this.key = decode(key, UTF_8);
        this.value = decode(value, valueEncoding);
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyValuePair that = (KeyValuePair) o;
        return Arrays.equals(key, that.key);
    }

    @Override
    public String toString() {
        return encode(key, UTF_8) + " --> " + encode(value, Strings.HEX);
    }

    @Override
    public int compareTo(KeyValuePair o) {
        int result = compare(key, o.key);
        if(result == 0) {
            throw new IllegalArgumentException("duplicate key: " + Strings.encode(o.key, UTF_8));
        }
        return result;
    }

    private static int compare(byte[] a, byte[] b) {
        if (a == b) {
            return 0;
        }
        final int len = Math.min(a.length, b.length);
        int i = 0;
        boolean mismatch = false;
        for ( ; i < len; i++) {
            if (a[i] != b[i]) {
                mismatch = true;
                break;
            }
        }
        return mismatch ? a[i] - b[i] : a.length - b.length;
    }
}
