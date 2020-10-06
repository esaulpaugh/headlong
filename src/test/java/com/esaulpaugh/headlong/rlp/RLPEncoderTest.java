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
import com.esaulpaugh.headlong.rlp.util.FloatingPoint;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class RLPEncoderTest {

    @Test
    public void encodeSequentially() {
        Object[] objects = new Object[] {
                new Object[0],
                new byte[0]
        };
        assertArrayEquals(
                new byte[] { (byte)0xc0, (byte)0x80 },
                RLPEncoder.encodeSequentially(objects)
        );
    }

    @Test
    public void encodeAsList() {
        Object[] objects = new Object[] {
                new Object[0],
                new byte[0]
        };

        byte[] expectedRLP = new byte[] { (byte)0xc2, (byte)0xc0, (byte)0x80 };

        assertArrayEquals(expectedRLP, RLPEncoder.encodeAsList(objects));

        ByteBuffer dest = ByteBuffer.allocate(expectedRLP.length);
        RLPEncoder.encodeAsList(objects, dest);
        assertArrayEquals(expectedRLP, dest.array());
    }

    @Test
    public void toList() {

        RLPString item0 = RLPDecoder.RLP_STRICT.wrapString(new byte[] {(byte) 0x81, (byte) 0x80 });
        RLPString item1 = RLPDecoder.RLP_STRICT.wrapString(new byte[] {(byte) 0x7e });
        RLPList item2 = RLPDecoder.RLP_STRICT.wrapList(new byte[] {(byte) 0xc1, (byte) 0x80 });

        RLPList rlpList = RLPEncoder.toList(item0, item1, item2);
        List<RLPItem> elements = rlpList.elements(RLPDecoder.RLP_STRICT);

        assertEquals(3, elements.size());

        assertNotSame(elements.get(0), item0);
        assertNotSame(elements.get(1), item1);
        assertNotSame(elements.get(2), item2);

        assertEquals(elements.get(0), item0);
        assertEquals(elements.get(1), item1);
        assertEquals(elements.get(2), item2);
    }

    @Test
    public void testLongList() {

        final byte[] bytes = new byte[] {
                (byte) 0xf9, (byte) 1,
                (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, (byte) 0xFF, (byte) 0x81, (byte) 0x90, (byte) 0x81, (byte) 0xb6, (byte) '\u230A',
                (byte) 0xb8, 56, 0x09,(byte)0x80,-1,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, -3, -2, 0, 0,
                (byte) 0xf8, 0x38, 0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 36, 74, 0, 0,
                (byte) 0x84, 'c', 'a', 't', 's',
                (byte) 0x84, 'd', 'o', 'g', 's',
                (byte) 0xca, (byte) 0x84, 92, '\r', '\n', '\f', (byte) 0x84, '\u0009', 'o', 'g', 's',
                0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,
                0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,
        };

        RLPList longList = RLPDecoder.RLP_STRICT.wrapList(bytes);

        List<RLPItem> elements = longList.elements(RLPDecoder.RLP_STRICT);

        RLPList rebuilt = RLPEncoder.toList(elements);

//        System.out.println(longList.toString(Strings.HEX));
//        System.out.println(rebuilt.toString(Strings.HEX));

        assertEquals(longList, rebuilt);
    }

    @Test
    public void testDatatypes() {

        final Random rando = TestUtils.seededRandom();

        for (int k = 0; k < 100; k++) {

            final char c = (char) rando.nextInt(128);

            final byte[] buffer = TestUtils.randomBytes(1 + rando.nextInt(90), rando);

            final String str = Strings.encode(buffer, Strings.UTF_8);
            final byte by = (byte) rando.nextInt();
            final short sh = (short) rando.nextInt();

            final int i = rando.nextInt();
            final long l = rando.nextLong();

            rando.nextBytes(buffer);

            final BigInteger signed = new BigInteger(buffer);
            final BigInteger unsigned = BigInteger.valueOf(i < 0 ? i * -1001L : i * 999L);

            final float f = rando.nextFloat();
            final double d = rando.nextDouble();
            final BigDecimal bd = new BigDecimal(BigInteger.ONE, 18);

            final byte[] rlp = RLPEncoder.encodeSequentially(
                    Integers.toBytes((short) c),
                    Strings.decode(str, Strings.UTF_8),
                    Integers.toBytes(by),
                    Integers.toBytes(sh),
                    Integers.toBytes(i),
                    Integers.toBytes(l),
                    signed.toByteArray(),
                    Integers.toBytesUnsigned(unsigned),
                    FloatingPoint.toBytes(f),
                    FloatingPoint.toBytes(d),
                    bd.unscaledValue().toByteArray()
            );

            final Iterator<RLPItem> iter = RLPDecoder.RLP_STRICT.sequenceIterator(rlp);

            final RLPItem charItem = iter.next();
            assertEquals(c, charItem.asChar(false));
            if (charItem.dataLength > 0) {
                assertEquals(c, charItem.asString(Strings.UTF_8).charAt(0));
            }
            assertEquals(str, iter.next().asString(Strings.UTF_8), str);
            assertEquals(by, iter.next().asByte());
            assertEquals(sh, iter.next().asShort(false));

            assertEquals(i, iter.next().asInt(false));
            assertEquals(l, iter.next().asLong());
            assertEquals(signed, iter.next().asBigIntSigned());
            assertEquals(unsigned, iter.next().asBigInt(false));

            assertEquals(f, iter.next().asFloat(false), Double.MIN_NORMAL);
            assertEquals(d, iter.next().asDouble(false), Double.MIN_NORMAL);
            assertEquals(bd, new BigDecimal(iter.next().asBigIntSigned(), bd.scale()));
        }
    }

    @Test
    public void testExceptions() throws Throwable {

        TestUtils.assertThrown(NullPointerException.class, () -> RLPEncoder.encodeSequentially(new byte[0], null, new byte[]{-1}));

        TestUtils.assertThrown(IllegalArgumentException.class, "unsupported object type: java.lang.String", () -> RLPEncoder.encodeSequentially((Object) new String[]{"00"}));

        TestUtils.assertThrown(IllegalArgumentException.class, "unsupported object type: java.lang.String", () -> RLPEncoder.encodeSequentially(new Object[]{new ArrayList<>(), "00"}));
    }

    @Test
    public void testEncodeToByteBuffer() throws Throwable {
        RLPEncoder.encodeSequentially(new HashSet<>(), ByteBuffer.allocate(0));
        RLPEncoder.encodeSequentially(new ArrayList<>(), ByteBuffer.allocate(0));
        RLPEncoder.encodeSequentially(new Object[0], ByteBuffer.allocate(0));
        TestUtils.assertThrown(
                IllegalArgumentException.class,
                "unsupported object type: java.util.HashMap",
                () -> RLPEncoder.encodeSequentially(new HashMap<>(), ByteBuffer.allocate(0))
        );
        TestUtils.assertThrown(
                IllegalArgumentException.class,
                "unsupported object type: java.nio.HeapByteBuffer",
                () -> RLPEncoder.encodeSequentially(new byte[0], ByteBuffer.allocate(0))
        );
        RLPEncoder.encodeSequentially(() -> new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Object next() {
                throw new NoSuchElementException();
            }
        }, ByteBuffer.allocate(0));

        TestUtils.assertThrown(NullPointerException.class, () -> RLPEncoder.encodeSequentially(() -> null, ByteBuffer.allocate(0)));
    }
}
