package com.esaulpaugh.headlong.rlp.eip778;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.rlp.RLPDecoder;
import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.util.FastHex;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static com.esaulpaugh.headlong.rlp.eip778.KeyValuePair.ID;
import static com.esaulpaugh.headlong.rlp.eip778.KeyValuePair.IP;
import static com.esaulpaugh.headlong.rlp.eip778.KeyValuePair.SECP256K1;
import static com.esaulpaugh.headlong.rlp.eip778.KeyValuePair.UDP;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class EIP778Test {

    private static final Record.Signer SIGNER = new Record.Signer() {

        private final byte[] SIG = FastHex.decode(
                "7098ad865b00a582051940cb9cf36836572411a47278783077011599ed5cd16b" +
                "76f2635f4e234738f30813a89eb9137e3e3df5266e3a1f11df72ecf1145ccb9c"
        );

        @Override
        public int signatureLength() {
            return SIG.length;
        }

        @Override
        public byte[] sign(byte[] message, int off, int len) {
            return SIG;
        }
    };

    private static final Record VECTOR = new Record(
            FastHex.decode(
                    "f884b8407098ad865b00a582051940cb9cf36836572411a4727878307701" +
                            "1599ed5cd16b76f2635f4e234738f30813a89eb9137e3e3df5266e3a1f11" +
                            "df72ecf1145ccb9c01826964827634826970847f00000189736563703235" +
                            "366b31a103ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1" +
                            "400f3258cd31388375647082765f"
            )
    );

    @Test
    public void testEip778() throws DecodeException {
        final KeyValuePair[] pairs = new KeyValuePair[] {
                new KeyValuePair(IP, "7f000001", HEX),
                new KeyValuePair(UDP, "765f", HEX),
                new KeyValuePair(ID, "v4", UTF_8),
                new KeyValuePair(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX)
        };

        Record record = new Record(1L, pairs, SIGNER);

        Assert.assertEquals(VECTOR.getRecord(RLPDecoder.RLP_STRICT), record.getRecord(RLPDecoder.RLP_STRICT));
        Assert.assertEquals(VECTOR.toString(), record.toString());
        Assert.assertEquals(VECTOR, record);
    }

    @Test
    public void nineLengths() throws DecodeException {
        final KeyValuePair[] pairs = new KeyValuePair[] {};
        Set<Integer> recordLengths = new HashSet<>();
        for (long seq = 0, p = 0; p <= 64; p +=8, seq = (long) Math.pow(2.0, p)) {
            long temp = seq - 2;
            int i = 0;
            do {
                if(temp >= 0) {
                    Record r = new Record(temp, pairs, SIGNER);
                    int len = r.getRecord(RLPDecoder.RLP_STRICT).encodingLength();
                    System.out.println(temp + " -> " + len);
                    recordLengths.add(len);
                }
                temp++;
            } while (++i < 4);
        }
        Assert.assertEquals(9, recordLengths.size());
    }

    @Test
    public void testDuplicateKey() throws Throwable {
        long seq = 3L;

        KeyValuePair[] pairs = new KeyValuePair[] {
                new KeyValuePair(IP, "7f000001", HEX),
                new KeyValuePair(UDP, "765f", HEX),
                new KeyValuePair(ID, "v4", UTF_8),
                new KeyValuePair(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX),
                new KeyValuePair(UDP, "765f", HEX)
        };

        for (KeyValuePair p : pairs) {
            System.out.println(p);
        }

        TestUtils.assertThrown(IllegalArgumentException.class, "duplicate key: " + UDP, () -> new Record(seq, pairs, SIGNER));
    }
}
