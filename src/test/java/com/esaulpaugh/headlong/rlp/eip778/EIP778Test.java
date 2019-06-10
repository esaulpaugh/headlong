/*
   Copyright 2019 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong.rlp.eip778;

import com.esaulpaugh.headlong.TestUtils;
import com.esaulpaugh.headlong.rlp.RLPDecoder;
import com.esaulpaugh.headlong.rlp.RLPList;
import com.esaulpaugh.headlong.rlp.RLPListIterator;
import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.util.FastHex;
import org.junit.Assert;
import org.junit.Test;

import java.security.SignatureException;
import java.util.HashSet;
import java.util.Set;

import static com.esaulpaugh.headlong.rlp.eip778.KeyValuePair.*;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class EIP778Test {

    private static final String ENR_STRING = "enr:-IS4QHCYrYZbAKWCBRlAy5zzaDZXJBGkcnh4MHcBFZntXNFrdvJjX04jRzjzCBOonrkTfj499SZuOh8R33Ls8RRcy5wBgmlkgnY0gmlwhH8AAAGJc2VjcDI1NmsxoQPKY0yuDUmstAHYpMa2_oxVtw0RW_QAdpzBQA8yWM0xOIN1ZHCCdl8";

    private static final Record.Signer SIGNER = new Record.Signer() {

        private final byte[] SIG = FastHex.decode(
                "7098ad865b00a582051940cb9cf36836572411a47278783077011599ed5cd16b"
              + "76f2635f4e234738f30813a89eb9137e3e3df5266e3a1f11df72ecf1145ccb9c"
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

    private static final Record VECTOR;

    static {
        try {
            VECTOR = Record.parse(ENR_STRING);
        } catch (DecodeException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEip778() throws DecodeException, SignatureException {
        final KeyValuePair[] pairs = new KeyValuePair[] {
                new KeyValuePair(IP, "7f000001", HEX),
                new KeyValuePair(UDP, "765f", HEX),
                new KeyValuePair(ID, "v4", UTF_8),
                new KeyValuePair(SECP256K1, "03ca634cae0d49acb401d8a4c6b6fe8c55b70d115bf400769cc1400f3258cd3138", HEX)
        };

        Record record = new Record(1L, pairs, SIGNER);

        Assert.assertEquals(VECTOR.getSignature(), record.getSignature());
        Assert.assertEquals(VECTOR.getContent(), record.getContent());
        Assert.assertEquals(VECTOR.getRLP(), record.getRLP());
        Assert.assertEquals(VECTOR.toString(), record.toString());
        Assert.assertEquals(VECTOR, record);

        RLPList content = record.decode((s,c) -> {});
        System.out.println("verified = " + content);
        RLPListIterator iter = content.iterator(RLPDecoder.RLP_STRICT);

        Assert.assertEquals(VECTOR.getSeq(), record.getSeq());

        long seq = iter.next().asLong();

        Assert.assertEquals(1L, seq);

        KeyValuePair[] decodedPairs = new KeyValuePair[pairs.length];
        int i = 0;
        while (iter.hasNext()) {
            decodedPairs[i++] = new KeyValuePair(iter.next().data(), iter.next().data());
        }
        Assert.assertArrayEquals(pairs, decodedPairs);

        Assert.assertEquals(ENR_STRING, record.toString());
    }

    @Test
    public void nineLengths() {
        Set<Integer> recordLengths = new HashSet<>();
        for (long p = 0, seq = 0; p <= 64; p += 8, seq = (long) Math.pow(2.0, p)) {
            long temp = seq - 2;
            int i = 0;
            do {
                if(temp >= 0) {
                    Record r = new Record(temp, KeyValuePair.EMPTY_ARRAY, SIGNER);
                    int len = r.getRLP().encodingLength();
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
