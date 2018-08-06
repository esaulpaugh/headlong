package com.esaulpaugh.headlong.rlp.codec;

import com.esaulpaugh.headlong.rlp.codec.decoding.ObjectNotation;
import com.esaulpaugh.headlong.rlp.codec.example.Student;
import com.esaulpaugh.headlong.rlp.codec.example.StudentRLPAdapter;
import com.esaulpaugh.headlong.rlp.codec.exception.DecodeException;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.List;

public class Main {

    private static void test() throws DecodeException {
        StudentRLPAdapter adapter = new StudentRLPAdapter();
        Student s = new Student("Plato", 9000.01f);
        byte[] rlp = adapter.toRLP(s);
        Student plato = null;

        System.out.println(Arrays.toString(rlp));

        System.out.println("Doing 50_000_000 decode-encodes of:\n" + ObjectNotation.fromEncoding(rlp));

        long start, end;

        for (int i = 0; i < 100_000; i++) {
            rlp = adapter.toRLP(s);
            plato = adapter.fromRLP(rlp);
        }
        start = System.nanoTime();
        for (int i = 0; i < 50_000_000; i++) {
            rlp = adapter.toRLP(s);
            plato = adapter.fromRLP(rlp);
//            if(i % 100000 == 0) System.out.println(i + " " + plato + " " + System.nanoTime());
        }
        end = System.nanoTime();

        System.out.println(((end - start) / 1000000.0) + " millis");

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

//        byte[] rlp = new byte[] { (byte) 0xca, (byte) 0xc9, (byte) 0x80, 0x00, (byte) 0x81, 0x00, (byte) 0x81, '\0', (byte) 0x81, '\u001B', (byte) '\u230A' };

        RLPList rlpList = (RLPList) RLPCodec.wrap(rlpEncoded);
//        List<Object> recursive = rlpList.elementsRecursive();

        List<RLPItem> elements = rlpList.elements();

        ObjectNotation woo = ObjectNotation.fromEncoding(rlpEncoded);
        System.out.println(woo.toString());
        List<Object> parsed = woo.parse();

        ObjectNotation on = ObjectNotation.fromEncoding(rlpEncoded);

//        String parsed =
        byte[] rlpEncoded2 = RLPCodec.encodeAll(on.parse());

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

        byte[] rlpEncoded3 = RLPCodec.encodeAll(on2.parse());

        byte[] encoded = RLPCodec.encodeAll(objects2);

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
