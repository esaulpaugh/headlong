package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.util.Strings;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class RLPSequenceStreamIteratorTest {

    @Ignore // run this by itself
    @Test
    public void testStream() throws IOException, DecodeException, InterruptedException {

        final long zero = System.currentTimeMillis();

        final PipedOutputStream pos = new PipedOutputStream();

        Thread send = new Thread(() -> {
            try {
                Thread.sleep(25L);
                pos.write(new byte[] { 0x00, 0x00, 0x00 });
                logWrite(zero);
                Thread.sleep(50L);
                pos.write(RLPEncoder.encode(Strings.decode("\u0009\u0009", UTF_8)));
                logWrite(zero);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });


        PipedInputStream pis = new PipedInputStream();
        pis.connect(pos);

        RLPSequenceStreamIterator iter = new RLPSequenceStreamIterator(RLPDecoder.RLP_STRICT, pis);

        send.start();

        assertNotAvailable(pis);
        Assert.assertFalse(iter.hasNext());
        logRead(false, zero);

        Thread.sleep(50L);

        assertAvailable(pis);
        Assert.assertTrue(iter.hasNext());
        logRead(true, zero);

        Assert.assertArrayEquals(new byte[1], iter.next().data());
        Assert.assertTrue(iter.hasNext());
        Assert.assertArrayEquals(new byte[1], iter.next().data());
        Assert.assertTrue(iter.hasNext());
        Assert.assertArrayEquals(new byte[1], iter.next().data());

        assertNotAvailable(pis);
        Assert.assertFalse(iter.hasNext());
        logRead(false, zero);

        Thread.sleep(50L);

        assertAvailable(pis);
        Assert.assertTrue(iter.hasNext());
        logRead(true, zero);

        Assert.assertEquals("\u0009\u0009", iter.next().asString(UTF_8));

        assertNotAvailable(pis);
        Assert.assertFalse(iter.hasNext());
        logRead(false, zero);

        send.join();
    }

    private static void read(InputStream is, byte[] buffer) throws IOException {
        int available = is.available();
        int read = 0;
        while (read < available) {
            read += is.read(buffer);
        }
        System.out.println("read = " + read);
    }

    private static void assertNotAvailable(InputStream is) throws IOException {
        final int available = is.available();
        Assert.assertFalse("available: " + available, available > 0);
    }

    private static void assertAvailable(InputStream is) throws IOException {
        final int available = is.available();
        Assert.assertTrue("available: " + available, available > 0);
    }

    private static void logWrite(final long zero) {
        System.out.println("t=" + (System.currentTimeMillis() - zero) + "\u0009write");
    }

    private static void logRead(boolean success, final long zero) {
        System.out.println("t=" + (System.currentTimeMillis() - zero) + "\u0009read " + (success ? "success" : "failure"));
    }
}
