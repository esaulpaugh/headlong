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
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.esaulpaugh.headlong.TestUtils.CustomRunnable;
import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.TestUtils.requireNoTimeout;
import static com.esaulpaugh.headlong.TestUtils.shutdownAwait;
import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_LENIENT;
import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.EMPTY_BYTE_ARRAY;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RLPDecoderTest {

    private static final byte[] LONG_LIST_BYTES = new byte[] {
            (byte) 0xf8, (byte) 148,
            (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, (byte) 0xFF, (byte) 0x81, (byte) 0x90, (byte) 0x81, (byte) 0xb6, (byte) '\u230A',
            (byte) 0xb8, 56, 0x09,(byte)0x80,-1,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, -3, -2, 0, 0,
            (byte) 0xf8, 0x38, 0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 36, 74, 0, 0,
            (byte) 0x84, 'c', 'a', 't', 's',
            (byte) 0x84, 'd', 'o', 'g', 's',
            (byte) 0xca, (byte) 0x84, 92, '\r', '\n', '\f', (byte) 0x84, '\u0009', 'o', 'g', 's',
    };

    @Test
    public void testLenient() throws Throwable {

        final String errPrefix = "deserialized integers with leading zeroes are invalid; index: ";

        Random r = TestUtils.seededRandom();

        final byte[] bytes = TestUtils.randomBytes(10, r);
        while(bytes[2] == 0) {
            bytes[2] = (byte) r.nextInt();
        }

        bytes[0] = bytes[1] = 0;

        TestUtils.assertThrown(IllegalArgumentException.class, errPrefix + "0, len: 10", () -> Integers.getBigInt(bytes, 0, bytes.length, false));

        TestUtils.assertThrown(IllegalArgumentException.class, errPrefix + "1, len: 9", () -> Integers.getBigInt(bytes, 1, bytes.length - 1, false));

        Integers.getBigInt(bytes, 2, bytes.length - 2, false);

        Integers.getBigInt(bytes, 0, bytes.length, true);
        Integers.getBigInt(bytes, 1, bytes.length - 1, true);

        byte[][] vectors = new byte[][] {
                new byte[] { 0 },
                new byte[] { (byte) 0x82, 0, 99 },
                new byte[] { (byte) 0x84, 0, -128, 2, 1 },
                new byte[] { (byte) 0x88, 0, -1, 6, 5, 4, 3, 2, 1 },
                RLPEncoder.encodeString(new byte[] { 0, 0, -50, 90, 12, 4, 13, 21, 89, -120 })
        };

        TestUtils.assertThrown(IllegalArgumentException.class, errPrefix + "0, len: 1", RLP_STRICT.wrap(vectors[0])::asByte);
        TestUtils.assertThrown(IllegalArgumentException.class, errPrefix + "1, len: 2", () -> RLP_STRICT.wrap(vectors[1]).asShort(false));
        TestUtils.assertThrown(IllegalArgumentException.class, errPrefix + "1, len: 4", RLP_STRICT.wrap(vectors[2])::asInt);
        TestUtils.assertThrown(IllegalArgumentException.class, errPrefix + "1, len: 8", RLP_STRICT.wrap(vectors[3])::asLong);
        TestUtils.assertThrown(IllegalArgumentException.class, errPrefix + "1, len: 10", RLP_STRICT.wrapString(vectors[4])::asBigInt);

        assertEquals(0, RLP_STRICT.wrap(vectors[0]).asByte(true));
        assertEquals(99, RLP_STRICT.wrap(vectors[1]).asShort(true));
        assertEquals(8389121, RLP_STRICT.wrap(vectors[2]).asInt(true));
        assertEquals(0xff060504030201L, RLP_STRICT.wrap(vectors[3]).asLong(true));
        assertEquals(new BigInteger("00ce5a0c040d155988", 16), RLP_STRICT.wrapString(vectors[4]).asBigInt(true));
    }

    @Disabled("slow")
    @Test
    public void exhaustiveFuzz() throws InterruptedException, TimeoutException {
        ExecutorService executorService = Executors.newWorkStealingPool();
        ExhaustiveFuzzTask[] tasks = new ExhaustiveFuzzTask[256];
        for (int i = 0; i < tasks.length; i++) {
            System.out.print(i + " -> ");
            tasks[i] = new ExhaustiveFuzzTask(new byte[] { (byte) i, 0, 0, 0 });
            executorService.submit(tasks[i]);
        }
        requireNoTimeout(shutdownAwait(executorService, 2000L));
        long valid = 0, invalid = 0;
        for (ExhaustiveFuzzTask task : tasks) {
            if(task != null) {
                valid += task.valid;
                invalid += task.invalid;
            }
        }
        String result = valid + " / " + (valid + invalid) + " (" + invalid + " invalid)";
        System.out.println(result);
        assertEquals("2273312768 / 4294967296 (2021654528 invalid)", result);
    }

    private static class ExhaustiveFuzzTask implements Runnable {

        private final byte[] four;
        long valid, invalid;
        private final String tag;

        ExhaustiveFuzzTask(byte[] four) {
            this.four = four;
            this.tag = Arrays.toString(four);
            System.out.println(tag);
        }

        @Override
        public void run() {
            byte[] four = this.four;
            byte one = 0, two = 0, three = 0;
            int valid = 0, invalid = 0;
            boolean gogo = true;
            do {
                four[1] = one++;
                if(one == 0) {
                    four[2] = two++;
                    if(two == 0) {
                        four[3] = three++;
                        if(three == 0) {
                            gogo = false;
                        }
                    }
                }
                try {
                    RLP_STRICT.wrap(four, 0, 4);
                    valid++;
                } catch (IllegalArgumentException iae) {
                    invalid++;
                }
            } while(gogo);
            System.out.println(tag + "END " + Strings.encode(four));
            this.valid = valid;
            this.invalid = invalid;
        }
    }

    @Test
    public void randomFuzz() {
        final Random r = TestUtils.seededRandom();
        fuzzRLP(RLP_STRICT, r, 25_000);
        fuzzRLP(RLP_LENIENT, r, 25_000);
    }

    private static void fuzzRLP(RLPDecoder decoder, Random r, final int n) {
        final byte[] buffer = new byte[56];
        int valid = 0, invalid = 0;
        for (int i = 0; i < n; i++) {
            r.nextBytes(buffer);
            final String first = Strings.encode(buffer[0]);
            try {
                RLPItem item = decoder.wrap(buffer);
                valid++;
                if (item.asBoolean()) {
                    switch (first) {
                    case "c0":
                    case "80":
                    case "00":
                        throw exception(buffer);
                    default:
                    }
                } else {
                    switch (first) {
                    case "c0":
                    case "80":
                    case "00":
                        break;
                    default:
                        throw exception(buffer);
                    }
                    if (item.asChar(true) != '\u0000') {
                        throw exception(buffer);
                    }
                }
            } catch (IllegalArgumentException iae) {
                invalid++;
                switch (first) {
                case "81":
                    if(!decoder.lenient && DataType.isSingleByte(buffer[1])) {
                        break;
                    }
                    throw exception(buffer);
                case "b8": case "b9": case "ba": case "bb": case "bc": case "bd": case "be": case "bf":
                case "f8": case "f9": case "fa": case "fb": case "fc": case "fd": case "fe": case "ff":
                    break;
                default:
                    throw exception(buffer);
                }
            }
        }
        System.out.println("valid: " + valid + " / " + n + " (" + invalid + " invalid) lenient=" + decoder.lenient);
    }

    private static RuntimeException exception(byte[] buffer) {
        return new RuntimeException(Strings.encode(buffer));
    }

    @Test
    public void testListIterable() throws Throwable {
        final RLPList rlpList = RLP_STRICT.wrapList(LONG_LIST_BYTES);
        assertEquals(DataType.LIST_LONG, rlpList.type());
        assertFalse(rlpList.isString());
        assertTrue(rlpList.isList());

        final List<RLPItem> elements = rlpList.elements();
        assertEquals(DataType.LIST_SHORT, elements.get(0).type());
        assertEquals(DataType.STRING_LONG, elements.get(1).type());
        assertEquals(DataType.LIST_LONG, elements.get(2).type());
        assertEquals(DataType.STRING_SHORT, elements.get(3).type());
        assertEquals(DataType.STRING_SHORT, elements.get(4).type());
        assertEquals(DataType.LIST_SHORT, elements.get(5).type());

        byte[] copy = Arrays.copyOf(LONG_LIST_BYTES, LONG_LIST_BYTES.length);

        copy[copy.length - 11]++;

        RLPList brokenList = RLP_STRICT.wrapList(copy);

        assertThrown(ShortInputException.class, "element @ index 139 exceeds its container: 151 > 150", () -> {
            for(RLPItem item : brokenList) {
                System.out.println(item.asString(Strings.HEX));
            }
        });
    }

    @Test
    public void duplicate() {
        RLPList rlpList = RLP_STRICT.wrapList(LONG_LIST_BYTES);
        assertEquals(rlpList, rlpList.duplicate());
        RLPString rlpString = RLP_STRICT.wrapString((byte) 0x00);
        assertEquals(rlpString, rlpString.duplicate());

        assertTrue(rlpString.isString());
        assertFalse(rlpString.isList());
    }

    @Test
    public void strictAndLenient() throws Throwable {
        byte[] invalidAf = new byte[] {
                (byte)0xc8, (byte)0x80, 0, (byte)0x81, (byte) 0xAA, (byte)0x81, (byte)'\u0080', (byte)0x81, '\u007f', (byte)'\u230A'
        };

        RLPList list = RLP_STRICT.wrapList(invalidAf);

        TestUtils.assertThrown(
                IllegalArgumentException.class,
                "invalid rlp for single byte @ 7",
                () -> list.elements(RLP_STRICT)
        );

        list.elements(RLP_LENIENT);
    }

    @Test
    public void list() {
        RLPList rlpList = RLP_STRICT.wrapList(LONG_LIST_BYTES);
        List<RLPItem> actualList = rlpList.elements(RLP_STRICT);

        assertEquals(148, rlpList.dataLength);
        assertEquals(6, actualList.size());
        assertEquals(10, actualList.get(0).dataLength);
    }

    @Disabled("can cause OutOfMemoryError")
    @Test
    public void hugeStrings() {
        int lol;
        byte[] buffer;
        RLPString huge;
        String data;

        long[] dataLengths = new long[] {
                255,
                Short.MAX_VALUE,
                Short.MAX_VALUE * 50,
                Short.MAX_VALUE * 1000,
                (long) (Integer.MAX_VALUE * 0.921) // lower this if you can't increase heap
        };
        for (long dataLen : dataLengths) {
            lol = Integers.len(dataLen);
            buffer = new byte[1 + lol + (int) dataLen];
            buffer[0] = (byte) (0xb7 + lol);
            Integers.putLong(dataLen, buffer, 1);
            huge = RLP_STRICT.wrapString(buffer);
            data = huge.asString(UTF_8);
            System.out.println(dataLen);
            assertEquals(dataLen, data.length());
        }
    }

    @Disabled("can cause OutOfMemoryError")
    @Test
    public void hugeListsHighMem() {

//        System.out.println(Runtime.getRuntime().maxMemory());
//        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
//        System.out.println(memoryBean.getHeapMemoryUsage().getMax());

        int lol;
        byte[] buffer;
        RLPList huge;
        int i;

        long[] dataLengths = new long[] {
                255,
                Short.MAX_VALUE,
                Short.MAX_VALUE * 50,
                Short.MAX_VALUE * 1000,
                Integer.MAX_VALUE - 13 // max safe array size is MAX_VALUE - 8; need 5 bytes for prefix
        }; // 257 * 8,355,967 = Integer.MAX_VALUE - 13 - 115

        final int elementDataLen = 37; // increase item size if can't increase heap
        final byte elementLeadByte = (byte) (0x80 + elementDataLen);// (byte) (0xb7 + 1); // (byte) (0xf7 + 1)
        final int elementEncodedLen = elementDataLen + 1;

        for (long dataLen : dataLengths) {
            System.out.println("dataLen = " + dataLen);
            lol = Integers.len(dataLen);
            System.out.println("length of length = " + lol);
            buffer = new byte[1 + lol + (int) dataLen];
            buffer[0] = (byte) (0xf7 + lol);
            Integers.putLong(dataLen, buffer, 1);
            i = 1 + lol;
            int size = 0;
            final int lim = buffer.length - elementEncodedLen;
            while(i < lim) {
                buffer[i] = elementLeadByte;
//                Integers.putLong(elementDataLen, buffer, i + 1);
                size++;
                i += elementEncodedLen;
            }
            size += buffer.length - i;
            huge = RLP_STRICT.wrapList(buffer);
            ArrayList<RLPItem> elements = new ArrayList<>(size);
            huge.elements(RLP_STRICT, elements);
            System.out.println("trailing single byte items = " + (buffer.length - i));
            System.out.println("list size = " + size);
            assertEquals(size, elements.size());
        }
    }

    @Test
    public void hugeListsLowMem() {

        long[] dataLengths = new long[] {
                56,
                Integer.MAX_VALUE / (((int) Math.pow(2, 23)) - 1),
                Integer.MAX_VALUE / (((int) Math.pow(2, 15)) - 1),
                Integer.MAX_VALUE / (((int) Math.pow(2, 7)) - 1)
        };

        for (int k = 0; k < dataLengths.length; k++) {
            long dataLen = dataLengths[k];
            int lol = Integers.len(dataLen);
            System.out.print("length of length == " + (k + 1) + ", ");
            assertEquals(k + 1, lol);
            byte[] buffer = new byte[1 + lol + (int) dataLen];
            Arrays.fill(buffer, (byte) 0x09);
            buffer[0] = (byte) (0xf7 + lol);
            Integers.putLong(dataLen, buffer, 1);

            RLPList huge = RLP_STRICT.wrapList(buffer);

            int count = 0;
            int i = huge.dataIndex;
            final int end = huge.endIndex;
            while(i < end) {
                i = RLP_STRICT.wrap(buffer, i).endIndex;
                count++;
            }
            System.out.println("dataLen " + dataLen + " == " + count);
            assertEquals(dataLen, count);
        }
    }

    @Test
    public void exceedsContainerShort() throws Throwable {

        byte[] a0 = new byte[] { (byte) 0x81 };
        byte[] a1 = new byte[] { (byte) 0xc1 };

        assertThrown(ShortInputException.class, "@ index 0", wrapLenient(a0));
        assertThrown(ShortInputException.class, "@ index 0", wrapLenient(a1));

        byte[] b0 = new byte[] { (byte) 0xc1, (byte) 0x81 };
        byte[] b1 = new byte[] { (byte) 0xc1, (byte) 0xc1 };

        assertThrown(ShortInputException.class, "@ index 1", decodeList(b0));
        assertThrown(ShortInputException.class, "@ index 1", decodeList(b1));

        byte[] c0 = new byte[] { (byte) 0xc1, (byte) 0x81, (byte) 0x00 };
        byte[] c1 = new byte[] { (byte) 0xc1, (byte) 0xc1, (byte) 0x00 };

        assertThrown(IllegalArgumentException.class, "@ index 1", decodeList(c0));
        assertThrown(IllegalArgumentException.class, "@ index 1", decodeList(c1));
    }

    @Test
    public void exceedsContainerLong() throws Throwable {

        byte[] a0 = new byte[57]; a0[0] = (byte) 0xb8; a0[1] = 56;
        byte[] a1 = new byte[57]; a1[0] = (byte) 0xf8; a1[1] = 56;

        assertThrown(ShortInputException.class, "@ index 0", wrapLenient(a0));
        assertThrown(ShortInputException.class, "@ index 0", wrapLenient(a1));

        byte[] b0 = new byte[58]; b0[0] = (byte) 0xf8; b0[1] = 56; b0[57] = (byte) 0x81;
        byte[] b1 = new byte[58]; b1[0] = (byte) 0xf8; b1[1] = 56; b1[56] = (byte) 0xc2;

        assertThrown(ShortInputException.class, "@ index 57", decodeList(b0));
        assertThrown(ShortInputException.class, "@ index 56", decodeList(b1));

        byte[] c0 = new byte[59]; c0[0] = (byte) 0xf8; c0[1] = 56; c0[57] = (byte) 0x81;
        byte[] c1 = new byte[59]; c1[0] = (byte) 0xf8; c1[1] = 56; c1[56] = (byte) 0xc2;

        assertThrown(IllegalArgumentException.class, "@ index 57", decodeList(c0));
        assertThrown(IllegalArgumentException.class, "@ index 56", decodeList(c1));
    }

    @Test
    public void booleans() {
        assertFalse(RLP_STRICT.wrap((byte) 0xc0).asBoolean());
        assertFalse(RLP_STRICT.wrap((byte) 0x80).asBoolean());
        assertFalse(RLP_STRICT.wrap((byte) 0x00).asBoolean());

        assertTrue(RLP_LENIENT.wrap(new byte[] { (byte) 0x81, (byte) 0x00 }).asBoolean());
        assertTrue(RLP_LENIENT.wrap(new byte[] { (byte) 0x81, (byte) 0x01 }).asBoolean());

        assertTrue(RLP_STRICT.wrap(new byte[] { (byte) 0x81, (byte) 0x80 }).asBoolean());

        assertTrue(RLP_STRICT.wrap((byte) 0x01).asBoolean());

        assertTrue(RLP_STRICT.wrap((byte) 0x79).asBoolean());
    }

    @Test
    public void chars() {
        byte[][] burma17 = new byte[256][];
        for (int i = 0; i < burma17.length; i++) {
            burma17[i] = RLPEncoder.encodeString((byte) i);
        }

        HashSet<Character> chars = new HashSet<>(512);
        HashSet<Character> sizeTwo = new HashSet<>(256);
        for (byte[] bytes : burma17) {
            RLPItem item = RLP_STRICT.wrap(bytes);
            Character c = (char) item.asByte(true);
            if (!chars.add(c)) {
                throw new RuntimeException(item.toString());
            }
            if(bytes.length != 1) {
                sizeTwo.add(c);
            }
        }
        assertEquals(256, chars.size());
        assertEquals(128, sizeTwo.size());

        assertEquals('\0', RLP_STRICT.wrap((byte) 0xc0).asChar(false));
        assertEquals('\0', RLP_STRICT.wrap((byte) 0x80).asChar(false));
        assertEquals('\0', RLP_STRICT.wrap((byte) 0x00).asChar(true));
    }

    @Test
    public void collect() {
        
        Set<Object> objectSet = new HashSet<>();
        int numAdded = collectUntil(LONG_LIST_BYTES, 0, LONG_LIST_BYTES.length, objectSet);
        assertEquals(1, numAdded);
        assertEquals(1, objectSet.size());

        assertEquals(1, RLPDecoder.RLP_STRICT.stream(LONG_LIST_BYTES)
                .collect(Collectors.toSet()).size());
        
        byte[] rlp = new byte[9];
        for (int i = 0; i < rlp.length; i++) {
            rlp[i] = (byte) i;
        }
        Set<RLPItem> hashSet = new HashSet<>();
        collectN(rlp, 0, 5, hashSet);
        assertEquals(5, hashSet.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(hashSet.contains(RLP_STRICT.wrap((byte) i)));
        }

        hashSet = new HashSet<>();
        int n = collectUntil(rlp, 0, 7, hashSet);
        assertEquals(7, n);
        assertEquals(7, hashSet.size());
        for (int i = 0; i < 7; i++) {
            assertTrue(hashSet.contains(RLP_STRICT.wrap((byte) i)));
        }

        hashSet = new HashSet<>();
        collectN(rlp, 4, 3, hashSet);
        assertEquals(3, hashSet.size());
        for (int i = 4; i < 7; i++) {
            assertTrue(hashSet.contains(RLP_STRICT.wrap((byte) i)));
        }

        hashSet = new HashSet<>();
        n = collectUntil(rlp, 1, 6, hashSet);
        assertEquals(5, n);
        assertEquals(5, hashSet.size());
        for (int i = 1; i < 6; i++) {
            assertTrue(hashSet.contains(RLP_STRICT.wrap((byte) i)));
        }

        hashSet = new HashSet<>();
        n = collectAll(rlp, 0, hashSet);
        assertEquals(9, n);
        for (int i = 0; i < 9; i++) {
            assertTrue(hashSet.contains(RLP_STRICT.wrap((byte) i)));
        }

        List<RLPItem> list = collectN(rlp, 4);
        assertEquals(4, list.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(list.get(i), RLP_STRICT.wrap((byte) i));
        }

        list = collectN(rlp, 1, 3);
        assertEquals(3, list.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(list.get(i), RLP_STRICT.wrap((byte) (i + 1)));
        }

        list = collectAll(rlp, 1);
        assertEquals(8, list.size());

        list = collectUntil(rlp, 3);
        assertEquals(3, list.size());

        list = collectUntil(rlp, 2, 8);
        assertEquals(6, list.size());
        for (int i = 0; i < 6; i++) {
            assertEquals(list.get(i), RLP_STRICT.wrap((byte) (i + 2)));
        }
    }

    @Test
    public void negativeDataLen() throws Throwable {
        byte[] alpha = new byte[] {
                (byte) 0xbf,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

        assertThrown(IllegalArgumentException.class, "found: -1", wrapLenient(alpha));

        byte[] beta = new byte[] {
                (byte) 0xbf,
                (byte) 0x80, 0, 0, 0, 0, 0, 0, 0 };

        assertThrown(IllegalArgumentException.class, "found: -9223372036854775808", wrapLenient(beta));
    }

    private static CustomRunnable wrapLenient(final byte[] rlp) {
        return () -> RLP_LENIENT.wrap(rlp, 0);
    }

    private static CustomRunnable decodeList(final byte[] rlp) {
        return () -> RLP_LENIENT.wrapList(rlp, 0).elements(RLP_STRICT);
    }

    @Test
    public void iterators() throws Throwable {
        byte[] a = new byte[] { 1 };
        byte[] b = new byte[] { 2 };
        byte[] c = new byte[0];

        byte[] list = new byte[4];
        assertEquals(list.length, RLPEncoder.encodeAsList(new Object[] { a, b, c }, list, 0));
        Iterator<RLPItem> listIter = RLP_STRICT.listIterator(list);

        for (RLPItem item : RLP_STRICT.wrapList(list)) {
            System.out.print(item);
        }

        assertTrue(listIter.hasNext());
        assertArrayEquals(a, listIter.next().asBytes());
        assertTrue(listIter.hasNext());
        assertArrayEquals(b, listIter.next().asBytes());
        assertTrue(listIter.hasNext());
        assertArrayEquals(c, listIter.next().asBytes());

        assertFalse(listIter.hasNext());
        assertThrown(NoSuchElementException.class, listIter::next);

        byte[] sequence = RLPEncoder.encodeSequentially(c, a, b);
        Iterator<RLPItem> seqIter = RLP_STRICT.sequenceIterator(sequence);

        assertTrue(seqIter.hasNext());
        assertArrayEquals(c, seqIter.next().asBytes());
        assertTrue(seqIter.hasNext());
        assertArrayEquals(a, seqIter.next().asBytes());
        assertTrue(seqIter.hasNext());
        assertArrayEquals(b, seqIter.next().asBytes());

        assertFalse(seqIter.hasNext());
        assertThrown(NoSuchElementException.class, seqIter::next);
    }

    @Test
    public void testStreaming() {
        List<RLPItem> collection = RLP_STRICT.stream(RLPStreamTest.RLP_BYTES)
                .collect(Collectors.toList());

        String joined = collection.stream()
                .filter(RLPItem::isList)
                .peek(System.out::println)
                .map(RLPItem::asRLPList)
                .map(RLPList::elements)
                .flatMap(List::stream)
                .filter(item -> item.dataLength <= Long.BYTES)
//                .map(RLPItem::asLong)
                .map(item -> item.asLong(true))
                .filter(item -> item > 0)
                .map(Math::sqrt)
                .map(String::valueOf)
                .collect(Collectors.joining("\n"));

        System.out.println(joined);
    }

    @Test
    public void testExport() {
        RLPList list = RLP_STRICT.wrapList(LONG_LIST_BYTES);

        byte[] buffer = new byte[list.encodingLength()];
        int idx = list.export(buffer, 0);
        assertEquals(list.encodingLength(), idx);
        assertArrayEquals(buffer, list.encoding());

        buffer = new byte[list.dataLength];
        idx = list.exportData(buffer, 0);
        assertEquals(list.dataLength, idx);
        assertArrayEquals(buffer, list.data());
    }

    public static List<RLPItem> collectAll(byte[] encodings) {
        return collectAll(encodings, 0);
    }

    public static List<RLPItem> collectAll(byte[] encodings, int index) {
        return collectUntil(encodings, index, encodings.length);
    }

    public static List<RLPItem> collectUntil(byte[] encodings, int endIndex) {
        return collectUntil(encodings, 0, endIndex);
    }

    public static List<RLPItem> collectUntil(byte[] encodings, int index, int endIndex) {
        ArrayList<RLPItem> dest = new ArrayList<>();
        collectUntil(encodings, index, endIndex, dest);
        return dest;
    }

    public static List<RLPItem> collectN(byte[] encodings, int n) {
        return collectN(encodings, 0, n);
    }

    public static List<RLPItem> collectN(byte[] encodings, int index, int n) {
        return RLP_STRICT.stream(encodings, index)
                .limit(n)
                .collect(Collectors.toList());
    }

    public static int collectAll(byte[] encodings, int index, Collection<? super RLPItem> dest) {
        return collectUntil(encodings, index, encodings.length, dest);
    }

    public static int collectUntil(byte[] encodings, int index, int endIndex, Collection<? super RLPItem> dest) {
        return collect(RLP_STRICT, encodings, index, (count, idx) -> idx < endIndex, dest);
    }

    public static void collectN(byte[] encodings, int index, int n, Collection<? super RLPItem> dest) {
        RLP_STRICT.stream(encodings, index)
                .limit(n)
                .collect(Collectors.toCollection(() -> dest));
    }

    @FunctionalInterface
    public interface BiIntPredicate {
        boolean test(int count, int index);
    }

    /**
     * For gathering sequential items into a collection.
     *
     * @param buffer the buffer containing the encodings
     * @param index the index into the buffer of the first encoding
     * @param predicate the condition under which an item is to be added to the collection
     * @param collection    the collection to which the items will be added
     * @return  the number of items added
     */
    public static int collect(RLPDecoder decoder, byte[] buffer, int index, BiIntPredicate predicate, Collection<? super RLPItem> collection) {
        int count = 0;
        while (predicate.test(count, index)) {
            RLPItem item = decoder.wrap(buffer, index);
            collection.add(item);
            count++;
            index = item.endIndex;
        }
        return count;
    }

    @Test
    public void testExports() {
        final byte[] in = new byte[] { (byte)0x83,1,3,5 };
        RLPItem item = RLP_STRICT.wrapString(in);
        final byte[] out = new byte[] { 0,0,0,0,0 };
        assertEquals(1 + item.dataLength, item.exportData(out, 1));
        assertArrayEquals(new byte[] {0,1,3,5,0}, out);

        assertEquals(1 + item.encodingLength(), item.export(out, 1));
        assertArrayEquals(new byte[] {0,(byte)0x83,1,3,5}, out);

        final byte[] in2 = new byte[] { 0, 0, (byte) 0xc5, 0, 1, 2, 3, (byte) 0xc0 };
        item = RLP_STRICT.wrapList(in2, 2);

        assertEquals(item.dataLength, item.exportData(out, 0));
        assertArrayEquals(new byte[] { 0, 1, 2, 3, (byte) 0xc0 }, out);

        final byte[] out2 = new byte[6];
        assertEquals(item.encodingLength(), item.export(out2, 0));
        assertArrayEquals(new byte[] { (byte) 0xc5, 0, 1, 2, 3, (byte) 0xc0 }, out2);

        final byte[] longString = new byte[58];
        longString[0] = (byte) 0xb8;
        longString[1] = 56;
        longString[50] = 10;
        longString[57] = -3;

        final byte[] out3 = new byte[60];
        assertEquals(2 + longString.length, RLP_STRICT.wrap(longString).export(out3, 2));
        final byte[] expected = new byte[60];
        expected[2] = (byte) 0xb8;
        expected[3] = 56;
        expected[52] = 10;
        expected[59] = -3;
        assertArrayEquals(expected, out3);
    }

    @Test
    public void testOverflow() throws Throwable {
        byte[] bytes = new byte[] { (byte) 0xbf,
                (byte) 0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff,
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        };
        assertThrown(
                IllegalArgumentException.class,
                "element @ index 0 exceeds its container: 9223372036854775807 > 9",
                () -> RLP_STRICT.wrap(bytes)
        );
        bytes[0] = (byte) 0xff;
        assertThrown(
                IllegalArgumentException.class,
                "element @ index 0 exceeds its container: 9223372036854775807 > 9",
                () -> RLP_STRICT.wrap(bytes)
        );
    }
}
