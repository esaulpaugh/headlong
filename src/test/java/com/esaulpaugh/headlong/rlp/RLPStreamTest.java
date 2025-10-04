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
package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Stream;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RLPStreamTest {

    private static final byte TEST_BYTE = 0x79;
    static final byte[] TEST_BYTES = Strings.decode("'wort'X3", UTF_8);
    private static final String TEST_STRING = "Le job à Monaco s’était mal passé ; son œil perdu se moquait sûrement de lui depuis les eaux claires et peu profondes au large de la côte … Mais bordel quand même, le « bric-à-brac » résultant était sécurisé, et tout serait vain s’il ne le gardait pas bien hors de vue des gendarmes encore à deux yeux.\n";

    static final byte[] RLP_BYTES = new byte[] {
            (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, (byte) 0xFF, (byte) 0x81, (byte) 0x90, (byte) 0x81, (byte) 0xb6, (byte) '\u230A',
            (byte) 0xb8, 56, 0x09,(byte)0x80,-1,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, -3, -2, 0, 0,
            (byte) 0xf8, 0x38, 0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 36, 74, 0, 0,
            (byte) 0x84, 'c', 'a', 't', 's',
            (byte) 0x84, 'd', 'o', 'g', 's',
            (byte) 0xca, (byte) 0x84, 92, '\r', '\n', '\f', (byte) 0x84, '\u0009', 'o', 'g', 's',
    };

    @Test
    public void testRLPOutputStream() throws Throwable {
        assertThrown(NullPointerException.class, () -> {try(RLPOutputStream ros = new RLPOutputStream(null)){}});
        try (Baos baos = new Baos(); RLPOutputStream ros = new RLPOutputStream(baos)) {
            ros.write(0xc0);
            ros.write(new byte[] { (byte) 0x7f, (byte) 0x20 });
            ros.writeSequence(new byte[] { 0x01 }, new byte[] { 0x02 });
            ros.writeSequence(Collections.singletonList(new byte[] { 0x03 }));
            ros.writeList(Arrays.asList(new byte[] { 0x04 }, new byte[] { 0x05 }, new byte[] { 0x06 }));
            byte[] bytes = baos.toByteArray();
            assertEquals("81c0827f20010203c3040506", Strings.encode(bytes));
        }
        Object[] objects = new Object[] {
                Strings.decode("0573490923738490"),
                new HashSet<byte[]>(),
                new Object[] { new byte[] { 0x77, 0x61 } }
        };
        try (Baos baos = new Baos(); RLPOutputStream ros = new RLPOutputStream(baos, 0)) {
            ros.writeSequence(objects);
            assertEquals(Notation.forObjects(objects), Notation.forEncoding(baos.toByteArray()));
        }

        assertThrown(IllegalArgumentException.class, "bufferLen too large: 131073 > 131072", () -> new RLPOutputStream(null, 131_073));

        try (Baos baos = new Baos(); RLPOutputStream ros = new RLPOutputStream(baos, 65536)) {
            ros.writeList(objects);
            assertEquals(Notation.forObjects((Object) objects), Notation.forEncoding(baos.toByteArray()));
            assertEquals("ce880573490923738490c0c3827761", baos.toString());
            assertEquals("ce880573490923738490c0c3827761", ros.toString());
        }
    }

    @Test
    public void testObjectRLPStream() throws IOException {

        // write RLP
        Baos baos = new Baos();
        try (ObjectOutputStream oos = new ObjectOutputStream(new RLPOutputStream(baos))) {
            oos.writeUTF("hello");
//        oos.flush();
            oos.writeChar('Z');
//            oos.writeObject(Tuple.of("jinro", new byte[] { (byte) 0xc0 }, new Boolean[] { false, true }));
            oos.flush();
        }

        // decode RLP
        Iterator<RLPItem> iter = RLP_STRICT.sequenceIterator(baos.toByteArray());
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        int count = 0;
        while (iter.hasNext()) {
            RLPItem item = iter.next();
            item.copyData(decoded);
            count++;
        }

        System.out.println("count = " + count);
        System.out.println("decoded len = " + decoded.toByteArray().length);

        // read objects
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decoded.toByteArray()));
        assertEquals("hello", ois.readUTF());
        assertEquals('Z', ois.readChar());
//        assertEquals(Tuple.of("jinro", new byte[] { (byte) 0xc0 }, new Boolean[] { false, true }), ois.readObject());
    }

    @Test
    public void testCopyToOutputStream() throws IOException {
        final byte[] encoding = new byte[] { (byte) 0xc1, (byte) 0x80 };
        final Baos b = new Baos();
        final RLPList x = RLP_STRICT.wrapList(encoding);
        assertArrayEquals(Strings.EMPTY_BYTE_ARRAY, b.toByteArray());
        x.copy(b);
        assertArrayEquals(encoding, b.toByteArray());
        x.copy(b);
        assertArrayEquals(new byte[] { (byte) 0xc1, (byte) 0x80, (byte) 0xc1, (byte) 0x80 }, b.toByteArray());
        x.copyData(b);
        assertArrayEquals(new byte[] { (byte) 0xc1, (byte) 0x80, (byte) 0xc1, (byte) 0x80, (byte) 0x80 }, b.toByteArray());
    }

    @Test
    public void testCopyToRLPOutputStream() throws IOException {
        final Random r = TestUtils.seededRandom();
        final byte[] encoding = new byte[] { (byte) 0x87, 0, 1, 2, 3, 4, 5, 6 };
        final RLPString y = RLP_STRICT.wrapString(encoding);
        {
            final Baos b = new Baos();
            final RLPOutputStream rlpOut = new RLPOutputStream(b, r.nextInt(26));
            y.copyData(rlpOut);
            assertArrayEquals(encoding, b.toByteArray());
        }
        final Baos b = new Baos();
        final RLPOutputStream rlpOut = new RLPOutputStream(b);
        y.copy(rlpOut);
        assertArrayEquals(new byte[] { (byte) 0x88, (byte) 0x87, 0, 1, 2, 3, 4, 5, 6 }, b.toByteArray());
    }

    @Test
    public void testStreamEasy() throws Throwable {
        RLPItem[] collected = RLPDecoderTest.collectAll(RLP_BYTES).toArray(RLPItem.EMPTY_ARRAY);
        Stream<RLPItem> stream = RLPDecoder.stream(RLP_STRICT.sequenceIterator(RLP_BYTES, 0));
        RLPItem[] streamed = stream.toArray(RLPItem[]::new);

        assertTrue(Arrays.deepEquals(collected, streamed));

        List<byte[]> encodings = new ArrayList<>(collected.length);
        for (RLPItem item : collected) {
            encodings.add(item.encoding());
        }

        assertThrown(IllegalArgumentException.class, "len is out of range: 10", () -> encodings.stream()
                .map(RLP_STRICT::wrapItem)
                .mapToInt(RLPItem::asInt)
                .forEach(System.out::println));
    }

    @Test
    public void testStreamHard() throws Throwable {
        new ReceiveStreamTask().call();
    }

    @Test
    public void testUnrecoverable() throws Throwable {
        try (PipedOutputStream pos = new PipedOutputStream();
             PipedInputStream pis = new PipedInputStream(pos, 512);
             Stream<RLPItem> stream = RLPDecoder.stream(RLP_STRICT.sequenceIterator(pis))) {
            pos.write(0x81);
            pos.write(0x00);
            Iterator<RLPItem> iter = stream.iterator();
            assertThrown(IllegalArgumentException.class, "invalid rlp for single byte @ 0", () -> System.out.println(iter.hasNext()));
            try (Stream<RLPItem> stream2 = RLPDecoder.stream(RLP_STRICT.sequenceIterator(pis))) {
                pos.write(0xf8);
                pos.write(0x37);
                Iterator<RLPItem> iter2 = stream2.iterator();
                for (int i = 0; i < 3; i++) {
                    assertThrown(
                            IllegalArgumentException.class,
                            "long element data length must be 56 or greater; found: 55 for element @ 0",
                            () -> System.out.println(iter2.hasNext())
                    );
                }
            }
        }
    }

    @Test
    public void testInterfaces() {
        try (Stream<RLPItem> stream = RLPDecoder.stream(RLP_STRICT.sequenceIterator(new ByteArrayInputStream(new byte[0])))) {
            stream.forEach(System.out::println);
        }
        for (RLPItem item : (Iterable<RLPItem>) () -> RLP_STRICT.sequenceIterator(new ByteArrayInputStream(new byte[3]))) {
            assertNotNull(item);
        }
    }

    private static class ReceiveStreamTask implements Callable<Void> {

        private final long startTime = System.nanoTime();

        @Override
        public Void call() throws Exception {
            final PipedInputStream pis = new PipedInputStream(512);
            final CyclicBarrier receiveBarrier = new CyclicBarrier(2);
            final CyclicBarrier sendBarrier = new CyclicBarrier(2);
            final Thread senderThread = new Thread(new SendStreamTask(startTime, pis, receiveBarrier, sendBarrier));
            try (final Closeable ignored = pis) {
                final Iterator<RLPItem> iter = RLP_STRICT.sequenceIterator(pis);

                senderThread.setPriority(Thread.MAX_PRIORITY);
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                senderThread.start();

                final Runnable[] subtasks = new Runnable[] {
                        () -> assertNoNext(iter),
                        () -> {
                            assertHasNext(iter);
                            assertArrayEquals(new byte[] { TEST_BYTE }, iter.next().asBytes());
                            assertNoNext(iter);
                            assertNoNext(iter);
                        },
                        () -> {
                            for (byte b : TEST_BYTES) {
                                assertHasNext(iter);
                                assertArrayEquals(new byte[] { b }, iter.next().asBytes());
                            }
                            assertNoNext(iter);
                        },
                        () -> assertNoNext(iter),
                        () -> assertNoNext(iter),
                        () -> {
                            assertHasNext(iter);
                            assertTrue(iter.hasNext());
                            assertEquals(TEST_STRING, iter.next().asString(UTF_8));
                            assertHasNext(iter);
                            assertTrue(iter.hasNext());
                            assertArrayEquals(new byte[] { TEST_BYTE }, iter.next().asBytes());
                            assertNoNext(iter);
                            assertNoNext(iter);
                        }
                };

                for(Runnable subtask : subtasks) {
                    signalWait(sendBarrier, receiveBarrier);
                    subtask.run();
                }

                senderThread.join();
            } catch (Exception ex) {
                senderThread.interrupt();
                throw ex;
            }
            return null;
        }

        private void assertNoNext(Iterator<RLPItem> iter) throws RuntimeException {
            try {
                RLPStreamTest.assertNoNext(startTime, iter);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        private void assertHasNext(Iterator<RLPItem> iter) {
            RLPStreamTest.assertHasNext(startTime, iter);
        }
    }

    private static class SendStreamTask implements Runnable {

        private final long startTime;
        private final Thread receiver;
        private final PipedOutputStream pos;
        private final CyclicBarrier receiveBarrier;
        private final CyclicBarrier sendBarrier;

        SendStreamTask(long startTime, PipedInputStream pis, CyclicBarrier receiveBarrier, CyclicBarrier sendBarrier) throws IOException {
            this.startTime = startTime;
            this.receiver = Thread.currentThread();
            this.pos = new PipedOutputStream(pis);
            this.sendBarrier = sendBarrier;
            this.receiveBarrier = receiveBarrier;
        }

        @Override
        public void run() {
            try (final Closeable ignored = this.pos) {
                final byte[] rlpString = RLPEncoder.string(Strings.decode(TEST_STRING, UTF_8));
                Runnable[] subtasks = new Runnable[]{
                        () -> write(TEST_BYTE),
                        () -> {
                            for (byte b : TEST_BYTES) {
                                write(b);
                            }
                        },
                        () -> write(rlpString[0]),
                        () -> write(rlpString[1]),
                        () -> {
                            for (int i = 2; i < rlpString.length; i++) {
                                write(rlpString[i]);
                            }
                            write(TEST_BYTE);
                        }
                };

                doWait(sendBarrier);
                for (Runnable subtask : subtasks) {
                    signalWait(receiveBarrier, sendBarrier);
                    subtask.run();
                }
                doWait(receiveBarrier);
            } catch (InterruptedException | IOException ex) {
                ex.printStackTrace();
                receiver.interrupt();
                throw new RuntimeException(ex);
            }
        }

        private void write(byte b) throws RuntimeException {
            try {
                pos.write(b);
                logWrite(startTime, "'" + (char) b + "' (0x" + Strings.encode(b) +")");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void doWait(CyclicBarrier barrier) throws InterruptedException {
        try {
            barrier.await();
        } catch (BrokenBarrierException bbe) {
            throw new RuntimeException(bbe);
        }
    }

    private static void signalWait(CyclicBarrier theirs, CyclicBarrier ours) throws InterruptedException {
        try {
            theirs.await(); // wake up other thread
            ours.await(); // wait to be woken up
        } catch (BrokenBarrierException bbe) {
            throw new RuntimeException(bbe);
        }
    }

    private static void assertHasNext(long zero, Iterator<RLPItem> iter) {
        assertTrue(iter.hasNext());
        logReceipt(zero, true);
    }

    private static void assertNoNext(long zero, Iterator<RLPItem> iter) throws Throwable {
        assertFalse(iter.hasNext());
        assertThrown(NoSuchElementException.class, iter::next);
        assertFalse(iter.hasNext());
        logReceipt(zero, false);
    }

    private static void logWrite(long startTime, String message) {
        System.out.println(timestamp(startTime) + "\u0009write " + message);
    }

    private static void logReceipt(long startTime, boolean hasNext) {
        System.out.println(timestamp(startTime) + '\u0009' + (hasNext ? "hasNext" : "no next"));
    }

    private static String timestamp(long startTime) {
        double elapsedMillis = (System.nanoTime() - startTime) / 1000000.0;
        String tString = String.valueOf(elapsedMillis);
        StringBuilder sb = new StringBuilder("t=");
        sb.append(tString);
        int n = 10 - tString.length();
        for (int i = 0; i < n; i++) {
            sb.append('0');
        }
        return sb.toString();
    }

    public static class Baos extends ByteArrayOutputStream {

        Baos() {}

        @Override
        public synchronized String toString() {
            return Strings.encode(buf, 0, count, Strings.HEX);
        }
    }
}
