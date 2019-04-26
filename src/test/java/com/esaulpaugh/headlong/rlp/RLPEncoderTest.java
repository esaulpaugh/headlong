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
package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.esaulpaugh.headlong.util.Strings.HEX;

public class RLPEncoderTest {

    @Test
    public void encodeSequentially() {
        Object[] objects = new Object[] {
                new Object[0],
                new byte[0]
        };

        byte[] encoded = RLPEncoder.encodeSequentially(objects);

        Assert.assertEquals(
                Strings.encode(new byte[] { (byte)0xc0, (byte)0x80 }, HEX),
                Strings.encode(encoded, HEX)
        );
    }

    @Test
    public void encodeAsList() {

        Object[] objects = new Object[] {
                new Object[0],
                new byte[0]
        };

        byte[] encoded = RLPEncoder.encodeAsList(objects);

        Assert.assertEquals(
                Strings.encode(new byte[] { (byte)0xc2, (byte)0xc0, (byte)0x80 }, HEX),
                Strings.encode(encoded, HEX)
        );
    }

    @Test
    public void toList() throws DecodeException {

        RLPItem item0 = RLPDecoder.RLP_STRICT.wrap(new byte[] {(byte) 0x81, (byte) 0x80 });
        RLPItem item1 = RLPDecoder.RLP_STRICT.wrap(new byte[] {(byte) 0x7e });
        RLPItem item2 = RLPDecoder.RLP_STRICT.wrap(new byte[] {(byte) 0xc1, (byte) 0x80 });

        RLPList rlpList = RLPEncoder.toList(item0, item1, item2);
        List<RLPItem> elements = rlpList.elements(RLPDecoder.RLP_STRICT);

        Assert.assertEquals(3, elements.size());

        Assert.assertNotSame(elements.get(0), item0);
        Assert.assertNotSame(elements.get(1), item1);
        Assert.assertNotSame(elements.get(2), item2);

        Assert.assertEquals(elements.get(0), item0);
        Assert.assertEquals(elements.get(1), item1);
        Assert.assertEquals(elements.get(2), item2);
    }
}
