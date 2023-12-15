[![Maven Central](https://img.shields.io/maven-central/v/com.esaulpaugh/headlong.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.esaulpaugh%22%20AND%20a:%22headlong%22)
[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![jdk1.8+](https://img.shields.io/badge/JDK-1.8+-blue.svg)](https://openjdk.java.net/)
[![Java CI](https://github.com/esaulpaugh/headlong/workflows/Java%20CI%20gradle/badge.svg)](https://github.com/esaulpaugh/headlong/actions?query=workflow%3A"Java+CI")
[![Gitter](https://badges.gitter.im/esaulpaugh-headlong/community.svg)](https://gitter.im/esaulpaugh-headlong/community)

Contract ABI and Recursive Length Prefix made easy for the JVM.

ABI spec: https://solidity.readthedocs.io/en/latest/abi-spec.html

RLP spec: https://ethereum.org/en/developers/docs/data-structures-and-encoding/rlp

SHA-256 (headlong-10.0.1.jar): 78a870190445090c5def91eca8b1e71a1ba6b6729a9a8ebab02e46dbab7eea95

## Usage

### ABI package

#### Encoding Function Calls

```java
Function baz = Function.parse("baz(uint32,bool)"); // canonicalizes and parses any signature
// or
Function f2 = Function.fromJson("{\"type\":\"function\",\"name\":\"foo\",\"inputs\":[{\"name\":\"complex_nums\",\"type\":\"tuple[]\",\"components\":[{\"name\":\"real\",\"type\":\"fixed168x10\"},{\"name\":\"imaginary\",\"type\":\"fixed168x10\"}]}]}");

Tuple bazArgs = Tuple.of(69L, true);
Tuple complexNums = Tuple.singleton(new Tuple[] { Tuple.of(new BigDecimal("0.0090000000"), new BigDecimal("1.9500000000")) });

// Two equivalent styles:
ByteBuffer one = baz.encodeCall(bazArgs);
ByteBuffer two = baz.encodeCallWithArgs(69L, true);

System.out.println("baz call hex:\n" + Strings.encode(one) + "\n"); // hexadecimal encoding (without 0x prefix)

Tuple recoveredArgs = baz.decodeCall(two); // decode the encoding back to the original args

System.out.println("baz args:\n" + recoveredArgs + "\n"); // toString()
System.out.println("equal:\n" + recoveredArgs.equals(bazArgs) + "\n"); // test for equality

System.out.println("baz call debug:\n" + Function.formatCall(one.array()) + "\n"); // human-readable, for debugging function calls (expects input to start with 4-byte selector)
System.out.println("baz args debug:\n" + ABIType.format(baz.getInputs().encode(bazArgs).array()) + "\n"); // human-readable, for debugging encodings without a selector
System.out.println("f2 call debug:\n" + Function.formatCall(f2.encodeCall(complexNums).array()) + "\n");
System.out.println("f2 args debug:\n" + ABIType.format(f2.getInputs().encode(complexNums).array()));
```

#### Decoding Return Values

```java
Function foo = Function.parse("foo((fixed[],int8)[1][][5])", "(int,string)");

// decode return type (int256,string)
Tuple decoded = foo.decodeReturn(
    FastHex.decode(
          "000000000000000000000000000000000000000000000000000000000000002A"
        + "0000000000000000000000000000000000000000000000000000000000000040"
        + "000000000000000000000000000000000000000000000000000000000000000e"
        + "59616f62616e6745696768747939000000000000000000000000000000000000"
    )
);

System.out.println(decoded.equals(Tuple.of(BigInteger.valueOf(42L), "YaobangEighty9")));
```

```java
Function fooTwo = Function.parse("fooTwo()", "(uint8)");
int returned = fooTwo.decodeSingletonReturn(FastHex.decode("00000000000000000000000000000000000000000000000000000000000000FF")); // uint8 corresponds to int
System.out.println(returned);
```

#### Using TupleType

```java
TupleType tt = TupleType.parse("(int72[][],address)");
ByteBuffer b0 = tt.getNonCapturing(0).encode(new BigInteger[][] { });
ByteBuffer b1 = tt.getNonCapturing(1).encode(Address.wrap("0x52908400098527886E0F7030069857D2E4169EE7"));
ByteBuffer b2 = tt.encode(Tuple.of(new BigInteger[][] {}, Address.wrap("0x52908400098527886E0F7030069857D2E4169EE7")));
```

#### Misc

```java
Event event = Event.fromJson("{\"type\":\"event\",\"name\":\"an_event\",\"inputs\":[{\"name\":\"a\",\"type\":\"bytes\",\"indexed\":true},{\"name\":\"b\",\"type\":\"uint256\",\"indexed\":false}],\"anonymous\":true}");
Tuple args = event.decodeArgs(new byte[][] { new byte[32] }, new byte[32]);
System.out.println(event);
System.out.println(args);

// create any type directly (advanced)
ArrayType<ABIType<Object>, Object> at = TypeFactory.create("(address,int)[]");
ABIType<Object> unknown = TypeFactory.create(at.getCanonicalType());
```

### RLP package

```java
// for an example class Student implementing some example interface
public Student(byte[] rlp) {
    Iterator<RLPItem> iter = RLPDecoder.RLP_STRICT.sequenceIterator(rlp);
    
    this.name = iter.next().asString(Strings.UTF_8);
    this.gpa = iter.next().asFloat(false);
    this.publicKey = iter.next().asBytes();
    this.balance = new BigDecimal(iter.next().asBigInt(), iter.next().asInt());
}

@Override
public Object[] toObjectArray() {
    return new Object[] {
            // instances of byte[]
            Strings.decode(name, Strings.UTF_8),
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
implementation("com.esaulpaugh:headlong:10.0.2-SNAPSHOT")
```

```xml
<dependency>
    <groupId>com.esaulpaugh</groupId>
    <artifactId>headlong</artifactId>
    <version>10.0.2-SNAPSHOT</version>
</dependency>
```
Alternatively:

* Run `gradle build` or `gradle jar` which output to `build/libs`
* Use `mvn package` which outputs to `target`
* Execute `ant all build-jar` which outputs to `build/lib`
* Add headlong as a project dependency

## Command line interface

https://github.com/esaulpaugh/headlong-cli

## Benchmarks

![Screenshot](https://github.com/esaulpaugh/headlong/blob/master/benchmark_results.jpg)
GraalVM 20.0.1 on ARM

## Misc

Also includes optimized implementations of:

* EIP-778 Ethereum Node Records
* EIP-55 Mixed-case checksum address encoding
* Keccak
* hexadecimal

headlong depends on gson v2.10.1. Test suite should take less than one minute to run. Test packages require junit. Jar size is ~120 KiB. Java 8+.

See the wiki for more, such as packed encoding (and decoding) and RLP Object Notation: https://github.com/esaulpaugh/headlong/wiki

Licensed under Apache 2.0 terms
