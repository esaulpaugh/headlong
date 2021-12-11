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
import com.esaulpaugh.headlong.rlp.util.Notation;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RLPJsonDecodeTest {

    @Test
    public void testValid() throws IOException {
        String exampleJson = TestUtils.readFileResourceAsString("tests/ethereum/RLPTests/RandomRLPTests/example.json");

        for (Map.Entry<String, JsonElement> e : RLPJsonEncodeTest.parseEntrySet(exampleJson)) {
            decodeRecursively(RLPJsonEncodeTest.parseOut(e.getValue().getAsJsonObject()));
        }
    }

    @Test
    public void testInvalid() throws IOException {

        String testCasesJson = TestUtils.readFileResourceAsString("tests/ethereum/RLPTests/invalidRLPTest.json");

        for (Map.Entry<String, JsonElement> e : RLPJsonEncodeTest.parseEntrySet(testCasesJson)) {

            byte[] invalidRLP = RLPJsonEncodeTest.parseOut(e.getValue().getAsJsonObject());

            Throwable throwable = null;
            try {
                decodeRecursively(invalidRLP);
            } catch (Throwable t) {
                throwable = t;
            }
            if(!(throwable instanceof IllegalArgumentException)) {
                System.err.println(Notation.forEncoding(invalidRLP));
                throw new RuntimeException("no decode exception! " + e.getKey() + " " + e.getValue());
            }
        }
    }

    static void decodeRecursively(byte[] rlp) {
        RLPItem item = RLPDecoder.RLP_STRICT.wrap(rlp);
        if(item instanceof RLPString) {
            item.asString(Strings.HEX);
        } else {
            ArrayList<Object> collector = new ArrayList<>();
            elementsRecursive((RLPList) item, collector, RLPDecoder.RLP_STRICT);
        }
    }

    private static void elementsRecursive(RLPList list, Collection<Object> results, RLPDecoder decoder) {
        List<RLPItem> actualList = list.elements(decoder);
        for (RLPItem element : actualList) {
            if(element instanceof RLPList) {
                List<Object> subList = new ArrayList<>();
                elementsRecursive((RLPList) element, subList, decoder);
                results.add(subList);
            } else {
                results.add(element);
            }
        }
    }
}
