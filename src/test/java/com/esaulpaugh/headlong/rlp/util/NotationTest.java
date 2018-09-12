package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.rlp.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.esaulpaugh.headlong.util.Strings.HEX;

public class NotationTest {

    private static final String NOTATION = "(\n" +
            "  \"636174\", \n" +
            "  \"20\", \n" +
            "  { {  }, \"09\" }, \n" +
            "  \"00\"\n" +
            ")";

    @Test
    public void parse() throws DecodeException {
        byte[] rlp2 = Strings.decode("8363617420c2c00900", HEX);
        String notation = Notation.forEncoding(rlp2).toString();
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

        List<Object> rlp2Objects = NotationParser.parse(notation);
        byte[] rlp3 = RLPEncoder.encodeSequentially(rlp2Objects);
        System.out.println(Strings.encode(rlp3, HEX)); // "8363617420c2c00900"

        Assert.assertArrayEquals(rlp2, rlp3);
    }
}
