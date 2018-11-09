package com.esaulpaugh.headlong.rlp.example;

import com.esaulpaugh.headlong.rlp.DecodeException;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.Assert;
import org.junit.Test;

import static com.esaulpaugh.headlong.util.Strings.HEX;

public class StudentTest {

    private static final byte[] STUDENT_RLP_SEQUENTIAL = Strings.decode("85506c61746f84460ca00ab88a3232b0883839e5de6a8bf0555b6304b703041e82fe7568aa8b6837aa62740a83fe5aaa8736a1c2a27080f77142702cdf4a81ca2744bda44397bbd58c63f35c0eb6796bf485d750a0b9bfa4a2f3be5b9030a7f2b13d6a4d468e22b32fe92506b11af5517d425bc68f26f2525a61f1a954c50933874c7d97b1cd8ff65d55f651cb7c455876278787ac3a40b4269da3281d03da142f61ba27534caabf68f4cf30bcd23399b8cdf6fddc601f76012819f4572f901661ec6a5122f901661ec6a51279", HEX);

    private static final byte[] STUDENT_RLP_LIST = Strings.decode("f8cd85506c61746f84460ca00ab88a3232b0883839e5de6a8bf0555b6304b703041e82fe7568aa8b6837aa62740a83fe5aaa8736a1c2a27080f77142702cdf4a81ca2744bda44397bbd58c63f35c0eb6796bf485d750a0b9bfa4a2f3be5b9030a7f2b13d6a4d468e22b32fe92506b11af5517d425bc68f26f2525a61f1a954c50933874c7d97b1cd8ff65d55f651cb7c455876278787ac3a40b4269da3281d03da142f61ba27534caabf68f4cf30bcd23399b8cdf6fddc601f76012819f4572f901661ec6a5122f901661ec6a51279", HEX);

    public static final String STUDENT_TO_STRING = "Plato, 9000.01, 42614923710474099051865073204632262589579065351888983454633476946624606237920155844117624881925308800846339143726224240899171443679555954603453329411015471777430813363275539775856672253880435777567715200132619026358726307537867398362020608423752638708533909946520930698520947692165070514401896564444792357016643365814597705551067712, $2552.7185792349726775956284153005464480874316939890710382513661202185792349726775956284153005464480874316939890710382513661202";

    @Test
    public void decodeEncode() throws DecodeException {
        Student plato = new Student(STUDENT_RLP_LIST, 0);
        Assert.assertEquals(STUDENT_TO_STRING, plato.toString());

        byte[] rlp = plato.toRLP();
        Assert.assertArrayEquals(STUDENT_RLP_LIST, rlp);
    }

    @Test
    public void adapterDecodeEncode() throws DecodeException {

        StudentRLPAdapter adapter = new StudentRLPAdapter();

        Student plato = adapter.decode(STUDENT_RLP_LIST, 0);

        Assert.assertEquals(STUDENT_TO_STRING, plato.toString());

        byte[] rlp = adapter.encode(plato);
//        System.out.println(Hex.toHexString(rlp));

        Assert.assertArrayEquals(STUDENT_RLP_LIST, rlp);
    }
}
