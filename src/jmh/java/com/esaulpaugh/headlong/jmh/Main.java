package com.esaulpaugh.headlong.jmh;

import com.esaulpaugh.headlong.jmh.abi.MeasureFunction;
import com.esaulpaugh.headlong.jmh.abi.MeasurePadding;
import com.esaulpaugh.headlong.jmh.rlp.MeasureKeyValuePairSort;
import com.esaulpaugh.headlong.jmh.util.MeasureBase64;
import com.esaulpaugh.headlong.jmh.util.MeasureHex;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {

    public static final int THREE = 3;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(MeasureFunction.class.getSimpleName())
                .include(MeasureKeyValuePairSort.class.getSimpleName())
                .include(MeasurePadding.class.getSimpleName())
                .include(MeasureBase64.class.getSimpleName())
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
