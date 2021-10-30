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
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.rlp.KVP.*;
import static com.esaulpaugh.headlong.util.Strings.ASCII;
import static com.esaulpaugh.headlong.util.Strings.BASE_64_URL_SAFE;
import static com.esaulpaugh.headlong.util.Strings.EMPTY_BYTE_ARRAY;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class EIP778Test {

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

    private static final String ENR_STRING = "enr:-IS4QHCYrYZbAKWCBRlAy5zzaDZXJBGkcnh4MHcBFZntXNFrdvJjX04jRzjzCBOonrkTfj499SZuOh8R33Ls8RRcy5wBgmlkgnY0gmlwhH8AAAGJc2VjcDI1NmsxoQPKY0yuDUmstAHYpMa2_oxVtw0RW_QAdpzBQA8yWM0xOIN1ZHCCdl8";

    private static final Record.Verifier VERIFIER = (s,c) -> {
        if(!Arrays.equals(s, SIG)) throw new SignatureException();
    };

    private static final String RECORD_HEX;

    static {
        try {
            RECORD_HEX = Strings.encode(Record.parse(ENR_STRING, VERIFIER).getRLP().encoding());
        } catch (SignatureException e) {
            throw new RuntimeException(e);
        }
    }

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
                () -> new Record(new Record.Signer() {
                    @Override
                    public int signatureLength() {
                        return 0;
                    }

                    @Override
                    public byte[] sign(byte[] content) {
                        return null;
                    }
                }, seq)
        );
        assertThrown(
                InvalidParameterException.class,
                "signer specifies negative signature length",
                () -> new Record(new Record.Signer() {
                    @Override
                    public int signatureLength() {
                        return -1;
                    }

                    @Override
                    public byte[] sign(byte[] content) {
                        return null;
                    }
                }, 0x07)
        );
        assertThrown(
                NullPointerException.class,
                () -> new Record(new Record.Signer() {
                    @Override
                    public int signatureLength() {
                        return 0;
                    }

                    @Override
                    public byte[] sign(byte[] content) {
                        return null;
                    }
                }, 0L)
        );
    }

    @Test
    public void testSort() {
        Random r = TestUtils.seededRandom();
        for (int j = 0; j < 50; j++) {
            for (int i = 0; i < 50; i++) {
                String a = TestUtils.generateASCIIString(j, r);
                String b = TestUtils.generateASCIIString(j, r);
                if (!a.equals(b)) {
                    int str = a.compareTo(b) < 0 ? 0 : 1;
                    KVP pairA = new KVP(a, EMPTY_BYTE_ARRAY);
                    KVP pairB = new KVP(b, EMPTY_BYTE_ARRAY);
                    int pair = pairA.compareTo(pairB) < 0 ? 0 : 1;
                    assertEquals(str, pair);
                }
            }
        }
    }

    @Test
    public void testEip778() throws SignatureException {
        final long seq = 1L;
        final List<KVP> pairs = Arrays.asList(
                new KVP(IP, "7f000001", HEX),
                new KVP(UDP, "765f", HEX),
                new KVP(ID, "v4", UTF_8),
                new KVP(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX)
        );
        final KVP[] array = pairs.toArray(EMPTY_ARRAY);

        final Record record = new Record(SIGNER, seq, pairs);

        assertEquals(RECORD_HEX, record.getRLP().encodingString(HEX));

        assertEquals(VECTOR.getSignature(), record.getSignature());
        assertEquals(VECTOR.getContent(), record.getContent());
        assertEquals(VECTOR.getSeq(), record.getSeq());
        assertEquals(VECTOR.getRLP(), record.getRLP());
        assertEquals(VECTOR.toString(), record.toString());
        assertEquals(VECTOR, record);

        RLPList content = record.getContent();
        System.out.println("verified = " + content);
        final Iterator<RLPItem> contentIter = content.iterator(RLPDecoder.RLP_STRICT);

        assertEquals(seq, record.getSeq());
        assertEquals(seq, contentIter.next().asLong());

        Arrays.sort(array);

        List<KVP> pairList = record.getPairs();

        assertArrayEquals(array, record.getPairs().toArray(EMPTY_ARRAY));

        Map<String, byte[]> map = record.map();
        assertArrayEquals(Strings.decode("765f"), record.map().get(UDP));
        assertArrayEquals(Strings.decode("v4", UTF_8), map.get(ID));
        assertArrayEquals(Strings.decode("03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138"), map.get(SECP256K1));

        assertEquals(seq, record.visitAll((k, v) -> {}));

        final Iterator<KVP> listIter = pairList.iterator();
        final Iterator<Map.Entry<String, byte[]>> mapIter = map.entrySet().iterator();
        int i = 0;
        while (contentIter.hasNext() || listIter.hasNext() || mapIter.hasNext()) {
            final KVP e = array[i];
            testEqual(e, new KVP(contentIter.next(), contentIter.next()));
            testEqual(e, listIter.next());
            Map.Entry<String, byte[]> entry = mapIter.next();
            testEqual(e, new KVP(entry.getKey(), entry.getValue()));
            testEqual(e, e.withValue(e.value().asBytes()));
            testEqual(e, e.withValue(e.value().asString(HEX), HEX));
            testEqual(e, e.withValue(e.value().asString(UTF_8), UTF_8));
            testEqual(e, e.withValue(e.value().asString(BASE_64_URL_SAFE), BASE_64_URL_SAFE));
            i++;
        }
        assertEquals(ENR_STRING, record.toString());

        assertEquals(record, Record.parse(record.toString(), VERIFIER));
    }

    private static void testEqual(KVP a, KVP b) {
        assertNotSame(a, b);
        assertEquals(a, b);
    }

    @Test
    public void testZeroLenSig() {
        Record record = new Record(new Record.Signer() {
                    @Override
                    public int signatureLength() {
                        return 0;
                    }

                    @Override
                    public byte[] sign(byte[] content) {
                        return new byte[0];
                    }
                },
                1L,
                new KVP(IP, "7f000001", HEX),
                new KVP(UDP, "765f", HEX),
                new KVP(ID, "v4", UTF_8),
                new KVP(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX)
        );
        assertEquals(0, record.getSignature().dataLength);
    }

    @Test
    public void testIncorrectSignatureLength() throws Throwable {
        assertThrown(InvalidParameterException.class,
                "unexpected signature length: 32 != 64",
                        () -> new Record(new Record.Signer() {
                            @Override
                            public int signatureLength() {
                                return 64;
                            }

                            @Override
                            public byte[] sign(byte[] content) {
                                return new byte[32];
                            }
                        },
                        90L,
                        new KVP(IP, "7f000001", HEX),
                        new KVP(UDP, "765f", HEX),
                        new KVP(ID, "v4", UTF_8),
                        new KVP(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX)));
    }

    @Test
    public void testDuplicateKeys() throws Throwable {
        byte[] keyBytes = new byte[0];
        final List<KVP> pairs = Arrays.asList(new KVP(keyBytes, new byte[0]), new KVP(keyBytes, new byte[1]));
        assertThrown(IllegalArgumentException.class, "duplicate key", () -> pairs.sort(Comparator.naturalOrder()));

        final List<KVP> pairs2 = Arrays.asList(new KVP(new byte[] { 2 }, new byte[0]), new KVP(new byte[] { 2 }, new byte[1]));
        assertThrown(IllegalArgumentException.class, "duplicate key", () -> pairs2.sort(Comparator.naturalOrder()));
    }

    @Test
    public void nineLengths() {
        Set<Integer> recordLengths = new HashSet<>();
        for (long p = 0, seq = 0; p <= 64; p += 8, seq = (long) Math.pow(2.0, p)) {
            long temp = seq - 2;
            int i = 0;
            do {
                if(temp >= 0) {
                    Record r = new Record(SIGNER, temp);
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

        final List<KVP> pairs = Arrays.asList(
                new KVP(IP, "7f000001", HEX),
                new KVP(UDP, "765f", HEX),
                new KVP(ID, "v4", UTF_8),
                new KVP(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX),
                new KVP(UDP, "0000", HEX)
        );

        for (KVP p : pairs) {
            System.out.println(p);
        }

        assertThrown(IllegalArgumentException.class, "duplicate key: " + UDP, () -> new Record(SIGNER, seq, pairs));
    }

    @Test
    public void testRecordWith() {
        {
            Record with = VECTOR.with(SIGNER, 808L, new KVP(UDP, "0009", HEX));
            Map<String, byte[]> map = with.map();
            assertEquals(4, map.size());
            assertEquals(808L, with.getSeq());
            assertArrayEquals(Strings.decode("0009", HEX), map.get(UDP));
        }

        Record with = VECTOR.with(SIGNER, 4L, new KVP(TCP6, "656934", HEX));
        assertEquals(4L, with.getSeq());
        Map<String, byte[]> map = with.map();
        assertEquals(5, map.size());
        assertArrayEquals(Strings.decode("656934", HEX), map.get(TCP6));
    }

    @Test
    public void testRecordWith2() throws Throwable {
        assertEquals(4L, VECTOR.map().size());

        Record with = VECTOR.with(SIGNER, Long.MAX_VALUE, new KVP(UDP6, "8007", HEX), new KVP(IP6, "ff00ff00", HEX));
        Map<String, byte[]> map = with.map();
        assertEquals(6, map.size());
        assertEquals(Long.MAX_VALUE, with.getSeq());
        assertArrayEquals(Strings.decode("8007", HEX), map.get(UDP6));

        TestUtils.assertThrown(IllegalArgumentException.class, "duplicate key: tcp", () -> with.with(SIGNER, 0L, new KVP(TCP, "blah", ASCII), new KVP(TCP, "bleh", ASCII)));
    }

    @Test
    public void testSignVerify() throws Throwable {
        Record r = new Record(new Record.Signer() {
            @Override
            public int signatureLength() {
                return 64;
            }

            @Override
            public byte[] sign(byte[] content) {
                Arrays.fill(content, (byte) 0xff);
                return SIG;
            }
        },
        1L,
        new KVP(IP, "7f000001", HEX),
        new KVP(UDP, "765f", HEX),
        new KVP(ID, "v4", UTF_8),
        new KVP(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX));
        assertEquals(ENR_STRING, r.toString());

        assertThrown(SignatureException.class, "moops", () -> Record.parse(ENR_STRING, (signature, content) -> {
            throw new SignatureException("moops");
        }));

        Record r2 = Record.parse(ENR_STRING, (signature, content) -> {
            Arrays.fill(content, (byte) 0xff);
            Arrays.fill(signature, (byte) 0x07);
        });

        assertEquals(ENR_STRING, r2.toString());
    }

    @Test
    public void testEncode() {
        ByteBuffer bb = Record.encode(SIGNER, 1L, new KVP(IP, "7f000001", HEX),
                new KVP(UDP, "765f", HEX),
                new KVP(ID, "v4", UTF_8),
                new KVP(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX));

        assertEquals(RECORD_HEX, Strings.encode(bb));
    }
}
