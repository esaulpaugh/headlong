package com.esaulpaugh.headlong.rlp.util;

import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.esaulpaugh.headlong.util.Strings.HEX;

public class NotationTest {

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
    public void parse() throws DecodeException {

        byte[] rlp = FastHex.decode("c0c1808180f8507f3bcec00080860030ffcc00090102c1700001f83bb8390102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738390005");
        String notation = Notation.forEncoding(rlp).toString(); // Arrays.copyOfRange(rlp, 10, rlp.length)
        System.out.println(notation);

    /*
(
  {  },
  { "" },
  "80",
  {
    "7f",
    "3b",
    { {  }, "00", "", "0030ffcc0009", "01", "02", { "70" } },
    "00",
    "01",
    {
      "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f30313233343536373839"
    }
  },
  "00",
  "05"
)
    */

        Notation n = Notation.forEncoding(RLPEncoder.encodeSequentially(NotationParser.parse(NOTATION)));
        Assert.assertEquals(n.toString(), notation);

        List<Object> objects = NotationParser.parse(notation);
        byte[] rlp2 = RLPEncoder.encodeSequentially(objects);
        System.out.println(Strings.encode(rlp2, HEX));

        Assert.assertArrayEquals(rlp, rlp2);
    }
}
