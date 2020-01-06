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
package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.util.FastHex;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NotationTest {

    private static final byte[] ENCODING = FastHex.decode("c0c1808180f8507f3bcec00080860030ffcc00090102c1700001f83bb8390102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738390005");

    private static final String NOTATION = "(\n" +
            "  {  }, \n" +
            "  { \"\" }, \n" +
            "  \"80\", \n" +
            "  {\n" +
            "    \"7f\", \n" +
            "    \"3b\", \n" +
            "    { {  }, \"00\", \"\", \"0030ffcc0009\", \"01\", \"02\", { \"70\" } }, \n" +
            "    \"00\", \n" +
            "    \"01\", \n" +
            "    {\n" +
            "      \"0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f30313233343536373839\"\n" +
            "    }\n" +
            "  }, \n" +
            "  \"00\", \n" +
            "  \"05\"\n" +
            ")";

    @Test
    public void test() throws DecodeException {

        String notation = Notation.forEncoding(ENCODING).toString(); // Arrays.copyOfRange(rlp, 10, rlp.length)

        System.out.println(notation);

        Notation n = Notation.forEncoding(RLPEncoder.encodeSequentially(NotationParser.parse(NOTATION)));
        assertEquals(n.toString(), notation);

        List<Object> objects = NotationParser.parse(notation);

        assertEquals(n, Notation.forObjects(objects));

        byte[] rlp2 = RLPEncoder.encodeSequentially(objects);
        System.out.println(FastHex.encodeToString(rlp2));

        assertArrayEquals(ENCODING, rlp2);
    }
}
