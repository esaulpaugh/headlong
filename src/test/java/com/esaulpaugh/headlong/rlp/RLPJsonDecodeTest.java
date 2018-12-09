package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.rlp.util.Notation;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class RLPJsonDecodeTest {

    @Test
    public void testInvalid() throws IOException, DecodeException {

        String testCasesJson = TestUtils.readResourceAsString(RLPJsonEncodeTest.class, "tests/json/invalidRLPTest.json");

        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(testCasesJson);
        JsonObject obj = element.getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> entries = obj.entrySet();

        for (Map.Entry<String, JsonElement> e : entries) {

            JsonObject jsonObject = e.getValue().getAsJsonObject();
            System.out.println(jsonObject);
            JsonElement out = jsonObject.get("out");

            String outString = out.getAsString();

            byte[] invalidRLP = FastHex.decode(outString);

            Throwable throwable = null;
            try {
                RLPItem item = RLPDecoder.RLP_STRICT.wrap(invalidRLP);
                if(item instanceof RLPString) {
                    item.asString(Strings.HEX);
                } else {
                    ArrayList<Object> collector = new ArrayList<>();
                    ((RLPList) item).elementsRecursive(collector, RLPDecoder.RLP_STRICT);
                }
            } catch (Throwable t) {
                throwable = t;
            }
            if(!(throwable instanceof DecodeException)) {
                System.err.println(Notation.forEncoding(invalidRLP).toString());
                throw new RuntimeException("no decode exception! " + e.getKey() + " " + e.getValue());
            }
        }
    }

}
