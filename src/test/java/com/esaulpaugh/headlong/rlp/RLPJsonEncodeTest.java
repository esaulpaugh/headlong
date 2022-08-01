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
import com.esaulpaugh.headlong.abi.util.JsonUtils;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static com.esaulpaugh.headlong.TestUtils.parseArrayToBytesHierarchy;
import static com.esaulpaugh.headlong.TestUtils.parseBigIntegerStringPoundSign;
import static com.esaulpaugh.headlong.TestUtils.parseLong;
import static com.esaulpaugh.headlong.TestUtils.parseString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class RLPJsonEncodeTest {

    @Test
    public void testCases() throws IOException {

        String testCasesJson = TestUtils.readFileResourceAsString("tests/ethereum/RLPTests/rlptest.json");

        for (Map.Entry<String, JsonElement> entry : parseEntrySet(testCasesJson)) {
            JsonObject value = entry.getValue().getAsJsonObject();
            assertArrayEquals(parseOut(value), parseIn(value));
        }
    }

    static Set<Map.Entry<String, JsonElement>> parseEntrySet(String json) {
        return JsonUtils.parse(json)
                .getAsJsonObject()
                .entrySet();
    }

    static byte[] parseIn(JsonObject value) {
        JsonElement in = value.get("in");
        if(in.isJsonArray()) {
            return RLPEncoder.list(parseArrayToBytesHierarchy(in.getAsJsonArray()));
        } else if(in.isJsonPrimitive()) {
            try {
                return RLPEncoder.string(Integers.toBytes(parseLong(in)));
            } catch (NumberFormatException nfe) {
                return RLPEncoder.string(
                        in.getAsString().startsWith("#")
                                ? parseBigIntegerStringPoundSign(in).toByteArray()
                                : Strings.decode(parseString(in), Strings.UTF_8)
                );
            }
        } else if(in.isJsonObject()) {
            throw new Error("unexpected json object");
        } else if(in.isJsonNull()) {
            throw new Error("unexpected json null");
        } else {
            throw new Error();
        }
    }

    static byte[] parseOut(JsonObject value) {
        JsonElement out = value.get("out");
        System.out.println(out);
        String outString = out.getAsString();
        return outString.startsWith("0x")
                ? FastHex.decode(outString, 2, outString.length() - 2)
                : FastHex.decode(outString);
    }
}
