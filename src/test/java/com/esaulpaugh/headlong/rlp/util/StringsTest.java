package com.esaulpaugh.headlong.rlp.util;

import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Random;

import static com.esaulpaugh.headlong.rlp.util.Strings.BASE64;
import static com.esaulpaugh.headlong.rlp.util.Strings.HEX;
import static com.esaulpaugh.headlong.rlp.util.Strings.NO_PADDING;
import static com.esaulpaugh.headlong.rlp.util.Strings.UTF_8;

public class StringsTest {

    private void randomTest(Random r, int utf8) {
        for (int i = 0; i < 500_000; i++) {
            byte[] x = new byte[r.nextInt(1000)]; // r.nextInt(14)
            r.nextBytes(x);
            String s = Strings.encode(x, utf8);
            byte[] y = Strings.decode(s, utf8);
            Assert.assertArrayEquals(x, y);
        }
    }

    @Test
    public void utf8() {
        Random r = new Random(new SecureRandom().nextLong());
        for (int j = 0; j < 500_000; j++) {
            byte[] x = new byte[r.nextInt(1000)]; // r.nextInt(14)
            for (int i = 0; i < x.length; i++) {
//                x[i] = (byte) (r.nextInt(95) + 32);
                x[i] = (byte) r.nextInt(128);
            }
            String s = Strings.encode(x, UTF_8);
//            System.out.println(s);
            byte[] y = Strings.decode(s, UTF_8);
            Assert.assertArrayEquals(x, y);
        }
    }

    @Test
    public void base64() {
        Random r = new Random(new SecureRandom().nextLong());
        randomTest(r, BASE64);
    }

    @Test
    public void hex() {
        Random r = new Random(new SecureRandom().nextLong());
        randomTest(r, HEX);
    }

    @Test
    public void noPadding() {
        Random r = new Random(new SecureRandom().nextLong());
        for (int i = 0; i < 500_000; i++) {
            byte[] x = new byte[r.nextInt(1000)]; // r.nextInt(14)
            r.nextBytes(x);
            String s = Strings.toBase64(x, 0, x.length, NO_PADDING);
            byte[] y = Strings.fromBase64(s, NO_PADDING);
            Assert.assertArrayEquals(x, y);
        }
    }
}
