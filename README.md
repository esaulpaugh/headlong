# headlong
An ultra-fast Recursive Length Prefix library in Java for use on the Ethereum network (see https://github.com/ethereum/wiki/wiki/RLP ). Highly optimized to avoid unnecessary loops, branches, array accesses, and object creation. Where possible, switch statements are used instead of series of ifs (DataType.java) and loops are unrolled. Some loops are avoided by using switch statements whose cases fall through (Integers.java).

When decoding, the source array is retained throughout and is shared by all items. The first time an item's data (payload) bytes are read is when the caller specifically demands that item's data.

When encoding, long items' length bytes are inserted without the use of an intermediate byte array.

Decode tested with data up to 2,147,483,634 bytes in length (list of 2,147,483,634 single-byte items). See RLPDecoderTest.java for more.

Performance tests run end-to-end: each decode instantiates a new model object and populates its fields with data from the RLP-encoding, while each encode serializes the fields of a model object and RLP-encodes them together.

    Doing 1,000,000 decodes of:
    (
      "506c61746f", 
      "460ca00a", 
      "05df4c1779444a46b5e2c7c250804dfe56eb0d15e7110529027dfa417ef15022a2a41538fb0bacf0075fa8b8b8296a31423086215e499999b18b26706ed5c72c0f87a4357a95e436cde13fd701db67fcc4b2a8c02054e3f44a51198bf4ab28765afbd20a77ab3402dce279dacdbdcb010607a063909ed060c3ae328ae0b31c859bde1a2bd6f6e077a7ee", 
      "269da3281d03da142f61ba27534caabf68f4cf30bcd23399b8cdf6fddc601f76012819f4572f901661ec6a5122f901661ec6a512", 
      "79"
    )
    424.605648 millis
    Plato, 9000.01, 4985171925177469519069106538310315399061439994905457630737444576842633660032341334045557753398756774641171344454574773116437915011585098223616997808646108791102204299408333055218850214970458738645766594728222970017526845486857081446623778582802968310343875618558953334719391315040897001167211719468738969861395985801476512020932590, $2552.7185792349726775956284153005464480874316939890710382513661202185792349726775956284153005464480874316939890710382513661202


Usage:

        // decode
        final byte[] rlp0 = new byte[] { (byte) 0xc0, (byte) 0x83, 'c', 'a', 't', 0x09, 0x09 };
        RLPItem item0 = RLP_STRICT.wrap(rlp0, 1); // wrap item at index 1
        String cat = item0.asString(UTF_8); // "cat"
        RLPItem item1 = RLP_STRICT.wrap(rlp0, item0.endIndex);
        String hex = item1.asString(HEX); // "09"

        // encode a list item with n elements
        byte[] rlp1 = RLPEncoder.encodeAsList(new byte[0], FloatingPoint.toBytes(0.5f), new Object[] {} );
        System.out.println(Strings.encode(rlp1, HEX)); // "c780843f000000c0"

        // concatenate n encodings
        byte[] rlp2 = RLPEncoder.encodeSequentially(Strings.decode(cat, UTF_8), Integers.toBytes(32L), new Object[] { new Object[] {}, new byte[] { '\t' } }, FloatingPoint.toBytes(0.0));
        System.out.println(Strings.encode(rlp2, HEX)); // "8363617420c2c00980"

        // Object notation and parser for debugging
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
        List<Object> rlp2Objects = Parser.parse(notation);
        byte[] rlp3 = RLPEncoder.encodeSequentially(rlp2Objects);
        System.out.println(Strings.encode(rlp3, HEX)); // "8363617420c2c00900"
