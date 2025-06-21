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
import com.esaulpaugh.headlong.util.FloatingPoint;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
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
                RLPEncoder.sequence(objects)
        );
    }

    @Test
    public void encodeAsList() {
        Object[] objects = new Object[] {
                new Object[0],
                new byte[0]
        };

        byte[] expectedRLP = new byte[] { (byte)0xc2, (byte)0xc0, (byte)0x80 };

        assertArrayEquals(expectedRLP, RLPEncoder.list(objects));

        ByteBuffer dest = ByteBuffer.allocate(expectedRLP.length);
        RLPEncoder.putList(Arrays.asList(objects), dest);
        assertArrayEquals(expectedRLP, dest.array());
    }

    @Test
    public void toList() throws Throwable {

        assertThrown(ClassCastException.class, () -> RLPDecoder.RLP_LENIENT.wrapList(new byte[] {(byte) 0x81, (byte) 0x80 }));

        RLPString item0 = RLPDecoder.RLP_STRICT.wrapString(new byte[] {(byte) 0x81, (byte) 0x80 });
        RLPString item1 = RLPDecoder.RLP_STRICT.wrapString(new byte[] {(byte) 0x7e });
        RLPList item2 = RLPDecoder.RLP_STRICT.wrapList(new byte[] {(byte) 0xc1, (byte) 0x80 });

        RLPList rlpList = RLPList.wrap(item0, item1, item2);
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
        final byte[] utf8 = "« Dans le compte final, jamais n’aurais été la vache, » pensa-t-il. « Jamais ne serais la vache. » Ni miette, ni fête, ni boisson maléfique ne l’enlèveraient de ses travaux.".getBytes(StandardCharsets.UTF_8);
        final String note = "(\n" +
                "  [\n" +
                "    [ [ '', '00', 'ff', '90', 'b6', '0a' ] ],\n" +
                "    '" + Strings.encode(utf8) + "',\n" +
                "    [\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '24',\n" +
                "      '4a',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '00',\n" +
                "      '24',\n" +
                "      '4a',\n" +
                "      '00',\n" +
                "      '00'\n" +
                "    ],\n" +
                "    '63617473',\n" +
                "    '646f6773',\n" +
                "    [ '5c0d0a0c', '096f6773' ],\n" +
                "    '22416c6d696768747920616e64206d6f7374206d6572636966756c204661746865722c2077652068756d626c79206265736565636820546865652c206f662054687920677265617420676f6f646e6573732c20746f20726573747261696e20746865736520696d6d6f646572617465207261696e73207769746820776869636820776520686176652068616420746f20636f6e74656e642e204772616e742075732066616972207765617468657220666f7220426174746c652e2047726163696f75736c7920686561726b656e20746f20757320617320736f6c64696572732077686f2063616c6c205468656520746861742c2061726d656420776974682054687920706f7765722c207765206d617920616476616e63652066726f6d20766963746f727920746f20766963746f72792c20616e6420637275736820746865206f707072657373696f6e20616e64207769636b65646e657373206f66206f757220656e656d6965732c20616e642065737461626c69736820546879206a75737469636520616d6f6e67206d656e20616e64206e6174696f6e732e20416d656e2e22'\n" +
                "  ]\n" +
                ")";

        final List<Object> raw = Notation.parse(note);
        final byte[] enc = RLPEncoder.sequence(raw);
        System.out.println(Strings.encode(enc));

        final RLPList orig = RLPDecoder.RLP_STRICT.wrapList(enc);
        final RLPList rebuilt = RLPList.wrap(orig.elements(RLPDecoder.RLP_STRICT));

        assertEquals(rebuilt.toString(), Notation.forObjects(Notation.parse(rebuilt.toString())).toString());
        assertEquals(orig.encodingString(Strings.HEX), rebuilt.encodingString(Strings.HEX));
        assertEquals(orig, rebuilt);
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

            byte[] skippedItem = new byte[rando.nextInt(75)];

            final byte[] rlp = RLPEncoder.sequence(
                    skippedItem,
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

            final Iterator<RLPItem> iter = RLPDecoder.RLP_STRICT.sequenceIterator(rlp, RLPDecoder.RLP_STRICT.wrap(rlp).endIndex);

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
            RLPItem bigIntItem = iter.next();
            assertEquals(unsigned, bigIntItem.asBigInt());
            assertEquals(unsigned, bigIntItem.asBigInt(false));
            assertEquals(unsigned, bigIntItem.asBigInt(true));

            assertEquals(f, iter.next().asFloat(false), Double.MIN_NORMAL);
            assertEquals(d, iter.next().asDouble(false), Double.MIN_NORMAL);
            assertEquals(bd, new BigDecimal(iter.next().asBigIntSigned(), bd.scale()));
        }
    }

    @Test
    public void testExceptions() throws Throwable {

        TestUtils.assertThrown(IllegalArgumentException.class, "unsupported object type. expected instanceof byte[], Iterable, or Object[]", () -> RLPEncoder.sequence(new byte[0], null, new byte[]{-1}));

        TestUtils.assertThrown(IllegalArgumentException.class, "unsupported object type. expected instanceof byte[], Iterable, or Object[]", () -> RLPEncoder.sequence((Object) new String[]{"00"}));

        TestUtils.assertThrown(IllegalArgumentException.class, "unsupported object type. expected instanceof byte[], Iterable, or Object[]", () -> RLPEncoder.sequence(new Object[]{new ArrayList<>(), "00"}));
    }

    @Test
    public void testEncodeToByteBuffer() throws Throwable {
        RLPEncoder.putSequence(new HashSet<>(), ByteBuffer.allocate(0));
        RLPEncoder.putSequence(new ArrayList<>(), ByteBuffer.allocate(0));
        RLPEncoder.putSequence(Collections.emptyList(), ByteBuffer.allocate(0));
        TestUtils.assertThrown(
                IllegalArgumentException.class,
                "unsupported object type. expected instanceof byte[], Iterable, or Object[]",
                () -> RLPEncoder.sequence(new HashMap<>(), ByteBuffer.allocate(0))
        );
        TestUtils.assertThrown(
                IllegalArgumentException.class,
                "unsupported object type. expected instanceof byte[], Iterable, or Object[]",
                () -> RLPEncoder.sequence(new byte[0], ByteBuffer.allocate(0))
        );
        RLPEncoder.putSequence(() -> new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Object next() {
                throw new NoSuchElementException();
            }
        }, ByteBuffer.allocate(0));

        TestUtils.assertThrown(NullPointerException.class, () -> RLPEncoder.putSequence(() -> null, ByteBuffer.allocate(0)));

        byte[] dest = new byte[6];
        int idx = RLPEncoder.putSequence(Collections.singletonList(new byte[]{0, 1, 2}), dest, 2);
        assertArrayEquals(new byte[] { 0, 0, (byte) 0x83, 0, 1, 2 }, dest);
        assertEquals(dest.length, idx);
    }
}
