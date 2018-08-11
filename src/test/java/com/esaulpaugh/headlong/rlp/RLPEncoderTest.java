package com.esaulpaugh.headlong.rlp;

import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.List;

public class RLPEncoderTest {

    @Test
    public void encodeSequentially() {
        Object[] objects = new Object[] {
                new Object[0],
                new byte[0]
        };

        byte[] encoded = RLPEncoder.encodeSequentially(objects);

        Assert.assertEquals(
                Hex.toHexString(new byte[] { (byte)0xc0, (byte)0x80 }),
                Hex.toHexString(encoded)
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
                Hex.toHexString(new byte[] { (byte)0xc2, (byte)0xc0, (byte)0x80 }),
                Hex.toHexString(encoded)
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
