package com.esaulpaugh.headlong.abi.beta.example;

import com.esaulpaugh.headlong.abi.beta.type.Function;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.beta.type.Function.SELECTOR_LEN;

public class Test {

    public static void main(String[] args0) throws ParseException {

        ByteBuffer buffer;


        Function f00 = new Function("bar(bytes3[2])");
        Object[] args00 = new Object[] {
                new byte[][] { "abc".getBytes(Charset.forName("UTF-8")), "def".getBytes(Charset.forName("UTF-8")) }
        };
        buffer = f00.encodeCall(args00);
        printABI(buffer.array());


        Function f0 = new Function("f(uint,uint32[],bytes10,bytes)");

        Object[] args0_ = new Object[] {
                BigInteger.valueOf(0x123),
                new int[] { 0x456, 0x789 },
                "1234567890".getBytes(Charset.forName("UTF-8")),
                "Hello, world!".getBytes(Charset.forName("UTF-8"))
        };
        buffer = f0.encodeCall(args0_);
        printABI(buffer.array());


//if(true) return;






//        Typing.create("bytes11[9]").validate(new byte[9][11]);
//        System.out.println();
//        Typing.create("bytes[9]");
//        System.out.println();
//        Typing.create("()");


        Function f = new Function("sam(bytes,bool,uint256[])"); // uint8[1][]
//        Object[] args = new Object[] { new byte[] { 3, 7 } }; // new byte[][] { new byte[1] }
        Object[] args = new Object[] {
                "dave".getBytes(StandardCharsets.UTF_8),
                true,
                new BigInteger[] { BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3) }
        };
        Throwable t = f.error(args);
        if(t != null) {
            System.err.println(t.getMessage());
        }
        buffer = f.encodeCall(args);
        printABI(buffer.array());

        Function g = new Function("g(uint[][],string[])");
        System.out.println(g.getSelectorHex());
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
        printABI(buffer.array());

        System.out.println("\n" + Hex.toHexString(buffer.array()));
    }

    private static void printABI(byte[] abi) {
        System.out.println(Hex.toHexString(Arrays.copyOfRange(abi, 0, SELECTOR_LEN)));
        final int end = abi.length;
        int i = 4;
        while(i < end) {
            System.out.println( (i / 32) + "\t" + Hex.toHexString(Arrays.copyOfRange(abi, i, i + 32)));
            i += 32;
        }
        System.out.println("\n" + Hex.toHexString(abi));
    }

}
