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

import com.esaulpaugh.headlong.rlp.KeyValuePair;
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
import java.util.List;
import java.util.Random;

@State(Scope.Thread)
public class MeasureKeyValuePairSort {

    private static final int SIZE = 20;
    private static final int KEY_LEN_BOUND = 20;
    private static final int VALUE_LEN_BOUND = 10;

    final Random rand = new Random();

    final KeyValuePair[] array = new KeyValuePair[SIZE];
    final List<KeyValuePair> arrayList = new ArrayList<>();
    List<KeyValuePair> arraysArrayList;

    @Setup(Level.Invocation)
    public void init() {
        rand.setSeed(rand.nextLong() + System.nanoTime());

        arrayList.clear();

        byte[] key, value;
        for (int i = 0; i < SIZE; i++) {
            key = new byte[10 + rand.nextInt(KEY_LEN_BOUND - 10)]; // 10-byte min to avoid duplicate keys
            value = new byte[rand.nextInt(VALUE_LEN_BOUND)];
            rand.nextBytes(key);
            rand.nextBytes(value);
            KeyValuePair pair = new KeyValuePair(key, value);
            array[i] = pair;
            arrayList.add(pair);
        }
        arraysArrayList = Arrays.asList(Arrays.copyOf(array, array.length));
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = 2)
    public void sortArray() {
        Arrays.sort(array, KeyValuePair.PAIR_COMPARATOR);
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = 2)
    public void sortArrayList() {
        arrayList.sort(KeyValuePair.PAIR_COMPARATOR);
    }

    @Benchmark
    @Fork(value = 1, warmups = 1)
    @BenchmarkMode(Mode.Throughput)
    @Warmup(iterations = 1)
    @Measurement(iterations = 2)
    public void sortArraysArrayList() {
        arraysArrayList.sort(KeyValuePair.PAIR_COMPARATOR);
    }
}
