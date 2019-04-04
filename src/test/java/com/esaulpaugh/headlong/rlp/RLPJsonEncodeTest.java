package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.rlp.util.Integers;
import com.esaulpaugh.headlong.util.FastHex;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static com.esaulpaugh.headlong.TestUtils.*;

public class RLPJsonEncodeTest {

    @Test
    public void testCases() throws IOException {

        String testCasesJson = TestUtils.readResourceAsString(RLPJsonEncodeTest.class, "tests/RLPTests/rlptest.json");

        for (Map.Entry<String, JsonElement> entry : parseEntrySet(testCasesJson)) {

            JsonObject jsonObject = entry.getValue().getAsJsonObject();
            JsonElement in = jsonObject.get("in");

            byte[] expected = getOutBytes(entry);
            byte[] actual;
            if(in.isJsonArray()) {

                ArrayList<Object> arrayList = parseArrayToBytesHierarchy(in.getAsJsonArray());

                actual = RLPEncoder.encodeAsList(arrayList);


            } else if(in.isJsonObject()) {
                System.err.println("json object");
                parseObject(in);
                actual = null;
            } else {

                try {
                    long lo = parseLong(in);
                    actual = RLPEncoder.encode(Integers.toBytes(lo));
                } catch (NumberFormatException nfe) {

                    String inString = in.getAsString();

                    byte[] inBytes;
                    if(inString.startsWith("#")) {
                        BigInteger inBigInt = parseBigIntegerStringPoundSign(in);
                        inBytes = inBigInt.toByteArray();
                        actual = RLPEncoder.encode(inBytes);
                    } else {
                        String string = parseString(in);
                        inBytes = string.getBytes(Charset.forName("UTF-8"));
                        actual = RLPEncoder.encode(inBytes);
                    }
                }
            }

            String e = FastHex.encodeToString(expected);
            String a = FastHex.encodeToString(actual);
            try {
                Assert.assertEquals(e, a);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    static Set<Map.Entry<String, JsonElement>> parseEntrySet(String json) {
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(json);
        JsonObject obj = element.getAsJsonObject();
        return obj.entrySet();
    }

    static byte[] getOutBytes(Map.Entry<String, JsonElement> e) {
        JsonObject jsonObject = e.getValue().getAsJsonObject();

        System.out.println(jsonObject);

        JsonElement out = jsonObject.get("out");

        String outString = out.getAsString();

        return FastHex.decode(outString.substring(outString.indexOf("0x") + "0x".length()));
    }
}
