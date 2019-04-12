package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.NoSuchElementException;

import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class RLPSequenceStreamIteratorTest {

    private static final long STEP_MILLIS = 12L;

    private final Object lock = new Object();

    private long zero;
    private int readNum;

    private static final byte TEST_BYTE = 0x79;
    private static final byte[] TEST_BYTES = new byte[] { 0x04, 0x03, 0x02 };
    private static final String TEST_STRING = "\u0009\u0009";

//    @Ignore // try increasing STEP_MILLIS first
    @Test
    public void testStream() throws Throwable {

        readNum = -1;

        final PipedOutputStream pos = new PipedOutputStream();
        final PipedInputStream pis = new PipedInputStream(pos, 512);

        RLPSequenceStreamIterator iter = new RLPSequenceStreamIterator(RLPDecoder.RLP_STRICT, pis);

        Thread send = newSendThread(pos);
        send.setPriority(Thread.MAX_PRIORITY);
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        zero = System.nanoTime();
        send.start();
        synchronized (lock) {
            lock.wait();
        }

        assertReadFailure(iter);

        Thread.sleep(STEP_MILLIS);

        assertReadSuccess(pis, iter);
        Assert.assertArrayEquals(new byte[] { TEST_BYTE }, iter.next().data());
        assertReadFailure(iter);

        Thread.sleep(STEP_MILLIS);

        assertReadSuccess(pis, iter);
        for (byte b : TEST_BYTES) {
            Assert.assertTrue(iter.hasNext());
            Assert.assertArrayEquals(new byte[] { b }, iter.next().data());
        }
        assertReadFailure(iter);

        Thread.sleep(STEP_MILLIS);

        assertReadSuccess(pis, iter);
        Assert.assertTrue(iter.hasNext());
        Assert.assertTrue(iter.hasNext());
        Assert.assertEquals(TEST_STRING, iter.next().asString(UTF_8));
        TestUtils.assertThrown(NoSuchElementException.class, iter::next);
        assertReadFailure(iter);
        Assert.assertFalse(iter.hasNext());
        Assert.assertFalse(iter.hasNext());
        TestUtils.assertThrown(NoSuchElementException.class, iter::next);

        send.join();
    }

    private Thread newSendThread(PipedOutputStream pos) {
        return new Thread(() -> {
            synchronized (lock) {
                lock.notify();
            }
            try {
                Thread.sleep(STEP_MILLIS / 2);
                pos.write(TEST_BYTE);
                logWrite(zero);
                Thread.sleep(STEP_MILLIS);
                for (byte b : TEST_BYTES) {
                    pos.write(b);
                    logWrite(zero);
                }
                Thread.sleep(STEP_MILLIS);
                pos.write(RLPEncoder.encode(Strings.decode(TEST_STRING, UTF_8)));
                logWrite(zero);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void assertReadSuccess(InputStream is, RLPSequenceStreamIterator iter) throws IOException {
        assertAvailable(is);
        Assert.assertTrue(iter.hasNext());
        logRead(zero, ++readNum, true);
    }

    private void assertReadFailure(RLPSequenceStreamIterator iter) {
        Assert.assertFalse(iter.hasNext());
        logRead(zero, ++readNum, false);
    }

    private static void assertAvailable(InputStream is) throws IOException {
        final int available = is.available();
        Assert.assertTrue("available: " + available, available > 0);
    }

    private static void logWrite(final long zero) {
        System.out.println("t=" + timestamp(zero) + "\u0009write");
    }

    private static void logRead(final long zero, int readNum, boolean success) {
        System.out.println("t=" + timestamp(zero) + "\u0009read " + (success ? "success" : "failure") + ", #" + readNum);
    }

    private static String timestamp(long zero) {
        double t = (System.nanoTime() - zero) / 1000000.0;
        String tString = String.valueOf(t);
        StringBuilder sb = new StringBuilder(tString);
        int n = 10 - tString.length();
        for (int i = 0; i < n; i++) {
            sb.append('0');
        }
        return sb.toString();
    }
}
