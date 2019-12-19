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
import com.esaulpaugh.headlong.exception.DecodeException;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Random;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static org.junit.jupiter.api.Assertions.*;

public class EqualsTest {

    @Test
    public void testEquals() throws ParseException {

        Random r = new Random(TestUtils.getSeed(System.nanoTime()));

        int n = 0;
        do {

            MonteCarloTestCase mctc = new MonteCarloTestCase(r.nextLong());

            String canonical = mctc.function.getCanonicalSignature();
            if(mctc.rawSignature.equals(canonical)) {
                continue;
            }

            Function a = mctc.function;
            Function b = new Function(canonical);

//            System.out.println(raw);

            boolean equals = a.getParamTypes().recursiveEquals(b.getParamTypes());

//            System.out.println(equals);

            assertTrue(equals);

            assertEquals(a, b);

            n++;
        } while (n < 100);
    }

    @Test
    public void testBooleanNotEquals() throws Throwable {
        Function f = new Function("baz(uint32,bool)");
        Tuple argsTuple = new Tuple(69L, true);
        ByteBuffer one = f.encodeCall(argsTuple);

        final byte[] array = one.array();

        System.out.println(Function.formatCall(array));

        Tuple decoded;

        array[array.length - 1] = 0;
        System.out.println(Function.formatCall(array));
        decoded = f.decodeCall(array);
        assertNotEquals(decoded, argsTuple);

        array[array.length - 32] = (byte) 0x80;
        System.out.println(Function.formatCall(array));
        assertThrown(DecodeException.class, "exceeds bit limit", () -> f.decodeCall(array));

        for (int i = array.length - 32; i < array.length; i++) {
            array[i] = (byte) 0xFF;
        }
        array[array.length - 1] = (byte) 0xFE;
        System.out.println(Function.formatCall(array));
        assertThrown(DecodeException.class, "signed value given for unsigned type", () -> f.decodeCall(array));
    }
}
