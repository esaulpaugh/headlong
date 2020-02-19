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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static com.esaulpaugh.headlong.TestUtils.CustomRunnable;
import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_LENIENT;
import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
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

        byte[] bytes = new byte[10];

        r.nextBytes(bytes);
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
                RLPEncoder.encode(new byte[] { 0, 0, -50, 90, 12, 4, 13, 21, 89, -120 })
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
    public void exhaustiveFuzz() throws InterruptedException {
        ExecutorService executorService = Executors.newWorkStealingPool();
        ExhaustiveFuzzTask[] tasks = new ExhaustiveFuzzTask[256];
        for (int i = 0; i < tasks.length; i++) {
            System.out.print(i + " -> ");
            tasks[i] = new ExhaustiveFuzzTask(new byte[] { (byte) i, 0, 0, 0 });
            executorService.submit(tasks[i]);
        }
        long valid = 0, invalid = 0;
        executorService.shutdown();
        executorService.awaitTermination(12L, TimeUnit.MINUTES);
        for (ExhaustiveFuzzTask task : tasks) {
            valid += task.valid;
            invalid += task.invalid;
        }
        System.out.println(valid + " / " + (valid + invalid) + " (" + invalid + " invalid)");
    }

    private static class ExhaustiveFuzzTask implements Runnable {

        private final byte[] four;
        private long valid, invalid;
        private final String tag;

        private ExhaustiveFuzzTask(byte[] four) {
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
        RLPDecoder decoder = RLP_STRICT;
        Random r = TestUtils.seededRandom();
        byte[] buffer = new byte[56];
        int valid = 0, invalid = 0;
        for (int i = 0; i < 500_000; i++) {
            r.nextBytes(buffer);
            try {
                RLPItem item = decoder.wrap(buffer);
                valid++;
                String first = Strings.encode(buffer[0]);
                if (item.asBoolean()) {
                    switch (first) {
                    case "c0":
                    case "80":
                    case "00":
                        throw new RuntimeException(Strings.encode(buffer));
                    default:
                    }
                } else {
                    switch (first) {
                    case "c0":
                    case "80":
                    case "00":
                        break;
                    default:
                        throw new RuntimeException(Strings.encode(buffer));
                    }
                    if (item.asChar(false) != '\u0000') {
                        throw new RuntimeException(Strings.encode(buffer));
                    }
                }
            } catch (IllegalArgumentException iae) {
                invalid++;
                String first = Strings.encode(buffer[0]);
                switch (first) {
                case "00":
                    if(!decoder.lenient) {
                        break;
                    }
                case "81":
                    if(!decoder.lenient || buffer[1] >= 0x00) {
                        break;
                    }
                    throw new RuntimeException(Strings.encode(buffer));
                case "b8": case "b9": case "ba": case "bb": case "bc": case "bd": case "be": case "bf":
                case "f8": case "f9": case "fa": case "fb": case "fc": case "fd": case "fe": case "ff":
                    break;
                default:
                    throw new RuntimeException(Strings.encode(buffer));
                }
            }
        }
        System.out.println("valid: " + valid + " / " + (valid + invalid) + " (" + invalid + " invalid)");
    }

    @Test
    public void testListIterable() throws Throwable {
        RLPList rlpList = RLP_STRICT.wrapList(LONG_LIST_BYTES);
        for(RLPItem item : rlpList) {
            System.out.println(item.type() + " " + item.dataLength + " " + item.asString(Strings.HEX));
        }

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
        assertEquals(rlpList, rlpList.duplicate(RLP_STRICT));
        RLPString rlpString = RLP_STRICT.wrapString((byte) 0x00);
        assertEquals(rlpString, rlpString.duplicate(RLP_STRICT));
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
        int lol;
        byte[] buffer;
        RLPList huge;
        int i;

        long[] dataLengths = new long[] {
                255,
                Short.MAX_VALUE,
                Short.MAX_VALUE * 50,
                Short.MAX_VALUE * 1000,
                Integer.MAX_VALUE / 13 // try this if using 32-bit java and getting OOME
//                Integer.MAX_VALUE - 13
        };

        for (long dataLen : dataLengths) {
            System.out.println("dataLen = " + dataLen);
            lol = Integers.len(dataLen);
            System.out.println("length of length = " + lol);
            buffer = new byte[1 + lol + (int) dataLen];
            Arrays.fill(buffer, (byte) 0x09);
            buffer[0] = (byte) (0xf7 + lol);
            Integers.putLong(dataLen, buffer, 1);
            i = 1 + lol;
            final int size = buffer.length - i;

            huge = RLP_STRICT.wrapList(buffer);

            int count = 0;
            int j = huge.dataIndex;
            while(j < huge.endIndex) {
                RLPItem element = RLP_STRICT.wrap(buffer, j);
                count++;
                j = element.endIndex;
            }
            System.out.println("size = " + size + ", count = " + count + "\n");
            assertEquals(size, count);
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
            burma17[i] = RLPEncoder.encode((byte) i);
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

    private static final BiPredicate<Integer, Integer> UNTIL_COUNT_FIVE = (count, index) -> count < 5;
    private static final BiPredicate<Integer, Integer> UNTIL_INDEX_SEVEN = (count, index) -> index < 7;

    @Test
    public void collect() {
        byte[] rlp = new byte[9];
        for (int i = 0; i < rlp.length; i++) {
            rlp[i] = (byte) i;
        }
        Set<RLPItem> hashSet = new HashSet<>();
        int n = RLP_STRICT.collect(rlp, 0, UNTIL_COUNT_FIVE, hashSet);
        assertEquals(5, n);
        assertEquals(5, hashSet.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(hashSet.contains(RLP_STRICT.wrap((byte) i)));
        }

        hashSet = new HashSet<>();
        n = RLP_STRICT.collect(rlp, 0, UNTIL_INDEX_SEVEN, hashSet);
        assertEquals(7, n);
        assertEquals(7, hashSet.size());
        for (int i = 0; i < 7; i++) {
            assertTrue(hashSet.contains(RLP_STRICT.wrap((byte) i)));
        }

        hashSet = new HashSet<>();
        RLP_STRICT.collectN(rlp, 4, 3, hashSet);
        assertEquals(3, hashSet.size());
        for (int i = 4; i < 7; i++) {
            assertTrue(hashSet.contains(RLP_STRICT.wrap((byte) i)));
        }

        hashSet = new HashSet<>();
        n = RLP_STRICT.collectBefore(rlp, 1, 6, hashSet);
        assertEquals(5, n);
        assertEquals(5, hashSet.size());
        for (int i = 1; i < 6; i++) {
            assertTrue(hashSet.contains(RLP_STRICT.wrap((byte) i)));
        }

        hashSet = new HashSet<>();
        n = RLP_STRICT.collectAll(rlp, 0, hashSet);
        assertEquals(9, n);
        for (int i = 0; i < 9; i++) {
            assertTrue(hashSet.contains(RLP_STRICT.wrap((byte) i)));
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

        byte[] list = RLPEncoder.encodeAsList(a, b, c);
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
        ArrayList<RLPItem> collection = RLP_STRICT.stream(RLPStreamTest.RLP_BYTES)
                .collect();

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
}
