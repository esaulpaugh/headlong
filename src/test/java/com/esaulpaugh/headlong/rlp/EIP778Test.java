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

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.util.FastHex;
import org.junit.jupiter.api.Test;

import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.rlp.KeyValuePair.ID;
import static com.esaulpaugh.headlong.rlp.KeyValuePair.IP;
import static com.esaulpaugh.headlong.rlp.KeyValuePair.PAIR_COMPARATOR;
import static com.esaulpaugh.headlong.rlp.KeyValuePair.SECP256K1;
import static com.esaulpaugh.headlong.rlp.KeyValuePair.UDP;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EIP778Test {

    private static final String ENR_STRING = "enr:-IS4QHCYrYZbAKWCBRlAy5zzaDZXJBGkcnh4MHcBFZntXNFrdvJjX04jRzjzCBOonrkTfj499SZuOh8R33Ls8RRcy5wBgmlkgnY0gmlwhH8AAAGJc2VjcDI1NmsxoQPKY0yuDUmstAHYpMa2_oxVtw0RW_QAdpzBQA8yWM0xOIN1ZHCCdl8";

    static final byte[] SIG = FastHex.decode(
            "7098ad865b00a582051940cb9cf36836572411a47278783077011599ed5cd16b"
          + "76f2635f4e234738f30813a89eb9137e3e3df5266e3a1f11df72ecf1145ccb9c"
    );

    private static final Record.Signer SIGNER = new Record.Signer() {

        @Override
        public int signatureLength() {
            return SIG.length;
        }

        @Override
        public byte[] sign(byte[] content) {
            return SIG;
        }
    };

    private static final Record.Verifier VERIFIER = (s,c) -> {
        if(!Arrays.equals(s, SIG)) throw new SignatureException();
    };

    private static final Record VECTOR;

    static {
        try {
            VECTOR = Record.parse(ENR_STRING, VERIFIER);
        } catch (SignatureException se) {
            throw new RuntimeException(se);
        }
    }

    private static final byte[] MAX_LEN_LIST = new byte[] {
            (byte) 0xf9, (byte) 1, (byte) 41,
            (byte) 55,
            (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, (byte) 0xFF, (byte) 0x81, (byte) 0x90, (byte) 0x81, (byte) 0xb6, (byte) '\u230A',
            (byte) 0xb8, 56, 0x09,(byte)0x80,-1,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, -3, -2, 0, 0,
            (byte) 0xf8, 0x38, 0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 36, 74, 0, 0,
            (byte) 0x84, 'c', 'a', 't', 's',
            (byte) 0x84, 'd', 'o', 'g', 's',
            (byte) 0xca, (byte) 0x84, 92, '\r', '\n', '\f', (byte) 0x84, '\u0009', 'o', 'g', 's',
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,

            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0
    };

    @Test
    public void testParseErrs() throws Throwable {
        assertThrown(IllegalArgumentException.class, "unconsumed trailing bytes", () -> Record.parse(ENR_STRING + "A", VERIFIER));
        assertEquals(300, MAX_LEN_LIST.length);
        assertThrown(SignatureException.class, () -> Record.decode(MAX_LEN_LIST, VERIFIER));
        byte[] maxLenPlusOne = Arrays.copyOf(MAX_LEN_LIST, MAX_LEN_LIST.length + 1);
        maxLenPlusOne[2]++; // increment len in RLP prefix
        assertThrown(IllegalArgumentException.class, "record length exceeds maximum: 301 > 300", () -> Record.decode(maxLenPlusOne, VERIFIER));
    }

    @Test
    public void testErrs() throws Throwable {
        final long seq = -TestUtils.seededRandom().nextInt(Integer.MAX_VALUE);
        assertThrown(
                IllegalArgumentException.class,
                "negative seq",
                () -> new Record(seq, new ArrayList<>(), new Record.Signer() {
                    @Override
                    public int signatureLength() {
                        return 0;
                    }

                    @Override
                    public byte[] sign(byte[] content) {
                        return null;
                    }
                })
        );
        assertThrown(
                RuntimeException.class,
                "signer specifies negative signature length",
                () -> new Record(0x07, new ArrayList<>(), new Record.Signer() {
                    @Override
                    public int signatureLength() {
                        return -1;
                    }

                    @Override
                    public byte[] sign(byte[] content) {
                        return null;
                    }
                })
        );
        assertThrown(
                NullPointerException.class,
                () -> new Record(0L, new ArrayList<>(), new Record.Signer() {
                    @Override
                    public int signatureLength() {
                        return 0;
                    }

                    @Override
                    public byte[] sign(byte[] content) {
                        return null;
                    }
                })
        );
    }

    @Test
    public void testEip778() throws SignatureException {
        final long seq = 1L;
        final List<KeyValuePair> pairs = Arrays.asList(
                new KeyValuePair(IP, "7f000001", HEX),
                new KeyValuePair(UDP, "765f", HEX),
                new KeyValuePair(ID, "v4", UTF_8),
                new KeyValuePair(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX)
        );
        final KeyValuePair[] array = pairs.toArray(new KeyValuePair[0]);

        final Record record = new Record(seq, pairs, SIGNER);

        assertEquals(VECTOR.getSignature(), record.getSignature());
        assertEquals(VECTOR.getContent(), record.getContent());
        assertEquals(VECTOR.getSeq(), record.getSeq());
        assertEquals(VECTOR.getRLP(), record.getRLP());
        assertEquals(VECTOR.toString(), record.toString());
        assertEquals(VECTOR, record);

        RLPList content = record.getContent();
        System.out.println("verified = " + content);
        Iterator<RLPItem> iter = content.iterator(RLPDecoder.RLP_STRICT);

        assertEquals(seq, record.getSeq());
        assertEquals(seq, iter.next().asLong());

        Arrays.sort(array);
        int i = 0;
        while (iter.hasNext()) {
            assertEquals(array[i++], new KeyValuePair(iter.next().asBytes(), iter.next().asBytes()));
        }
        assertEquals(ENR_STRING, record.toString());

        assertEquals(record, Record.parse(record.toString(), VERIFIER));
    }

    @Test
    public void testZeroLenSig() {
        final long seq = 1L;
        final List<KeyValuePair> pairs = Arrays.asList(
                new KeyValuePair(IP, "7f000001", HEX),
                new KeyValuePair(UDP, "765f", HEX),
                new KeyValuePair(ID, "v4", UTF_8),
                new KeyValuePair(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX)
        );
        final Record record = new Record(seq, pairs, new Record.Signer() {
            @Override
            public int signatureLength() {
                return 0;
            }

            @Override
            public byte[] sign(byte[] content) {
                return new byte[0];
            }
        });
        System.out.println(record.getSignature());
        for(RLPItem it : record.getContent()) {
            System.out.println(it);
        }
    }

    @Test
    public void testIncorrectSignatureLength() throws Throwable {
        final List<KeyValuePair> pairs = Arrays.asList(
                new KeyValuePair(IP, "7f000001", HEX),
                new KeyValuePair(UDP, "765f", HEX),
                new KeyValuePair(ID, "v4", UTF_8),
                new KeyValuePair(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX)
        );
        assertThrown(RuntimeException.class, "incorrect signature length: 32 != 64", () -> new Record(90L, pairs, new Record.Signer() {
            @Override
            public int signatureLength() {
                return 64;
            }

            @Override
            public byte[] sign(byte[] content) {
                return new byte[32];
            }
        }));
    }

    @Test
    public void testDuplicateKeys() throws Throwable {
        byte[] keyBytes = new byte[0];
        final List<KeyValuePair> pairs = Arrays.asList(new KeyValuePair(keyBytes, new byte[0]), new KeyValuePair(keyBytes, new byte[1]));
        assertThrown(IllegalArgumentException.class, "duplicate key", () -> pairs.sort(PAIR_COMPARATOR));

        final List<KeyValuePair> pairs2 = Arrays.asList(new KeyValuePair(new byte[] { 2 }, new byte[0]), new KeyValuePair(new byte[] { 2 }, new byte[1]));
        assertThrown(IllegalArgumentException.class, "duplicate key", () -> pairs2.sort(PAIR_COMPARATOR));
    }

    @Test
    public void nineLengths() {
        Set<Integer> recordLengths = new HashSet<>();
        for (long p = 0, seq = 0; p <= 64; p += 8, seq = (long) Math.pow(2.0, p)) {
            long temp = seq - 2;
            int i = 0;
            do {
                if(temp >= 0) {
                    Record r = new Record(temp, new ArrayList<>(0), SIGNER);
                    int len = r.getRLP().encodingLength();
                    System.out.println(temp + " -> " + len);
                    recordLengths.add(len);
                }
                temp++;
            } while (++i < 4);
        }
        assertEquals(9, recordLengths.size());
    }

    @Test
    public void testDuplicateKey() throws Throwable {
        long seq = 3L;

        final List<KeyValuePair> pairs = Arrays.asList(
                new KeyValuePair(IP, "7f000001", HEX),
                new KeyValuePair(UDP, "765f", HEX),
                new KeyValuePair(ID, "v4", UTF_8),
                new KeyValuePair(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX),
                new KeyValuePair(UDP, "765f", HEX)
        );

        for (KeyValuePair p : pairs) {
            System.out.println(p);
        }

        assertThrown(IllegalArgumentException.class, "duplicate key: " + UDP, () -> new Record(seq, pairs, SIGNER));
    }
}
