[![Maven Central](https://img.shields.io/maven-central/v/com.esaulpaugh/headlong.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.esaulpaugh%22%20AND%20a:%22headlong%22)
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![jdk1.8+](https://img.shields.io/badge/JDK-1.8+-blue.svg)](https://openjdk.java.net/)
[![Java CI](https://github.com/esaulpaugh/headlong/workflows/Java%20CI/badge.svg)](https://github.com/esaulpaugh/headlong/actions?query=workflow%3A"Java+CI")
[![Gitter](https://badges.gitter.im/esaulpaugh-headlong/community.svg)](https://gitter.im/esaulpaugh-headlong/community)

Contract ABI (v2) and Recursive Length Prefix made easy for the JVM. Everything heavily optimized for maximum throughput (ABI function call encoding up to 500x faster than a popular competitor. One function init plus one encode up to 50x faster (run benchmarks with `gradle jmh` or run the maven-generated benchmarks jar)).

ABI spec: https://solidity.readthedocs.io/en/latest/abi-spec.html

RLP spec: https://github.com/ethereum/wiki/wiki/RLP

SHA-256 (headlong-3.6.0.jar): ead38696478200f86060724a95a943cec38e3a247e7624045b2186b0a579430e

## Usage

### ABI codec

#### Encoding Function Calls

```java
Function f = new Function("baz(uint32,bool)"); // canonicalizes and parses any signature
// or
Function f2 = Function.fromJson("{\"type\":\"function\",\"name\":\"foo\",\"inputs\":[{\"name\":\"complex_nums\",\"type\":\"tuple[]\",\"components\":[{\"name\":\"real\",\"type\":\"decimal\"},{\"name\":\"imaginary\",\"type\":\"decimal\"}]}]}");

Tuple args = new Tuple(69L, true);

// Two equivalent styles:
ByteBuffer one = f.encodeCall(args);
ByteBuffer two = f.encodeCallWithArgs(69L, true);

System.out.println(Function.formatCall(one.array())); // a multi-line hex representation
System.out.println(f.decodeCall((ByteBuffer) two.flip()).equals(args));
```

#### Decoding Return Values

```java
Function foo = new Function("foo((fixed[],int8)[1][][5])", "(ufixed,string)");

// decode return type (ufixed,string)
Tuple decoded = foo.decodeReturn(
        FastHex.decode(
                "0000000000000000000000000000000000000000000000000000000000000045"
              + "0000000000000000000000000000000000000000000000000000000000000020"
              + "0000000000000000000000000000000000000000000000000000000000000004"
              + "7730307400000000000000000000000000000000000000000000000000000000"
        )
);
        
System.out.println(decoded.equals(new Tuple(new BigDecimal(BigInteger.valueOf(69L), 18), "w00t")));
```

### RLP codec

```java
// for an example class Student
public Student(byte[] rlp) {
    Iterator<RLPItem> iter = RLP_STRICT.sequenceIterator(rlp);
    
    this.name = iter.next().asString(UTF_8);
    this.gpa = iter.next().asFloat();
    this.publicKey = iter.next().asBytes();
    this.balance = new BigDecimal(iter.next().asBigInt(), iter.next().asInt());
}

@Override
public Object[] toObjectArray() {
    return new Object[] {
            // instances of byte[]
            Strings.decode(name, UTF_8),
            FloatingPoint.toBytes(gpa),
            publicKey,
            balance.unscaledValue().toByteArray(),
            Integers.toBytes(balance.scale())
            // include an Object[] or Iterable and its elements will be encoded as an RLP list (which may include other lists)
    };
}

@Override
public byte[] toRLP() {
    return RLPEncoder.encodeSequentially(toObjectArray());
}
```

## Build

Now available in Maven Central Repository.

Or build locally:

Clone the project and install to your local maven repository using `gradle publishToMavenLocal` or `mvn install`, then declare it as a dependency:

```groovy
implementation 'com.esaulpaugh:headlong:3.6.1-SNAPSHOT'
```

```xml
<dependency>
    <groupId>com.esaulpaugh</groupId>
    <artifactId>headlong</artifactId>
    <version>3.6.1-SNAPSHOT</version>
</dependency>
```
Alternatively:

* Run `gradle build` or `gradle jar` which output to `build/libs`
* Use `mvn package` which outputs to `target`
* Execute `ant all build-jar` which outputs to `build/lib`
* Add headlong as a project dependency

## Command line interface

https://github.com/esaulpaugh/headlong-cli

## Demo app

https://github.com/esaulpaugh/headlong-android

## Misc

Also includes optimized implementations of:

* EIP-778 Ethereum Node Records
* hexadecimal
* Keccak

headlong (optionally) depends on gson and bouncycastle. Test suite should take less than one minute to run. Test packages require junit. Jar size is ~125 KiB as of 07/19/20. Java 8+.

See the wiki for more, such as TupleTypes, packed encoding (and decoding), and RLP Object Notation: https://github.com/esaulpaugh/headlong/wiki

Licensed under Apache 2.0 terms
