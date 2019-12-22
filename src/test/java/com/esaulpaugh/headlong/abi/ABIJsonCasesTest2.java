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
import com.esaulpaugh.headlong.abi.util.Deserializer;
import com.esaulpaugh.headlong.util.JsonUtils;
import com.esaulpaugh.headlong.util.FastHex;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.encode;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ABIJsonCasesTest2 {

    private static final String ABI_V2_CASES_PATH = "tests/ethers-io/tests/tests/contract-interface-abi2.json";
    private static final String HEADLONG_CASES_PATH = "tests/headlong/tests/abi_tests.json";

    private static class TestCase {

        private final String name;

        private final Function function;
        private final TupleType types;
        private final Tuple values;
        private final byte[] result;

        public TestCase(JsonObject object, boolean function) throws ParseException {
            this.name = object.get("name").getAsString();
            String typesStr = object.get("types").getAsString();
            String valuesStr = object.get("values").getAsString();
            String resultStr = object.get("result").getAsString();

            this.types = Deserializer.parseTupleType(typesStr);
            this.values = Deserializer.parseTupleValue(this.types, valuesStr);
            this.result = FastHex.decode(resultStr, 2, resultStr.length() - 2);

            if(function) {
                this.function = Function.parse(name + types.canonicalType);
            } else {
                this.function = null;
            }
        }

        private boolean test(boolean function) throws ABIException {
            byte[] encoding = function ? this.function.encodeCall(values).array() : types.encode(values).array();
            try {
                assertArrayEquals(result, encoding);
                return true;
            } catch (AssertionError ae) {
                if(function) {
                    System.out.println(this.function.getCanonicalSignature() + ", " + this.values);
                    System.out.println(buildCallComparison(result, encoding));
                } else {
                    String[] resultTokens = TupleType.format(result).split("[\n]");
                    String[] encodingTokens = TupleType.format(encoding).split("[\n]");
                    System.out.println(types.canonicalType);
                    int i = 0;
                    for (; i < resultTokens.length; i++) {
                        String r = resultTokens[i];
                        String e = encodingTokens[i];
                        System.out.println(r + " " + e + (r.equals(e) ? "" : " ****"));
                    }
                    for (; i < encodingTokens.length; i++) {
                        System.out.println("----------------------------------------------------------------" + " " + encodingTokens[i]);
                    }
                }
                // return false;
                throw ae;
            }
        }

        private static String buildCallComparison(byte[] expected, byte[] actual) {

            StringBuilder sb = new StringBuilder("ID\t")
                    .append(encode(Arrays.copyOfRange(expected, 0, Function.SELECTOR_LEN), HEX)).append(' ')
                    .append(encode(Arrays.copyOfRange(actual, 0, Function.SELECTOR_LEN), HEX))
                    .append('\n');
            int idx = Function.SELECTOR_LEN;
            final int min = Math.min(expected.length, actual.length);
            while(idx < min) {
                byte[] expectedRow = Arrays.copyOfRange(expected, idx, idx + UNIT_LENGTH_BYTES);
                byte[] actualRow = Arrays.copyOfRange(actual, idx, idx + UNIT_LENGTH_BYTES);
                sb.append(idx >>> UnitType.LOG_2_UNIT_LENGTH_BYTES)
                        .append('\t')
                        .append(encode(expectedRow, HEX)).append(' ')
                        .append(encode(actualRow, HEX)).append(Arrays.equals(expectedRow, actualRow) ? "" : " ****")
                        .append('\n');
                idx += UNIT_LENGTH_BYTES;
            }
            return sb.toString();
        }
    }

    @Test
    public void testAbiV2Cases() throws ParseException, IOException, ABIException {
        final JsonArray testCases = JsonUtils.parseArray(TestUtils.readResourceAsString(ABIJsonCasesTest.class, ABI_V2_CASES_PATH));
        for (JsonElement e : testCases) {
            new TestCase(e.getAsJsonObject(), false).test(false);
        }
        System.out.println(testCases.size() + " cases passed");
    }

    @Test
    public void testHeadlongCases() throws ParseException, IOException, ABIException {
        final JsonArray testCases = JsonUtils.parseArray(TestUtils.readResourceAsString(ABIJsonCasesTest.class, HEADLONG_CASES_PATH));
        for (JsonElement e : testCases) {
            new TestCase(e.getAsJsonObject(), true).test(true);
        }
        System.out.println(testCases.size() + " cases passed");
    }
}
