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
import com.esaulpaugh.headlong.abi.util.Uint;
import com.esaulpaugh.headlong.abi.util.WrappedKeccak;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.TestUtils.assertThrownMessageMatch;
import static com.esaulpaugh.headlong.TestUtils.requireNoTimeout;
import static com.esaulpaugh.headlong.TestUtils.shutdownAwait;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncodeTest {

    private static final Class<IllegalArgumentException> ILLEGAL = IllegalArgumentException.class;

    private static final Class<StringIndexOutOfBoundsException> SIOOBE = StringIndexOutOfBoundsException.class;

    private static final String EMPTY_PARAMETER = "empty parameter";

    @Disabled("may take minutes to run")
    @Test
    public void fuzzSignatures() throws InterruptedException, TimeoutException {

        final byte[] alphabet = Strings.decode("x0123456789", Strings.ASCII); // new char[128]; // "(),abcdefgilmnorstuxy8[]"
        final int alphabetLen = alphabet.length;
        if (alphabetLen == 128) {
            for (int i = 0; i < alphabetLen; i++) {
                alphabet[i] = (byte) i;
            }
        }

        final int[] iterations = new int[] {
                128,
                128,
                128,
                128,
                8_192,
                524_288,
                1_000_000,
                1_000_000,
                1_000_000,
                1_000_000,
                1_000_000,
                1_000_000,
                1_000_000,
                1_000_000,
                1_000_000
        };

        final String prefix = "ufixed";
        final int prefixLen = prefix.length();

        final Random rand = TestUtils.seededRandom();
        final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        final Runnable runnable = () -> {
            for (int len = 0; len <= 14; len++) {
                System.out.println(len + "(" + Thread.currentThread().getId() + ")");
                final byte[] temp = new byte[len];
                final int last = len - 1;
                if(len > 0) {
                    temp[0] = '(';
                    if(len > prefixLen) {
                        for (int i = 0; i < prefixLen; i++) {
                            temp[i + 1] = (byte) prefix.charAt(i);
                        }
                    }
                    if(len > 1) {
                        temp[last] = ')';
                    }
                }
                final int num = iterations[len]; // 1_000_000 + (int) Math.pow(3.7, len);
                for (int j = 0; j < num; j++) {
                    for (int i = 1 + prefixLen; i < last; i++) {
                        temp[i] = alphabet[rand.nextInt(alphabetLen)];
                    }
                    String sig = new String(temp, 0, 0, len);
                    try {
                        TupleType tt = TupleType.parse(sig);
                        if(map.containsKey(sig)) continue;
                        String canon = tt.canonicalType;
                        map.put(sig, canon);
                        System.out.println("\t\t\t" + len + ' ' + sig + (sig.equals(canon) ? "" : " --> " + canon));
                    } catch (IllegalArgumentException | ClassCastException ignored) {
                        /* do nothing */
                    } catch (Throwable t) {
                        System.err.println(sig);
                        throw new RuntimeException(t);
                    }
                }
            }
        };
        final int parallelism = Runtime.getRuntime().availableProcessors();
        System.out.println("p = " + parallelism);
        final ExecutorService pool = Executors.newFixedThreadPool(parallelism);
        for (int k = 0; k < parallelism; k++) {
            pool.submit(runnable);
        }
        requireNoTimeout(shutdownAwait(pool, 3600L));

        final int size = map.size();
        System.out.println("\nsize=" + size);

        List<String> list = new ArrayList<>();
        for(Map.Entry<String, String> e : map.entrySet()) {
            list.add(e.getKey());
        }
        Collections.sort(list);

        list.forEach(System.out::println);

        assertEquals(2 + (32 * 80), size); // for prefix fixed or ufixed
    }

    private static final String ABI = "a71100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000700000000000000000000000000000000000000000000000000000000000000005a0000000000000000000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000001a000000000000000000000000000000000000000000000000000000000000004a00000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000230c5748dd399896196f734a17ecf80b2178adc067b361d5ba994ae6400000000b95ffb49ee209c0d760c3350ecaae6d4ad4ff7b62af4d91ef3b5ae4e00000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000001a00000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000001816e178e3509b80a6a66a8827c8d5e62be37692efe0e22e59f936b8b000000000000000000000000000000000000000000000000000000000000000000000001a48b4c097589c15784ef0b97c17aa6dbb752f61fd1a272618d87a535000000000000000000000000000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000033b6ae013f517ec3c2082aa77400d768168266af2c3c3680404075f300000000093ab24d3e36bb7194c68063c595ead7554b2e8ce3fa1e6498c0c1208000000009fd62e78a55909d9e882d50a8074384af470c9c13e618ccc761f09a700000000000000000000000000000000000000000000000000000000000000000000000182a731ded7c7b3dd45c528564df732e138a71dbccd14954c45b5c14e000000000000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000001a0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000010daf12dcf419efe89f949a26f943bd7236aafbb8d6c4749fd27ed36f000000000000000000000000000000000000000000000000000000000000000000000003e6501ff6bc4074725885f5e65cc263b9ab0f098c14bc7cce3c2e4d2900000000f351c27189ce03746ce7dab3c560fb2bb430551b55a77a18380f066100000000e9753f8a5bb5e0da442d7e11d38d1eaafe5b6df463fd2256be199e380000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000001a0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000d05d841062e000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000001b052db5529dc0433400000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000013160000000000000000000000000000000000000000000000000000000000000001000000000051299437b715c73400c5e6e655c925eb10ee34669cf8f1ccf8febd";

    @Test
    public void testFunctionFormat() throws Throwable {
        testFormat(true, Function::formatCall);
        testFormatHash(Function::formatCall, "137fe81220baa4ad0300a7a31ac26b0b07549af69af96d436ca3b2a0ebd1b949", "1663cedd" + ABI);
    }

    @Test
    public void testTupleFormat() throws Throwable {
        testFormat(false, TupleType::format);
        testFormatHash(TupleType::format, "c16f9cf84bd8229553d38586ad15784cfd1dc05b45ec307cd074f3be04968777", ABI);

    }

    private static void testFormatHash(java.util.function.Function<byte[], String> format, String hashHex, String abiHex) {
        byte[] utf8 = Strings.decode(format.apply(Strings.decode(abiHex)), Strings.UTF_8);
        byte[] hash = new WrappedKeccak(256).digest(utf8);
        assertEquals(hashHex, Strings.encode(hash));
    }

    private static void testFormat(boolean func, java.util.function.Function<byte[], String> format) throws Throwable {
        final int expectedMod = func ? 4 : 0;
        byte[] master = new byte[260];
        Arrays.fill(master, (byte) 0xff);
        for (int i = 0; i < 260; i++) {
            byte[] x = Arrays.copyOfRange(master, 0, i);
            int mod = i % UNIT_LENGTH_BYTES;
            if(mod == expectedMod) {
                String formatted = format.apply(x);
                assertEquals(i * 2, formatted.codePoints().filter(ch -> ch == 'f').count());
                int div = (i - expectedMod) / UNIT_LENGTH_BYTES;
                if(div > 0) {
                    String ffff = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
                    boolean containsFfff = formatted.contains(ffff);
                    Assertions.assertTrue(containsFfff);
                    final String labeled;
                    if(func) {
                        labeled = paddedLabel(String.valueOf(div - 1)) + ffff;
                    } else {
                        String hex = Long.toHexString((div - 1) * UNIT_LENGTH_BYTES);
                        StringBuilder label = new StringBuilder();
                        int zeroes = 6 - hex.length();
                        for (int j = 0; j < zeroes; j++) {
                            label.append(' ');
                        }
                        label.append(hex);
                        labeled = paddedLabel(label.toString()) + ffff;
                    }
                    Assertions.assertTrue(formatted.contains(labeled));
                }
            } else {
                assertThrown(ILLEGAL, "expected length mod 32 == 0, found: ", () -> format.apply(x));
            }
        }
    }

    private static String paddedLabel(String label) {
        StringBuilder sb = new StringBuilder();
        sb.append(label);
        int n = 9 - sb.length();
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static final List<String> CLASS_CAST_MESSAGES = Arrays.asList(
            "com.esaulpaugh.headlong.abi.ArrayType cannot be cast to com.esaulpaugh.headlong.abi.TupleType",
            "class com.esaulpaugh.headlong.abi.ArrayType cannot be cast to class com.esaulpaugh.headlong.abi.TupleType",
            "Cannot cast com.esaulpaugh.headlong.abi.ArrayType to com.esaulpaugh.headlong.abi.TupleType",
            "Cannot cast class com.esaulpaugh.headlong.abi.ArrayType to class com.esaulpaugh.headlong.abi.TupleType",
            "com.esaulpaugh.headlong.abi.ArrayType incompatible with com.esaulpaugh.headlong.abi.TupleType" // IBM semeru 1.8.0
    );

    @Test
    public void testIllegalSignatures() throws Throwable {

        assertThrownMessageMatch(ClassCastException.class, CLASS_CAST_MESSAGES, () -> Function.parse("f()[]"));

        assertThrown(ILLEGAL, "unrecognized type: ", () -> TupleType.parse(""));
        assertThrown(ILLEGAL, "unrecognized type: \"(\"", () -> TupleType.parse("("));
        assertThrown(ILLEGAL, "unrecognized type: \")\"", () -> TupleType.parse(")"));
        assertThrown(ILLEGAL, "unrecognized type: \"aaaaaa\"", () -> TupleType.parse("aaaaaa"));

        testSIOOBE("");
        testSIOOBE(")");

        assertThrown(ILLEGAL, "unrecognized type: \"(\"", () -> Function.parse("("));

        assertThrown(ILLEGAL, "unrecognized type: \"([\"", () -> Function.parse("(["));
        assertThrown(ILLEGAL, "unrecognized type: \"(int\"", () -> Function.parse("(int"));
        assertThrown(ILLEGAL, "unrecognized type: \"(bool[],\"", () -> Function.parse("(bool[],"));
        assertThrown(ILLEGAL, "unrecognized type: \"(()\"", () -> Function.parse("(()"));
        assertThrown(ILLEGAL, "unrecognized type: \"(())...\"", () -> Function.parse("(())..."));
        assertThrown(ILLEGAL, "unrecognized type: \"((((()))\"", () -> Function.parse("((((()))"));
    }

    private static void testSIOOBE(String signature) throws Throwable {
        assertThrownMessageMatch(
                SIOOBE,
                Arrays.asList(
                        "begin 0, end -1, length " + signature.length(),
                        "String index out of range: -1",
                        "String index out of range: 0",
                        "Range [0, -1) out of bounds for length " + signature.length() // JDK 18
                ),
                () -> Function.parse(signature)
        );
    }

    @Test
    public void emptyParamTest() throws Throwable {
        assertThrown(ILLEGAL, EMPTY_PARAMETER, () -> Function.parse("(,"));

        assertThrown(ILLEGAL, "@ index 0, " + EMPTY_PARAMETER, () -> new Function("baz(,)"));

        assertThrown(ILLEGAL, "@ index 1, " + EMPTY_PARAMETER, () -> new Function("baz(bool,)"));

        assertThrown(ILLEGAL, "@ index 1, @ index 1, " + EMPTY_PARAMETER, () -> new Function("baz(bool,(int,,))"));
    }

    @Test
    public void illegalCharsTest() throws Throwable {
        assertThrown(ILLEGAL, "illegal char", () -> Function.parse("œ()"));

        assertThrown(ILLEGAL, "illegal char 0x2a6 '\u02a6' @ index 2", () -> new Function("ba\u02a6z(uint32,bool)"));

        assertThrown(ILLEGAL, "@ index 1, @ index 0, unrecognized type: \"bool\u02a6\"", () -> new Function("baz(int32,(bool\u02a6))"));
    }

    @Test
    public void simpleFunctionTest() {
        Function f = new Function("baz(uint32,bool)"); // canonicalizes and parses any signature automatically
        Tuple args = new Tuple(69L, true);

        // Two equivalent styles:
        ByteBuffer one = f.encodeCall(args);
        ByteBuffer two = f.encodeCallWithArgs(69L, true);

        System.out.println(Function.formatCall(one.array())); // a multi-line hex representation

        Tuple decoded = f.decodeCall((ByteBuffer) two.flip());

        assertEquals(decoded, args);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testArrayLen() throws Throwable {

        assertThrown(ILLEGAL, "@ index 0, bad array length", () -> Function.parse("abba(()[-04])"));

        assertThrown(ILLEGAL, "@ index 0, bad array length", () -> Function.parse("zaba(()[04])"));

        assertEquals(4, ((ArrayType<TupleType, Tuple[]>) Function.parse("yaba(()[4])").getInputs().get(0)).getLength());
    }

    @Test
    public void uint8ArrayTest() {
        Function f = new Function("baz(uint8[])");

        Tuple args = Tuple.singleton(new int[] { 0xFF, 1, 1, 2, 0 });
        ByteBuffer two = f.encodeCall(args);

        Tuple decoded = f.decodeCall((ByteBuffer) two.flip());

        assertEquals(decoded, args);
    }

    @Test
    public void tupleArrayTest() {
        Function f = new Function("((int16)[2][][1])");

        Object[] argsIn = new Object[] {
                new Tuple[][][] { new Tuple[][] { new Tuple[] { new Tuple(9), new Tuple(-11) } } }
        };

        ByteBuffer buf = f.encodeCallWithArgs(argsIn);

        assertArrayEquals(Strings.decode("f9354bbb0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000009fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff5"), buf.array());
    }

    @Test
    public void fixedLengthDynamicArrayTest() throws Throwable {

        final Random rand = TestUtils.seededRandom();

        Supplier<Object> bytesSupplier = () -> TestUtils.randomBytes(rand.nextInt(33), rand);
        Supplier<Object> stringSupplier = () -> { byte[] v = TestUtils.randomBytes(rand.nextInt(33), rand); return Strings.encode(v, Strings.UTF_8); };
        Supplier<Object> booleanArraySupplier = () -> { boolean[] v = new boolean[rand.nextInt(4)]; Arrays.fill(v, rand.nextBoolean()); return v; };
        Supplier<Object> intArraySupplier = () -> { BigInteger[] v = new BigInteger[rand.nextInt(4)]; Arrays.fill(v, BigInteger.valueOf(rand.nextInt())); return v; };

        testFixedLenDynamicArray("bytes", new byte[1 + rand.nextInt(34)][], bytesSupplier);
        testFixedLenDynamicArray("string", new String[1 + rand.nextInt(34)], stringSupplier);
        testFixedLenDynamicArray("bool[]", new boolean[1 + rand.nextInt(34)][], booleanArraySupplier);
        testFixedLenDynamicArray("int[]", new BigInteger[1 + rand.nextInt(34)][], intArraySupplier);

        final String msg = "array lengths differ, expected: <32> but was: <0>";
        assertThrown(AssertionFailedError.class, msg, () -> testFixedLenDynamicArray("bytes", new byte[0][], null));
        assertThrown(AssertionFailedError.class, msg, () -> testFixedLenDynamicArray("string", new String[0], null));
        assertThrown(AssertionFailedError.class, msg, () -> testFixedLenDynamicArray("bool[]", new boolean[0][], null));
        assertThrown(AssertionFailedError.class, msg, () -> testFixedLenDynamicArray("int[]", new BigInteger[0][], null));
    }

    private static void testFixedLenDynamicArray(String baseType, Object[] args, Supplier<Object> supplier) {
        final int n = args.length;
        TupleType a = TupleType.of(baseType + "[" + n + "]");

        String[] types = new String[n];
        Arrays.fill(types, baseType);
        TupleType b = TupleType.parse("(" + TupleType.of(types) + ")");

        System.out.println(a + " vs " + b);

        for (int i = 0; i < args.length; i++) {
            args[i] = supplier.get();
        }

        Tuple aArgs = new Tuple((Object) args);
        Tuple bArgs = new Tuple(new Tuple((Object[]) args));

        byte[] aEncoding = a.encode(aArgs).array();
        ByteBuffer bDest = ByteBuffer.allocate(b.measureEncodedLength(bArgs));
        b.encode(bArgs, bDest);
        byte[] bEncoding = bDest.array();

//        System.out.println(ABIType.format(aEncoding));
//        System.out.println(ABIType.format(bEncoding));

        assertArrayEquals(aEncoding, bEncoding);
    }

    @Test
    public void testIsEmpty() {
        assertTrue(TupleType.EMPTY.isEmpty());
        assertTrue(TupleType.parse("()").isEmpty());
        assertFalse(TupleType.parse("(int)").isEmpty());
        assertFalse(TupleType.parse("(bool,string)").isEmpty());
    }

    @Test
    public void paddingTest() {
        final Function f = new Function("(bool,uint8,int64,uint64,address,ufixed,bytes2,(string),bytes,function)");
        final TupleType paramTypes = f.getInputs();

        StringBuilder sb = new StringBuilder();
        for(ABIType<?> type : paramTypes.elementTypes) {
            sb.append(type.getClass().getSimpleName()).append(',');
        }
        Assertions.assertEquals("BooleanType,IntType,LongType,BigIntegerType,AddressType,BigDecimalType,ArrayType,TupleType,ArrayType,ArrayType,", sb.toString());

        Tuple args = new Tuple(
                true,
                1,
                1L,
                BigInteger.valueOf(8L),
                Address.wrap("0x0000000000000000000000000000000000000009"),
                new BigDecimal(BigInteger.TEN, 18),
                new byte[] { 1, 0 },
                Tuple.singleton("\u0002"),
                new byte[] { 0x04 },
                new byte[] { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23 }
        );

        final int len = f.measureCallLength(args) + 7 + 8;

        byte[] ffff = new byte[len];
        Arrays.fill(ffff, (byte) 0xff);

        ByteBuffer full = (ByteBuffer) ByteBuffer.wrap(ffff).position(7);
        ByteBuffer empty = (ByteBuffer) ByteBuffer.allocate(len).position(7);

        f.encodeCall(args, full)
                .encodeCall(args, empty);

        byte[] fullBytes = full.array();
        byte[] emptyBytes = empty.array();
        byte[] xor = new byte[len];
        for(int i = 0; i < len; i++) {
            xor[i] = (byte) (fullBytes[i] ^ emptyBytes[i]);
        }

        final String xorStr = Strings.encode(xor);

        System.out.println(Strings.encode(full));
        System.out.println("^");
        System.out.println(Strings.encode(empty));
        System.out.println("=");
        System.out.println(xorStr);

        assertEquals(
                "ffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffff",
                xorStr
        );

        assertEquals(paramTypes.encode(args).limit(), paramTypes.measureEncodedLength(args));
    }

    @Test
    public void testEncodeElements() {
        TupleType tt = TupleType.parse("(uint64,uint32,bool[])");

        Object a = BigInteger.valueOf(7L);
        Object b = 9L;
        Object c = new boolean[0];

        ByteBuffer ee = tt.encodeElements(a, b, c);

        assertArrayEquals(tt.encode(Tuple.of(a, b, c)).array(), ee.array());
        assertEquals(
                "0000000000000000000000000000000000000000000000000000000000000007000000000000000000000000000000000000000000000000000000000000000900000000000000000000000000000000000000000000000000000000000000600000000000000000000000000000000000000000000000000000000000000000",
                Strings.encode(ee)
        );
    }

    @Test
    public void testScaleErr() throws Throwable {
        assertThrown(
                IllegalArgumentException.class,
                "BigDecimal scale mismatch: actual != expected: 1 != 9",
                () -> Function.parse("(fixed56x9)").encodeCall(Tuple.of(new BigDecimal("0.2")))
        );
    }

    @Test
    public void testNestedErrMessage() throws Throwable {
        assertThrown(
                IllegalArgumentException.class,
                "tuple index 0: array index 1: signed val exceeds bit limit: 9 >= 8",
                () -> Function.parse("(int8[])").encodeCall(Tuple.singleton(new int[] { 120, 256 }))
        );
    }

    @Test
    public void testTypeSafety() throws Throwable {
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
    public void testUnconsumedTrailing() throws Throwable {
        final ABIType<?> type = TypeFactory.create("uint");
        final int extra = 1 + TestUtils.seededRandom().nextInt(40);
        assertThrown(
                ILLEGAL,
                "unconsumed bytes: " + extra + " remaining",
                () -> type.decode(new byte[UNIT_LENGTH_BYTES + extra])
        );
    }

    @Test
    public void testMinAndMax() throws Throwable {

        BooleanType bool = TypeFactory.create("bool");
        assertEquals(BigInteger.ZERO, bool.minValue());
        assertEquals(BigInteger.ONE, bool.maxValue());

        for (int i = 8; i <= 256; i += 8) {
            final UnitType<?> unsigned = TypeFactory.create("uint" + i);
            final UnitType<?> signed = TypeFactory.create("int" + i);

//            System.out.println(unsigned.minValue() + " ==> " + unsigned.maxValue() + ", " + signed.minValue() + " ==> " + signed.maxValue());

            testMinAndMax(unsigned, "signed value given for unsigned type", " > ");
            testMinAndMax(signed, " >= ", " >= ");

            Uint uint = new Uint(i);
            final long mask = uint.maskLong;
            if(mask != 0) {
                final long uMax = unsigned.maxValue().longValueExact();
                final long uMin = unsigned.minValue().longValueExact();
                final long max = signed.maxValue().longValueExact();
                final long min = signed.minValue().longValueExact();

                assertEquals(mask, uMax);
                assertEquals(mask, max * 2 + 1);

                assertEquals(Long.toBinaryString(uMax).substring(1), Long.toBinaryString(max));
                assertEquals(0L, uMin);
                assertEquals(Long.SIZE - i, Long.numberOfLeadingZeros(uMax));
                assertEquals(0, Long.numberOfTrailingZeros(uMax));

                assertEquals(i - 1, Long.numberOfTrailingZeros(min));
                assertEquals(0, Long.numberOfTrailingZeros(max));
            }
            assertEquals(uint.range, unsigned.maxValue().subtract(unsigned.minValue()).add(BigInteger.ONE));
            assertEquals(uint.halfRange, signed.maxValue().add(BigInteger.ONE));
        }
    }

    private static void testMinAndMax(UnitType<?> type, String err1Substr, String err2Substr) throws Throwable {
        assertThrown(ILLEGAL, err1Substr, () -> type.validateBigInt(type.minValue().subtract(BigInteger.ONE)));
        type.validateBigInt(type.minValue());
        type.validateBigInt(type.maxValue());
        assertThrown(ILLEGAL, err2Substr, () -> type.validateBigInt(type.maxValue().add(BigInteger.ONE)));
    }

    private static final double DELTA = 0.000000000000000001d;
    private static final BigDecimal O_1 = new BigDecimal("0.1");
    private static final BigDecimal O_000000000000000001 = new BigDecimal("0.000000000000000001");

    @Test
    public void testDecimalMinMax() throws Throwable {
        BigDecimalType decimal = TypeFactory.create("decimal");
        BigDecimal decimalMin = new BigDecimal(new BigInteger("-170141183460469231731687303715884105728"), 10);
        BigDecimal decimalMax = new BigDecimal(new BigInteger("170141183460469231731687303715884105727"), 10);

        assertThrown(ILLEGAL, "signed val exceeds bit limit: 128 >= 128", () -> decimal.validate(decimalMin.subtract(O_1)));
        decimal.validate(decimalMin);
        decimal.validate(decimalMax);
        assertThrown(ILLEGAL, "signed val exceeds bit limit: 128 >= 128", () -> decimal.validate(decimalMax.add(O_1)));

        BigDecimalType ufixed = TypeFactory.create("ufixed");
        BigDecimal u128Max = new BigDecimal(new BigInteger("340282366920938463463374607431768211455"), 18);

        System.out.println(ufixed.minDecimal() + " " + ufixed.maxDecimal());

        assertEquals(0.0d, ufixed.minDecimal().doubleValue(), DELTA);
        assertEquals(u128Max.doubleValue(), ufixed.maxDecimal().doubleValue(), DELTA);

        ufixed.validate(new BigDecimal(BigInteger.ZERO, 18));
        assertThrown(ILLEGAL, "BigDecimal scale mismatch: actual != expected: 1 != 18", () -> ufixed.validate(O_1.negate()));
        ufixed.validate(O_000000000000000001);
        assertThrown(ILLEGAL, "signed value given for unsigned type", () -> ufixed.validate(O_000000000000000001.negate()));

        ufixed.validate(u128Max);
        assertThrown(ILLEGAL, "unsigned val exceeds bit limit: 129 > 128", () -> ufixed.validate(u128Max.add(BigDecimal.ONE)));
        assertThrown(ILLEGAL, "unsigned val exceeds bit limit: 129 > 128", () -> ufixed.validate(u128Max.add(O_1)));

        BigDecimalType fixed = TypeFactory.create("fixed");

        assertEquals(((UnitType<?>) TypeFactory.create("uint128")).maxValue(), ufixed.maxValue());
        assertEquals(((UnitType<?>) TypeFactory.create("uint128")).minValue(), ufixed.minValue());

        assertEquals(((UnitType<?>) TypeFactory.create("int128")).maxValue(), fixed.maxValue());
        assertEquals(((UnitType<?>) TypeFactory.create("int128")).minValue(), fixed.minValue());
    }
}
