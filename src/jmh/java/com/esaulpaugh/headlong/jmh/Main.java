package com.esaulpaugh.headlong.jmh;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(KeyValuePairSort.class.getSimpleName())
                .include(FunctionEncode.class.getSimpleName())
                .warmupForks(1)
                .warmupIterations(1)
                .forks(1)
                .measurementIterations(5)
                .mode(Mode.SingleShotTime)
                .build();

        new Runner(opt).run();
    }
}
