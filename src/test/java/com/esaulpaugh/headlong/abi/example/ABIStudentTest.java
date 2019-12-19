package com.esaulpaugh.headlong.abi.example;

import com.esaulpaugh.headlong.abi.Function;
import com.esaulpaugh.headlong.abi.TupleType;
import com.esaulpaugh.headlong.exception.DecodeException;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ABIStudentTest {

    public static final String STUDENT_ABI = "00000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000082f7a6292f900000000000000000000000000000000000000000000000000000000000000e000000000000000000000000000000000000000000000000000000000000001a000000000000000000000000000000000000000000000000000000000000000790000000000000000000000000000000000000000000000000000000000000005506c61746f000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008a3232b0883839e5de6a8bf0555b6304b703041e82fe7568aa8b6837aa62740a83fe5aaa8736a1c2a27080f77142702cdf4a81ca2744bda44397bbd58c63f35c0eb6796bf485d750a0b9bfa4a2f3be5b9030a7f2b13d6a4d468e22b32fe92506b11af5517d425bc68f26f2525a61f1a954c50933874c7d97b1cd8ff65d55f651cb7c455876278787ac3a40000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000034269da3281d03da142f61ba27534caabf68f4cf30bcd23399b8cdf6fddc601f76012819f4572f901661ec6a5122f901661ec6a512000000000000000000000000";

    @Test
    public void abiDecodeEncode() throws DecodeException {
        final ByteBuffer studentAbi = ByteBuffer.wrap(Strings.decode(STUDENT_ABI));

        TupleType tt = TupleType.parse("(string,fixed128x9,bytes,bytes,uint16)");

        ABIStudent plato = new ABIStudent(tt.decode(studentAbi));

        ByteBuffer reencoded = tt.encode(plato.toTuple());

        System.out.println(Function.hexOf(reencoded));
        System.out.println(Function.hexOf(tt.encodePacked(plato.toTuple())));
        System.out.println(plato);

        assertArrayEquals(studentAbi.array(), reencoded.array());
    }
}
