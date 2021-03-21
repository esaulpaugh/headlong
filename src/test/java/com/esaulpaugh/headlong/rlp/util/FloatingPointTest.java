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
package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.util.Integers;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FloatingPointTest {

    @Test
    public void testFloat() {
        Random r = TestUtils.seededRandom();
        for (int i = 0; i < 20; i++) {
            final float flo = r.nextFloat();
            byte[] floBytes = FloatingPoint.toBytes(flo);
            byte[] floPutted = new byte[floBytes.length];
            int len = FloatingPoint.putFloat(flo, floPutted, 0);
            assertEquals(floBytes.length, len);
            assertArrayEquals(floBytes, floPutted);

            float floGotten = FloatingPoint.getFloat(floBytes, 0, floBytes.length, false);
            assertEquals(flo, floGotten);
        }
    }

    @Test
    public void testDouble() {
        Random r = TestUtils.seededRandom();
        for (int i = 0; i < 20; i++) {
            final double dub = r.nextDouble();
            byte[] dubBytes = FloatingPoint.toBytes(dub);
            byte[] dubPutted = new byte[dubBytes.length];
            int len = FloatingPoint.putDouble(dub, dubPutted, 0);
            assertEquals(dubBytes.length, len);
            assertArrayEquals(dubBytes, dubPutted);

            double dubGotten = FloatingPoint.getDouble(dubBytes, 0, dubBytes.length, false);
            assertEquals(dub, dubGotten);
        }
    }

    @Test
    public void testBigDecimal() {
        Random r = TestUtils.seededRandom();
        for (int i = 0; i < 20; i++) {
            byte[] random = new byte[1 + r.nextInt(20)];
            final BigDecimal bigDec = new BigDecimal(new BigInteger(random), r.nextInt(20));
            byte[] bytes = Integers.toBytesUnsigned(bigDec.unscaledValue());

            BigDecimal gotten = getBigDecimal(bytes, 0, bytes.length, bigDec.scale(), false);
            assertEquals(bigDec, gotten);
        }
    }

    public static BigDecimal getBigDecimal(byte[] bytes, int i, int unscaledNumBytes, int scale, boolean lenient) {
        return new BigDecimal(Integers.getBigInt(bytes, i, unscaledNumBytes, lenient), scale);
    }
}
