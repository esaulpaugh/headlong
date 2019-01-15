package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.FastHex;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;

public class PackedEncoderTest {

    @Test
    public void testPacked() throws ParseException {

        TupleType tupleType = Function.parseTupleType("(int8,bytes1,uint16,string)");

        Tuple test = new Tuple(-1, new byte[] { 0x42 }, 0x2424, "Hello, world!");

        tupleType.validate(test);

        int packedLen = tupleType.byteLengthPacked(test);

        Assert.assertEquals(FastHex.decode("ff42242448656c6c6f2c20776f726c6421").length, packedLen);

    }
}
