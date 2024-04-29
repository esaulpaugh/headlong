/*
   Copyright 2020 Evan Saulpaugh

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

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SuperSerialTest {

    @Test
    public void testSuperSerial() throws Throwable {
        // java -jar headlong-cli-0.2-SNAPSHOT.jar -e "(uint[],int[],uint32,(int32,uint8,(bool[],int8,int40,int64,int,int,int[]),bool,bool,int256[]),int,int)" "([  ], [ '' ], '80', ['7f', '3b', [ [  ], '', '', '30ffcc0009', '01', '02', [ '70' ] ], '', '01', ['0092030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f2020']], '', '05')"

        assertThrown(IllegalArgumentException.class, "tuple index 0: signed val exceeds bit limit: 256 >= 256",
                () -> SuperSerial.deserialize(TupleType.parse("(int256)"), "('0092030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f2020')", false)
        );

        Tuple x = SuperSerial.deserialize(TupleType.parse("(uint256)"), "('92030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f2020')", false);
        assertEquals(new BigInteger("92030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f2020", 16), x.get(0));

        assertThrown(IllegalArgumentException.class, "RLPList not allowed for this type: int8",
                () -> SuperSerial.deserialize(TupleType.of("int8"), "(['90', '80', '77'])", false)
        );

        String sig = "(uint[],int[],uint32,(int32,uint8,(bool[],int8,int40,int64,int,int,int[]),bool,bool,int256[]),int,int)";

        Function f = new Function(sig);

        String vals = "([  ], [ '' ], '80', ['7f', '3b', [ [  ], '', '', '30ffcc0009', '01', '02', [ '70' ] ], '', '01', ['92030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f2020']], '', '05')";

        Tuple decoded = SuperSerial.deserialize(f.getInputs(), vals, false);

        f.getInputs().validate(decoded);

        ByteBuffer bb = f.getInputs().encode(decoded);

        Tuple dd = f.getInputs().decode(bb);

        assertEquals(decoded, dd);
    }

    @Test
    public void testBoolean() throws Throwable {

        TupleType<?> _bool_ = TupleType.parse("(bool)");
        Tuple _true = Single.of(true);
        Tuple _false = Single.of(false);

        assertEquals("01", SuperSerial.serialize(_bool_, _true, true));
        assertEquals("80", SuperSerial.serialize(_bool_, _false, true));
        assertEquals("(\n  '01'\n)", SuperSerial.serialize(_bool_, _true, false));
        assertEquals("(\n  ''\n)", SuperSerial.serialize(_bool_, _false, false));

        Single<Boolean> t = SuperSerial.deserialize(_bool_, "('01')", false);
        assertTrue((boolean) t.get(0));
        t = SuperSerial.deserialize(_bool_, "('')", false);
        assertFalse((boolean) t.get(0));
        assertThrown(IllegalArgumentException.class, "illegal boolean RLP. Expected 0x01 or 0x80.",
                () -> SuperSerial.deserialize(_bool_, "('00')", false));
        assertThrown(IllegalArgumentException.class, "illegal boolean RLP. Expected 0x01 or 0x80.",
                () -> SuperSerial.deserialize(_bool_, "('fcd1')", false));

        t = SuperSerial.deserialize(_bool_, "01", true);
        assertTrue((boolean) t.get(0));
        t = SuperSerial.deserialize(_bool_, "80", true);
        assertFalse((boolean) t.get(0));
        assertThrown(IllegalArgumentException.class, "illegal boolean RLP. Expected 0x01 or 0x80.",
                () -> SuperSerial.deserialize(_bool_, "00", true));
        assertThrown(IllegalArgumentException.class, "illegal boolean RLP. Expected 0x01 or 0x80.",
                () -> SuperSerial.deserialize(_bool_, "82fcd1", true));
    }

    @Test
    public void testToFromRLP() {
        final Triple<Boolean, int[], byte[][]> t = Tuple.of(false, new int[] { 0, 1, 2, 3 }, new byte[][] { new byte[0], new byte[1], new byte[] { -1 } });
        final TupleType<?> tt = TupleType.parse("(bool,int8[],bytes[])");
        final byte[] x = SuperSerial.toRLP(tt, t);
        Tuple t_ = SuperSerial.fromRLP(tt, x);
        assertEquals(t, t_);
    }
}
