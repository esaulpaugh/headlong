package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.NoSuchElementException;

import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class RLPSequenceStreamIteratorTest {

    @Ignore
    @Test
    public void testStream() throws Throwable {

        final byte[] TEST_BYTES = new byte[] { 0x00, 0x00, 0x00 };
        final String TEST_STRING = "\u0009\u0009";

        final long zero = System.currentTimeMillis();

        final PipedOutputStream pos = new PipedOutputStream();

        Thread send = new Thread(() -> {
            try {
                pos.write(1);
                Thread.sleep(20L);
                for (byte b : TEST_BYTES) {
                    pos.write(b);
                    logWrite(zero);
                }
                Thread.sleep(20L);
                pos.write(RLPEncoder.encode(Strings.decode(TEST_STRING, UTF_8)));
                logWrite(zero);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        int readNum = 0;


        PipedInputStream pis = new PipedInputStream();
        pis.connect(pos);

        RLPSequenceStreamIterator iter = new RLPSequenceStreamIterator(RLPDecoder.RLP_STRICT, pis);

        send.setPriority(Thread.MAX_PRIORITY);
        send.start();
        send.setPriority(Thread.MAX_PRIORITY);

        Assert.assertFalse(iter.hasNext());
        logRead(zero, readNum++, false);

        while(pis.available() <= 0) {
            Thread.sleep(0, 500_000);
            System.out.print('.');
        }
        System.out.println();

        assertAvailable(pis);
        Assert.assertTrue(iter.hasNext());
        logRead(zero, readNum++, true);

        Assert.assertArrayEquals(new byte[] { 1 }, iter.next().data());

        Assert.assertFalse(iter.hasNext());
        logRead(zero, readNum++, false);

        Thread.sleep(20L);

        assertAvailable(pis);
        Assert.assertTrue(iter.hasNext());
        logRead(zero, readNum++, true);

        Assert.assertTrue(iter.hasNext());
        Assert.assertArrayEquals(new byte[1], iter.next().data());
        Assert.assertTrue(iter.hasNext());
        Assert.assertArrayEquals(new byte[1], iter.next().data());
        Assert.assertTrue(iter.hasNext());
        Assert.assertArrayEquals(new byte[1], iter.next().data());

        Assert.assertFalse(iter.hasNext());
        logRead(zero, readNum++, false);

        Thread.sleep(20L);

        assertAvailable(pis);
        Assert.assertTrue(iter.hasNext());
        logRead(zero, readNum++, true);
        Assert.assertTrue(iter.hasNext());
        Assert.assertTrue(iter.hasNext());

        Assert.assertEquals(TEST_STRING, iter.next().asString(UTF_8));
        TestUtils.assertThrown(NoSuchElementException.class, iter::next);

        Assert.assertFalse(iter.hasNext());
        logRead(zero, readNum++, false);
        Assert.assertFalse(iter.hasNext());
        Assert.assertFalse(iter.hasNext());
        TestUtils.assertThrown(NoSuchElementException.class, iter::next);

        send.join();
    }

    private static void assertAvailable(InputStream is) throws IOException {
        final int available = is.available();
        Assert.assertTrue("available: " + available, available > 0);
    }

    private static void logWrite(final long zero) {
        System.out.println("t=" + (System.currentTimeMillis() - zero) + "\u0009write");
    }

    private static void logRead(final long zero, int readNum, boolean success) {
        System.out.println("t=" + (System.currentTimeMillis() - zero) + "\u0009read " + (success ? "success" : "failure") + ", #" + readNum);
    }
}
