Contract ABI and Recursive Length Prefix made easy in Java (or Android). Everything heavily optimized for maximum throughput (ABI function call encoding up to 652x faster than a popular competitor).

Usage of the ABI codec:

    Function f = new Function("baz(uint32,bool)"); // canonicalizes and parses any signature automatically
    Tuple argsTuple = new Tuple(69L, true);
    ByteBuffer one = f.encodeCall(argsTuple);
    ByteBuffer two = f.encodeCallForArgs(69L, true);
    
    System.out.println(Function.formatABI(one.array())); // a nicely formatted hex representation
    
    Tuple decoded = f.decodeCall((ByteBuffer) two.flip());
    
    System.out.println(decoded.equals(argsTuple));

And of the RLP codec:

    // for an example class Student
    public Student(byte[] rlp) throws DecodeException {
        SequenceIterator iter = RLP_STRICT.sequenceIterator(rlp);

        this.name = iter.next().asString(UTF_8);
        this.gpa = iter.next().asFloat();
        this.publicKey = iter.next().asBigInt();
        this.balance = new BigDecimal(iter.next().asBigInt(), iter.next().asInt());
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

Also includes a fast hex codec and an optimized Keccak implementation.

### Build

Clone the project and install to your local maven repository using `gradle publishToMavenLocal` or `mvn install`. Then you can use one of these:

    implementation 'com.esaulpaugh:headlong:1.0-SNAPSHOT'

    <dependency>
        <groupId>com.esaulpaugh</groupId>
        <artifactId>headlong</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>

Alternatively:

* Run `gradle build` or `gradle jar` which output to /build/libs
* Use `mvn package` which outputs to /target
* Add headlong as a project dependency

Tests should take 2-4 minutes to run. Test packages require junit and gson. Otherwise headlong has no dependencies. Size is ~106 KB as of 12/13/18.

See the wiki for more, such as RLP Lists and RLP Object Notation: https://github.com/esaulpaugh/headlong/wiki

Licensed under Apache 2.0
