package com.esaulpaugh.headlong.rlp;

import java.util.Arrays;

import static com.esaulpaugh.headlong.util.Strings.encode;
import static com.esaulpaugh.headlong.util.Strings.decode;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class EIP778 {

    private static final int MAX_RECORD_LEN = 300;

    public static final String ID = "id";
    public static final String SECP256K1 = "secp256k1";
    public static final String IP = "ip";
    public static final String TCP = "tcp";
    public static final String UDP = "udp";

    public static class Record {

        private final byte[] record;

        public Record(long seq, KeyValuePair[] pairs, Signer signer) {
            byte[] content = RLPEncoder.encodeEIP778RecordContent(seq, pairs);
            byte[] signature = signer.sign(RLPEncoder.encodeAsList((Object) content));
            byte[] record = RLPEncoder.encodeAsList(signature, content);

            if(record.length > MAX_RECORD_LEN) {
                throw new IllegalArgumentException("record length exceeds maximum: " + record.length + " > " + MAX_RECORD_LEN);
            }

            this.record = record;
        }

        public byte[] getRecord() {
            return Arrays.copyOf(record, record.length);
        }

        public interface Signer {
            byte[] sign(byte[] message);
        }
    }

    public static class KeyValuePair implements Comparable<KeyValuePair> {
        final byte[] key;
        final byte[] value;

        public KeyValuePair(String key, String value) {
            this.key = decode(key, UTF_8);
            this.value = decode(value, UTF_8);
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
        public String toString() {
            return encode(key, UTF_8) + " --> " + encode(value, UTF_8);
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
        public int compareTo(KeyValuePair o) {
            return compare(key, o.key);
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
}
