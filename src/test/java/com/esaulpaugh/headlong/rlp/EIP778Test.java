package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.rlp.eip778.KeyValuePair;
import com.esaulpaugh.headlong.rlp.eip778.Record;
import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.util.FastHex;
import org.junit.Assert;
import org.junit.Test;

import static com.esaulpaugh.headlong.rlp.eip778.KeyValuePair.*;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class EIP778Test {

    private static final Record.Signer SIGNER = new Record.Signer() {

        private final String sigHex =
                "7098ad865b00a582051940cb9cf36836572411a47278783077011599ed5cd16b" +
                "76f2635f4e234738f30813a89eb9137e3e3df5266e3a1f11df72ecf1145ccb9c";

        @Override
        public int signatureLength() {
            return FastHex.decode(sigHex).length;
        }

        @Override
        public byte[] sign(byte[] message, int off, int len) {
            return FastHex.decode(sigHex);
        }

        @Override
        public void sign(byte[] message, int off, int len, byte[] dest, int destIdx) {
            byte[] sig = FastHex.decode(sigHex);
            System.arraycopy(sig, 0, dest, destIdx, sig.length);
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

        long seq = 1L;

        KeyValuePair[] pairs = new KeyValuePair[] {
                new KeyValuePair(IP, "7f000001", HEX),
                new KeyValuePair(UDP, "765f", HEX),
                new KeyValuePair(ID, "v4", UTF_8),
                new KeyValuePair(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX)
        };

        for (KeyValuePair p : pairs) {
            System.out.println(p);
        }

        Record record = new Record(seq, pairs, SIGNER);

        System.out.println(record);

        Assert.assertEquals(VECTOR.getRecord(RLPDecoder.RLP_STRICT), record.getRecord(RLPDecoder.RLP_STRICT));
        Assert.assertEquals(VECTOR.toString(), record.toString());
        Assert.assertEquals(VECTOR, record);
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
