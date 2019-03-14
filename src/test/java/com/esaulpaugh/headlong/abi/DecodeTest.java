package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.FastHex;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;

public class DecodeTest {

    private final Tuple expected = new Tuple(new BigDecimal(BigInteger.valueOf(69L), 18), "w00t");

    @Test
    public void testDecode() throws ParseException {

        Function g = new Function("g(uint[5][4][],(bool,int))", "(ufixed,string)");

        Tuple decoded = g.decodeReturnValues(
                FastHex.decode(
                        "0000000000000000000000000000000000000000000000000000000000000045"
                                + "0000000000000000000000000000000000000000000000000000000000000020"
                                + "0000000000000000000000000000000000000000000000000000000000000004"
                                + "7730307400000000000000000000000000000000000000000000000000000000"
                )
        );
        Assert.assertEquals(expected, decoded);

        decoded = g.getOutputTypes().decode(
                FastHex.decode(
                        "0000000000000000000000000000000000000000000000000000000000000045"
                                + "0000000000000000000000000000000000000000000000000000000000000020"
                                + "0000000000000000000000000000000000000000000000000000000000000004"
                                + "7730307400000000000000000000000000000000000000000000000000000000"
                )
        );
        Assert.assertEquals(expected, decoded);

        decoded = TupleType.parse("(ufixed,string)").decode(ByteBuffer.wrap(FastHex.decode(
                "0000000000000000000000000000000000000000000000000000000000000045"
                        + "0000000000000000000000000000000000000000000000000000000000000020"
                        + "0000000000000000000000000000000000000000000000000000000000000004"
                        + "7730307400000000000000000000000000000000000000000000000000000000"
        )));
        Assert.assertEquals(expected, decoded);

        decoded = TupleType.parseElements("ufixed,string").decode(ByteBuffer.wrap(FastHex.decode(
                "0000000000000000000000000000000000000000000000000000000000000045"
                        + "0000000000000000000000000000000000000000000000000000000000000020"
                        + "0000000000000000000000000000000000000000000000000000000000000004"
                        + "7730307400000000000000000000000000000000000000000000000000000000"
        )));
        Assert.assertEquals(expected, decoded);
    }

}
