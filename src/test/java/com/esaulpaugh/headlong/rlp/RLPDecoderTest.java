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
import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.exception.UnrecoverableDecodeException;
import com.esaulpaugh.headlong.rlp.util.Integers;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiPredicate;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_LENIENT;
import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;
import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.TestUtils.CustomRunnable;
import static org.junit.jupiter.api.Assertions.*;

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
    public void testListIterable() throws Throwable {
        RLPList rlpList = RLP_STRICT.wrapList(LONG_LIST_BYTES);
        for(RLPItem item : rlpList) {
            System.out.println(item.asString(Strings.HEX));
        }

        byte[] copy = Arrays.copyOf(LONG_LIST_BYTES, LONG_LIST_BYTES.length);

        copy[copy.length - 11]++;

        RLPList brokenList = RLP_STRICT.wrapList(copy);

        assertThrown(NoSuchElementException.class, "element @ index 139 exceeds its container: 151 > 150", () -> {
            for(RLPItem item : brokenList) {
                System.out.println(item.asString(Strings.HEX));
            }
        });
    }

    @Test
    public void duplicate() throws DecodeException {
        RLPList rlpList = RLP_STRICT.wrapList(LONG_LIST_BYTES);
        assertEquals(rlpList, rlpList.duplicate(RLP_STRICT));
        RLPString rlpString = RLP_STRICT.wrapString((byte) 0x00);
        assertEquals(rlpString, rlpString.duplicate(RLP_STRICT));
    }

    @Test
    public void strictAndLenient() throws Throwable {
        byte[] invalidAf = new byte[] {
                (byte)0xc8, (byte)0x80, 0, (byte)0x81, (byte) 0xAA, (byte)0x81, (byte)'\u0080', (byte)0x81, '\u007f', (byte)'\u230A' };

        RLPList list = RLP_STRICT.wrapList(invalidAf);

        TestUtils.assertThrown(
                UnrecoverableDecodeException.class,
                "invalid rlp for single byte @ 7",
                () -> list.elements(RLP_STRICT)
        );

        list.elements(RLP_LENIENT);
    }

    @Test
    public void list() throws DecodeException {
        RLPList rlpList = RLP_STRICT.wrapList(LONG_LIST_BYTES);
        List<RLPItem> actualList = rlpList.elements(RLP_STRICT);

        assertEquals(148, rlpList.dataLength);
        assertEquals(6, actualList.size());
        assertEquals(10, actualList.get(0).dataLength);
    }

    @Disabled("can cause OutOfMemoryError")
    @Test
    public void hugeStrings() throws DecodeException {
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
            huge = (RLPString) RLP_STRICT.wrap(buffer);
            data = huge.asString(UTF_8);
            System.out.println(dataLen);
            assertEquals(dataLen, data.length());
        }
    }

    @Disabled("can cause OutOfMemoryError")
    @Test
    public void hugeListsHighMem() throws DecodeException {

        System.out.println(Runtime.getRuntime().maxMemory());

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        System.out.println(memoryBean.getHeapMemoryUsage().getMax());

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
    public void hugeListsLowMem() throws DecodeException {
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

        final Class<? extends Throwable> clazz = DecodeException.class;

        byte[] a0 = new byte[] { (byte) 0x81 };
        byte[] a1 = new byte[] { (byte) 0xc1 };

        assertThrown(clazz, "@ index 0", wrapLenient(a0));
        assertThrown(clazz, "@ index 0", wrapLenient(a1));

        byte[] b0 = new byte[] { (byte) 0xc1, (byte) 0x81 };
        byte[] b1 = new byte[] { (byte) 0xc1, (byte) 0xc1 };

        assertThrown(clazz, "@ index 1", decodeList(b0));
        assertThrown(clazz, "@ index 1", decodeList(b1));

        byte[] c0 = new byte[] { (byte) 0xc1, (byte) 0x81, (byte) 0x00 };
        byte[] c1 = new byte[] { (byte) 0xc1, (byte) 0xc1, (byte) 0x00 };

        assertThrown(clazz, "@ index 1", decodeList(c0));
        assertThrown(clazz, "@ index 1", decodeList(c1));
    }

    @Test
    public void exceedsContainerLong() throws Throwable {

        final Class<? extends Throwable> clazz = DecodeException.class;

        byte[] a0 = new byte[57]; a0[0] = (byte) 0xb8; a0[1] = 56;
        byte[] a1 = new byte[57]; a1[0] = (byte) 0xf8; a1[1] = 56;

        assertThrown(clazz, "@ index 0", wrapLenient(a0));
        assertThrown(clazz, "@ index 0", wrapLenient(a1));

        byte[] b0 = new byte[58]; b0[0] = (byte) 0xf8; b0[1] = 56; b0[57] = (byte) 0x81;
        byte[] b1 = new byte[58]; b1[0] = (byte) 0xf8; b1[1] = 56; b1[56] = (byte) 0xc2;

        assertThrown(clazz, "@ index 57", decodeList(b0));
        assertThrown(clazz, "@ index 56", decodeList(b1));

        byte[] c0 = new byte[59]; c0[0] = (byte) 0xf8; c0[1] = 56; c0[57] = (byte) 0x81;
        byte[] c1 = new byte[59]; c1[0] = (byte) 0xf8; c1[1] = 56; c1[56] = (byte) 0xc2;

        assertThrown(clazz, "@ index 57", decodeList(c0));
        assertThrown(clazz, "@ index 56", decodeList(c1));
    }

    @Test
    public void booleans() throws DecodeException {
        byte[] burma17 = new byte[] { (byte) 0xc5, (byte) 0x82, (byte) 0x10, (byte) 0x10, (byte) 0xc0 };

        RLPItem nonEmpty = RLP_LENIENT.wrap( burma17, 1);
        assertTrue(nonEmpty.asBoolean());

        RLPItem empty = RLP_LENIENT.wrap( burma17, 4);
        assertFalse(empty.asBoolean());
    }

    @Test
    public void chars() throws DecodeException {
        byte[] burma17 = new byte[] { (byte) 0xc5, (byte) 0x82, (byte) 0x10, (byte) 0x10, (byte) 0xc0 };

        RLPItem nonEmpty = RLP_LENIENT.wrap( burma17, 1);
        assertEquals(Character.valueOf('\u1010'), Character.valueOf(nonEmpty.asChar()));

        RLPItem empty = RLP_LENIENT.wrap( burma17, 4);
        assertEquals(Character.valueOf('\0'), Character.valueOf(empty.asChar()));
    }

    private static final BiPredicate<Integer, Integer> UNTIL_COUNT_FIVE = (count, index) -> count < 5;
    private static final BiPredicate<Integer, Integer> UNTIL_INDEX_SEVEN = (count, index) -> index < 7;

    @Test
    public void collect() throws DecodeException {
        byte[] rlp = new byte[9];
        for (int i = 0; i < rlp.length; i++) {
            rlp[i] = (byte) i;
        }
        Set<RLPItem> hashSet = new HashSet<>();
        int n = RLP_STRICT.collect(0, rlp, UNTIL_COUNT_FIVE, hashSet);
        assertEquals(5, n);
        assertEquals(5, hashSet.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(hashSet.contains(RLP_STRICT.wrap((byte) i)));
        }

        hashSet = new HashSet<>();
        n = RLP_STRICT.collect(0, rlp, UNTIL_INDEX_SEVEN, hashSet);
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
        n = RLP_STRICT.collectBefore(1, rlp, 6, hashSet);
        assertEquals(5, n);
        assertEquals(5, hashSet.size());
        for (int i = 1; i < 6; i++) {
            assertTrue(hashSet.contains(RLP_STRICT.wrap((byte) i)));
        }

        hashSet = new HashSet<>();
        n = RLP_STRICT.collectAll(0, rlp, hashSet);
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

        assertThrown(DecodeException.class, "found: -1", wrapLenient(alpha));

        byte[] beta = new byte[] {
                (byte) 0xbf,
                (byte) 0x80, 0, 0, 0, 0, 0, 0, 0 };

        assertThrown(DecodeException.class, "found: -9223372036854775808", wrapLenient(beta));
    }

    @Test
    public void iterators() throws Throwable {
        byte[] a = new byte[] { 1 };
        byte[] b = new byte[] { 2 };
        byte[] c = new byte[0];

        byte[] list = RLPEncoder.encodeAsList(a, b, c);
        RLPListIterator listIter = RLP_STRICT.listIterator(list);

        assertTrue(listIter.hasNext());
        assertArrayEquals(a, listIter.next().asBytes());
        assertTrue(listIter.hasNext());
        assertArrayEquals(b, listIter.next().asBytes());
        assertTrue(listIter.hasNext());
        assertArrayEquals(c, listIter.next().asBytes());

        assertFalse(listIter.hasNext());
        assertThrown(NoSuchElementException.class, listIter::next);

        byte[] sequence = RLPEncoder.encodeSequentially(c, a, b);
        RLPIterator seqIter = RLP_STRICT.sequenceIterator(sequence);

        assertTrue(seqIter.hasNext());
        assertArrayEquals(c, seqIter.next().asBytes());
        assertTrue(seqIter.hasNext());
        assertArrayEquals(a, seqIter.next().asBytes());
        assertTrue(seqIter.hasNext());
        assertArrayEquals(b, seqIter.next().asBytes());

        assertFalse(seqIter.hasNext());
        assertThrown(NoSuchElementException.class, seqIter::next);
    }

    private static CustomRunnable wrapLenient(final byte[] rlp) {
        return () -> RLP_LENIENT.wrap(rlp, 0);
    }

    private static CustomRunnable decodeList(final byte[] rlp) {
        return () -> RLP_LENIENT.wrapList(rlp, 0).elements(RLP_STRICT);
    }
}
