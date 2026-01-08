/*
   Copyright 2020-2026 Evan Saulpaugh

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

import com.esaulpaugh.headlong.util.FastHex;
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
import static java.lang.String.format;

@State(Scope.Thread)
@Fork(value = 1, warmups = 1)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 1)
@Measurement(iterations = THREE)
public class MeasureHex {

    private static final byte[] SMALL = java.util.Base64.getUrlDecoder().decode("-IS4QHCYrYZbAKWCBRlAy5zzaDZXJBGkcnh4MHcBFZntXNFrdvJjX04jRzjzCBOonrkTfj499SZuOh8R33Ls8RRcy5wBgmlkgnY0gmlwhH8AAAGJc2VjcDI1NmsxoQPKY0yuDUmstAHYpMa2_oxVtw0RW_QAdpzBQA8yWM0xOIN1ZHCCdl8");

    private static final byte[] LARGE = new byte[500_000];

    @Setup(Level.Trial)
    public void setUp() {
        new Random(System.currentTimeMillis() + System.nanoTime())
                .nextBytes(LARGE);
    }

    private static String slowHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    @Benchmark
    public void largeHexFast(Blackhole blackhole) {
        blackhole.consume(FastHex.encodeToString(LARGE));
    }

    @Benchmark
    public void largeHexSlow(Blackhole blackhole) {
        blackhole.consume(slowHex(LARGE));
    }

    @Benchmark
    public void smallHexFast(Blackhole blackhole) {
        blackhole.consume(FastHex.encodeToString(SMALL));
    }

    @Benchmark
    public void smallHexSlow(Blackhole blackhole) {
        blackhole.consume(slowHex(SMALL));
    }
}
