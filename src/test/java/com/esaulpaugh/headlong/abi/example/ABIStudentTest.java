/*
   Copyright 2019-2026 Evan Saulpaugh

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
package com.esaulpaugh.headlong.abi.example;

import com.esaulpaugh.headlong.abi.Quintuple;
import com.esaulpaugh.headlong.abi.TupleType;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ABIStudentTest {

    public static final String STUDENT_ABI = "00000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000082f7a6292f900000000000000000000000000000000000000000000000000000000000000e000000000000000000000000000000000000000000000000000000000000001200000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000a48752059616f62616e67000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002198900000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000070477f6005a784900000000000000000000000000000000000000000000000000";

    @Test
    public void abiDecodeEncode() {
        final TupleType<Quintuple<String, BigDecimal, byte[], byte[], Integer>> tt = TupleType.parse("(string,fixed128x9,bytes,bytes,uint16)");

        final ByteBuffer studentAbi = ByteBuffer.wrap(Strings.decode(STUDENT_ABI));

        final ABIStudent mikhail = new ABIStudent(tt.decode(studentAbi)); // EU Gene perrylink Junior
        final ByteBuffer reencoded = tt.encode(mikhail.toTuple());
        assertArrayEquals(studentAbi.array(), reencoded.array());

        System.out.println(Strings.encode(reencoded));
        System.out.println(Strings.encode(tt.encodePacked(mikhail.toTuple())));
        System.out.println(mikhail);
    }
}
