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
package com.esaulpaugh.headlong.jmh.rlp;

import com.esaulpaugh.headlong.rlp.KVP;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static com.esaulpaugh.headlong.jmh.Main.THREE;

@State(Scope.Thread)
public class MeasureKeyValuePairSort {

    private static final int SIZE = 20;
    private static final int MIN_KEY_LEN = 10; // 10-byte min to avoid duplicate keys
    private static final int KEY_LEN_BOUND = 20;
    private static final int VALUE_LEN_BOUND = 10;

    final Random rand = new Random();

    final KVP[] array = new KVP[SIZE];
    final List<KVP> arrayList = new ArrayList<>();
    List<KVP> arraysArrayList;

    @Setup(Level.Invocation)
    public void init() {
        rand.setSeed(rand.nextLong() + System.nanoTime());

        arrayList.clear();

        byte[] key, value;
        for (int i = 0; i < SIZE; i++) {
            key = new byte[MIN_KEY_LEN + rand.nextInt(KEY_LEN_BOUND - MIN_KEY_LEN)];
            value = new byte[rand.nextInt(VALUE_LEN_BOUND)];
            rand.nextBytes(key);
            rand.nextBytes(value);
            KVP pair = new KVP(Strings.encode(key, Strings.UTF_8), value);
            array[i] = pair;
            arrayList.add(pair);
        }
        arraysArrayList = Arrays.asList(Arrays.copyOf(array, array.length));
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = THREE)
    public void sortArray() {
        Arrays.sort(array, Comparator.naturalOrder());
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = THREE)
    public void sortArrayList() {
        arrayList.sort(Comparator.naturalOrder());
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = THREE)
    public void sortArraysArrayList() {
        arraysArrayList.sort(Comparator.naturalOrder());
    }
}
