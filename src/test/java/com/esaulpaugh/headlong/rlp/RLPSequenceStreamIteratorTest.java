package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.NoSuchElementException;

import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class RLPSequenceStreamIteratorTest {

    private final Object notifySender = new Object();
    private final Object notifyReceiver = new Object();

    private long zero;
    private int readNum;

    private static final byte TEST_BYTE = 0x79;
    private static final byte[] TEST_BYTES = new byte[] { 0x04, 0x03, 0x02 };
    private static final String TEST_STRING = "\u0009\u0009\u0030\u0031";

    private Thread newSendThread(PipedOutputStream pos) {
        return new Thread(() -> {
            try {
                endSend(true);
                write(pos, TEST_BYTE);
                endSend(true);
                for (byte b : TEST_BYTES) {
                    write(pos, b);
                }
                endSend(true);
                byte[] rlpString = RLPEncoder.encode(Strings.decode(TEST_STRING, UTF_8));
                int i = 0;
                write(pos, rlpString[i++]);
                endSend(true);
                write(pos, rlpString[i++]);
                endSend(true);
                while(i < rlpString.length) {
                    write(pos, rlpString[i++]);
                }
                write(pos, TEST_BYTE);
                endSend(false);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
    }

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
        endReceive();

        assertNoNext(iter);

        endReceive();

        assertReadSuccess(iter);
        Assert.assertArrayEquals(new byte[] { TEST_BYTE }, iter.next().data());
        assertNoNext(iter);

        endReceive();

        for (byte b : TEST_BYTES) {
            assertReadSuccess(iter);
            Assert.assertArrayEquals(timestamp(), new byte[] { b }, iter.next().data());
        }
        assertNoNext(iter);

        endReceive();

        assertNoNext(iter);

        endReceive();

        assertNoNext(iter);

        endReceive();

        assertReadSuccess(iter);
        Assert.assertTrue(iter.hasNext());
        Assert.assertTrue(iter.hasNext());
        Assert.assertEquals(TEST_STRING, iter.next().asString(UTF_8));
        assertReadSuccess(iter);
        Assert.assertTrue(iter.hasNext());
        Assert.assertArrayEquals(new byte[] { TEST_BYTE }, iter.next().data());
        TestUtils.assertThrown(NoSuchElementException.class, iter::next);
        assertNoNext(iter);
        Assert.assertFalse(iter.hasNext());
        Assert.assertFalse(iter.hasNext());
        TestUtils.assertThrown(NoSuchElementException.class, iter::next);

        send.join();
    }

    @Test
    public void testStream2() throws IOException {
        readNum = -1;

        final PipedOutputStream pos = new PipedOutputStream();
        final PipedInputStream pis = new PipedInputStream(pos, 512);

        RLPSequenceStreamIterator iter = new RLPSequenceStreamIterator(RLPDecoder.RLP_STRICT, pis);

        pos.write((byte) 0x82);
        pos.write(0x48);
        pos.write(0x48);
        pos.write(100);
        pos.write(101);
        Assert.assertArrayEquals(new byte[] { 0x48, 0x48 }, iter.next().data());
        pos.write(102);
        iter.hasNext();
        Assert.assertArrayEquals(new byte[] { 100, 101, 102 }, iter.buffer());
        Assert.assertArrayEquals(new byte[] { 100 }, iter.next().data());
    }

    private void write(OutputStream os, byte b) throws IOException {
        os.write(b);
        logWrite(FastHex.encodeToString(b));
    }

    private void endSend(boolean wait) throws InterruptedException {
        synchronized (notifyReceiver) {
            notifyReceiver.notify();
        }
        if(wait) {
            synchronized (notifySender) {
                notifySender.wait();
            }
        }
//        System.out.println(timestamp() + "\u0009now sending");
    }

    private void endReceive() throws InterruptedException {
        synchronized (notifySender) {
            notifySender.notify();
        }
        synchronized (notifyReceiver) {
            notifyReceiver.wait();
        }
//        System.out.println(timestamp() + "\u0009now receiving");
    }

    private void assertReadSuccess(RLPSequenceStreamIterator iter) {
        Assert.assertTrue("no next() found, " + timestamp(), iter.hasNext());
        logRead(true);
    }

    private void assertNoNext(RLPSequenceStreamIterator iter) {
        if(iter.hasNext()) {
            throw new AssertionError("unexpected next(): " + iter.next().asString(HEX) + ", " + timestamp());
        }
        logRead(false);
    }

    private void logWrite(String message) {
        System.out.println(timestamp() + "\u0009write " + message);
    }

    private void logRead(boolean success) {
        System.out.println(timestamp() + "\u0009read " + (success ? "success, #" + ++readNum : "failure"));
    }

    private String timestamp() {
        double t = (System.nanoTime() - zero) / 1000000.0;
        String tString = String.valueOf(t);
        StringBuilder sb = new StringBuilder("t=");
        sb.append(tString);
        int n = 10 - tString.length();
        for (int i = 0; i < n; i++) {
            sb.append('0');
        }
        return sb.toString();
    }
}
