/*
   Copyright 2020 Evan Saulpaugh

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
package com.esaulpaugh.headlong.jmh;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

@State(Scope.Thread)
public class Padding {

    private static final int UNIT_LENGTH_BYTES = 32;

    Random r = new Random(System.nanoTime());

    ByteBuffer bb = ByteBuffer.allocate(96);

    int paddingLen;
    boolean negativeOnes;

    @Setup(Level.Invocation)
    public void setUp() {
        bb.position(32);
        paddingLen = r.nextInt(UNIT_LENGTH_BYTES);
        negativeOnes = r.nextBoolean();
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 2)
    public void cached() {
        insertPadding(paddingLen, negativeOnes, bb);
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 1)
    @Measurement(iterations = 2)
    public void uncached() {
        putN(negativeOnes ? (byte) -1 : (byte) 0, paddingLen, bb);
    }

    private static final byte[] CACHED_ZERO_PADDING = new byte[UNIT_LENGTH_BYTES];
    private static final byte[] CACHED_NEG1_PADDING = new byte[UNIT_LENGTH_BYTES];

    static {
        Arrays.fill(CACHED_NEG1_PADDING, (byte) -1);
    }

    static void insertPadding(int n, boolean negativeOnes, ByteBuffer dest) {
        dest.put(!negativeOnes ? CACHED_ZERO_PADDING : CACHED_NEG1_PADDING, 0, n);
    }

    private static void putN(byte val, int n, ByteBuffer dest) {
        for (int i = 0; i < n; i++) {
            dest.put(val);
        }
    }
}
