package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.util.FastHex;
import org.junit.Test;

import java.util.Random;

import static com.esaulpaugh.headlong.rlp.EIP778.*;

import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class EIP778Test {

    @Test
    public void testEip778() throws DecodeException {

        long seq = 6L;

        EIP778.KeyValuePair[] pairs = new EIP778.KeyValuePair[] {
                new EIP778.KeyValuePair(IP, "192.168.0.7"),
                new EIP778.KeyValuePair(UDP, "30301"),
                new EIP778.KeyValuePair(ID, "v4")
        };

        for (KeyValuePair p : pairs) {
            System.out.println(p);
        }

        Record.Signer signer = message -> {
            byte[] random = new byte[32];
            new Random().nextBytes(random);
            return random;
        };

        byte[] record = new EIP778.Record(seq, pairs, signer).getRecord();

        System.out.println("record len = " + record.length);
        System.out.println("record = " + FastHex.encodeToString(record) + '\n');

        RLPListIterator iter = RLPDecoder.RLP_STRICT.listIterator(record);

        System.out.println("signature = " + iter.next().asString(HEX));

        RLPSequenceIterator iter2 = RLPDecoder.RLP_STRICT.sequenceIterator(iter.next().data());

        System.out.println("seq = " + iter2.next().asLong());

        while (iter2.hasNext()) {
            System.out.println(iter2.next().asString(UTF_8) + ", " + iter2.next().asString(UTF_8));
        }
    }
}
