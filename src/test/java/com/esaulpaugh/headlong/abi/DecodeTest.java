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
package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.FastHex;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DecodeTest {

    private static final Function FUNCTION;

    static {
        try {
            FUNCTION = new Function("gogo((fixed[],int8)[1][][5])", "(ufixed,string)");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static final byte[] RETURN_BYTES = FastHex.decode(
              "0000000000000000000000000000000000000000000000000000000000000045"
            + "0000000000000000000000000000000000000000000000000000000000000020"
            + "0000000000000000000000000000000000000000000000000000000000000004"
            + "7730307400000000000000000000000000000000000000000000000000000000"
    );

    private static final Tuple EXPECTED = new Tuple(new BigDecimal(BigInteger.valueOf(69L), 18), "w00t");

    @Test
    public void testDecode() throws ParseException {

        Tuple decoded = FUNCTION.decodeReturn(RETURN_BYTES);
        assertEquals(EXPECTED, decoded);

        decoded = FUNCTION.getOutputTypes().decode(RETURN_BYTES);
        assertEquals(EXPECTED, decoded);

        decoded = TupleType.parse(FUNCTION.getOutputTypes().toString()).decode(ByteBuffer.wrap(RETURN_BYTES));
        assertEquals(EXPECTED, decoded);

        decoded = TupleType.parseElements("ufixed,string").decode(ByteBuffer.wrap(RETURN_BYTES));
        assertEquals(EXPECTED, decoded);
    }
}
