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
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.ParseException;

public class DecodeTest {

    private final Tuple expected = new Tuple(new BigDecimal(BigInteger.valueOf(69L), 18), "w00t");

    @Test
    public void testDecode() throws ParseException {

        Function getUfixedAndString = new Function("gogo((fixed[],int8)[1][][5])", "(ufixed,string)");

        Tuple decoded = getUfixedAndString.decodeReturn(
                FastHex.decode(
                        "0000000000000000000000000000000000000000000000000000000000000045"
                                + "0000000000000000000000000000000000000000000000000000000000000020"
                                + "0000000000000000000000000000000000000000000000000000000000000004"
                                + "7730307400000000000000000000000000000000000000000000000000000000"
                )
        );
        Assert.assertEquals(expected, decoded);

        decoded = getUfixedAndString.getOutputTypes().decode(
                FastHex.decode(
                        "0000000000000000000000000000000000000000000000000000000000000045"
                                + "0000000000000000000000000000000000000000000000000000000000000020"
                                + "0000000000000000000000000000000000000000000000000000000000000004"
                                + "7730307400000000000000000000000000000000000000000000000000000000"
                )
        );
        Assert.assertEquals(expected, decoded);

        decoded = TupleType.parse("(ufixed,string)").decode(ByteBuffer.wrap(FastHex.decode(
                "0000000000000000000000000000000000000000000000000000000000000045"
                        + "0000000000000000000000000000000000000000000000000000000000000020"
                        + "0000000000000000000000000000000000000000000000000000000000000004"
                        + "7730307400000000000000000000000000000000000000000000000000000000"
        )));
        Assert.assertEquals(expected, decoded);

        decoded = TupleType.parseElements("ufixed,string").decode(ByteBuffer.wrap(FastHex.decode(
                "0000000000000000000000000000000000000000000000000000000000000045"
                        + "0000000000000000000000000000000000000000000000000000000000000020"
                        + "0000000000000000000000000000000000000000000000000000000000000004"
                        + "7730307400000000000000000000000000000000000000000000000000000000"
        )));
        Assert.assertEquals(expected, decoded);
    }

}
