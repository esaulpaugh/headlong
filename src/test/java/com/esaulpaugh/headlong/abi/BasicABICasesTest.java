/*
   Copyright 2019-2026 Evan Saulpaugh

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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class BasicABICasesTest {

    private static final String RESOURCE = "tests/ethereum/ABITests/basic_abi_tests.json";

    static final JsonObject TESTS;

    static {
        try {
            TESTS = TestUtils.parseObject(TestUtils.readFileResourceAsString(RESOURCE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ABITestCase {
        final String key;

        final JsonArray args;
        final JsonArray types;
        final String result;
        final Function function;

        private ABITestCase(String key, JsonArray args, String result, JsonArray types, Function function) {
            this.key = key;
            this.args = args;
            this.types = types;
            this.result = result;
            this.function = function;
        }

        static ABITestCase forKey(String key) {
            final JsonObject jsonObject = TESTS.get(key).getAsJsonObject();

            JsonArray args = jsonObject.getAsJsonArray("args");
            String result = jsonObject.get("result").getAsString();
            JsonArray types = jsonObject.getAsJsonArray("types");

            final String[] typeStrings = new String[types.size()];
            for (int i = 0; i < types.size(); i++) {
                typeStrings[i] = types.get(i).getAsString();
            }
            TupleType<?> tt = TupleType.of(typeStrings);

            System.out.println(tt.canonicalType);

            return new ABITestCase(key, args, result, types, new Function("test" + tt.canonicalType));
        }

        void test(Object[] argsArray) {

            Tuple t = Tuple.from(argsArray);
            ByteBuffer bb = function.encodeCall(t);

            System.out.println("expected:   " + result);
            System.out.println("actual:     " + Strings.encode(Arrays.copyOfRange(bb.array(), Function.SELECTOR_LEN, bb.limit())));

            assertArrayEquals(Strings.decode(result), Arrays.copyOfRange(bb.array(), Function.SELECTOR_LEN, bb.limit()));
        }
    }

    @Test
    public void testGithubWikiTest() {

        ABITestCase testCase = ABITestCase.forKey("GithubWikiTest");

        Object[] argsArray = new Object[testCase.args.size()];
        argsArray[0] = parseBigInteger(testCase.args.get(0));
        argsArray[1] = parseLongArray(testCase.args.get(1).getAsJsonArray());
        argsArray[2] = parseBytesX(testCase.args.get(2).getAsString(), 10);
        argsArray[3] = Strings.decode(testCase.args.get(3).getAsString(), Strings.UTF_8);

        testCase.test(argsArray);
    }

    @Test
    public void testSingleInteger() {

        ABITestCase testCase = ABITestCase.forKey("SingleInteger");

        Object[] argsArray = new Object[testCase.args.size()];
        argsArray[0] = parseBigInteger(testCase.args.get(0));

        testCase.test(argsArray);
    }

    @Test
    public void testIntegerAndAddress() {

        ABITestCase testCase = ABITestCase.forKey("IntegerAndAddress");

        Object[] argsArray = new Object[testCase.args.size()];
        argsArray[0] = parseBigInteger(testCase.args.get(0));
        argsArray[1] = Address.wrap(Address.toChecksumAddress(testCase.args.get(1).getAsString()));

        testCase.test(argsArray);
    }

    public static long[] parseLongArray(final JsonArray array) {
        final int size = array.size();
        long[] longs = new long[size];
        for (int i = 0; i < size; i++) {
            JsonElement element = array.get(i);
            if (element.isJsonPrimitive()) {
                longs[i] = element.getAsLong();
            } else {
                throw new Error("unexpected element type");
            }
        }
        return longs;
    }

    public static byte[] parseBytesX(String string, int x) {
        if (string.length() == x) {
            byte[] bytesX = new byte[x];
            for (int i = 0; i < x; i++) {
                bytesX[i] = (byte) string.charAt(i);
            }
            return bytesX;
        } else {
            return Strings.decode(string);
        }
    }

    private static BigInteger parseBigInteger(JsonElement in) {
        return new BigInteger(in.getAsString(), 10);
    }
}
