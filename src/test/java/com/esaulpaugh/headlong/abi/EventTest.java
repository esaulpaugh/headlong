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

import org.junit.jupiter.api.Test;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

public class EventTest {

    @Test
    public void testEvent() throws ParseException {
        final String name = "ahoy";
        final boolean[] indexed = new boolean[] { false, false, true, false, true };
        final String paramsString ="(int,uint,(),bool[],ufixed256x10)";
        Event event = new Event(name, paramsString, indexed);

        assertEquals(name, event.getName());
        assertEquals(TupleType.parse(paramsString), event.getParams());
        assertArrayEquals(indexed, event.getIndexManifest());
        assertFalse(event.isAnonymous());

        assertEquals(TupleType.parse("((),ufixed256x10)"), event.getIndexedParams());
        assertEquals(TupleType.parse("(int256,uint256,bool[])"), event.getNonIndexedParams());
    }
}
