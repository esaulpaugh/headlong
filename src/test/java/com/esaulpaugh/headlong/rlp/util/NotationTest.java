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
            "  \"80\", \n" +
            "  {\n" +
            "    \"64\", \n" +
            "    \"3b\", \n" +
            "    { \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"60\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\", \"00\" }, \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"01\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"00\", \n" +
            "    \"05\"\n" +
            "  }\n" +
            ")";

    @Test
    public void parse() throws DecodeException {

        byte[] rlp = Strings.decode("8180f84a643bf20000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000000000000000000000000000000000100000000000005", HEX);
        String notation = Notation.forEncoding(rlp).toString();
        System.out.println(notation);

    /*
(
  "80",
  {
    "64",
    "3b",
    { "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "60", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00", "00" },
    "00",
    "00",
    "00",
    "00",
    "00",
    "00",
    "00",
    "00",
    "00",
    "00",
    "00",
    "00",
    "00",
    "01",
    "00",
    "00",
    "00",
    "00",
    "00",
    "00",
    "05"
  }
)
    */

        Notation n = Notation.forEncoding(RLPEncoder.encodeSequentially(NotationParser.parse(NOTATION)));
        Assert.assertEquals(n.toString(), notation);

        List<Object> rlp2Objects = NotationParser.parse(notation);
        byte[] rlp3 = RLPEncoder.encodeSequentially(rlp2Objects);
        System.out.println(Strings.encode(rlp3, HEX)); // "8363617420c2c00900"

        Assert.assertArrayEquals(rlp, rlp3);
    }
}
