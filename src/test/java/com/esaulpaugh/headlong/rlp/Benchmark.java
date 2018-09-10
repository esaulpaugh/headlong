package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.example.Student;
import com.esaulpaugh.headlong.rlp.util.Notation;
import com.joemelsha.crypto.hash.Keccak;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.crypto.Digest;
import org.spongycastle.crypto.digests.KeccakDigest;
import org.spongycastle.jcajce.provider.digest.Keccak.DigestKeccak;
import org.spongycastle.util.encoders.Hex;

import java.nio.charset.Charset;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Random;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_LENIENT;

public class Benchmark {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final byte[] STUDENT_RLP_SEQUENTIAL = Hex.decode("85506c61746f84460ca00ab88a3232b0883839e5de6a8bf0555b6304b703041e82fe7568aa8b6837aa62740a83fe5aaa8736a1c2a27080f77142702cdf4a81ca2744bda44397bbd58c63f35c0eb6796bf485d750a0b9bfa4a2f3be5b9030a7f2b13d6a4d468e22b32fe92506b11af5517d425bc68f26f2525a61f1a954c50933874c7d97b1cd8ff65d55f651cb7c455876278787ac3a40b4269da3281d03da142f61ba27534caabf68f4cf30bcd23399b8cdf6fddc601f76012819f4572f901661ec6a5122f901661ec6a51279");

    @Test
    public void decodeMicroBenchmark() throws DecodeException, DigestException {
        Student plato;
//        StudentRLPAdapter adapter = new StudentRLPAdapter();

        byte[] rlp = STUDENT_RLP_SEQUENTIAL;
//        byte[] temp = new byte[rlp.length];

        final int n = 1_000_000;

        System.out.println("Doing " + new DecimalFormat("#,###").format(n) + " decodes of Student object:\n" + Notation.forEncoding(rlp));

        long start, end;

        // warmup
        for (int i = 0; i < 2_000_000; i++) {
            plato = new Student(rlp, 0);
//            plato.toRLP(temp, 0);
//            rlp = adapter.encode(plato);
//            plato = adapter.decode(rlp);
        }
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            plato = new Student(rlp, 0);
//            plato.toRLP(temp, 0);
//            rlp = adapter.encode(plato);
//            plato = adapter.decode(rlp);
        }
        end = System.nanoTime();

//        Assert.assertArrayEquals(STUDENT_RLP_SEQUENTIAL, temp);

        System.out.println(((end - start) / 1000000.0) + " millis");

    }

    @Test
    public void microBenchmark() throws DecodeException {
        byte[] rlp = Hex.decode("f8cbf8c7a00000000000000000000000000000000000000000000000000000000000000000a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000a02f4399b08efe68945c1cf90ffe85bbe3ce978959da753f9e649f034015b8817da00000000000000000000000000000000000000000000000000000000000000000834000008080830f4240808080a004994f67dc55b09e814ab7ffc8df3686b4afb2bb53e60eae97ef043fe03fb829c0c0");

        final int n = 10_000_000;

        RLPList rlpList;

        long start, end;

        rlpList = (RLPList) RLP_LENIENT.wrap(rlp);
        rlpList.elements(RLP_LENIENT);
//            List<Object> results = new ArrayList<>();
//            rlpList.elementsRecursive(results, RLP_LENIENT);

        // warmup
        for (int i = 0; i < 5_500_000; i++) {
            rlpList = (RLPList) RLP_LENIENT.wrap(rlp);
            rlpList.elements(RLP_LENIENT);
//            List<Object> results = new ArrayList<>();
//            rlpList.elementsRecursive(results, RLP_LENIENT);
        }
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            rlpList = (RLPList) RLP_LENIENT.wrap(rlp);
            rlpList.elements(RLP_LENIENT);
//            List<Object> results = new ArrayList<>();
//            rlpList.elementsRecursive(results, RLP_LENIENT);
        }
        end = System.nanoTime();

        System.out.println(((end - start) / 1000000.0) + " millis");
    }

    @Test
    public void keccakBenchmark() throws DigestException {
        byte[] empty = "test".getBytes(UTF_8);
        MessageDigest k_ = new Keccak(256);
        k_.update(empty, 0, empty.length);
        byte[] out = k_.digest();
        System.out.println(Hex.toHexString(out));

        Random r = new Random(new SecureRandom().nextLong());

        for (int i = 20000; i >= 0; i--) {

            byte[] input = new byte[i];
            r.nextBytes(input);

            if(i % 1000 == 0) System.out.println(input.length);

            String prev, current;

            byte[] output;

            MessageDigest k0 = new Keccak(256);
            final int random = r.nextInt(input.length + 1);
            k0.update(input, 0, random);
            k0.update(input, random, input.length - random);
            output = k0.digest();
            current = Hex.toHexString(output);
//            System.out.println(current);
            prev = current;

            output = new Keccak(256).digest(input);
            current = Hex.toHexString(output);
//            System.out.println(current);
            Assert.assertEquals(prev, current);
            prev = current;

            MessageDigest k = new Keccak(256);
            k.update(input);
            output = k.digest();
            current = Hex.toHexString(output);
//            System.out.println(current);
            Assert.assertEquals(prev, current);
            prev = current;

            MessageDigest _256no = new Keccak(256);
            _256no.update(input);
            _256no.digest(output, 0, output.length);
            current = Hex.toHexString(output);
//            System.out.println(current);
            Assert.assertEquals(prev, current);
            prev = current;

//            System.out.println();

            MessageDigest md = new org.bouncycastle.jcajce.provider.digest.Keccak.Digest256();
            md.update(input);
            md.digest(output, 0, output.length);
            current = Hex.toHexString(output);
//            System.out.println(current);
            Assert.assertEquals(prev, current);
            prev = current;

            MessageDigest digestKeccak = new DigestKeccak(256);
            digestKeccak.update(input);
            digestKeccak.digest(output, 0, output.length);
            current = Hex.toHexString(output);
//            System.out.println(current);
            Assert.assertEquals(prev, current);
            prev = current;

            Digest spongy = new KeccakDigest(256);
            spongy.update(input, 0, input.length);
            spongy.doFinal(output, 0);
            current = Hex.toHexString(output);
//            System.out.println(current);
            Assert.assertEquals(prev, current);
            prev = current;

        }

        if(true)return;
    }
}
