package com.esaulpaugh.headlong.rlp.util;

import org.junit.Assert;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import static com.esaulpaugh.headlong.rlp.util.Strings.BASE64;

public class StringsTest {

    @Test
    public void base64() {
        Random r = new Random(new SecureRandom().nextLong());
        for (int i = 0; i < 10_000_000; i++) {
            byte[] x = new byte[r.nextInt(14)];
            r.nextBytes(x);
            String s = Strings.encode(x, BASE64);
            byte[] y = Strings.decode(s, BASE64);
            Assert.assertTrue(Arrays.equals(x, y));
        }
    }
}
