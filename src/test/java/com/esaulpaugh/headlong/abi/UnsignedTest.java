package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.TestUtils;
import org.junit.Test;

import java.math.BigInteger;

public class UnsignedTest {

    @Test
    public void testUnsigned() throws Throwable {
        TestUtils.assertThrown(
                IllegalArgumentException.class,
                "signed value given for unsigned type",
                () -> TupleType.parse("(uint)").validate(Tuple.singleton(BigInteger.valueOf(-1)))
        );

        TestUtils.assertThrown(
                IllegalArgumentException.class,
                "signed value given for unsigned type",
                () -> TupleType.parse("(uint48)").validate(Tuple.singleton(-1L))
        );
    }
}
