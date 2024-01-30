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

import com.esaulpaugh.headlong.util.FastHex;
import org.junit.jupiter.api.Test;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class EventTest {

    @Test
    public void testEvent() {
        final String name = "ahoy";
        final boolean[] indexed = new boolean[] { false, false, true, false, true };
        final String paramsString ="(int,uint,(),bool[],ufixed256x10)";
        Event<?> event = Event.create(name, TupleType.parse(paramsString), indexed);

        assertEquals(TypeEnum.EVENT, event.getType());
        assertEquals(name, event.getName());
        assertEquals(TupleType.parse(paramsString), event.getInputs());
        assertArrayEquals(indexed, event.getIndexManifest());
        for (int i = 0; i < indexed.length; i++) {
            assertEquals(indexed[i], event.isElementIndexed(i));
        }
        assertFalse(event.isAnonymous());

        assertEquals(TupleType.parse("((),ufixed256x10)"), event.getIndexedParams());
        assertEquals(TupleType.parse("(int256,uint256,bool[])"), event.getNonIndexedParams());
    }

    private static final String EVENT_JSON = "{\n" +
            "    \"type\": \"event\",\n" +
            "    \"name\": \"an_event\",\n" +
            "    \"inputs\": [\n" +
            "      {\n" +
            "        \"name\": \"a\",\n" +
            "        \"type\": \"bytes\",\n" +
            "        \"indexed\": true\n" +
            "      },\n" +
            "      {\n" +
            "        \"name\": \"b\",\n" +
            "        \"type\": \"uint256\",\n" +
            "        \"indexed\": false\n" +
            "      }\n" +
            "    ]\n" +
            "  }";

    @Test
    public void testTrailingBytes() throws Throwable {
        assertThrown(IllegalArgumentException.class,
                "unconsumed bytes: 2 remaining",
                () -> Event.fromJson(EVENT_JSON).decodeData(FastHex.decode("000000000000000000000000000000000000000000000000000000000000ffff0d0c")));
    }
}
