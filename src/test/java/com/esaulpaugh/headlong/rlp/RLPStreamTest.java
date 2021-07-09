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
import com.esaulpaugh.headlong.rlp.util.Notation;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RLPStreamTest {

    private static final byte TEST_BYTE = 0x79;
    static final byte[] TEST_BYTES = Strings.decode("'wort'X3", UTF_8);
    private static final String TEST_STRING = "2401";

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
		Object[] objects = new Object[] {
				Strings.decode("0573490923738490"),
				new HashSet<byte[]>(),
				new Object[] { new byte[] { 0x77, 0x61 } }
		};
		TestUtils.assertThrown(NullPointerException.class, () -> {try(RLPOutputStream ros = new RLPOutputStream(null)){}});
		try (RLPOutputStream ros = new RLPOutputStream()) {
			ros.write(0xc0);
			ros.write(new byte[] { (byte) 0x7f, (byte) 0x20 });
			ros.writeAll(new byte[] { 0x01 }, new byte[] { 0x02 });
			ros.writeAll(Collections.singletonList(new byte[] { 0x03 }));
			ros.writeList(new byte[] { 0x04 }, new byte[] { 0x05 }, new byte[] { 0x06 });
			byte[] bytes = ros.getByteArrayOutputStream().toByteArray();
			assertEquals("81c0827f20010203c3040506", Strings.encode(bytes));
		}
		try (RLPOutputStream ros = new RLPOutputStream()) {
			Notation notation = Notation.forObjects(objects);
			ros.writeAll(objects);
			byte[] bytes = ros.getByteArrayOutputStream().toByteArray();
			assertEquals(notation, Notation.forEncoding(bytes));
		}
		try (RLPOutputStream ros = new RLPOutputStream()) {
			ros.writeList(Arrays.asList(objects));
			byte[] bytes = ros.getByteArrayOutputStream().toByteArray();
			assertEquals(Notation.forObjects(new Object[] { objects }), Notation.forEncoding(bytes));
			assertEquals("ce880573490923738490c0c3827761", ros.getByteArrayOutputStream().toString());
			assertEquals("ce880573490923738490c0c3827761", ros.getOutputStream().toString());
			assertEquals("ce880573490923738490c0c3827761", ros.toString());
		}
	}

    @Test
    public void testObjectRLPStream() throws IOException {

        // write RLP
        RLPOutputStream ros = new RLPOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(ros)) {
			oos.writeUTF("hello");
//        oos.flush();
			oos.writeChar('Z');
//			oos.writeObject(new Tuple("jinro", new byte[] { (byte) 0xc0 }, new Boolean[] { false, true }));
			oos.flush();
		}

        // decode RLP
        RLPStream stream = new RLPStream(new ByteArrayInputStream(ros.getByteArrayOutputStream().toByteArray()));
        ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        int count = 0;
        for (RLPItem item : stream) {
            item.exportData(decoded);
            count++;
        }

        System.out.println("count = " + count);
        System.out.println("decoded len = " + decoded.toByteArray().length);

        // read objects
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(decoded.toByteArray()));
        assertEquals("hello", ois.readUTF());
        assertEquals('Z', ois.readChar());
//        assertEquals(new Tuple("jinro", new byte[] { (byte) 0xc0 }, new Boolean[] { false, true }), ois.readObject());
    }

    @Test
    public void testStreamEasy() throws Throwable {
        RLPItem[] collected = RLPDecoderTest.collectAll(RLP_BYTES).toArray(RLPItem.EMPTY_ARRAY);
        RLPItem[] streamed = RLP_STRICT.stream(RLP_BYTES).collect().toArray(RLPItem.EMPTY_ARRAY);

        assertTrue(Arrays.deepEquals(collected, streamed));

        List<byte[]> encodings = new ArrayList<>(collected.length);
        for (RLPItem item : collected) {
            encodings.add(item.encoding());
        }

        TestUtils.assertThrown(IllegalArgumentException.class, "len is out of range: 10", () -> encodings.stream()
                .map(RLP_STRICT::wrap)
                .mapToInt(RLPItem::asInt)
                .sum());
    }

    @Test
    public void testStreamHard() throws Throwable {
        ReceiveStreamThread thread = new ReceiveStreamThread();
        thread.start();
        thread.join();
        Throwable t = thread.throwable;
        if(t != null) {
            throw t;
        }
    }

    @Test
    public void testUnrecoverable() throws Throwable {
        try (PipedOutputStream pos = new PipedOutputStream();
             PipedInputStream pis = new PipedInputStream(pos, 512);
             RLPStream stream = new RLPStream(pis)) {
            pos.write(0x81);
            pos.write(0x00);
            Iterator<RLPItem> iter = stream.iterator();
            TestUtils.assertThrown(IllegalArgumentException.class, "invalid rlp for single byte @ 0", iter::hasNext);
            try (RLPStream stream2 = new RLPStream(pis)) {
                pos.write(0xf8);
                pos.write(0x37);
                Iterator<RLPItem> iter2 = stream2.iterator();
                for (int i = 0; i < 3; i++) {
                    TestUtils.assertThrown(
                            IllegalArgumentException.class,
                            "long element data length must be 56 or greater; found: 55 for element @ 0",
                            iter2::hasNext
                    );
                }
            }
        }
    }

    @Test
    public void testInterfaces() {
        try (RLPStream stream = new RLPStream(new ByteArrayInputStream(new byte[0]))) {
            for(RLPItem item : stream) {
                System.out.println(item);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final int RECEIVE = 0;
    private static final int SEND = 1;

    private static class ReceiveStreamThread extends Thread {

        private final Lock lock = new ReentrantLock();
        private final Condition send = lock.newCondition();
        private final Condition receive = lock.newCondition();

        private final long zero;
        private final PipedOutputStream pos;
        private final AtomicInteger mode;
        private final SendStreamThread senderThread;

        Throwable throwable;

        public ReceiveStreamThread() {
            this.zero = System.nanoTime();
            this.pos = new PipedOutputStream();
            this.mode = new AtomicInteger(SEND);
            this.senderThread = new SendStreamThread(zero, pos, lock, mode, send, receive);
        }

        @Override
        public void run() {
            try (RLPStream stream = new RLPStream(new PipedInputStream(pos, 512))) {

                Iterator<RLPItem> iter = stream.iterator();

                senderThread.setPriority(Thread.MAX_PRIORITY);
                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                senderThread.start();
                lock.lock();
                signalWaitRelock(lock, mode, SEND, send, RECEIVE, receive);

                assertNoNext(iter);
                signalWaitRelock(lock, mode, SEND, send, RECEIVE, receive);

                assertHasNext(iter);
                assertArrayEquals(new byte[] { TEST_BYTE }, iter.next().asBytes());
                assertNoNext(iter);
                assertNoNext(iter);
                signalWaitRelock(lock, mode, SEND, send, RECEIVE, receive);

                for (byte b : TEST_BYTES) {
                    assertHasNext(iter);
                    assertArrayEquals(new byte[] { b }, iter.next().asBytes());
                }
                assertNoNext(iter);

                signalWaitRelock(lock, mode, SEND, send, RECEIVE, receive);

                assertNoNext(iter);
                signalWaitRelock(lock, mode, SEND, send, RECEIVE, receive);

                assertNoNext(iter);

                signalWaitRelock(lock, mode, SEND, send, RECEIVE, receive);

                assertHasNext(iter);
                assertTrue(iter.hasNext());
                assertEquals(TEST_STRING, iter.next().asString(UTF_8));
                assertHasNext(iter);
                assertTrue(iter.hasNext());
                assertArrayEquals(new byte[] { TEST_BYTE }, iter.next().asBytes());
                assertNoNext(iter);
                assertNoNext(iter);
                senderThread.join();
            } catch (Throwable io) {
                throwable = io;
                senderThread.interrupt();
            } finally {
                trySignalUnlock(lock, mode, SEND, send);
            }
        }

        private void assertNoNext(Iterator<RLPItem> iter) throws Throwable {
            RLPStreamTest.assertNoNext(zero, iter);
        }

        private void assertHasNext(Iterator<RLPItem> iter) {
            RLPStreamTest.assertHasNext(zero, iter);
        }
    }

    private static class SendStreamThread extends Thread {

        private final long zero;
        private final OutputStream os;
        private final Lock lock;
        private final AtomicInteger mode;
        private final Condition send;
        private final Condition receive;

        SendStreamThread(long zero, OutputStream os, Lock lock, AtomicInteger mode, Condition send, Condition receive) {
            this.zero = zero;
            this.os = os;
            this.lock = lock;
            this.mode = mode;
            this.send = send;
            this.receive = receive;
        }

        @Override
        public void run() {
            lock.lock();
            try {
                signalWaitRelock(lock, mode, RECEIVE, receive, SEND, send);
                write(TEST_BYTE);
                signalWaitRelock(lock, mode, RECEIVE, receive, SEND, send);
                for (byte b : TEST_BYTES) {
                    write(b);
                }
                signalWaitRelock(lock, mode, RECEIVE, receive, SEND, send);
                byte[] rlpString = RLPEncoder.encodeString(Strings.decode(TEST_STRING, UTF_8));
                int i = 0;
                write(rlpString[i++]);
                signalWaitRelock(lock, mode, RECEIVE, receive, SEND, send);
                write(rlpString[i++]);
                signalWaitRelock(lock, mode, RECEIVE, receive, SEND, send);
                while(i < rlpString.length) {
                    write(rlpString[i++]);
                }
                write(TEST_BYTE);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            } finally {
                trySignalUnlock(lock, mode, RECEIVE, receive);
            }
        }

        private void write(byte b) throws IOException {
            os.write(b);
            logWrite(zero, "'" + (char) b + "' (0x" + Strings.encode(b) +")");
        }
    }

    private static void trySignalUnlock(Lock lock, AtomicInteger condition, int set, Condition signal) {
        try {
            signalUnlock(lock, condition, set, signal);
        } catch (IllegalMonitorStateException ignored) {
        }
    }

    private static void signal(AtomicInteger condition, int set, Condition signal) {
        condition.set(set);
        signal.signalAll();
    }

    private static void signalUnlock(Lock lock, AtomicInteger condition, int set, Condition signal) {
        signal(condition, set, signal);
        lock.unlock();
    }

    private static void signalWaitUnlock(Lock lock, AtomicInteger condition, int set, Condition signal, int waitFor, Condition wait) throws InterruptedException {
        try {
            signal(condition, set, signal);
            while (condition.get() != waitFor) {
                wait.await();
            }
        } finally {
            lock.unlock();
        }
    }

    private static void signalWaitRelock(Lock lock, AtomicInteger condition, int set, Condition signal, int waitFor, Condition wait) throws InterruptedException {
        signalWaitUnlock(lock, condition, set, signal, waitFor, wait);
        lock.lock();
    }

    static void assertHasNext(long zero, Iterator<RLPItem> iter) {
        assertTrue(iter.hasNext());
        logReceipt(zero, true);
    }

    static void assertNoNext(long zero, Iterator<RLPItem> iter) throws Throwable {
        assertFalse(iter.hasNext());
        TestUtils.assertThrown(NoSuchElementException.class, iter::next);
        assertFalse(iter.hasNext());
        logReceipt(zero, false);
    }

    static void logWrite(long zero, String message) {
        System.out.println(timestamp(zero) + "\u0009write " + message);
    }

    private static void logReceipt(long zero, boolean hasNext) {
        System.out.println(timestamp(zero) + '\u0009' + (hasNext ? "hasNext" : "no next"));
    }

    private static String timestamp(long zero) {
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
