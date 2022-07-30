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
package com.esaulpaugh.headlong.jmh.util;

import com.esaulpaugh.headlong.util.FastBase64;
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
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;

import static com.esaulpaugh.headlong.jmh.Main.THREE;

@State(Scope.Thread)
public class MeasureBase64 {

    private static final byte[] SMALL = java.util.Base64.getUrlDecoder().decode("-IS4QHCYrYZbAKWCBRlAy5zzaDZXJBGkcnh4MHcBFZntXNFrdvJjX04jRzjzCBOonrkTfj499SZuOh8R33Ls8RRcy5wBgmlkgnY0gmlwhH8AAAGJc2VjcDI1NmsxoQPKY0yuDUmstAHYpMa2_oxVtw0RW_QAdpzBQA8yWM0xOIN1ZHCCdl8");

    private static final byte[] LARGE = new byte[500_000];

    @Setup(Level.Trial)
    public void setUp() {
        new Random(System.currentTimeMillis() + System.nanoTime())
                .nextBytes(LARGE);
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = THREE)
    public void largeBase64BC(Blackhole blackhole) {
        blackhole.consume(org.bouncycastle.util.encoders.Base64.encode(LARGE));
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = THREE)
    public void largeBase64Commons(Blackhole blackhole) {
        blackhole.consume(org.apache.commons.codec.binary.Base64.encodeBase64(LARGE));
    }

    private static final int URL_SAFE_FLAGS = FastBase64.URL_SAFE_CHARS | FastBase64.NO_LINE_SEP | FastBase64.NO_PADDING;

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = THREE)
    public void largeBase64Fast(Blackhole blackhole) {
        blackhole.consume(FastBase64.encodeToString(LARGE, 0, LARGE.length, URL_SAFE_FLAGS));
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = THREE)
    public void largeBase64JavaUtil(Blackhole blackhole) {
        blackhole.consume(java.util.Base64.getUrlEncoder().encodeToString(LARGE));
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = THREE)
    public void smallBase64BC(Blackhole blackhole) {
        blackhole.consume(org.bouncycastle.util.encoders.Base64.toBase64String(SMALL));
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = THREE)
    public void smallBase64Commons(Blackhole blackhole) {
        blackhole.consume(org.apache.commons.codec.binary.Base64.encodeBase64(SMALL));
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = THREE)
    public void smallBase64Fast(Blackhole blackhole) {
        blackhole.consume(FastBase64.encodeToString(SMALL, 0, SMALL.length, URL_SAFE_FLAGS));
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = THREE)
    public void smallBase64JavaUtil(Blackhole blackhole) {
        blackhole.consume(java.util.Base64.getUrlEncoder().encodeToString(SMALL));
    }
}
