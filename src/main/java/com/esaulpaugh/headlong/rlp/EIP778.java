package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.util.Strings;

import java.util.Arrays;

public class EIP778 {

    private static final int MAX_RECORD_LEN = 300;

    public static final String ID = "id";
    public static final String SECP256K1 = "secp256k1";
    public static final String IP = "ip";
    public static final String TCP = "tcp";
    public static final String UDP = "udp";

    public static class Record {

        private byte[] record;

        Record(long seq, KeyValuePair[] pairs, Signer signer) {
            byte[] content = RLPEncoder.encodeEIP778RecordContent(seq, pairs);
            byte[] signature = signer.sign(RLPEncoder.encodeAsList((Object) content));
            byte[] record = RLPEncoder.encodeAsList(signature, content);

            if(record.length > MAX_RECORD_LEN) {
                throw new IllegalArgumentException("record overflow: " + record.length + " > " + MAX_RECORD_LEN);
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
            this.key = Strings.decode(key, Strings.UTF_8);
            this.value = Strings.decode(value, Strings.UTF_8);
        }

        public KeyValuePair(byte[] key, byte[] value) {
            this.key = key;
            this.value = value;
        }

        public byte[] getKey() {
            return key;
        }

        public byte[] getValue() {
            return value;
        }

        @Override
        public String toString() {
            return Strings.encode(key, Strings.UTF_8) + " --> " + Strings.encode(value, Strings.UTF_8);
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
            for (; i < len; i++) {
                if (a[i] != b[i]) {
                    mismatch = true;
                    break;
                }
            }
            if (mismatch) {
                return Byte.compare(a[i], b[i]);
            }
            return a.length - b.length;
        }
    }
}
