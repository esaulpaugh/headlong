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
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.IntFunction;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.rlp.KVP.CLIENT;
import static com.esaulpaugh.headlong.rlp.KVP.EMPTY_ARRAY;
import static com.esaulpaugh.headlong.rlp.KVP.ID;
import static com.esaulpaugh.headlong.rlp.KVP.IP;
import static com.esaulpaugh.headlong.rlp.KVP.IP6;
import static com.esaulpaugh.headlong.rlp.KVP.SECP256K1;
import static com.esaulpaugh.headlong.rlp.KVP.TCP;
import static com.esaulpaugh.headlong.rlp.KVP.TCP6;
import static com.esaulpaugh.headlong.rlp.KVP.UDP;
import static com.esaulpaugh.headlong.rlp.KVP.UDP6;
import static com.esaulpaugh.headlong.util.Strings.ASCII;
import static com.esaulpaugh.headlong.util.Strings.BASE_64_URL_SAFE;
import static com.esaulpaugh.headlong.util.Strings.EMPTY_BYTE_ARRAY;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                        return 2;
                    }

                    @Override
                    public byte[] sign(byte[] content) {
                        return null;
                    }
                }, seq)
        );
        assertThrown(
                InvalidParameterException.class,
                "signer specifies bad signature length: 1",
                () -> new Record(new Record.Signer() {
                    @Override
                    public int signatureLength() {
                        return 1;
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
                        return 2;
                    }

                    @Override
                    public byte[] sign(byte[] content) {
                        return null;
                    }
                }, 0L)
        );
    }

    private static String generateUtf8String(final int len, Random r) {
        final StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            final int codePoint;
            switch (r.nextInt(3)) {
            case 0: // ASCII
                codePoint = r.nextInt(0x80); // 0..127
                break;
            case 1: // 2-byte (U+0080..U+07FF)
                codePoint = 0x80 + r.nextInt(0x800 - 0x80);
                break;
            default: // 3-byte (U+0800..U+FFFF, excluding surrogates)
                codePoint = 0x0800 + r.nextInt(0xFFFF - 0x0800 + 1);
                if (Character.isSurrogate((char) codePoint)) {
                    sb.appendCodePoint(0x0800); // replace surrogate with valid code point
                    continue;
                }
                break;
            }
            sb.appendCodePoint(codePoint);
        }
        return sb.toString();
    }

    @Test
    public void testSortPairs() {
        final Random r = TestUtils.seededRandom();
        testSort(50, j -> TestUtils.generateASCIIString(j, r));
        testSort(50, j -> generateUtf8String(j, r));
    }

    private static void testSort(final int n, IntFunction<String> generator) {
        for (int len = 0; len < n; len++) {
            for (int i = 0; i < n; i++) {
                final String a = generator.apply(len);
                final String b = generator.apply(len);
                if (!a.equals(b)) {
                    final KVP pairA = new KVP(a, a.getBytes(StandardCharsets.UTF_8));
                    final KVP pairB = new KVP(b, b.getBytes(StandardCharsets.UTF_8));
                    assertEquals(Integer.signum(a.compareTo(b)), Integer.signum(pairA.compareTo(pairB)), "Failed for a='" + a + "', b='" + b + "'");
                }
            }
        }
    }

    @Test
    public void testSortRecords() {
        final Random r = TestUtils.seededRandom();
        final Record[] records = new Record[] {
                VECTOR.with(SIGNER, 101L),
                VECTOR.with(SIGNER, 52L, new KVP(UDP, EMPTY_BYTE_ARRAY)),
                VECTOR.with(SIGNER, 50L, new KVP("ÜDP", EMPTY_BYTE_ARRAY)),
                VECTOR,
                VECTOR.with(SIGNER, 99L),
                VECTOR.with(SIGNER, 0L)
        };
        TestUtils.shuffle(records, r);
        Arrays.sort(records);
        final StringBuilder sb = new StringBuilder();
        for (Record e : records) {
            sb.append(e.getSeq()).append(',');
        }
        assertEquals("0,1,50,52,99,101,", sb.toString());
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

        final LinkedHashMap<String, RLPItem> map = record.orderedMap();
        assertEquals("765f", map.get(UDP).asString(HEX));
        assertEquals("v4", map.get(ID).asString(UTF_8));
        assertEquals("03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", map.get(SECP256K1).asString(HEX));

        final Iterator<KVP> listIter = pairList.iterator();
        final Iterator<Map.Entry<String, RLPItem>> mapIter = map.entrySet().iterator();
        int i = 0;
        while (contentIter.hasNext() || listIter.hasNext() || mapIter.hasNext()) {
            final KVP e = array[i];
            assertEquals(e.key, e.key());
            testEqual(e, new KVP(contentIter.next().asRLPString(), contentIter.next().asRLPString()));
            testEqual(e, listIter.next());
            Map.Entry<String, RLPItem> entry = mapIter.next();
            testEqual(e, new KVP(entry.getKey(), entry.getValue().asBytes()));
            testEqual(e, e.withValue(e.value().asBytes()));
            testEqual(e, e.withValue(e.value().asString(HEX), HEX));
            testEqual(e, e.withValue(e.value().asString(UTF_8), UTF_8));
            testEqual(e, e.withValue(e.value().asString(BASE_64_URL_SAFE), BASE_64_URL_SAFE));
            i++;
        }
        assertEquals(ENR_STRING, record.toString());

        assertEquals(record, Record.parse(record.toString(), VERIFIER));

        RLPString prevKey = null;
        final RLPString dummyValue = RLPDecoder.RLP_STRICT.wrapBits(0L);
        for (KVP pair : record) {
            if(prevKey != null) {
                int c = pair.key.compareTo(prevKey);
                assertTrue(c > 0);
                assertEquals(pair.compareTo(new KVP(prevKey, dummyValue)), c);
            }
            prevKey = pair.key;
        }
    }

    private static void testEqual(KVP a, KVP b) {
        assertNotSame(a, b);
        assertEquals(a, b);
    }

    @Test
    public void testSmallSig() {
        Record record = new Record(new Record.Signer() {
                    @Override
                    public int signatureLength() {
                        return 2;
                    }

                    @Override
                    public byte[] sign(byte[] content) {
                        return new byte[2];
                    }
                },
                1L,
                new KVP(IP, "7f000001", HEX),
                new KVP(UDP, "765f", HEX),
                new KVP(ID, "v4", UTF_8),
                new KVP(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX)
        );
        assertEquals(2, record.getSignature().dataLength);
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
        final List<KVP> pairs = Arrays.asList(new KVP("", new byte[0]), new KVP("", new byte[1]));
        assertThrown(IllegalArgumentException.class, "duplicate key", () -> pairs.sort(Comparator.naturalOrder()));

        final List<KVP> pairs2 = Arrays.asList(new KVP("d", new byte[0]), new KVP("d", new byte[1]));
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
            LinkedHashMap<String, RLPItem> map = with.orderedMap();
            assertEquals(4, map.size());
            assertEquals(808L, with.getSeq());
            assertArrayEquals(Strings.decode("0009", HEX), map.get(UDP).asBytes());
        }

        Record with = VECTOR.with(SIGNER, 4L, new KVP(TCP6, "656934", HEX));
        assertEquals(4L, with.getSeq());
        LinkedHashMap<String, RLPItem> map = with.orderedMap();
        assertEquals(5, map.size());
        assertArrayEquals(Strings.decode("656934", HEX), map.get(TCP6).asBytes());
    }

    @Test
    public void testRecordWith2() throws Throwable {
        assertEquals(4L, VECTOR.orderedMap().size());

        Record with = VECTOR.with(SIGNER, Long.MAX_VALUE, new KVP(UDP6, "8007", HEX), new KVP(IP6, "ff00ff00", HEX));
        LinkedHashMap<String, RLPItem> map = with.orderedMap();
        assertEquals(6, map.size());
        assertEquals(Long.MAX_VALUE, with.getSeq());
        assertArrayEquals(Strings.decode("8007", HEX), map.get(UDP6).asBytes());

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

    @Test
    public void testMissingPrefix() throws Throwable {
        assertThrown(IllegalArgumentException.class, "prefix \"enr:\" not found", () -> Record.parse("abcd", VERIFIER));
    }

    @Test
    public void testOutOfOrder() throws Throwable {
        String enr =    "enr:-IS4QHCYrYZbAKWCBRlAy5zzaDZXJBGkcnh4MHcBFZntXNFrdvJjX04jRzjzCBOonrkTfj499SZuOh8R33Ls8RRc" +
                        "y5wBgmlwhH8AAAGDdWRwgnZfgmlkgnY0iXNlY3AyNTZrMaEDymNMrg1JrLQB2KTGtv6MVbcNEVv0AHacwUAPMljNMTg";
        assertThrown(IllegalArgumentException.class,
                "key out of order",
                () -> Record.parse(enr, VERIFIER));
    }

    @Test
    public void testToString() {
        assertEquals("ip --> 3235352e3130312e302e313238", new KVP(IP, "255.101.0.128", ASCII).toString());
        assertEquals(
                "client --> [\"dd\", \"\"]",
                new KVP(RLPDecoder.RLP_STRICT.wrapString(RLPEncoder.string(Strings.decode(CLIENT, UTF_8))), RLPDecoder.RLP_STRICT.wrapBits(0xc482646480L)).toString()
        );
    }
}
