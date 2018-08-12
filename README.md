# headlong
An ultra-fast Recursive Length Prefix library in Java for use on the Ethereum network (see https://github.com/ethereum/wiki/wiki/RLP). Highly optimized to avoid unnecessary loops, branches, and array copies. Where possible, a switch statement is used in lieu of a series of ifs (DataType.java). Loops of a known finite length are unrolled. Some loops are replaced with switch statements whose cases fall through (Integers.java).

When decoding, the source array is retained throughout and is shared by all items. The first time an item's data (payload) bytes are read is when the caller specifically demands that item's data.

Example usage:

    public Student(byte[] rlp, int index) throws DecodeException {
        RLPItem item = RLP_STRICT.wrap(rlp, index);
        this.name = item.asString(UTF_8);
        item = RLP_STRICT.wrap(rlp, item.endIndex);
        this.gpa = item.asFloat();
        item = RLP_STRICT.wrap(rlp, item.endIndex);
        this.publicKey = item.asBigInt();
        item = RLP_STRICT.wrap(rlp, item.endIndex);
        BigInteger intVal = item.asBigInt();
        item = RLP_STRICT.wrap(rlp, item.endIndex);
        this.balance = new BigDecimal(intVal, item.asInt());
    }
    
    @Override
    public Object[] toObjectArray() {
        return new Object[] {
            Strings.decode(name, UTF_8),
            FloatingPoint.toBytes(gpa),
            publicKey.toByteArray(),
            balance.unscaledValue().toByteArray(),
            Integers.toBytes(balance.scale())
        };
    }
    
    @Override
    public byte[] toRLP() {
        return RLPEncoder.encodeSequentially(toObjectArray());
    }
    
Alternative style:

    public class StudentRLPAdapter implements RLPAdapter<Student> {

        @Override
        public Student decode(byte[] rlp, int index) throws DecodeException {
            RLPList rlpList = (RLPList) RLP_STRICT.wrap(rlp, index);
            List<RLPItem> elements = rlpList.elements(RLP_STRICT);
            return new Student(
                    elements.get(0).asString(UTF_8),
                    elements.get(1).asFloat(),
                    elements.get(2).asBigInt(),
                    new BigDecimal(elements.get(3).asBigInt(), elements.get(4).asInt())
            );
        }
        
        @Override
        public byte[] encode(Student student) {
            return RLPEncoder.encodeAsList(student.toObjectArray());
        }
    }
    
Features support for integers (including negative), floating point numbers, chars, and booleans, as well as Strings, byte arrays, Object arrays, and Iterable<Object>.

Decode tested with data up to 2,147,483,634 bytes in length (list of 2,147,483,634 single-byte items). See RLPDecoderTest.java for more.

Object notation and parser for debugging:

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
    List<Object> rlp2Objects = Parser.parse(notation);
    byte[] rlp3 = RLPEncoder.encodeSequentially(rlp2Objects);
    System.out.println(Strings.encode(rlp3, HEX)); // "8363617420c2c00900"
