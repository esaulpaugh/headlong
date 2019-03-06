[![Maven Central](https://img.shields.io/maven-central/v/com.esaulpaugh/headlong.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.esaulpaugh%22%20AND%20a:%22headlong%22)

Contract ABI and Recursive Length Prefix made easy in Java (or Android). Everything heavily optimized for maximum throughput (ABI function call encoding up to 652x faster than a popular competitor).

Usage of the ABI codec:

```java
Function f = new Function("baz(uint32,bool)"); // canonicalizes and parses any signature automatically
Tuple args = new Tuple(69L, true);

// Two equivalent styles:
ByteBuffer one = f.encodeCall(args);
ByteBuffer two = f.encodeCallWithArgs(69L, true);

System.out.println(Function.formatCall(one.array())); // a multi-line hex representation

Tuple decoded = f.decodeCall((ByteBuffer) two.flip());

System.out.println(decoded.equals(args));
```

And of the RLP codec:

```java
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
```

Also includes a fast hex codec and an optimized Keccak implementation.

See also the example app that demos the ABI encoder: https://github.com/esaulpaugh/headlong-android

### Build

Now available in Maven Central Repository.

Or build locally:

Clone the project and install to your local maven repository using `gradle publishToMavenLocal` or `mvn install`. Then you can use one of these:

```groovy
implementation 'com.esaulpaugh:headlong:1.0-SNAPSHOT'
```

```xml
<dependency>
    <groupId>com.esaulpaugh</groupId>
    <artifactId>headlong</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```
Alternatively:

* Run `gradle build` or `gradle jar` which output to /build/libs
* Use `mvn package` which outputs to /target
* Add headlong as a project dependency

Depends on gson. Tests should take less than one minute to run. Test packages require junit. Jar size is ~119 KB as of 03/06/19.

See the wiki for more, such as TupleTypes, packed encoding, RLP Lists, and RLP Object Notation: https://github.com/esaulpaugh/headlong/wiki

Licensed under Apache 2.0
