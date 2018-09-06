package com.esaulpaugh.headlong.abi.beta;

import org.junit.Test;

import java.security.SecureRandom;
import java.text.ParseException;

// -1908903771199546974
public class MonteCarloTest {

    private static final int N = 10000;

    @Test
    public void monteCarlo() throws ParseException {

        SecureRandom sr = new SecureRandom();

        final long[] seeds = new long[N];
        for (int i = 0; i < seeds.length; i++) {
            seeds[i] = sr.nextLong();
        }

        StringBuilder log = new StringBuilder();

//        Random rng = new Random();

        for(final long seed : seeds) {
//            System.out.println("new seed " + seed);
            final MonteCarloTestCase.Params params = new MonteCarloTestCase.Params(seed); // -667342700048419528L
            try {
                final MonteCarloTestCase testCase = new MonteCarloTestCase(params);

//                System.out.println("SEED = " + params.seed); // -3790512102648160282

                log.append(testCase.run()).append('\n');

//                rng.setSeed(-667342700048419528L); // -667342700048419528
//                String rawFunctionSignature = generateFunctionSignature(rng, 0);
//                System.out.println("raw: " + rawFunctionSignature);
//                Function function = new Function(rawFunctionSignature);
//                System.out.println(function.getCanonicalSignature());
//                final Tuple in = generateTuple(function.paramTypes, rng);

//                Function function = testCase.function();
//                final Tuple in = testCase.argsTuple;
//
//                ByteBuffer abi = function.encodeCall(in);
//
//                byte[] array = abi.array();
//
////                EncodeTest.printABI(abi.array());
//
//                final Tuple out = function.decodeCall(array);
//
//                boolean equal = in.equals(out);
//                System.out.println(equal);
//
//                if(!equal) {
//                    findInequality(function.paramTypes, in, out);
//                }
//
//                Assert.assertEquals(in, out);

            } catch (Throwable t) {
                System.err.println("SEED = " + params.seed);
                throw t;
            }
        }

        System.out.println(log.toString());
    }
}
