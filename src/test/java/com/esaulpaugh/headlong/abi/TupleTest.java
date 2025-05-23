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
import com.esaulpaugh.headlong.util.Strings;
import com.joemelsha.crypto.hash.Keccak;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.TestUtils.uniformBigInteger;
import static com.esaulpaugh.headlong.TestUtils.uniformLong;
import static com.esaulpaugh.headlong.TestUtils.wildBigInteger;
import static com.esaulpaugh.headlong.TestUtils.wildLong;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TupleTest {

    @Test
    public void metaTest1() throws InterruptedException, ExecutionException, TimeoutException {
        final int parallelism = 24;
        final boolean unsigned = true;

        for (int bitLen = 0; bitLen < 22; bitLen++) {
            final long pow = (long) Math.pow(2.0, bitLen);
            final long powMinus1 = pow - 1;
            final long samples = pow * (bitLen / 2 + 13);
            final long taskSamples = 1 + samples / parallelism;
            System.out.println("bitLen=" + bitLen + ", samples=" + samples);
            final long[] a = new long[(int) Math.ceil(pow / (double) Long.SIZE)];
            {
                final long[] b = new long[a.length];
                final int bits = bitLen;
                TestUtils.parallelRun(parallelism, 1_000L, (int i) -> {
                    final ThreadLocalRandom r = ThreadLocalRandom.current();
                    final long[] dest = i % 2 == 0 ? a : b;
                    for (long s = 0; s < taskSamples; s++) {
                        final long z = uniformLong(r, unsigned, bits) & powMinus1;
                        final int idx = (int) (z / Long.SIZE);
                        final long x = dest[idx];
                        if (x != -1L) {
                            final long y = x | (0x80000000_00000000L >>> (z & 63));
                            if (y != x) {
                                dest[idx] = y;
                            }
                        }
                    }
                }).run();
                for (int i = 0; i < a.length; i++) {
                    a[i] |= b[i];
                }
            }

            int missed = 0;
            int missedChunks = 0;
            final int fullChunks = (int) (pow / Long.SIZE);
            for (int i = 0; i < fullChunks; i++) {
                final long val = a[i];
                if (val != 0b11111111_11111111_11111111_11111111_11111111_11111111_11111111_11111111L) {
                    final int zeroes = Long.SIZE - Long.bitCount(val);
                    missed += zeroes;
                    missedChunks++;
                    System.err.println("chunk " + i + " value " + zeroPad(Long.toBinaryString(val)));
                }
            }
            final int finalBits = (int) (pow % Long.SIZE);
            if (finalBits > 0L) {
                final int shift = Long.SIZE - finalBits;
                final long last = a[a.length - 1];
                final long expected = -1L << shift;
                if (last != expected) {
                    missed += finalBits - Long.bitCount(last >>> shift);
                    System.err.println("last = " + Long.toBinaryString(last));
                }
            }

//            System.out.println(pow - missed + " / " + pow + " " + (1 - ((double) missed / pow)) + '\n');
            if (missedChunks != 0 || missed != 0) {
                throw new AssertionError("missed " + missed + ", missedChunks=" + missedChunks);
            }
        }
    }

    private static String zeroPad(String binary) {
        final int pad = Long.SIZE - binary.length();
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pad; i++) {
            sb.append('0');
        }
        return sb.append(binary).toString();
    }

    @Test
    public void metaTest2() throws Throwable {
        final Random r = new Random();
        assertEquals(0L, uniformLong(null, false, 0));
        assertEquals(0L, uniformLong(null, true, 0));
        assertEquals(0L, wildLong(r, false, 0));
        assertEquals(0L, wildLong(r, true, 0));

        assertEquals(BigInteger.ZERO, uniformBigInteger(null, false, 0));
        assertEquals(BigInteger.ZERO, uniformBigInteger(null, true, 0));
        assertEquals(BigInteger.ZERO, wildBigInteger(r, false, 0));
        assertEquals(BigInteger.ZERO, wildBigInteger(r, true, 0));

        uniformLong(r, false, 63);
        uniformLong(r, true, 63);

        uniformLong(r, false, 64);
        assertThrown(IllegalArgumentException.class, "too many bits for unsigned: 64", () -> uniformLong(r, true, 64));

        assertThrown(IllegalArgumentException.class, "too many bits for signed: 65", () -> uniformLong(r, false, 65));
        assertThrown(IllegalArgumentException.class, "too many bits for unsigned: 65", () -> uniformLong(r, true, 65));
    }

    @Test
    public void testTuple() {
        final Tuple emptyA = new Tuple();
        final Tuple emptyB = Tuple.from();

        assertEquals(Tuple.EMPTY, emptyA);
        assertEquals(Tuple.EMPTY, emptyB);

        assertTrue(Tuple.EMPTY.isEmpty());
        assertTrue(emptyA.isEmpty());
        assertTrue(emptyB.isEmpty());

        assertFalse(Single.of(0).isEmpty());
        assertFalse(Single.of(false).isEmpty());
        assertFalse(Single.of(new Object()).isEmpty());
    }

    private static final Object[] OBJECTS = new Object[] {
            new byte[0],
            new int[0],
            new short[0],
            new long[0],
            new boolean[0],
            new Throwable() {},
            new BigInteger[0],
            new BigDecimal[0],
            true,
            5,
            9L,
            "aha",
            '\0',
            Tuple.EMPTY,
            0.1f,
            1.9d
    };

    @Test
    public void testTypeSafety() throws Throwable {

        final MonteCarloTestCase.Limits limits = new MonteCarloTestCase.Limits(3, 3, 3, 3);
        final Random r = TestUtils.seededRandom();
        final Random instance = new Random();
        final Keccak k = new Keccak(256);
        final Object defaultObj = new Object();

        long seed = TestUtils.getSeed();
        for (int idx = 0; idx < limits.maxTupleLength; idx++) {
            for (int i = 0; i < 30; i++, seed++) {
                final MonteCarloTestCase testCase = new MonteCarloTestCase(seed, limits, instance, k);
                final Object[] elements = testCase.argsTuple.elements;
                if (idx < elements.length) {
                    Object replacement = OBJECTS[r.nextInt(OBJECTS.length)];
                    elements[idx] = elements[idx].getClass() != replacement.getClass()
                            ? replacement
                            : defaultObj;
                    try {
                        assertThrown(IllegalArgumentException.class, " but found ", () -> testCase.function.encodeCall(Tuple.from(elements)));
                    } catch (ClassCastException | AssertionError e) {
                        System.err.println("seed = " + seed);
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
        }
    }

    @Test
    public void testTypeSafety2() throws Throwable {
        TestUtils.assertThrown(IllegalArgumentException.class, "tuple index 0 is null",
                () -> Single.of(null)
        );

        TestUtils.assertThrown(IllegalArgumentException.class, "tuple index 1 is null",
                () -> Tuple.of(true, null, true)
        );

        TestUtils.assertThrown(IllegalArgumentException.class, "tuple index 1 is null",
                () -> Tuple.of(true, null, true)
        );

        TestUtils.assertThrown(IllegalArgumentException.class, "tuple index 1: null",
                () -> Function.parse("foo(bool,int32)").encodeCallWithArgs(true, null)
        );

        TestUtils.assertThrown(IllegalArgumentException.class, "tuple index 1: class mismatch: java.lang.Object != java.lang.Integer (int32 requires Integer but found Object)",
                () -> Function.parse("foo(bool,int32)").encodeCallWithArgs(false, new Object())
        );

        TestUtils.assertThrown(IllegalArgumentException.class, "tuple index 0: class mismatch: java.lang.Long != [I (int32[] requires int[] but found Long)",
                () -> Function.parse("foo(int32[])").encodeCallWithArgs(10L)
        );

        TestUtils.assertThrown(IllegalArgumentException.class, "tuple index 0: class mismatch: [[[[I != [[[[J (uint32[][][7][] requires long[][][][] but found int[][][][])",
                () -> Function.parse("foo(uint32[][][7][])").encodeCallWithArgs((Object) new int[][][][] {})
        );

        TestUtils.assertThrown(IllegalArgumentException.class, "tuple index 0: class mismatch: java.lang.String != [Lcom.esaulpaugh.headlong.abi.Address; (address[5] requires Address[] but found String)",
                () -> Function.parse("foo(address[5])").encodeCallWithArgs("0xaaaaaaaaaaaaaaaaaaa")
        );

        TestUtils.assertThrown(IllegalArgumentException.class, "tuple index 0: array length mismatch: boolean[2][][] != boolean[3][][] (bool[][][3] requires length 3 but found 2)",
                () -> Function.parse("foo(bool[][][3])").encodeCallWithArgs((Object) new boolean[][][] {new boolean[][]{}, new boolean[][]{}})
        );

        TestUtils.assertThrown(IllegalArgumentException.class, "tuple index 0: array length mismatch: byte[31] != byte[21] (bytes21 requires length 21 but found 31)",
                () -> Function.parse("foo(bytes21)").encodeCallWithArgs((Object) new byte[31])
        );
    }

    @Test
    public void testArrayLens() throws Throwable {
//        assertInstanceOf(ArrayType.class, TypeFactory.create("int[" + (Integer.MAX_VALUE + 14L) + "]"));
        assertThrown(
                IllegalArgumentException.class,
                () -> TypeFactory.create("int[" + (Integer.MAX_VALUE - 13) + "]")
        );
        assertThrown(
                IllegalArgumentException.class,
                () -> TypeFactory.create("int[" + Integer.MAX_VALUE+ "]")
        );
        assertThrown(
                IllegalArgumentException.class,
                "bad array length",
                () -> TypeFactory.create("int[" + (Integer.MAX_VALUE + 1L) + "]")
        );
        assertThrown(
                IllegalArgumentException.class,
                "bad array length",
                () -> TypeFactory.create("int[" + Long.MAX_VALUE + "]")
        );
        assertThrown(
                IllegalArgumentException.class,
                "bad array length",
                () -> TypeFactory.create("int[" + ArrayType.DYNAMIC_LENGTH + "]")
        );
        assertThrown(
                IllegalArgumentException.class,
                "bad array length",
                () -> TypeFactory.create("int[" + -2 + "]")
        );
    }

    @Test
    public void testStringFlags() {
        ArrayType<ByteType, Byte, String> none = TupleType.parse(ABIType.FLAGS_NONE, "(string)")
                                                            .<ArrayType<ByteType, Byte, String>>get(0)
                                                            .asArrayType();
        assertEquals(ABIType.FLAGS_NONE, none.getFlags());
        assertEquals(TypeFactory.create(ABIType.FLAGS_NONE, "string"), none);

        ArrayType<ByteType, Byte, String> legacy = TupleType.parse(ABIType.FLAG_LEGACY_DECODE, "(string)")
                                                                .<ArrayType<ByteType, Byte, String>>get(0)
                                                                .asArrayType();
        assertEquals(ABIType.FLAG_LEGACY_DECODE, legacy.getFlags());
        assertEquals(TypeFactory.create(ABIType.FLAG_LEGACY_DECODE, "string"), legacy);
    }

    @Test
    public void testGenerics() {
        TupleType<Single<Single<String>>> in = TupleType.parse(ABIType.FLAG_LEGACY_DECODE, "((string))");
        TupleType<Single<Single<String>>> t = new Function(
                TypeEnum.FUNCTION,
                "name",
                in,
                in,
                "",
                Function.newDefaultDigest()).getOutputs();
        ByteBuffer bb = t.encode(Single.of(Single.of("")));
        Single<Single<String>> s = t.decode(bb);
        System.out.println(s);
    }

    @Test
    public void fuzzNulls() throws Throwable {
        final MonteCarloTestCase.Limits limits = new MonteCarloTestCase.Limits(3, 3, 3, 3);
        final Random r = new Random(TestUtils.getSeed(31 * System.nanoTime() ^ 0x3636363636363636L));
        final Keccak k = new Keccak(256);
        for (int i = 0; i < 1000; i++) {
            MonteCarloTestCase mctc = new MonteCarloTestCase(r.nextLong(), limits, r, k);
            Tuple args = mctc.argsTuple;
            if (args.elements.length > 0) {
                int idx = r.nextInt(args.elements.length);
                replace(args.elements, idx);
                assertThrown(IllegalArgumentException.class, ": null", () -> mctc.function.encodeCall(args));
            }
        }
    }

    private static void replace(Object[] parent, int index) {
        Object element = parent[index];
        if(element instanceof Object[]) {
            Object[] eArr = (Object[]) element;
            if(eArr.length > 0) {
                Object inner = parent[index];
                if(inner instanceof Object[]) {
                    Object[] innerArr = (Object[]) inner;
                    if(innerArr.length > 0) {
                        innerArr[innerArr.length - 1] = null;
                        return;
                    }
                }
                eArr[0] = null;
                return;
            }
        }
        parent[index] = null;
    }

    @Test
    public void testSubtubleType() throws Throwable {
        TupleType<?> tt = TupleType.parse("(bytes3,uint16[],string)");

        assertEquals(TupleType.EMPTY, tt.select(false, false, false));
        assertEquals(TupleType.of("string"), tt.select(false, false, true));
        assertEquals(TupleType.of("uint16[]"), tt.select(false, true, false));
        assertEquals(TupleType.of("uint16[]", "string"), tt.select(false, true, true));
        assertEquals(TupleType.of("bytes3"), tt.select(true, false, false));
        assertEquals(TupleType.of("bytes3", "string"), tt.select(true, false, true));
        assertEquals(TupleType.of("bytes3", "uint16[]"), tt.select(true, true, false));
        assertEquals(TupleType.of("bytes3", "uint16[]", "string"), tt.select(true, true, true));

        assertThrown(IllegalArgumentException.class, "expected manifest length 3 but found length 2", () -> tt.select(true, true));
        assertThrown(IllegalArgumentException.class, "expected manifest length 3 but found length 4", () -> tt.select(false, false, false, false));

        assertEquals(TupleType.of("bytes3", "uint16[]", "string"), tt.exclude(false, false, false));
        assertEquals(TupleType.of("bytes3", "uint16[]"), tt.exclude(false, false, true));
        assertEquals(TupleType.of("bytes3", "string"), tt.exclude(false, true, false));
        assertEquals(TupleType.of("bytes3"), tt.exclude(false, true, true));
        assertEquals(TupleType.of("uint16[]", "string"), tt.exclude(true, false, false));
        assertEquals(TupleType.of("uint16[]"), tt.exclude(true, false, true));
        assertEquals(TupleType.of("string"), tt.exclude(true, true, false));
        assertEquals(TupleType.EMPTY, tt.exclude(true, true, true));

        assertThrown(IllegalArgumentException.class, "expected manifest length 3 but found length 0", () -> tt.select(new boolean[0]));
        assertThrown(IllegalArgumentException.class, "expected manifest length 3 but found length 5", () -> tt.select(new boolean[5]));
    }

    @Test
    public void testNameOverwrites() throws Throwable {
        testNameOverwrite("(bool)", "moo", "jumbo");
        testNameOverwrite("(())", "zZz", "Jumb0");

        assertThrown(IllegalArgumentException.class, "expected 2 element names but found 0",
                () -> TypeFactory.createTupleTypeWithNames("(bool,string)"));

        assertThrown(IllegalArgumentException.class, "expected 3 element names but found 2",
                () -> TypeFactory.createTupleTypeWithNames("(bool,string,int)", "a", "b"));

        assertThrown(IllegalArgumentException.class, "expected 2 element names but found 4",
                () -> TypeFactory.createTupleTypeWithNames("(bool,string)", new String[4]));

        TupleType<?> tt = TypeFactory.createTupleTypeWithNames("(bool,string)", "a", "b");
        assertThrown(ArrayIndexOutOfBoundsException.class, () -> tt.getElementName(-1));
        assertEquals("a", tt.getElementName(0));
        assertEquals("b", tt.getElementName(1));
        assertThrown(ArrayIndexOutOfBoundsException.class, () -> tt.getElementName(2));
    }

    private static void testNameOverwrite(String typeStr, String aName, String cName) {
        assertNotEquals(aName, cName);

        final TupleType<?> a = TypeFactory.createTupleTypeWithNames(typeStr, aName);
        assertEquals(aName, a.getElementName(0));

        final TupleType<?> b = TypeFactory.create(typeStr);
        assertEquals(aName, a.getElementName(0));
        assertNull(b.getElementName(0));

        final TupleType<?> c = TypeFactory.createTupleTypeWithNames(typeStr, cName);
        assertEquals(aName, a.getElementName(0));
        assertNull(b.getElementName(0));
        assertEquals(cName, c.getElementName(0));

        assertNotEquals(aName, cName);

        final TupleType<?> x = wrap(new String[] { aName }, a.get(0));
        assertEquals(aName, x.getElementName(0));

        final TupleType<?> y = wrap(null, b.get(0));
        assertEquals(aName, x.getElementName(0));
        assertNull(y.getElementName(0));

        final TupleType<?> z = wrap(new String[] { cName }, c.get(0));
        assertEquals(aName, x.getElementName(0));
        assertNull(y.getElementName(0));
        assertEquals(cName, z.getElementName(0));
    }

    private static TupleType<?> wrap(String[] elementNames, ABIType<?>... elements) {
        final StringBuilder canonicalBuilder = new StringBuilder("(");
        boolean dynamic = false;
        int flags = ABIType.FLAGS_UNSET;
        for (ABIType<?> e : elements) {
            canonicalBuilder.append(e.canonicalType).append(',');
            dynamic |= e.isDynamic();
            if (e.getFlags() != flags) {
                if (flags == ABIType.FLAGS_UNSET) {
                    flags = e.getFlags();
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
        return new TupleType<>(TestUtils.completeTupleTypeString(canonicalBuilder), dynamic, elements, elementNames, null, null, flags);
    }

    @Test
    public void testTupleImmutability() throws Throwable {
        Object[] args = new Object[] { "a", "b", "c" };
        Tuple t = Tuple.from(args); // shallow copy

        args[1] = 'x';
        assertEquals("a", t.get(0));
        assertEquals("b", t.get(1));
        assertEquals("c", t.get(2));

        args = new Object[] { "a", "b", "c" };
        t = new Tuple((Object[]) args); // no shallow copy

        args[1] = 'x';
        assertEquals("a", t.get(0));
        assertEquals(Character.valueOf('x'), t.get(1));
        assertEquals("c", t.get(2));

        testRemove(t.iterator());
    }

    @Test
    public void testTupleTypeImmutability() throws Throwable {
        testRemove(TupleType.parse("(bool,int,string)").iterator());
    }

    private static void testRemove(Iterator<?> iter) throws Throwable {
        assertTrue(iter.hasNext());
        iter.next();
        assertThrown(UnsupportedOperationException.class, iter::remove);
    }

    @Test
    public void testDecodeIndex0() {
        TupleType<Triple<Boolean, Tuple[], String>> tt = TupleType.parse("(bool,(bool,int24[2],(bool,bool)[2])[1],string)");
        Triple<Boolean, Tuple[], String> args = Tuple.of(true, new Tuple[] { Tuple.of(true, new int[] { 1, 2 }, new Tuple[] { Tuple.of(true, false), Tuple.of(true, false) }) }, "ya");
        ByteBuffer bb = tt.encode(args);
        System.out.println(Strings.encode(bb));
        String ya = tt.decode(bb, 2);
        assertEquals("ya", ya);
    }

    @Test
    public void testDecodeIndex1() {
        TupleType<Triple<Boolean, boolean[][], String[][]>> tt = TupleType.parse("(bool,bool[3][2],string[][])");
        Triple<Boolean, boolean[][], String[][]> args = Tuple.of(true, new boolean[][] { new boolean[] { true, false, true }, new boolean[] { false, false, true } }, new String[][] { new String[] { "wooo", "moo" } });
        ByteBuffer bb = tt.encode(args);
        System.out.println(Strings.encode(bb));
        String[][] s = tt.decode(bb, 2);
        assertTrue(Objects.deepEquals(new String[][] { new String[] { "wooo", "moo" } }, s));
    }

    @Test
    public void testDecodeIndex2() throws IOException {
        final String en = TestUtils.readFileResourceAsString("tests/headlong/tests/string_en.txt");
        final String[] fr = TestUtils.readFileResourceAsString("tests/headlong/tests/string_fr.txt").split("\\R");
//        System.out.println(en);
//        Arrays.stream(fr).forEach(System.out::println);
        TupleType<Tuple> tt = TupleType.parse("(bool,uint16,address,int64,uint64,address,string[][])");
        Tuple args = Tuple.from(
                true,
                90,
                Address.wrap("0x0000000000000000000000000000000000000000"),
                100L,
                BigInteger.valueOf(110L),
                Address.wrap("0x0000110000111100001111110000111111110000"),
                new String[][] { new String[] { en, fr[5], fr[1] }, new String[] { fr[4], fr[3] }, new String[] { fr[2] } }
        );
        final ByteBuffer bb = tt.encode(args);
        boolean bool = tt.decode(bb, 0);
        assertTrue(bool);
        int uint16 = tt.decode(bb, 1);
        assertEquals(90, uint16);
        Address address = tt.decode(bb, 2);
        assertEquals(Address.wrap("0x0000000000000000000000000000000000000000"), address);
        long int64 = tt.decode(bb, 3);
        assertEquals(100L, int64);
        BigInteger uint64 = tt.decode(bb, 4);
        assertEquals(new BigInteger("110"), uint64);
        Address address2 = tt.decode(bb, 5);
        assertEquals(Address.wrap("0x0000110000111100001111110000111111110000"), address2);
        String[][] arrs = tt.decode(bb, 6);
        assertArrayEquals(new String[][] { new String[] { en, fr[5], fr[1] }, new String[] { fr[4], fr[3] }, new String[] { fr[2] } }, arrs);
    }

    @Test
    public void testSelectExclude() {
        TupleType<?> _uintBool_ = TupleType.parse("(uint,bool)");
        TupleType<?> _uint_ = TupleType.parse("(uint)");
        TupleType<?> _bool_ = TupleType.parse("(bool)");

        assertEquals(TupleType.EMPTY,   _uintBool_.select(false, false));
        assertEquals(_bool_,            _uintBool_.select(false, true));
        assertEquals(_uint_,            _uintBool_.select(true, false));
        assertEquals(_uintBool_,        _uintBool_.select(true, true));

        assertEquals(_uintBool_,        _uintBool_.exclude(false, false));
        assertEquals(_uint_,            _uintBool_.exclude(false, true));
        assertEquals(_bool_,            _uintBool_.exclude(true, false));
        assertEquals(TupleType.EMPTY,   _uintBool_.exclude(true, true));

        TupleType<?> tt2 = TupleType.parse("((int,bool))");
        assertEquals(tt2, tt2.select(true));
        assertEquals(TupleType.EMPTY, tt2.exclude(true));

        assertEquals(TupleType.EMPTY, TupleType.EMPTY.select());
        assertEquals(TupleType.EMPTY, TupleType.EMPTY.exclude());

        TupleType<?> clone0 = _uintBool_.select(true, true);
        assertSame(_uintBool_.get(0), clone0.get(0));
        assertSame(_uintBool_.get(1), clone0.get(1));

        TupleType<?> clone1 = _uintBool_.exclude(false, false);
        assertSame(_uintBool_.get(0), clone1.get(0));
        assertSame(_uintBool_.get(1), clone1.get(1));

        assertSame(_uintBool_.get(0), _uintBool_.select(true, false).get(0));
        assertSame(_uintBool_.get(1), _uintBool_.select(false, true).get(0));
    }

    @Test
    public void testGetElement() {
        TupleType<?> tt = TupleType.parse("(bytes8,decimal)");
        ArrayType<ByteType, Byte, byte[]> at = tt.get(0);
        assertEquals(8, at.getLength());
        BigIntegerType decimal = tt.get(1);
        assertEquals("int168", decimal.getCanonicalType());
        assertEquals("iii", Single.of("iii").get0());

        TupleType<?> outer = TupleType.parse("((address,int256))");
        TupleType<?> inner = outer.get(0);
        assertEquals(TupleType.parse("(address,int)"), inner);
    }

    @Test
    public void testTupleLengthMismatch() throws Throwable {
        TupleType<Tuple> tt = TupleType.parse("(bool)");
        assertThrown(IllegalArgumentException.class, "tuple length mismatch: expected length 1 but found 0", () -> tt.validate(Tuple.EMPTY));
        assertThrown(IllegalArgumentException.class, "tuple length mismatch: expected length 1 but found 2", () -> tt.validate(Tuple.of("", "")));
    }

    @Test
    public void testParseBadFixed() throws Throwable {
        assertThrown(IllegalArgumentException.class, "unrecognized type: \"fixed45\"", () -> TypeFactory.create("fixed45"));
    }

    @Test
    public void testLengthLimit() throws Throwable {
        final byte[] typeBytes = new byte[1602];
        final int midpoint = typeBytes.length / 2;
        int i = 0;
        while (i < midpoint) {
            typeBytes[i++] = '(';
        }
        while (i < typeBytes.length) {
            typeBytes[i++] = ')';
        }
        final String ascii = Strings.encode(typeBytes, Strings.ASCII);
        assertThrown(IllegalArgumentException.class, "type length exceeds maximum: 1602 > 1600" , () -> TupleType.parse(ascii));

        final Random r = TestUtils.seededRandom();
        final StringBuilder sb = new StringBuilder(r.nextBoolean() ? "string" : "bool");
        for (int j = 0; j < 800; j++) {
            if (r.nextBoolean()) {
                sb.append("[]");
            } else {
                sb.append('[').append(r.nextInt(100)).append(']');
            }
        }
        final String arrType = sb.toString();
        assertThrown(IllegalArgumentException.class, "type length exceeds maximum: " + arrType.length() + " > 1600" , () -> TypeFactory.create(arrType));
    }

    @Test
    public void testToArray() {
        final MonteCarloTestCase.Limits limits = new MonteCarloTestCase.Limits(5, 5, 4, 3);
        final MessageDigest md = Function.newDefaultDigest();
        final long seed = TestUtils.getSeed();
        final Random r = new Random();
        for (int z = 0; z < 10; z++) {
            final Tuple values = new MonteCarloTestCase(seed + z, limits, r, md).argsTuple;

            final Tuple deepCopy = values.deepCopy();
            assertNotSame(values, deepCopy);
            assertEquals(values, deepCopy);

            final Tuple shallowCopy = Tuple.from(values.toArray());
            assertEquals(values, shallowCopy);

            final Object[] elements = new Object[values.size()];
            for (int i = 0; i < elements.length; i++) {
                elements[i] = values.get(i);
            }
            assertEquals(values, new Tuple(elements));
        }
    }

    @Test
    public void testTupleEquals() {
        assertEquals(Tuple.EMPTY, new Tuple());

        final Single<byte[]> s = Single.of(new byte[0]);
        final Pair<byte[], String> p = Tuple.of(new byte[1], "75");
        final Triple<byte[], String, Long> t = Tuple.of(new byte[2], "75", 75L);
        final Quadruple<byte[], Object, Number, Throwable> q = Tuple.of(new byte[3], "bbb", 19L, new Error());
        final Quintuple<Long, Long, Long, Long, Byte> q5 = Tuple.of(-999L, Long.MAX_VALUE, 0L, -1L, (byte)0);
        @SuppressWarnings("rawtypes")
        final Sextuple<Pair<byte[], String>, Pair<byte[], String>, Pair, Pair, Pair, Pair> s6 = Tuple.of(p, p, p, p, p, p);
        final Triple<Integer, String, Triple<byte[], String, Long>> n = Tuple.of(90, "_", t);
        final Triple<byte[], String, Long> x = n.get2();

        final Tuple t0 = Tuple.from();
        final Tuple t1 = Tuple.from((Object)"".getBytes());
        final Tuple t2 = Tuple.from(new byte[] {0}, new String("75"));
        final Tuple t3 = Tuple.from(new byte[2], new String("75"), 75L);
        final Tuple t4 = Tuple.from(q.elements);
        final Tuple t5 = Tuple.from(-1000L + 1, Long.MAX_VALUE, (long)0, (long)-1, ByteType.ZERO_BYTE);
        final Tuple t6 = Tuple.from(p, p, p, p, p, p);
        final Tuple t7 = Tuple.from(90, new String("_"), t);
        final Tuple t8 = t7.get(2);

        assertEquals(Tuple.EMPTY, t0);
        testEquals(s, t1);
        testEquals(p, t2);
        testEquals(t, t3);
        testEquals(q, t4);
        testEquals(q5, t5);
        testEquals(s6, t6);
        testEquals(n, t7);
        testEquals(x, t8);
    }

    private static void testEquals(Tuple a, Tuple b) {
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(a, b);
        assertEquals(a.toString(), b.toString());
        assertEquals(a.deepCopy(), b.deepCopy());
    }

    @Test
    void testTypeWitnesses() {
        abstract class Nonsense {
            public <T extends List<?>> T create(Boolean... bools) {
                return createList();
            }
            protected abstract <T extends List<?>> T createList();
        }

        assertTrue(
                new Nonsense() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public <T extends List<?>> T create(Boolean... bools) {
                        return (T) Stream.of(bools)
                                .filter(b -> b == super.<List<String>>create(bools).add(""))
                                .collect(Collectors.toList());
                    }
                    @SuppressWarnings("unchecked")
                    @Override
                    protected <T extends List<?>> T createList() {
                        return (T) new ArrayList<String>();
                    }
                }.<List<Boolean>>create(true, false).add(true),
                "nonsense failed"
        );
    }
}
