package com.esaulpaugh.headlong.abi.beta.example;

import com.esaulpaugh.headlong.abi.beta.type.Function;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.beta.type.Function.SELECTOR_LEN;

public class Test {

    public static void main(String[] args0) throws ParseException {

//        Typing.create("bytes11[9]").validate(new byte[9][11]);
//        System.out.println();
//        Typing.create("bytes[9]");
//        System.out.println();
//        Typing.create("()");

        Function f = new Function("(uint8[1][])");

        Object[] args = new Object[] { new byte[][] { new byte[1] } };

        Throwable t = f.error(args);
        if(t != null) {
            System.err.println(t.getMessage());
        }

        ByteBuffer buffer = f.encodeCall(args);

        System.out.println(Hex.toHexString(buffer.array()));


        Function g = new Function("g(uint[][],string[])");
        System.out.println(g.getSelectorHex());
        buffer = g.encodeCall(
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

//        System.out.println(Hex.toHexString(buffer.array()));
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
