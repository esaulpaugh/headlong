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
import com.esaulpaugh.headlong.abi.util.WrappedKeccak;
import com.joemelsha.crypto.hash.Keccak;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class MeasureFunction {

//    private Function f;
//    private Tuple args;
//
//    @Setup(Level.Trial)
//    public void setUp() {
//        f = new Function("sam(bytes,bool,uint256[])");
//        args = Tuple.of(
//                Strings.decode("dave", Strings.UTF_8),
//                true,
//                new BigInteger[] { BigInteger.ONE, BigInteger.valueOf(2), BigInteger.valueOf(3) }
//        );
//    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = 2)
    public void init_with_keccak(Blackhole blackhole) {
        blackhole.consume(Function.parse("sam(bytes,bool,uint256[])", new Keccak(256)));
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = 2)
    public void init_with_wrapped_bouncy_keccak(Blackhole blackhole) {
        blackhole.consume(Function.parse("sam(bytes,bool,uint256[])", new WrappedKeccak(256)));
    }
}