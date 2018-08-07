# headlong
An ultra-fast Recursive Length Prefix Java library for use on the ethereum network

Usage:

        // decode
        final byte[] rlp0 = new byte[] { (byte) 0xc0, (byte) 0x83, 'c', 'a', 't', 0x09, 0x09 };
        RLPItem item0 = RLPCodec.wrap(rlp0, 1); // wrap item at index 1
        String cat = item0.asString(UTF_8); // "cat"
        RLPItem item1 = RLPCodec.wrap(rlp0, item0.endIndex);
        String hex = item1.asString(HEX); // "09"

        // encode a list item with n elements
        byte[] rlp1 = RLPCodec.encodeAsList(new byte[0], FloatingPoint.toBytes(0.5f), new Object[] {} );
        System.out.println(Strings.encode(rlp1, HEX)); // "c780843f000000c0"

        // concatenate n encodings
        byte[] rlp2 = RLPCodec.encodeSequentially(Strings.decode(cat, UTF_8), Integers.toBytes(32L), new Object[] { new Object[] {}, new byte[] { '\t' } }, FloatingPoint.toBytes(0.0));
        System.out.println(Strings.encode(rlp2, HEX)); // "8363617420c2c00900"

        // Object notation and parser for debugging
        String notation = ObjectNotation.fromEncoding(rlp2).toString();
        System.out.println(notation);
    /*
        (
          "636174",
          "20",
          { {  }, "09" },
          "00"
        )
    */
        List<Object> rlp2Objects = Parser.parse(notation);
        byte[] rlp3 = RLPCodec.encodeSequentially(rlp2Objects);
        System.out.println(Strings.encode(rlp3, HEX)); // "8363617420c2c00900"
