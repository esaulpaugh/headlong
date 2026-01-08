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
package com.esaulpaugh.headlong.jmh;

import com.esaulpaugh.headlong.jmh.abi.MeasureFunction;
import com.esaulpaugh.headlong.jmh.util.MeasureHex;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {

    public static final int THREE = 3;
    public static final int FIVE = 5;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MeasureFunction.class.getSimpleName())
                .include(MeasureHex.class.getSimpleName())
                .warmupForks(1)
                .warmupIterations(1)
                .forks(1)
                .measurementIterations(THREE)
//                .mode(Mode.Throughput)
                .build();

        new Runner(opt).run();
    }
}
