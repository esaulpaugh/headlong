/*
   Copyright 2019 Evan Saulpaugh

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
package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.example.Student;
import com.esaulpaugh.headlong.example.StudentTest;
import com.esaulpaugh.headlong.util.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.util.Notation;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_LENIENT;
import static com.esaulpaugh.headlong.util.Strings.HEX;

@Disabled("not very useful")
public class Benchmark {

    @Test
    public void decodeMicroBenchmark() throws DecodeException {
        Student plato = null;
//        StudentRLPAdapter adapter = new StudentRLPAdapter();

        byte[] rlp = Strings.decode(StudentTest.STUDENT_RLP_SEQUENTIAL, HEX);
//        byte[] temp = new byte[rlp.length];

        final int n = 1_000_000;

        System.out.println("Doing " + new DecimalFormat("#,###").format(n) + " decodes of Student object:\n" + Notation.forEncoding(rlp));

        long start, end;

        // warmup
        for (int i = 0; i < 2_000_000; i++) {
            plato = new Student(rlp);
//            plato.toRLP(temp, 0);
//            rlp = adapter.encode(plato);
//            plato = adapter.decode(rlp);
        }
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            plato = new Student(rlp);
//            plato.toRLP(temp, 0);
//            rlp = adapter.encode(plato);
//            plato = adapter.decode(rlp);
        }
        end = System.nanoTime();

//        assertArrayEquals(STUDENT_RLP_SEQUENTIAL, temp);

        System.out.println(((end - start) / 1000000.0) + " millis " + plato);

    }

    @Test
    public void microBenchmark() throws DecodeException {
        byte[] rlp = Strings.decode("f8cbf8c7a00000000000000000000000000000000000000000000000000000000000000000a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000a02f4399b08efe68945c1cf90ffe85bbe3ce978959da753f9e649f034015b8817da00000000000000000000000000000000000000000000000000000000000000000834000008080830f4240808080a004994f67dc55b09e814ab7ffc8df3686b4afb2bb53e60eae97ef043fe03fb829c0c0", HEX);

        final int n = 10_000_000;

        RLPList rlpList;

        long start, end;

        rlpList = RLP_LENIENT.wrapList(rlp);
        rlpList.elements(RLP_LENIENT);

        // warmup
        for (int i = 0; i < 5_500_000; i++) {
            rlpList = RLP_LENIENT.wrapList(rlp);
            rlpList.elements(RLP_LENIENT);
        }
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            rlpList = RLP_LENIENT.wrapList(rlp);
            rlpList.elements(RLP_LENIENT);
        }
        end = System.nanoTime();

        System.out.println(((end - start) / 1000000.0) + " millis");
    }
}
