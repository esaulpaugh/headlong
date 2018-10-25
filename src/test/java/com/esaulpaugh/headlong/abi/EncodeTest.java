package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.Strings;
import org.junit.Assert;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;

import static com.esaulpaugh.headlong.util.Strings.CHARSET_UTF_8;
import static com.esaulpaugh.headlong.util.Strings.HEX;

public class EncodeTest {

    public static void main(String[] args_) throws ParseException {

//        if(true)return;

        Function f = new Function("f(uint,uint32[],bytes10,bytes)");
        Tuple args = new Tuple(BigInteger.valueOf(0x123), new int[] { 0x456, 0x789 }, "1234567890".getBytes(CHARSET_UTF_8), "Hello, world!".getBytes(CHARSET_UTF_8));
        ByteBuffer buffer = f.encodeCall(args);
        Function.formatABI(buffer.array());
        Tuple decoded = f.decodeCall(buffer.array());
        Assert.assertEquals(args, decoded);

//        if(true)return;

        ByteBuffer _buffer;

        Function f00 = new Function("(())");
        Object[] args00 = new Object[] {
                new Tuple()
//                new Tuple[] { new Tuple(new Tuple((Object) new Tuple[0])) }
        };
        _buffer = f00.encodeCall(args00);
        Function.formatABI(_buffer.array());

//        if(true)return;

        Function f0 = new Function("((int8,()))"); // bar(((((uint8))[1]))) // bar(((()[]))[1])
        Object[] args0 = new Object[] {
//                (byte) 5
                new Tuple(5, new Tuple())
//                new Tuple(new Tuple((byte) 5))
//                new Tuple( (byte) 5, new Tuple() )
//                new Tuple(new Tuple((byte) 5))
//                new Tuple( new Tuple( (Object) new Tuple[] { new Tuple(new Tuple((Object) (byte) 1 )) } ) )
//                new Tuple[] { new Tuple(new Tuple((Object) new Tuple[0])) }
        };
        _buffer = f0.encodeCall(args0);
        Function.formatABI(_buffer.array());

//        if(true)return;

        Function f2 = new Function("sam(bytes,bool,uint256[])"); // uint8[1][]
        Object[] args2 = new Object[] {
                "dave".getBytes(CHARSET_UTF_8),
                true,
                new BigInteger[] { BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3) }
        };
//        Throwable t = f2.error(args2);
//        if(t != null) {
//            System.err.println(t.getMessage());
//        }
        buffer = f2.encodeCall(args2);
        Function.formatABI(buffer.array());

        byte[] expected = Strings.decode("a5643bf20000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000a0000000000000000000000000000000000000000000000000000000000000000464617665000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000003", HEX);
        Assert.assertArrayEquals(expected, buffer.array());

        Function g = new Function("g(uint[][],string[])");
        System.out.println(g.selectorHex());
        buffer = g.encodeCall(
//                (Object)
                new BigInteger[][] {
                        new BigInteger[] {
                                BigInteger.ONE,
                                BigInteger.valueOf(2L),
                        },
                        new BigInteger[] {
                                BigInteger.valueOf(3L)
                        }
                },
                new String[] { "one", "two", "three" }
        );
        Function.formatABI(buffer.array());

        System.out.println("\n" + Strings.encode(buffer.array(), HEX));
    }
}
