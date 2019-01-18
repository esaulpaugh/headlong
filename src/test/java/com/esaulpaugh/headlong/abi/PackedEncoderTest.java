package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.FastHex;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;

public class PackedEncoderTest {

    @Test
    public void testPacked() throws ParseException {

        TupleType tupleType = TupleType.parse("(int8,bytes1,uint16,string)");

        Tuple test = new Tuple(-1, new byte[] { 0x42 }, 0x2424, "Hello, world!");

        tupleType.validate(test);

        int packedLen = tupleType.byteLengthPacked(test);

        Assert.assertEquals(FastHex.decode("ff42242448656c6c6f2c20776f726c6421").length, packedLen);

        byte[] dest = new byte[packedLen];

        tupleType.encodePacked(test, dest, 0);

        System.out.println(FastHex.encodeToString(dest));

        Assert.assertArrayEquals(FastHex.decode("ff42242448656c6c6f2c20776f726c6421"), dest);

        // ---------------------------

        Function function = new Function(tupleType.canonicalType);

        byte[] abi = FastHex.decode("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff420000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000024240000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000000d48656c6c6f2c20776f726c642100000000000000000000000000000000000000");

        byte[] call = new byte[Function.SELECTOR_LEN + abi.length];

        System.arraycopy(function.selector, 0, call, 0, Function.SELECTOR_LEN);
        System.arraycopy(abi, 0, call, Function.SELECTOR_LEN, abi.length);

        Tuple args = function.decodeCall(call);

        TupleType tt = TupleType.parse(tupleType.canonicalType);

        byte[] dest2 = tt.encodePacked(args);

//        System.out.println(FastHex.encodeToString(dest2));
    }

    @Test
    public void testTest() throws ParseException {

        TupleType tupleType = TupleType.parse("(int24,int32)");

        Tuple values = new Tuple(-2, -4);

        tupleType.validate(values);

        byte[] packed = tupleType.encodePacked(values);

//        System.out.println(FastHex.encodeToString(packed));
    }

}
