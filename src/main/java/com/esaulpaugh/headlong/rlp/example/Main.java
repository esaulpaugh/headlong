package com.esaulpaugh.headlong.rlp.example;

import com.esaulpaugh.headlong.rlp.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.rlp.RLPList;
import com.esaulpaugh.headlong.rlp.util.Integers;
import com.esaulpaugh.headlong.rlp.util.ObjectNotation;
import org.spongycastle.util.encoders.Hex;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_LENIENT;
import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.rlp.util.Strings.HEX;

public class Main {

    private static BigInteger newDummyECPubKey() {
        byte[] dummyECPubKey = new byte[138]; // 65
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(dummyECPubKey);
        return new BigInteger(dummyECPubKey);
    }

    private static void test() throws DecodeException {

//        int s0 = (int) 0x000000F2;
//        System.out.println(s0);
//        byte[] two = new byte[4];
//        int nnn = Integers.putInt(s0, two, 0);
//        System.out.println(Hex.toHexString(two));
//        long s = Integers.getInt(two, 0, nnn);
//        System.out.println(s);
//
//        RLPItem empty = RLP_LENIENT.wrap(new byte[] { (byte) 0x81, (byte) 0x00 }, 0); //  (byte) 0x81, (byte) 0x00
////        System.out.println(empty.asBoolean());
////        System.out.println(empty.asChar());
//        System.out.println(empty.asString(HEX));
//        System.out.println(empty.asByte());
//        System.out.println(empty.asShort());
//        System.out.println(empty.asInt());
//        System.out.println(empty.asLong());
//        System.out.println(ObjectNotation.forEncoding(new byte[] { 0x00, 0x00 }, 0, 0).toString());
//
//        try {
//            empty.duplicate(RLP_STRICT);
//        } catch (DecodeException de) {
//            System.out.println(de.toString());
//        }
//        ((RLPList) empty).elements(RLP_LENIENT);
//
//        if(true) return;

        StudentRLPAdapter adapter = new StudentRLPAdapter();

        Student plato = new Student(
                "Plato",
                9000.01f,
                newDummyECPubKey(),
                new BigDecimal("2552.7185792349726775956284153005464480874316939890710382513661202185792349726775956284153005464480874316939890710382513661202")
        );
        byte[] rlp = plato.toRLP();
//        byte[] rlp = adapter.encode(plato);

        System.out.println("rlp len = " + rlp.length);

        System.out.println(Hex.toHexString(rlp));

        Student decoded = new Student(rlp);
//        Student decoded = adapter.decode(rlp);

        System.out.println(plato);
//        System.out.println(decoded);

        boolean equal = plato.equals(decoded);

        System.out.println("equal = " + equal);

        byte[] temp = new byte[205];

//        if(equal) {

            final int n = 1_000_000;

            System.out.println("Doing " + new DecimalFormat("#,###").format(n) + " decodes of Student object:\n" + ObjectNotation.forEncoding(rlp));

            long start, end;

            // warmup
            for (int i = 0; i < 2_000_000; i++) {
//                plato.toRLP(temp, 0);
                plato = new Student(rlp);
//                rlp = adapter.encode(plato);
//                plato = adapter.decode(rlp);
            }
            start = System.nanoTime();
            for (int i = 0; i < n; i++) {
//                plato.toRLP(temp, 0);
                plato = new Student(rlp);
//                rlp = adapter.encode(plato);
//                plato = adapter.decode(rlp);
//            if(i % 100000 == 0) System.out.println(i + " " + plato + " " + System.nanoTime());
            }
            end = System.nanoTime();

            System.out.println(((end - start) / 1000000.0) + " millis");
//        }

        System.out.println(plato);
    }

//    private static void test2() throws DecodeException {
//
//        byte[] rlpEncoded = new byte[] {
//                (byte) 0xf8, (byte) 148,
//                (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, (byte) 0xFF, (byte) 0x81, (byte) 0x90, (byte) 0x81, (byte) 0xb6, (byte) '\u230A',
//                (byte) 0xb8, 56, 0x09,(byte)0x80,-1,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, -3, -2, 0, 0,
//                (byte) 0xf8, 0x38, 0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 36, 74, 0, 0,
//                (byte) 0x84, 'c', 'a', 't', 's',
//                (byte) 0x84, 'd', 'o', 'g', 's',
//                (byte) 0xca, (byte) 0x84, 92, '\r', '\n', '\f', (byte) 0x84, '\u0009', 'o', 'g', 's',
//        };
//
//        long start, end;
//
//        start = System.nanoTime();
//
//        RLPList list = (RLPList) RLPDecoder.wrap(rlpEncoded);
//        list.elements();
//
//        end = System.nanoTime();
//
//        System.out.println(((end - start) / 1000000.0) + " millis");
//
//    }

    public static void main(String[] args) throws DecodeException {

        System.out.println(Runtime.getRuntime().maxMemory());

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        System.out.println(memoryBean.getHeapMemoryUsage().getMax());

        test();

        if(true) return;

        final byte[] invalidAf = new byte[] { (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, 0x00, (byte) 0x81, (byte) 0x81, (byte) '\u0080', '\u007f', (byte) '\u230A' };

        final byte[] valid = new byte[] { (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, (byte) 0x80, (byte) 0x81, (byte) 0x81, (byte) 0x81, (byte) '\u0080', (byte) '\u230A' };

//        final byte[] rlpEncoded = valid;

        byte[] rlpEncoded = new byte[] {
                (byte) 0xf8, (byte) 148, // TODO test very long items
                (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, (byte) 0xFF, (byte) 0x81, (byte) 0x90, (byte) 0x81, (byte) 0xb6, (byte) '\u230A',
                (byte) 0xb8, 56, 0x09,(byte)0x80,-1,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, -3, -2, 0, 0,
                (byte) 0xf8, 0x38, 0,0,0,0,0,0,0,0,0,0,36,74,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 36, 74, 0, 0,
                (byte) 0x84, 'c', 'a', 't', 's',
                (byte) 0x84, 'd', 'o', 'g', 's',
                (byte) 0xca, (byte) 0x84, 92, '\r', '\n', '\f', (byte) 0x84, '\u0009', 'o', 'g', 's',
        };

        RLPItem it = RLP_STRICT.wrap(rlpEncoded);

        RLPItem dup = it.duplicate(RLP_STRICT);
        System.out.println((it != dup) + ", " + (it.getClass() == dup.getClass()) + " " + it.equals(dup));

//        byte[] rlp = new byte[] { (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, 0x00, (byte) 0x81, '\0', (byte) 0x81, '\u001B', (byte) '\u230A' };

        RLPList rlpList = (RLPList) RLP_STRICT.wrap(rlpEncoded);
//        List<Object> recursive = rlpList.elementsRecursive();

        List<RLPItem> elements = rlpList.elements(RLP_STRICT);

        ObjectNotation woo = ObjectNotation.forEncoding(rlpEncoded);
        System.out.println(woo.toString());
        List<Object> parsed = woo.parse();

        ObjectNotation on = ObjectNotation.forEncoding(rlpEncoded);

//        String parsed =
        byte[] rlpEncoded2 = RLPEncoder.encodeSequentially(on.parse());

        ObjectNotation on2 = ObjectNotation.forEncoding(rlpEncoded2);

//        File file = new File(" Desktop\\OBJECT_NOTATION_TEST.txt");
//        File w = on2.writeToFile(file);
//        ObjectNotation on3 = ObjectNotation.readFromFile(w);

        System.out.println(on2.equals(on));

//        System.out.println(treeString.length());
//        System.out.println(treeString);
//        System.out.println(Hex.toHexString(rlpEncoded2));

        List<Object> objects2 = on2.parse();

//        objects2.set(0, null); // TODO TEST

        byte[] rlpEncoded3 = RLPEncoder.encodeSequentially(on2.parse());

        byte[] encoded = RLPEncoder.encodeSequentially(objects2);

        System.out.println(Arrays.equals(encoded, rlpEncoded));

        System.out.println(Hex.toHexString(rlpEncoded));
        System.out.println(Hex.toHexString(rlpEncoded2));
        System.out.println(Hex.toHexString(rlpEncoded3));

        System.out.println(Arrays.equals(rlpEncoded, rlpEncoded2));
        System.out.println(Arrays.equals(rlpEncoded2, rlpEncoded3));
//        System.out.println(Arrays.deepEquals(data, objects2));

        System.out.println(on + "\n\n");
        System.out.println(on2);

    }
}
