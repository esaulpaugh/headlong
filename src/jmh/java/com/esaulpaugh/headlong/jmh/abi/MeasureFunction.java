/*
   Copyright 2018 Evan Saulpaugh

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
package com.esaulpaugh.headlong.jmh.abi;

import com.esaulpaugh.headlong.abi.Function;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.util.Strings;
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

import java.math.BigInteger;

import static com.esaulpaugh.headlong.jmh.Main.THREE;

@State(Scope.Benchmark)
public class MeasureFunction {

    private static final int BATCH_SIZE = 100_000;

    private Function f;
    private Tuple args;

    private final byte[] encodedCall = Strings.decode("a5643bf20000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000000000000000000000000000000000000000000464617665000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000003");

    @Setup(Level.Trial)
    public void setUp() {
        f = new Function("sam(bytes,bool,uint256[])");
        args = Tuple.of(
                Strings.decode("dave", Strings.UTF_8),
                true,
                new BigInteger[] { BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3) }
        );
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(batchSize = BATCH_SIZE, iterations = 1)
    @Measurement(batchSize = BATCH_SIZE, iterations = THREE)
    public void encode_call(Blackhole blackhole) {
        blackhole.consume(f.encodeCall(args));
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(batchSize = BATCH_SIZE, iterations = 1)
    @Measurement(batchSize = BATCH_SIZE, iterations = THREE)
    public void decode_call(Blackhole blackhole) {
        blackhole.consume(f.decodeCall(encodedCall));
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.AverageTime)
    @Warmup(batchSize = BATCH_SIZE, iterations = 1)
    @Measurement(batchSize = BATCH_SIZE, iterations = THREE)
    public void init_with_keccak(Blackhole blackhole) {
        blackhole.consume(Function.parse("sam(bytes,bool,uint256[])"));
    }

//    @Benchmark
//    @Fork(value = 1, warmups = 1)
//    @BenchmarkMode(Mode.AverageTime)
//    @Warmup(batchSize = BATCH_SIZE, iterations = 1)
//    @Measurement(batchSize = BATCH_SIZE, iterations = THREE)
//    public void init_with_wrapped_bouncy_keccak(Blackhole blackhole) {
//        blackhole.consume(Function.parse("sam(bytes,bool,uint256[])", new WrappedKeccak(256)));
//    }
}