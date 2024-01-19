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
import com.esaulpaugh.headlong.util.Deserializer;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.encode;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class AdvancedABICasesTest {

    private static final String ABI_V2_CASES_PATH = "tests/ethers-io/tests/tests/contract-interface-abi2.json";
    private static final String HEADLONG_CASES_PATH = "tests/headlong/tests/abi_tests.json";

    private static class TestCase {

        private final String name;

        private final Function function;
        private final TupleType types;
        private final Tuple values;
        private final byte[] result;

        public TestCase(JsonObject object, boolean function) {
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

        boolean test(boolean function) {
            byte[] encoding = function ? this.function.encodeCall(values).array() : types.encode(values).array();
            try {
                assertArrayEquals(result, encoding);
                return true;
            } catch (AssertionError ae) {
                if(function) {
                    System.out.println(this.function.getCanonicalSignature() + ", " + this.values);
                    System.out.println(buildCallComparison(result, encoding));
                } else {
                    String[] resultTokens = ABIType.format(result).split("\n");
                    String[] encodingTokens = ABIType.format(encoding).split("\n");
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
                    .append(encode(Arrays.copyOfRange(expected, 0, Function.SELECTOR_LEN), HEX))
                    .append("                                                         ")
                    .append(encode(Arrays.copyOfRange(actual, 0, Function.SELECTOR_LEN), HEX))
                    .append('\n');
            int idx = Function.SELECTOR_LEN;
            final int min = Math.min(expected.length, actual.length);
            while(idx < min) {
                byte[] expectedRow = Arrays.copyOfRange(expected, idx, idx + UNIT_LENGTH_BYTES);
                byte[] actualRow = Arrays.copyOfRange(actual, idx, idx + UNIT_LENGTH_BYTES);
                sb.append(idx / UNIT_LENGTH_BYTES)
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
    public void testAbiV2Cases() throws IOException {
        runCases(JsonUtils.parseArray(TestUtils.readFileResourceAsString(ABI_V2_CASES_PATH)), false);
    }

    @Test
    public void testHeadlongCases() throws IOException {
        runCases(JsonUtils.parseArray(TestUtils.readFileResourceAsString(HEADLONG_CASES_PATH)), true);
    }

    private static final String HEADLONG_X = "{\n" +
            "    \"name\": \"headlong_X\",\n" +
            "    \"types\": \"[\\\"uint24\\\",\\\"address\\\"]\",\n" +
            "    \"values\": \"[{\\\"type\\\":\\\"number\\\",\\\"value\\\":\\\"237\\\"},{\\\"type\\\":\\\"string\\\",\\\"value\\\":\\\"0x0000000000000A6E5195B6E7458D14A52989dAA9\\\"}]\",\n" +
            "    \"result\": \"0x9808bf8500000000000000000000000000000000000000000000000000000000000000ed0000000000000000000000000000000000000a6e5195b6e7458d14a52989daa9\",\n" +
            "    \"version\": \"5.6.0+commit.6447409\"\n" +
            "  }";

    @Test
    public void testCase() {
        TestCase tc = new TestCase(JsonUtils.parseObject(HEADLONG_X), true);
        tc.test(true);
    }

    private static void runCases(final JsonArray cases, final boolean function) {
        int i = 0;
        for (JsonElement e : cases) {
            TestCase tc = new TestCase(e.getAsJsonObject(), function);
//            System.out.println(i + ", " + tc.name);
            tc.test(function);
            i++;
        }
        System.out.println(i + "/" + cases.size() + " cases passed");
    }
}
