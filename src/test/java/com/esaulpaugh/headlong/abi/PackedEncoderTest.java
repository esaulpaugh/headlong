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
package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.abi.util.BizarroInts;
import com.esaulpaugh.headlong.abi.util.Uint;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PackedEncoderTest {

    @Test
    public void testOverwrite() {
        byte[] bytes = new byte[6];
        Arrays.fill(bytes, (byte) -1);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        TupleType tt = TupleType.parse("(uint40)");
        tt.encodePacked(Tuple.of(63L), bb);

        assertEquals("000000003fff", Strings.encode(bb));
    }

    @Test
    public void multipleDynamic() throws Throwable {
        TupleType tupleType = TupleType.parse("(int120[],(int96,ufixed256x47)[])");
        Tuple test = Tuple.of(new BigInteger[] { BigInteger.TEN }, new Tuple[] { Tuple.of(BigInteger.TEN, new BigDecimal(BigInteger.TEN, 47)) });
        ByteBuffer bb = tupleType.encodePacked(test);
        TestUtils.assertThrown(IllegalArgumentException.class, "multiple dynamic elements", () -> tupleType.decodePacked(bb.array()));

        TupleType _tt = TupleType.parse("(int144[][1])");
        Tuple _test = Tuple.singleton(new BigInteger[][] { new BigInteger[] { } });
        _tt.validate(_test);
        ByteBuffer _bb = _tt.encodePacked(_test);
        TestUtils.assertThrown(IllegalArgumentException.class, "array of dynamic elements", () -> _tt.decodePacked(_bb.array()));
    }

    @Test
    public void testTupleArray() {
        TupleType tupleType = TupleType.parse("((bool)[])");

        Tuple test = Tuple.singleton((new Tuple[] { Tuple.of(true), Tuple.of(false), Tuple.of(true) }));

        ByteBuffer bb = tupleType.encodePacked(test);

        assertEquals("010001", Strings.encode(bb));

        assertEquals(test, tupleType.decodePacked(bb.array()));
    }

    @Test
    public void testEmptyTupleArray() throws Throwable {
        TupleType tupleType = TupleType.parse("(()[])");

        Tuple test = Tuple.singleton((new Tuple[0]));

        ByteBuffer bb = tupleType.encodePacked(test);

        assertEquals("", Strings.encode(bb));

        TestUtils.assertThrown(IllegalArgumentException.class, "can't decode dynamic number of zero-length elements", () -> tupleType.decodePacked(bb.array()));
    }

    @Test
    public void testHard() {
        TupleType tupleType = TupleType.parse("((bytes,(uint8[2][2])))");

        Tuple test = Tuple.of(Tuple.of(new byte[0], Tuple.singleton(new int[][] { new int[] {1,2}, new int[] {3,4} })));

        ByteBuffer bb = tupleType.encodePacked(test);

        assertEquals("01020304", Strings.encode(bb));

        byte[] packed = Strings.decode("01020304");

        ByteBuffer buf = ByteBuffer.allocate(100);
        buf.put(new byte[17]);
        buf.put(packed);
        buf.put((byte) 0xff);

//        assertEquals(test, PackedDecoder.decode(tupleType, buf.array(), 17, 17 + packed.length));
    }

    @Test
    public void testStaticTupleInsideDynamic() {
        TupleType tupleType = TupleType.parse("((bytes1),bytes)");

        Tuple test = Tuple.of(Tuple.singleton(new byte[] { -1 }), new byte[] { -2, -3 });

        ByteBuffer bb = tupleType.encodePacked(test);

        assertEquals("fffefd", Strings.encode(bb));

        assertEquals(test, tupleType.decodePacked(Strings.decode("fffefd")));
    }

    @Test
    public void testDynamicInnerTuple() {
        TupleType tupleType = TupleType.parse("((bytes1,bytes,bytes1),bytes1)");

        Tuple test = Tuple.of(Tuple.of(new byte[] { 0 }, new byte[] { -1, -2 }, new byte[] { 1 }), new byte[] { -3 });

        ByteBuffer bb = tupleType.encodePacked(test);

        assertEquals("00fffe01fd", Strings.encode(bb));

        assertEquals(test, tupleType.decodePacked(Strings.decode("00fffe01fd")));
    }

    @Test
    public void testPacked() {
        TupleType tupleType = TupleType.parse("(int16,bytes1,uint16,string)");

        Tuple test = Tuple.of(-1, new byte[] { 0x42 }, 0x03, "Hello, world!");

        final int packedLen = tupleType.byteLengthPacked(test);
        final byte[] in = Strings.decode("ffff42000348656c6c6f2c20776f726c6421");
        assertEquals(18, in.length);
        assertEquals(in.length, packedLen);

        ByteBuffer bb = ByteBuffer.allocate(packedLen);

        tupleType.encodePacked(test, bb);
        assertEquals(in.length, bb.position());

        assertEquals(packedLen, bb.position());

        assertEquals("ffff42000348656c6c6f2c20776f726c6421", Strings.encode(bb));

        // ---------------------------

        Function function = new Function(tupleType.canonicalType);

        byte[] abi = function.getInputs().encode(test).array();

        String hex =
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff" +
                "4200000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000003" +
                "0000000000000000000000000000000000000000000000000000000000000080" +
                "000000000000000000000000000000000000000000000000000000000000000d" +
                "48656c6c6f2c20776f726c642100000000000000000000000000000000000000";

        assertEquals(hex, Strings.encode(abi));

        byte[] call = new byte[function.measureCallLength(test)];
        assertEquals(Function.SELECTOR_LEN + abi.length, call.length);

        System.arraycopy(function.selector(), 0, call, 0, Function.SELECTOR_LEN);
        System.arraycopy(abi, 0, call, Function.SELECTOR_LEN, abi.length);

        TupleType tt = TupleType.parse(tupleType.canonicalType);
        Tuple args = function.decodeCall(call);
        assertEquals("ffff42000348656c6c6f2c20776f726c6421", Strings.encode(tt.encodePacked(args)));

    }

    @Test
    public void testTest() {
        TupleType tupleType = TupleType.parse("(int24,bool,bool)");

        Tuple values = Tuple.of(-2, true, false);

        ByteBuffer bb = tupleType.encodePacked(values);
        assertEquals(bb.capacity(), bb.position());

        assertEquals("fffffe0100", Strings.encode(bb));

        assertEquals(values, tupleType.decodePacked(bb.array()));
    }

    @Test
    public void testDecodeA() {
        TupleType tupleType = TupleType.parse("(int16[2],int24[3],bytes,uint32[3],bool[3],uint64,int72)");

        Tuple test = Tuple.of(new int[] { 3, 5 }, new int[] { 7, 8, 9 }, new byte[0], new long[] { 9L, 0L, 0xFFFFFFFFL }, new boolean[] { true, false, true }, BigInteger.valueOf(6L), BigInteger.valueOf(-1L));

        ByteBuffer bb = tupleType.encodePacked(test);
        assertEquals(bb.capacity(), bb.position());

        assertEquals("000300050000070000080000090000000900000000ffffffff0100010000000000000006ffffffffffffffffff", Strings.encode(bb));

        assertEquals(test, tupleType.decodePacked(bb.array()));
    }

    @Test
    public void testDecodeB() {
        TupleType tupleType = TupleType.parse("(uint64[],int)");

        Tuple values = Tuple.of(new BigInteger[] { BigInteger.ONE, BigInteger.valueOf(2L), BigInteger.valueOf(3L), BigInteger.valueOf(4L) }, BigInteger.ONE);

        ByteBuffer bb = tupleType.encodePacked(values);
        byte[] packedArray = bb.array();
        assertEquals(packedArray.length, bb.position());

        assertEquals("00000000000000010000000000000002000000000000000300000000000000040000000000000000000000000000000000000000000000000000000000000001", Strings.encode(bb));

        assertEquals(values, tupleType.decodePacked(packedArray));
    }

    @Test
    public void testDecodeC() {
        TupleType tupleType = TupleType.parse("(bool,bool[],bool[2])");

        Tuple values = Tuple.of(true, new boolean[] { true, true, true },  new boolean[] { true, false });

        ByteBuffer bb = tupleType.encodePacked(values);
        assertEquals(bb.capacity(), bb.position());

        assertEquals("010101010100", Strings.encode(bb));

        assertEquals(values, tupleType.decodePacked(bb.array()));
    }

    @Test
    public void testUint24() {
        TupleType tupleType = TupleType.parse("(uint24)");

        Tuple values = Tuple.of((int) new Uint(24).toUnsignedLong(Integer.MIN_VALUE / 256));

        ByteBuffer bb = tupleType.encodePacked(values);
        assertEquals(bb.capacity(), bb.position());

        assertEquals("800000", Strings.encode(bb));

        assertEquals(values, tupleType.decodePacked(bb.array()));
    }

    @Test
    public void testBigDecimalArr() {
        TupleType tupleType = TupleType.parse("(ufixed48x21[])");

        BigDecimal val = new BigDecimal(new Uint(24).toUnsigned(Integer.MIN_VALUE / 256), 21);
        Tuple values = Tuple.singleton(new BigDecimal[] { val, val });

        ByteBuffer bb = tupleType.encodePacked(values);
        assertEquals(bb.capacity(), bb.position());

        assertEquals("000000800000000000800000", Strings.encode(bb));

        assertEquals(values, tupleType.decodePacked(bb.array()));
    }

    @Test
    public void testSignExtendInt() {
        byte[] bytes = Strings.decode("8FFFFF");
        int expected = BizarroInts.getInt(bytes, 0, 3);
        int result = PackedDecoder.decodeBigInteger(bytes, 0, 3).intValue();
        assertTrue(result < 0);
        assertEquals(expected, result);
    }

    @Test
    public void testSignExtendLong() {
        byte[] bytes = Strings.decode("8FFFFFFFFF");
        long expectedL = BizarroInts.getLong(bytes, 0, 5);
        long resultL = PackedDecoder.decodeBigInteger(bytes, 0, 5).longValue();
        assertTrue(resultL < 0);
        assertEquals(expectedL, resultL);
    }

    @Test
    public void testTupleLenPacked() {
        TupleType tt = TupleType.parse("(string[2][2])");
        Tuple vals = Tuple.singleton(new String[][] { new String[] { "a", "bc" }, new String[] { "defg", "hijklmno" }});
        int len = tt.byteLengthPacked(vals);
        assertEquals(15, len);
        assertEquals("6162636465666768696a6b6c6d6e6f", Strings.encode(tt.encodePacked(vals).array()));
    }

    @Test
    public void testTrailingBytes() throws Throwable {
        assertThrown(IllegalArgumentException.class,
                "unconsumed bytes: 4 remaining",
                () -> TupleType.parse("(uint16)").decodePacked(FastHex.decode("fffe00112233")));
    }
}
