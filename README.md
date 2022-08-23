[![Maven Central](https://img.shields.io/maven-central/v/com.esaulpaugh/headlong.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.esaulpaugh%22%20AND%20a:%22headlong%22)
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![jdk1.8+](https://img.shields.io/badge/JDK-1.8+-blue.svg)](https://openjdk.java.net/)
[![Java CI](https://github.com/esaulpaugh/headlong/workflows/Java%20CI/badge.svg)](https://github.com/esaulpaugh/headlong/actions?query=workflow%3A"Java+CI")
[![Gitter](https://badges.gitter.im/esaulpaugh-headlong/community.svg)](https://gitter.im/esaulpaugh-headlong/community)

Contract ABI and Recursive Length Prefix made easy for the JVM.

ABI spec: https://solidity.readthedocs.io/en/latest/abi-spec.html

RLP spec: https://github.com/ethereum/wiki/wiki/RLP

SHA-256 (headlong-8.0.0.jar): ce55ec4b3a9c9087f9f9cfad304e79688e7421e75b1b056853ecf5a79611f754

## Usage

### ABI codec

#### Encoding Function Calls

```java
Function f = new Function("baz(uint32,bool)"); // canonicalizes and parses any signature
// or
Function f2 = Function.fromJson("{\"type\":\"function\",\"name\":\"foo\",\"inputs\":[{\"name\":\"complex_nums\",\"type\":\"tuple[]\",\"components\":[{\"name\":\"real\",\"type\":\"fixed168x10\"},{\"name\":\"imaginary\",\"type\":\"fixed168x10\"}]}]}");

Tuple args = new Tuple(69L, true);

// Two equivalent styles:
ByteBuffer one = f.encodeCall(args);
ByteBuffer two = f.encodeCallWithArgs(69L, true);

System.out.println(Function.formatCall(one.array())); // a multi-line hex representation
System.out.println(f.decodeCall(two).equals(args));
```

#### Decoding Return Values

```java
Function foo = new Function("foo((fixed[],int8)[1][][5])", "(ufixed,string)");

// decode return type (ufixed,string)
Tuple decoded = foo.decodeReturn(
        FastHex.decode(
                "0000000000000000000000000000000000000000000000000000000000000045"
              + "0000000000000000000000000000000000000000000000000000000000000040"
              + "000000000000000000000000000000000000000000000000000000000000000e"
              + "59616f62616e6745696768747939000000000000000000000000000000000000"
        )
);
        
System.out.println(decoded.equals(new Tuple(new BigDecimal(BigInteger.valueOf(69L), 18), "YaobangEighty9")));
```

```java
Function fooTwo = new Function("fooTwo()", "(uint8)");
int returned = fooTwo.decodeSingletonReturn(FastHex.decode("00000000000000000000000000000000000000000000000000000000000000FF"));
```

#### Creating types directly

```java
BooleanType bool = TypeFactory.create("bool");
IntType uint24 = TypeFactory.create("uint24");
```

### RLP codec

```java
// for an example class Student
public Student(byte[] rlp) {
    Iterator<RLPItem> iter = RLP_STRICT.sequenceIterator(rlp);
    
    this.name = iter.next().asString(UTF_8);
    this.gpa = iter.next().asFloat(false);
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
    return RLPEncoder.sequence(toObjectArray());
}
```

## Build

Now available in Maven Central Repository.

Or build locally:

Clone the project and install to your local maven repository using `gradle publishToMavenLocal` or `mvn install`, then declare it as a dependency:

```kotlin
implementation("com.esaulpaugh:headlong:8.0.1-SNAPSHOT")
```

```xml
<dependency>
    <groupId>com.esaulpaugh</groupId>
    <artifactId>headlong</artifactId>
    <version>8.0.1-SNAPSHOT</version>
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
* EIP-55 Mixed-case checksum address encoding
* Keccak
* hexadecimal

headlong depends on gson v2.9.1. Test suite should take less than one minute to run. Test packages require junit. Jar size is ~120 KiB. Java 8+.

See the wiki for more, such as TupleTypes, packed encoding (and decoding), and RLP Object Notation: https://github.com/esaulpaugh/headlong/wiki

Licensed under Apache 2.0 terms
