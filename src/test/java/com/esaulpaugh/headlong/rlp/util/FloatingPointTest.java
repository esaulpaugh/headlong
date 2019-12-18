package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.exception.DecodeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

public class FloatingPointTest {

    @Test
    public void testFloat() throws DecodeException {
        Random r = new Random(TestUtils.getSeed(System.nanoTime()));
        for (int i = 0; i < 20; i++) {
            final float flo = r.nextFloat();
            byte[] floBytes = FloatingPoint.toBytes(flo);
            byte[] floPutted = new byte[floBytes.length];
            FloatingPoint.putFloat(flo, floPutted, 0);
            Assertions.assertArrayEquals(floBytes, floPutted);

            float floGotten = FloatingPoint.getFloat(floBytes, 0, floBytes.length);
            Assertions.assertEquals(flo, floGotten);
        }
    }

    @Test
    public void testDouble() throws DecodeException {
        Random r = new Random(TestUtils.getSeed(System.nanoTime()));
        for (int i = 0; i < 20; i++) {
            final double dub = r.nextDouble();
            byte[] dubBytes = FloatingPoint.toBytes(dub);
            byte[] dubPutted = new byte[dubBytes.length];
            FloatingPoint.putDouble(dub, dubPutted, 0);
            Assertions.assertArrayEquals(dubBytes, dubPutted);

            double dubGotten = FloatingPoint.getDouble(dubBytes, 0, dubBytes.length);
            Assertions.assertEquals(dub, dubGotten);
        }
    }

    @Test
    public void testBigDecimal() {
        Random r = new Random(TestUtils.getSeed(System.nanoTime()));
        for (int i = 0; i < 20; i++) {
            byte[] random = new byte[1 + r.nextInt(20)];
            final BigDecimal bigDec = new BigDecimal(new BigInteger(random), r.nextInt(20));
            byte[] bytes = bigDec.unscaledValue().toByteArray();

            BigDecimal gotten = FloatingPoint.getBigDecimal(bytes, 0, bytes.length, bigDec.scale());
            Assertions.assertEquals(bigDec, gotten);
        }
    }
}
