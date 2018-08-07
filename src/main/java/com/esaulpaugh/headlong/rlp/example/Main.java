package com.esaulpaugh.headlong.rlp.example;

import com.esaulpaugh.headlong.rlp.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPCodec;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.rlp.RLPList;
import com.esaulpaugh.headlong.rlp.util.ObjectNotation;
import org.spongycastle.util.encoders.Hex;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class Main {

    private static BigInteger newDummyECPubKey() {
        byte[] dummyECPubKey = new byte[65];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(dummyECPubKey);
        return new BigInteger(dummyECPubKey);
    }

    private static void test() throws DecodeException {
        StudentRLPAdapter adapter = new StudentRLPAdapter();

        Student plato = new Student(
                "Plato",
                9000.01f,
                newDummyECPubKey(),
                new BigDecimal("2552.7185792349726775956284153005464480874316939890710382513661202185792349726775956284153005464480874316939890710382513661202")
        );
        byte[] rlp = adapter.toRLP(plato);

        System.out.println("RLP len = " + rlp.length);

        System.out.println(Arrays.toString(rlp));

        Student decoded = adapter.fromRLP(rlp);

        System.out.println(plato);
        System.out.println(decoded);

        boolean equal = decoded.equals(plato);

        System.out.println("equal = " + equal);

        if(equal) {

            final int n = 1_000_000;

            System.out.println("Doing " + new DecimalFormat("#,###").format(n) + " encode-decodes of:\n" + ObjectNotation.fromEncoding(rlp));

            long start, end;

            for (int i = 0; i < 500_000; i++) {
                rlp = adapter.toRLP(plato);
                plato = adapter.fromRLP(rlp);
            }
            start = System.nanoTime();
            for (int i = 0; i < n; i++) {
                rlp = adapter.toRLP(plato);
                plato = adapter.fromRLP(rlp);
//            if(i % 100000 == 0) System.out.println(i + " " + plato + " " + System.nanoTime());
            }
            end = System.nanoTime();

            System.out.println(((end - start) / 1000000.0) + " millis");
        }

        System.out.println(plato);
    }

    public static void main(String[] args) throws DecodeException {

        test();

//        if(true) return;

        final byte[] invalidAf = new byte[] { (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, 0x00, (byte) 0x81, '\0', (byte) 0x81, '\u001B', (byte) '\u230A' };

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

        RLPItem it = RLPCodec.wrap(rlpEncoded);

        RLPItem dup = it.duplicate();
        System.out.println((it != dup) + ", " + (it.getClass() == dup.getClass()) + " " + it.equals(dup));

//        byte[] rlp = new byte[] { (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, 0x00, (byte) 0x81, '\0', (byte) 0x81, '\u001B', (byte) '\u230A' };

        RLPList rlpList = (RLPList) RLPCodec.wrap(rlpEncoded);
//        List<Object> recursive = rlpList.elementsRecursive();

        List<RLPItem> elements = rlpList.elements();

        ObjectNotation woo = ObjectNotation.fromEncoding(rlpEncoded);
        System.out.println(woo.toString());
        List<Object> parsed = woo.parse();

        ObjectNotation on = ObjectNotation.fromEncoding(rlpEncoded);

//        String parsed =
        byte[] rlpEncoded2 = RLPCodec.encodeSequentially(on.parse());

        ObjectNotation on2 = ObjectNotation.fromEncoding(rlpEncoded2);

//        File file = new File(" Desktop\\OBJECT_NOTATION_TEST.txt");
//        File w = on2.writeToFile(file);
//        ObjectNotation on3 = ObjectNotation.readFromFile(w);

        System.out.println(on2.equals(on));

//        System.out.println(treeString.length());
//        System.out.println(treeString);
//        System.out.println(Hex.toHexString(rlpEncoded2));

        List<Object> objects2 = on2.parse();

//        objects2.set(0, null); // TODO TEST

        byte[] rlpEncoded3 = RLPCodec.encodeSequentially(on2.parse());

        byte[] encoded = RLPCodec.encodeSequentially(objects2);

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
