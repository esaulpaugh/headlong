package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.abi.MonteCarloTest;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;

import static com.esaulpaugh.headlong.util.Strings.BASE64;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.NO_PADDING;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class StringsTest {

    private void randomTest(Random rand, int utf8) {
        for (int i = 0; i < 20_000; i++) {
            byte[] x = new byte[rand.nextInt(1000)]; // rand.nextInt(14)
            rand.nextBytes(x);
            String s = Strings.encode(x, utf8);
            byte[] y = Strings.decode(s, utf8);
            Assert.assertArrayEquals(x, y);
        }
    }

    @Test
    public void utf8() {
        Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        for (int j = 0; j < 20_000; j++) {
            byte[] x = new byte[rand.nextInt(1000)]; // rand.nextInt(14)
            for (int i = 0; i < x.length; i++) {
//                x[i] = (byte) (r.nextInt(95) + 32);
                x[i] = (byte) rand.nextInt(128);
            }
            String s = Strings.encode(x, UTF_8);
//            System.out.println(s);
            byte[] y = Strings.decode(s, UTF_8);
            Assert.assertArrayEquals(x, y);
        }
    }

    @Test
    public void base64() {
        Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        randomTest(rand, BASE64);
    }

    @Test
    public void hex() {
        Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        randomTest(rand, HEX);
    }

    @Test
    public void noPadding() {
        Random rand = new Random(MonteCarloTest.getSeed(System.nanoTime()));
        for (int i = 0; i < 20_000; i++) {
            byte[] x = new byte[rand.nextInt(1000)]; // rand.nextInt(14)
            rand.nextBytes(x);
            String s = Strings.toBase64(x, 0, x.length, NO_PADDING);
            byte[] y = Strings.fromBase64(s, NO_PADDING);
            Assert.assertArrayEquals(x, y);
        }
    }
}
