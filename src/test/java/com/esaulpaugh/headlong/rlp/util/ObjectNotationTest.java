package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.rlp.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import org.junit.Assert;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.util.List;

import static com.esaulpaugh.headlong.rlp.util.Strings.HEX;

public class ObjectNotationTest {

    private static final String NOTATION = "(\n" +
            "  \"636174\", \n" +
            "  \"20\", \n" +
            "  { {  }, \"09\" }, \n" +
            "  \"00\"\n" +
            ")";

    @Test
    public void parse() throws DecodeException {
        byte[] rlp2 = Hex.decode("8363617420c2c00900");
        String notation = ObjectNotation.forEncoding(rlp2).toString();
        System.out.println(notation);

    /*
        (
          "636174",
          "20",
          { {  }, "09" },
          ""
        )
    */

        Assert.assertEquals(NOTATION, notation);

        List<Object> rlp2Objects = Parser.parse(notation);
        byte[] rlp3 = RLPEncoder.encodeSequentially(rlp2Objects);
        System.out.println(Strings.encode(rlp3, HEX)); // "8363617420c2c00900"

        Assert.assertArrayEquals(rlp2, rlp3);
    }
}
