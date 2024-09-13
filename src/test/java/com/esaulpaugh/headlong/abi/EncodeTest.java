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
import com.esaulpaugh.headlong.util.Uint;
import com.esaulpaugh.headlong.util.WrappedKeccak;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.TestUtils.assertThrownWithAnySubstring;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EncodeTest {

    private static final Class<IllegalArgumentException> ILLEGAL = IllegalArgumentException.class;

    private static final String LETTERS     = "abcdefgilmnorstuxy";
    private static final String BASE_TYPE   = "[(" + LETTERS + "]+[" + LETTERS + ")\\d]+";
    private static final String SUFFIX      = "(\\)|\\[\\d*])*";
    private static final String SINGLE_TYPE = BASE_TYPE + SUFFIX;
    private static final Pattern TYPE_PATTERN       = Pattern.compile(SINGLE_TYPE + "(," + SINGLE_TYPE + ")*");
    private static final Pattern TUPLE_TYPE_PATTERN = Pattern.compile("^\\((" + TYPE_PATTERN + ")?\\)$");

    @Disabled("may take minutes to run")
    @SuppressWarnings("deprecation")
    @Test
    public void fuzzSignatures() throws InterruptedException, ExecutionException, TimeoutException {

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
                950_000,
                950_000,
                950_000,
                950_000,
                950_000,
                950_000,
                950_000,
                950_000,
                950_000
        };

        final String prefix = "ufixed";
        final int prefixLen = prefix.length();

        final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        final IntConsumer runnable = (int id) -> {
            final Random rand = TestUtils.seededRandom();
            for (int len = 0; len <= 14; len++) {
                System.out.println(len + "(" + Thread.currentThread().getId() + ")");
                final byte[] temp = new byte[len];
                final int last = len - 1;
                if (len > 0) {
                    temp[0] = '(';
                    if (len > prefixLen) {
                        for (int i = 0; i < prefixLen; i++) {
                            temp[i + 1] = (byte) prefix.charAt(i);
                        }
                    }
                    if (len > 1) {
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
                        TupleType<?> tt = TupleType.parse(sig);
                        if (map.containsKey(sig)) continue;
                        String canon = tt.canonicalType;
                        System.out.println("\t\t\t" + len + ' ' + sig + (sig.equals(canon) ? "" : " --> " + canon));
                        if (!TYPE_PATTERN.matcher(sig).matches() || !TYPE_PATTERN.matcher(canon).matches()) {
                            throw new RuntimeException("tuple fails TYPE_PATTERN: " + sig + " " + canon);
                        }
                        for (ABIType<?> t : tt) {
                            if (!TYPE_PATTERN.matcher(t.canonicalType).matches()) {
                                throw new RuntimeException("element fails TYPE_PATTERN: " + t.canonicalType);
                            }
                        }
                        if (!TUPLE_TYPE_PATTERN.matcher(sig).matches() || !TUPLE_TYPE_PATTERN.matcher(canon).matches()) {
                            throw new RuntimeException("tuple fails TUPLE_TYPE_PATTERN: " + sig + " " + canon);
                        }
                        map.put(sig, canon);
                    } catch (IllegalArgumentException ignored) {
                        /* do nothing */
                    } catch (Throwable t) {
                        System.err.println(sig);
                        throw new RuntimeException(t);
                    }
                }
            }
        };
        TestUtils.parallelRun(Runtime.getRuntime().availableProcessors(), 3600L, runnable)
                .run();

        final int size = map.size();
        System.out.println("\nsize=" + size);

        List<String> keyList = Collections.list(map.keys());
        Collections.sort(keyList);

        keyList.forEach(System.out::println);

        assertEquals(2 + (32 * 80), size); // for prefix fixed or ufixed
    }

    private static final String ABI = "a71100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000700000000000000000000000000000000000000000000000000000000000000005a0000000000000000000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000001a000000000000000000000000000000000000000000000000000000000000004a00000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000230c5748dd399896196f734a17ecf80b2178adc067b361d5ba994ae6400000000b95ffb49ee209c0d760c3350ecaae6d4ad4ff7b62af4d91ef3b5ae4e00000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000001a00000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000001816e178e3509b80a6a66a8827c8d5e62be37692efe0e22e59f936b8b000000000000000000000000000000000000000000000000000000000000000000000001a48b4c097589c15784ef0b97c17aa6dbb752f61fd1a272618d87a535000000000000000000000000000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000033b6ae013f517ec3c2082aa77400d768168266af2c3c3680404075f300000000093ab24d3e36bb7194c68063c595ead7554b2e8ce3fa1e6498c0c1208000000009fd62e78a55909d9e882d50a8074384af470c9c13e618ccc761f09a700000000000000000000000000000000000000000000000000000000000000000000000182a731ded7c7b3dd45c528564df732e138a71dbccd14954c45b5c14e000000000000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000001a0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000010daf12dcf419efe89f949a26f943bd7236aafbb8d6c4749fd27ed36f000000000000000000000000000000000000000000000000000000000000000000000003e6501ff6bc4074725885f5e65cc263b9ab0f098c14bc7cce3c2e4d2900000000f351c27189ce03746ce7dab3c560fb2bb430551b55a77a18380f066100000000e9753f8a5bb5e0da442d7e11d38d1eaafe5b6df463fd2256be199e380000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000001a0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000d05d841062e000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000001b052db5529dc0433400000000000000000000000000000000000000000000000000000000000000030000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000013160000000000000000000000000000000000000000000000000000000000000001000000000051299437b715c73400c5e6e655c925eb10ee34669cf8f1ccf8febd";

    @Test
    public void testFunctionAnnotate() {
        final Function foo = Function.parse("foo()");
        assertEquals("foo:\nID       c2985578", foo.annotateCall(Tuple.of()));
        assertEquals("", TupleType.EMPTY.annotate(new byte[0]));
        final Function f = new Function(
                TypeEnum.FUNCTION,
                "do_something",
                TypeFactory.createTupleTypeWithNames("(bool,(),string,(int8,uint8),address,(uint16,bytes))", "isFree", "dummy", "word", "pair", "cntct", "tup"),
                TupleType.EMPTY,
                "payable",
                Function.newDefaultDigest()
        );
        final Tuple args = Tuple.of(
                true,
                Tuple.EMPTY,
                "libertad..........................................................",
                Tuple.of(-128, 255),
                Address.wrap(Address.toChecksumAddress(BigInteger.TEN.shiftLeft(156))),
                Tuple.of(65535, "carajo]0]0]0]0]0]0]0]0]0]0]0]0]0".getBytes(StandardCharsets.US_ASCII))
        );
        final String annotated = f.annotateCall(args);
        assertEquals(
                "do_something:\n" +
                        "ID       d88de50f\n" +
                        "     0   0000000000000000000000000000000000000000000000000000000000000001\t[0] bool \"isFree\"\n" +
                        "    20   00000000000000000000000000000000000000000000000000000000000000c0\t[2] string \"word\" offset\n" +
                        "    40   ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff80\t[3] (int8,uint8) \"pair\"\n" +
                        "    60   00000000000000000000000000000000000000000000000000000000000000ff\t[3] ...\n" +
                        "    80   000000000000000000000000a000000000000000000000000000000000000000\t[4] address \"cntct\"\n" +
                        "    a0   0000000000000000000000000000000000000000000000000000000000000140\t[5] (uint16,bytes) \"tup\" offset\n" +
                        "    c0   0000000000000000000000000000000000000000000000000000000000000042\t[2] string length\n" +
                        "    e0   6c696265727461642e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e\t[2] string\n" +
                        "   100   2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e2e\t[2] ...\n" +
                        "   120   2e2e000000000000000000000000000000000000000000000000000000000000\t[2] ...\n" +
                        "   140   000000000000000000000000000000000000000000000000000000000000ffff\t[5] (uint16,bytes)\n" +
                        "   160   0000000000000000000000000000000000000000000000000000000000000040\t[5] ...\n" +
                        "   180   0000000000000000000000000000000000000000000000000000000000000020\t[5] ...\n" +
                        "   1a0   636172616a6f5d305d305d305d305d305d305d305d305d305d305d305d305d30\t[5] ...",
                f.annotateCall(f.encodeCall(args).array())
        );
        System.out.println(annotated);
    }

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
        final byte[] master = new byte[260];
        Arrays.fill(master, (byte) 0xff);
        for (int i = 0; i < master.length; i++) {
            byte[] x = Arrays.copyOfRange(master, 0, i);
            int mod = i % UNIT_LENGTH_BYTES;
            if (mod == expectedMod) {
                String formatted = format.apply(x);
                assertEquals(i * 2, formatted.codePoints().filter(ch -> ch == 'f').count());
                int div = (i - expectedMod) / UNIT_LENGTH_BYTES;
                if (div > 0) {
                    String ffff = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
                    boolean containsFfff = formatted.contains(ffff);
                    Assertions.assertTrue(containsFfff);
                    final String labeled;
                    if (func) {
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
                assertThrown(ILLEGAL, "expected length mod 32 == 0, found: ", () -> System.out.println(format.apply(x)));
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

        assertThrown(ILLEGAL, "unrecognized type: \"(())int)\"", () -> TupleType.parse("(())int)"));
        assertThrown(ILLEGAL, "unrecognized type: \"(int)())\"", () -> TupleType.parse("(int)())"));

        assertThrownWithAnySubstring(ClassCastException.class, CLASS_CAST_MESSAGES, () -> Function.parse("f()[]"));

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
        assertThrownWithAnySubstring(
                StringIndexOutOfBoundsException.class,
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
        assertThrown(ILLEGAL, "unrecognized type: \"(,\"", () -> Function.parse("(,"));

        assertThrown(ILLEGAL, "unrecognized type: \"(,)\"", () -> new Function("baz(,)"));

        assertThrown(ILLEGAL, "unrecognized type: \"(bool,)\"", () -> new Function("baz(bool,)"));

        assertThrown(ILLEGAL, "@ index 1, unrecognized type: \"(int,,)\"", () -> new Function("baz(bool,(int,,))"));
    }

    @Test
    public void illegalCharsTest() throws Throwable {
        assertThrown(ILLEGAL, "illegal char 0x153 '\u0153' @ index 0", () -> Function.parse("\u0153()"));

        assertThrown(ILLEGAL, "illegal char 0x2a6 '\u02a6' @ index 2", () -> new Function("ba\u02a6z(uint32,bool)"));

        assertThrown(ILLEGAL, "@ index 1, @ index 0, unrecognized type: \"bool\u02a6\"", () -> new Function("baz(int32,(bool\u02a6))"));
    }

    @Test
    public void simpleFunctionTest() {
        Function f = new Function("baz(uint32,bool)", "(uint32,bool)"); // canonicalizes and parses any signature automatically
        Pair<Long, Boolean> args = Tuple.of(69L, true);

        // Two equivalent styles:
        ByteBuffer one = f.encodeCall(args);
        ByteBuffer two = f.encodeCallWithArgs(69L, true);

        assertArrayEquals(one.array(), two.array());

        System.out.println(Function.formatCall(one.array())); // a multi-line hex representation

        assertEquals(0, two.position());
        Tuple decoded = f.decodeCall(two);
        assertEquals(Function.SELECTOR_LEN + UNIT_LENGTH_BYTES * 2, two.position());
        assertEquals(two.limit(), two.position());

        assertEquals(decoded, args);

        one.position(Function.SELECTOR_LEN);
        Tuple dec = f.decodeReturn(one);
        assertEquals(one.limit(), one.position());
        assertEquals(decoded, dec);
    }

    @Test
    public void testArrayLen() throws Throwable {
        assertThrown(ILLEGAL, "@ index 0, bad array length", () -> Function.parse("abba(()[-04])"));

        assertThrown(ILLEGAL, "@ index 0, bad array length", () -> Function.parse("zaba(()[04])"));

        assertEquals(4, Function.parse("yaba(()[4])").getInputs().get(0).asArrayType().getLength());
    }

    @Test
    public void uint8ArrayTest() {
        Function f = new Function("baz(uint8[])");

        Tuple args = Single.of(new int[] { 0xFF, 1, 1, 2, 0 });
        ByteBuffer two = f.encodeCall(args);

        Tuple decoded = f.decodeCall(two);

        assertEquals(decoded, args);
    }

    @Test
    public void tupleArrayTest() {
        Function f = new Function("((int16)[2][][1])");

        Object[] argsIn = new Object[] {
                new Tuple[][][] { new Tuple[][] { new Tuple[] { Single.of(9), Single.of(-11) } } }
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
        TupleType<Tuple> a = TupleType.of(baseType + "[" + n + "]");

        String[] types = new String[n];
        Arrays.fill(types, baseType);
        TupleType<Tuple> b = TupleType.parse("(" + TupleType.of(types) + ")");

        System.out.println(a + " vs " + b);

        for (int i = 0; i < args.length; i++) {
            args[i] = supplier.get();
        }

        Tuple aArgs = Single.of(args);
        Tuple bArgs = Single.of(Tuple.from(args));

        byte[] aEncoding = a.encode(aArgs).array();
        ByteBuffer bDest = ByteBuffer.allocate(b.measureEncodedLength(bArgs));
        assertEquals(0, bDest.position());
        b.encode(bArgs, bDest);
        assertEquals(bDest.limit(), bDest.position());
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
        final TupleType<Tuple> paramTypes = f.getInputs();

        StringBuilder sb = new StringBuilder();
        for (ABIType<?> type : paramTypes.elementTypes) {
            sb.append(type.getClass().getSimpleName()).append(',');
        }
        Assertions.assertEquals("BooleanType,IntType,LongType,BigIntegerType,AddressType,BigDecimalType,ArrayType,TupleType,ArrayType,ArrayType,", sb.toString());

        Tuple args = Tuple.from(
                true,
                1,
                1L,
                BigInteger.valueOf(8L),
                Address.wrap("0x0000000000000000000000000000000000000009"),
                new BigDecimal(BigInteger.TEN, 18),
                new byte[] { 1, 0 },
                Single.of("\u0002"),
                new byte[] { 0x04 },
                new byte[] { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23 }
        );

        final int prefixBytes = 7;
        final int trailingBytes = 8;
        final int len = prefixBytes + f.measureCallLength(args) + trailingBytes;

        byte[] ffff = new byte[len];
        Arrays.fill(ffff, (byte) 0xff);

        ByteBuffer full = (ByteBuffer) ByteBuffer.wrap(ffff).position(7);
        ByteBuffer empty = (ByteBuffer) ByteBuffer.allocate(len).position(7);

        f.encodeCall(args, full);
        assertEquals(len - trailingBytes, full.position());
        f.encodeCall(args, empty);
        assertEquals(len - trailingBytes, empty.position());

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
        TupleType<Triple<Object, Object, Object>> tt = TupleType.parse("(uint64,uint32,bool[])");

        Object a = BigInteger.valueOf(7L);
        Object b = 9L;
        Object c = new boolean[0];

        ByteBuffer ee = tt.encode(Tuple.of(a, b, c));

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
                "BigDecimal scale mismatch: expected scale 9 but found 1",
                () -> Function.parse("(fixed56x9)").encodeCall(Single.of(new BigDecimal("0.2")))
        );
    }

    @Test
    public void testNestedErrMessage() throws Throwable {
        assertThrown(
                IllegalArgumentException.class,
                "tuple index 0: array index 1: signed val exceeds bit limit: 9 >= 8",
                () -> Function.parse("(int8[])").encodeCall(Single.of(new int[] { 120, 256 }))
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
            final long mask = uint.rangeLong - 1;
            if (uint.numBits < 63) {
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
                assertEquals(i, Long.bitCount(uMax));

                assertEquals(0, Long.numberOfLeadingZeros(min));
                assertEquals(i - 1, Long.numberOfTrailingZeros(min));
                assertEquals(0, Long.numberOfTrailingZeros(max));
                assertEquals(i - 1, Long.bitCount(max));
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
    private static final BigDecimal O_0000000001 = new BigDecimal("0.0000000001");
    private static final BigDecimal O_000000000000000001 = new BigDecimal("0.000000000000000001");

    @Test
    public void testDecimalMinMax() throws Throwable {
        final BigIntegerType decimal = TypeFactory.create("decimal");
        assertEquals(decimal, TypeFactory.create("int168"));

        final BigDecimal decimalMin = new BigDecimal("-18707220957835557353007165858768422651595.9365500928");
        final BigDecimal decimalMax = new BigDecimal("18707220957835557353007165858768422651595.9365500927");

        assertThrown(ILLEGAL, "signed val exceeds bit limit: 168 >= 168", () -> decimal.validate(decimalMin.unscaledValue().subtract(BigInteger.ONE)));
        decimal.validate(decimalMin.unscaledValue());
        decimal.validate(decimalMax.unscaledValue());
        assertThrown(ILLEGAL, "signed val exceeds bit limit: 168 >= 168", () -> decimal.validate(decimalMax.unscaledValue().add(BigInteger.ONE)));
    }

    @Test
    public void testFixedUFixedMinMax() throws Throwable {
        final BigDecimalType fixed = TypeFactory.create("fixed");
        assertEquals(((UnitType<?>) TypeFactory.create("int128")).maxValue(), fixed.maxValue());
        assertEquals(((UnitType<?>) TypeFactory.create("int128")).minValue(), fixed.minValue());

        final BigDecimalType ufixed = TypeFactory.create("ufixed");
        final BigDecimal u128Max = new BigDecimal(new BigInteger("340282366920938463463374607431768211455"), 18);

        assertEquals(0.0d, ufixed.minDecimal().doubleValue(), DELTA);
        assertEquals(u128Max.doubleValue(), ufixed.maxDecimal().doubleValue(), DELTA);

        ufixed.validate(new BigDecimal(BigInteger.ZERO, 18));
        assertThrown(ILLEGAL, "BigDecimal scale mismatch: expected scale 18 but found 10", () -> ufixed.validate(O_0000000001.negate()));
        ufixed.validate(O_000000000000000001);
        assertThrown(ILLEGAL, "signed value given for unsigned type", () -> ufixed.validate(O_000000000000000001.negate()));

        ufixed.validate(u128Max);
        assertThrown(ILLEGAL, "unsigned val exceeds bit limit: 129 > 128", () -> ufixed.validate(u128Max.add(BigDecimal.ONE)));
        assertThrown(ILLEGAL, "unsigned val exceeds bit limit: 129 > 128", () -> ufixed.validate(u128Max.add(O_0000000001)));

        assertEquals(((UnitType<?>) TypeFactory.create("uint128")).maxValue(), ufixed.maxValue());
        assertEquals(((UnitType<?>) TypeFactory.create("uint128")).minValue(), ufixed.minValue());
    }

    @Test
    public void testCasts() throws Throwable {
        final Object[] args = new Object[] { (byte) -1, (short) 10, BigInteger.valueOf(10L), new BigDecimal(BigInteger.valueOf(57L), 1), 0f, -2.1d, new AtomicInteger(), new AtomicLong(98L) };
        {
            final ABIType<Object> int8 = TypeFactory.create("int8");
            assertEquals(Integer.class, int8.clazz());
            for (Object arg : args) {
                testCast(arg.getClass(), int8, arg);
            }
            testCast(Long.class, int8, 3L);
        }
        {
            final ABIType<Object> uint24 = TypeFactory.create("uint24");
            assertEquals(Integer.class, uint24.clazz());
            for (Object arg : args) {
                testCast(arg.getClass(), uint24, arg);
            }
            testCast(Long.class, uint24, 3L);
        }
        {
            final ABIType<Object> int64 = TypeFactory.create("int64");
            assertEquals(Long.class, int64.clazz());
            for (Object arg : args) {
                testCast(arg.getClass(), int64, arg);
            }
            testCast(Integer.class, int64, 5);
        }
        {
            final ABIType<Object> uint56 = TypeFactory.create("uint56");
            assertEquals(Long.class, uint56.clazz());
            for (Object arg : args) {
                testCast(arg.getClass(), uint56, arg);
            }
            testCast(Integer.class, uint56, 5);
        }
    }

    private static void testCast(Class<?> from, ABIType<Object> type, Object arg) throws Throwable {
        final String expectedMsg = "class mismatch: " + from.getName() + " != " + type.clazz().getName() + " (" + type + " requires " + type.clazz().getSimpleName() + " but found " + from.getSimpleName();
        assertThrown(IllegalArgumentException.class, expectedMsg, () -> type.validate(arg));
        assertThrown(IllegalArgumentException.class, expectedMsg, () -> type.encode(arg));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCasts2() throws Throwable {
        assertThrown(ClassCastException.class, () -> ((ABIType<Object>) (ABIType<?>) ByteType.INSTANCE).encode(7));
        testCast(Integer.class, TypeFactory.create("uint64"), 10);
        testCast(Byte.class, TypeFactory.create("bool"), (byte) 11);
        testCast(BigInteger.class, TypeFactory.create("address"), BigInteger.valueOf(12L));
        testCast(Double.class, TypeFactory.create("int96"), 13.2d);
        testCast(Long.class, TypeFactory.create("fixed"), 14L);
    }

    @Test
    public void testSingletonTypes() {
        assertEquals("BYTE", ByteType.INSTANCE.canonicalType);
        assertEquals("BYTE", ByteType.INSTANCE.getCanonicalType());
        assertEquals(Byte.class, ByteType.INSTANCE.clazz);
        assertEquals(Byte.class, ByteType.INSTANCE.clazz());
        assertFalse(ByteType.INSTANCE.dynamic);
        assertFalse(ByteType.INSTANCE.isDynamic());

        assertEquals(byte[].class, ByteType.INSTANCE.arrayClass());
        assertEquals(1, ByteType.INSTANCE.typeCode());
        assertEquals(1, ABIType.TYPE_CODE_BYTE);

        assertEquals("bool", BooleanType.INSTANCE.canonicalType);
        assertEquals("bool", BooleanType.INSTANCE.getCanonicalType());
        assertEquals(Boolean.class, BooleanType.INSTANCE.clazz);
        assertEquals(Boolean.class, BooleanType.INSTANCE.clazz());
        assertFalse(BooleanType.INSTANCE.dynamic);
        assertFalse(BooleanType.INSTANCE.isDynamic());

        assertEquals(boolean[].class, BooleanType.INSTANCE.arrayClass());
        assertEquals(0, BooleanType.INSTANCE.typeCode());
        assertEquals(0, BooleanType.TYPE_CODE_BOOLEAN);

        assertEquals("address", AddressType.INSTANCE.canonicalType);
        assertEquals("address", AddressType.INSTANCE.getCanonicalType());
        assertEquals(Address.class, AddressType.INSTANCE.clazz);
        assertEquals(Address.class, AddressType.INSTANCE.clazz());
        assertFalse(AddressType.INSTANCE.dynamic);
        assertFalse(AddressType.INSTANCE.isDynamic());

        assertEquals(Address[].class, AddressType.INSTANCE.arrayClass());
        assertEquals(8, AddressType.INSTANCE.typeCode());
        assertEquals(8, AddressType.TYPE_CODE_ADDRESS);
    }
}
