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
import com.esaulpaugh.headlong.util.JsonUtils;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ABIJsonCasesTest {

    private static final String RESOURCE = "tests/ethereum/ABITests/basic_abi_tests.json";

    private static final String TEST_CASES;

    static {
        try {
            TEST_CASES = TestUtils.readResourceAsString(ABIJsonCasesTest.class, RESOURCE);
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

        private static ABITestCase forKey(String key) throws ParseException {

            JsonObject tests = JsonUtils.parseObject(TEST_CASES);
            Set<Map.Entry<String, JsonElement>> entries = tests.entrySet();

            JsonObject jsonObject = null;
            for (Map.Entry<String, JsonElement> e : entries) {
                if (key.equals(e.getKey())) {
                    jsonObject = e.getValue().getAsJsonObject();
                    System.out.println(jsonObject);
                    break;
                }
            }
            if (jsonObject == null) {
                throw new RuntimeException(key + " not found");
            }

            JsonArray args = JsonUtils.getArray(jsonObject, "args");
            String result = JsonUtils.getString(jsonObject, "result");
            JsonArray types = JsonUtils.getArray(jsonObject, "types");

            ABIType<?>[] array = new ABIType[types.size()];

            int i = 0;
            for (JsonElement type : types) {
                String s = type.getAsString();
                final int openIdx = s.indexOf('(');
                if (openIdx >= 0) {
                    array[i++] = Function.parse("(" + s + ")").getParamTypes().get(0);
                } else {
                    array[i++] = TypeFactory.create(s);
                }
            }

            TupleType tt = TupleType.wrap(array);

            System.out.println(tt.canonicalType);

            return new ABITestCase(key, args, result, types, new Function("test" + tt.canonicalType));
        }

        private void test(Object[] argsArray) throws ValidationException {

            Tuple t = new Tuple(argsArray);
            ByteBuffer bb = function.encodeCall(t);

            System.out.println("expected:   " + result);
            System.out.println("actual:     " + Strings.encode(Arrays.copyOfRange(bb.array(), Function.SELECTOR_LEN, bb.limit())));

            assertArrayEquals(FastHex.decode(result), Arrays.copyOfRange(bb.array(), Function.SELECTOR_LEN, bb.limit()));
        }
    }

    @Test
    public void testGithubWikiTest() throws ParseException, ValidationException {

        ABITestCase testCase = ABITestCase.forKey("GithubWikiTest");

        Object[] argsArray = new Object[testCase.args.size()];
        argsArray[0] = TestUtils.parseBigInteger(testCase.args.get(0));
        argsArray[1] = TestUtils.parseIntArray(testCase.args.get(1).getAsJsonArray());
        argsArray[2] = TestUtils.parseBytesX(testCase.args.get(2).getAsString(), 10);
        argsArray[3] = TestUtils.parseBytes(testCase.args.get(3).getAsString());

        testCase.test(argsArray);
    }

    @Test
    public void testSingleInteger() throws ParseException, ValidationException {

        ABITestCase testCase = ABITestCase.forKey("SingleInteger");

        Object[] argsArray = new Object[testCase.args.size()];
        argsArray[0] = TestUtils.parseBigInteger(testCase.args.get(0));

        testCase.test(argsArray);
    }

    @Test
    public void testIntegerAndAddress() throws ParseException, ValidationException {

        ABITestCase testCase = ABITestCase.forKey("IntegerAndAddress");

        Object[] argsArray = new Object[testCase.args.size()];
        argsArray[0] = TestUtils.parseBigInteger(testCase.args.get(0));
        argsArray[1] = TestUtils.parseAddress(testCase.args.get(1));

        testCase.test(argsArray);
    }
}
