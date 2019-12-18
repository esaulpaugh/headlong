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
package com.esaulpaugh.headlong.example;

import com.esaulpaugh.headlong.abi.Function;
import com.esaulpaugh.headlong.abi.TupleType;
import com.esaulpaugh.headlong.util.exception.DecodeException;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.text.ParseException;

import static com.esaulpaugh.headlong.util.Strings.HEX;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StudentTest {

    public static final String STUDENT_RLP_SEQUENTIAL = "85506c61746f84460ca00ab88a3232b0883839e5de6a8bf0555b6304b703041e82fe7568aa8b6837aa62740a83fe5aaa8736a1c2a27080f77142702cdf4a81ca2744bda44397bbd58c63f35c0eb6796bf485d750a0b9bfa4a2f3be5b9030a7f2b13d6a4d468e22b32fe92506b11af5517d425bc68f26f2525a61f1a954c50933874c7d97b1cd8ff65d55f651cb7c455876278787ac3a40b4269da3281d03da142f61ba27534caabf68f4cf30bcd23399b8cdf6fddc601f76012819f4572f901661ec6a5122f901661ec6a51279";

    public static final String STUDENT_RLP_LIST = "f8cd85506c61746f84460ca00ab88a3232b0883839e5de6a8bf0555b6304b703041e82fe7568aa8b6837aa62740a83fe5aaa8736a1c2a27080f77142702cdf4a81ca2744bda44397bbd58c63f35c0eb6796bf485d750a0b9bfa4a2f3be5b9030a7f2b13d6a4d468e22b32fe92506b11af5517d425bc68f26f2525a61f1a954c50933874c7d97b1cd8ff65d55f651cb7c455876278787ac3a40b4269da3281d03da142f61ba27534caabf68f4cf30bcd23399b8cdf6fddc601f76012819f4572f901661ec6a5122f901661ec6a51279";

    public static final String STUDENT_TO_STRING = "Plato, 9000.01, 42614923710474099051865073204632262589579065351888983454633476946624606237920155844117624881925308800846339143726224240899171443679555954603453329411015471777430813363275539775856672253880435777567715200132619026358726307537867398362020608423752638708533909946520930698520947692165070514401896564444792357016643365814597705551067712, $2552.7185792349726775956284153005464480874316939890710382513661202185792349726775956284153005464480874316939890710382513661202";

    public static final String STUDENT_ABI = "00000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000082f7a6292f900000000000000000000000000000000000000000000000000000000000000e000000000000000000000000000000000000000000000000000000000000001a000000000000000000000000000000000000000000000000000000000000000790000000000000000000000000000000000000000000000000000000000000005506c61746f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008a3232b0883839e5de6a8bf0555b6304b703041e82fe7568aa8b6837aa62740a83fe5aaa8736a1c2a27080f77142702cdf4a81ca2744bda44397bbd58c63f35c0eb6796bf485d750a0b9bfa4a2f3be5b9030a7f2b13d6a4d468e22b32fe92506b11af5517d425bc68f26f2525a61f1a954c50933874c7d97b1cd8ff65d55f651cb7c455876278787ac3a40000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000034269da3281d03da142f61ba27534caabf68f4cf30bcd23399b8cdf6fddc601f76012819f4572f901661ec6a5122f901661ec6a512000000000000000000000000";

    @Test
    public void rlpDecodeEncode() throws DecodeException {
        final byte[] studentRlp = Strings.decode(STUDENT_RLP_SEQUENTIAL, HEX);
        Student plato = new Student(studentRlp);
        assertEquals(STUDENT_TO_STRING, plato.toString());

        byte[] rlp = plato.toRLP();
        assertArrayEquals(studentRlp, rlp);
    }

    @Test
    public void adapterDecodeEncode() throws DecodeException {

        StudentRLPAdapter adapter = new StudentRLPAdapter();

        final byte[] studentRlp = Strings.decode(STUDENT_RLP_LIST, HEX);
        Student plato = adapter.decode(studentRlp, 0);

        assertEquals(STUDENT_TO_STRING, plato.toString());

        byte[] rlp = adapter.encode(plato);
//        System.out.println(Hex.toHexString(rlp));

        assertArrayEquals(studentRlp, rlp);
    }

    @Test
    public void abiDecodeEncode() throws ParseException {
        final ByteBuffer studentAbi = ByteBuffer.wrap(Strings.decode(STUDENT_ABI));

        TupleType tt = TupleType.parse("(string,fixed128x9,bytes,bytes,uint16)");

        Student plato = new Student(tt.decode(studentAbi));

        ByteBuffer reencoded = tt.encode(plato.toTuple());

        System.out.println(Function.hexOf(reencoded));
        System.out.println(Function.hexOf(tt.encodePacked(plato.toTuple())));
        System.out.println(plato);

        assertArrayEquals(studentAbi.array(), reencoded.array());
    }
}
