package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.util.Integers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_LENIENT;
import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.rlp.util.Strings.UTF_8;

public class RLPDecoderTest {

    @Test
    public void strictAndLenient() throws DecodeException {
        byte[] invalidAf = new byte[] {
                (byte)0xc8, (byte)0x80, 0, (byte)0x81, (byte) 0xAA, (byte)0x81, (byte)'\u0080', (byte)0x81, '\u007f', (byte)'\u230A' };

        RLPList list = (RLPList) RLP_STRICT.wrap(invalidAf);

        Throwable t = null;
        try {
            list.elements(RLP_STRICT);
        } catch (DecodeException e) {
            t = e;
        }
        Assert.assertNotNull(t);
        Assert.assertEquals(DecodeException.class, t.getClass());
        Assert.assertEquals("invalid rlp for single byte @ 7", t.getMessage());

        list.elements(RLP_LENIENT);
    }

    @Test
    public void list() throws DecodeException {
        byte[] rlpEncoded = new byte[] {
                (byte) 0xf8, (byte) 148,
                (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, (byte) 0xFF, (byte) 0x81, (byte) 0x90, (byte) 0x81, (byte) 0xb6, (byte) '\u230A',
                (byte) 0xb8, 56, 0x09,(byte)0x80,-1,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, -3, -2, 0, 0,
                (byte) 0xf8, 0x38, 0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 36, 74, 0, 0,
                (byte) 0x84, 'c', 'a', 't', 's',
                (byte) 0x84, 'd', 'o', 'g', 's',
                (byte) 0xca, (byte) 0x84, 92, '\r', '\n', '\f', (byte) 0x84, '\u0009', 'o', 'g', 's',
        };

        RLPList rlpList = (RLPList) RLP_STRICT.wrap(rlpEncoded);
        List<RLPItem> actualList = rlpList.elements(RLP_STRICT);

        Assert.assertEquals(148, rlpList.dataLength);
        Assert.assertEquals(6, actualList.size());
        Assert.assertEquals(10, actualList.get(0).dataLength);
    }

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
            lol = Integers.numBytes(dataLen);
            buffer = new byte[1 + lol + (int) dataLen];
            buffer[0] = (byte) (0xb7 + lol);
            Integers.putLong(dataLen, buffer, 1);
            huge = (RLPString) RLP_STRICT.wrap(buffer);
            data = huge.asString(UTF_8);
            System.out.println(dataLen);
            Assert.assertEquals(dataLen, data.length());
        }
    }

    @Ignore // memory hog, run by itself
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
            lol = Integers.numBytes(dataLen);
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
            huge = (RLPList) RLP_STRICT.wrap(buffer);
            ArrayList<RLPItem> elements = new ArrayList<>(size);
            huge.elements(RLP_STRICT, elements);
            System.out.println("trailing single byte items = " + (buffer.length - i));
            System.out.println("list size = " + size);
            Assert.assertEquals(size, elements.size());
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
                Integer.MAX_VALUE - 13
        };

        final byte elementLeadByte = 0x09;
        final int elementEncodedLen = 1;

        for (long dataLen : dataLengths) {
            System.out.println("dataLen = " + dataLen);
            lol = Integers.numBytes(dataLen);
            System.out.println("length of length = " + lol);
            buffer = new byte[1 + lol + (int) dataLen];
            Arrays.fill(buffer, (byte) 0x09);
            buffer[0] = (byte) (0xf7 + lol);
            Integers.putLong(dataLen, buffer, 1);
            i = 1 + lol;
            final int size = buffer.length - i;

            huge = (RLPList) RLP_STRICT.wrap(buffer);

            int count = 0;
            int j = huge.dataIndex;
            while(j < huge.endIndex) {
                RLPItem element = RLP_STRICT.wrap(buffer, j);
                count++;
                j = element.endIndex;
            }
            System.out.println("size = " + size + ", count = " + count + "\n");
            Assert.assertEquals(size, count);
        }
    }

    private interface CustomRunnable {
        void run() throws Throwable;
    }

    @Test
    public void exceedsContainerShort() throws Throwable {

        final Class<? extends Throwable> clazz = DecodeException.class;

        byte[] a0 = new byte[] { (byte) 0x81 };
        byte[] a1 = new byte[] { (byte) 0xc1 };

        assertThrown(clazz, "@ index 0", () -> RLP_LENIENT.wrap(a0, 0));
        assertThrown(clazz, "@ index 0", () -> RLP_LENIENT.wrap(a1, 0));

        byte[] b0 = new byte[] { (byte) 0xc1, (byte) 0x81 };
        byte[] b1 = new byte[] { (byte) 0xc1, (byte) 0xc1 };

        assertThrown(clazz, "@ index 1", () -> ((RLPList) RLP_LENIENT.wrap(b0, 0)).elements(RLP_STRICT));
        assertThrown(clazz, "@ index 1", () -> ((RLPList) RLP_LENIENT.wrap(b1, 0)).elements(RLP_STRICT));

        byte[] c0 = new byte[] { (byte) 0xc1, (byte) 0x81, (byte) 0x00 };
        byte[] c1 = new byte[] { (byte) 0xc1, (byte) 0xc1, (byte) 0x00 };

        assertThrown(clazz, "@ index 1", () -> ((RLPList) RLP_LENIENT.wrap(c0, 0)).elements(RLP_STRICT));
        assertThrown(clazz, "@ index 1", () -> ((RLPList) RLP_LENIENT.wrap(c1, 0)).elements(RLP_STRICT));
    }

    @Test
    public void exceedsContainerLong() throws Throwable {

        final Class<? extends Throwable> clazz = DecodeException.class;

        byte[] a0 = new byte[57]; a0[0] = (byte) 0xb8; a0[1] = 56;
        byte[] a1 = new byte[57]; a1[0] = (byte) 0xf8; a1[1] = 56;

        assertThrown(clazz, "@ index 0", () -> RLP_LENIENT.wrap(a0, 0));
        assertThrown(clazz, "@ index 0", () -> RLP_LENIENT.wrap(a1, 0));

        byte[] b0 = new byte[58]; b0[0] = (byte) 0xf8; b0[1] = 56; b0[57] = (byte) 0x81;
        byte[] b1 = new byte[58]; b1[0] = (byte) 0xf8; b1[1] = 56; b1[56] = (byte) 0xc2;

        assertThrown(clazz, "@ index 57", () -> ((RLPList) RLP_LENIENT.wrap(b0, 0)).elements(RLP_STRICT));
        assertThrown(clazz, "@ index 56", () -> ((RLPList) RLP_LENIENT.wrap(b1, 0)).elements(RLP_STRICT));

        byte[] c0 = new byte[59]; c0[0] = (byte) 0xf8; c0[1] = 56; c0[57] = (byte) 0x81;
        byte[] c1 = new byte[59]; c1[0] = (byte) 0xf8; c1[1] = 56; c1[56] = (byte) 0xc2;

        assertThrown(clazz, "@ index 57", () -> ((RLPList) RLP_LENIENT.wrap(c0, 0)).elements(RLP_STRICT));
        assertThrown(clazz, "@ index 56", () -> ((RLPList) RLP_LENIENT.wrap(c1, 0)).elements(RLP_STRICT));
    }

    private static void assertThrown(Class<? extends Throwable> clazz, String substr, CustomRunnable r) throws Throwable {
        try {
            r.run();
        } catch (Throwable t) {
            if(clazz.isAssignableFrom(t.getClass()) && t.getMessage().contains(substr)) {
                return;
            }
            throw t;
        }
        throw new AssertionError("no " + clazz.getName() + " thrown");
    }

    @Test
    public void booleans() throws DecodeException {
        byte[] burma17 = new byte[] { (byte) 0xc5, (byte) 0x82, (byte) 0x10, (byte) 0x10, (byte) 0xc0 };

        RLPItem nonEmpty = RLP_LENIENT.wrap( burma17, 1);
        Assert.assertTrue(nonEmpty.asBoolean());

        RLPItem empty = RLP_LENIENT.wrap( burma17, 4);
        Assert.assertFalse(empty.asBoolean());
    }

    @Test
    public void chars() throws DecodeException {
        byte[] burma17 = new byte[] { (byte) 0xc5, (byte) 0x82, (byte) 0x10, (byte) 0x10, (byte) 0xc0 };

        RLPItem nonEmpty = RLP_LENIENT.wrap( burma17, 1);
        Assert.assertEquals(Character.valueOf('\u1010'), Character.valueOf(nonEmpty.asChar()));

        RLPItem empty = RLP_LENIENT.wrap( burma17, 4);
        Assert.assertEquals(Character.valueOf('\0'), Character.valueOf(empty.asChar()));
    }
}
