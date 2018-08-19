package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.Encoder;
import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.RLPIntegers;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Stack;

public class Type {

    private static final String CLASS_NAME_BOOLEAN = Boolean.class.getName();
    private static final String CLASS_NAME_BYTE = Byte.class.getName();
    private static final String CLASS_NAME_SHORT = Short.class.getName();
    private static final String CLASS_NAME_INT = Integer.class.getName();
    private static final String CLASS_NAME_LONG = Long.class.getName();

    private static final String CLASS_NAME_BIG_INTEGER = BigInteger.class.getName();
    private static final String CLASS_NAME_BIG_DECIMAL = BigDecimal.class.getName();
    private static final String CLASS_NAME_STRING = String.class.getName();

    private static final String CLASS_NAME_ELEMENT_BOOLEAN = boolean[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_BYTE = byte[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_SHORT = short[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_INT = int[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_LONG = long[].class.getName().replaceFirst("\\[", "");

    private static final String CLASS_NAME_ELEMENT_BIG_INTEGER = BigInteger[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_BIG_DECIMAL = BigDecimal[].class.getName().replaceFirst("\\[", "");
    private static final String CLASS_NAME_ELEMENT_STRING = String[].class.getName().replaceFirst("\\[", "");

//    private static final String CLASS_NAME_ARRAY_BOOLEAN = boolean[].class.getName();
    private static final String CLASS_NAME_ARRAY_BYTE = byte[].class.getName();

    private static final String CLASS_NAME_ELEMENT_ARRAY_BYTE = byte[][].class.getName().replaceFirst("\\[", "");

    private static final BigInteger ADDRESS_ARITHMETIC_LIMIT = BigInteger.valueOf(2).pow(160);
    private static final BigInteger UINT_256_ARITHMETIC_LIMIT = BigInteger.valueOf(2).pow(256);

    private final String typeString;

    private transient final  Stack<Integer> fixedArrayLengthStack = new Stack<>(); // TODO parse typeString from left to right?
    private transient final String className;

    transient final Integer baseTypeByteLen;
    transient final Integer byteLen;

    transient String abiBaseType;
//    transient final BigInteger baseArithmeticLimit; // signed vs unsigned
    transient final Integer baseTypeBitLimit;

    Type(String typeString) {
        this.typeString = typeString;
        String javaBaseType = buildJavaClassName(typeString.length() - 1);
        StringBuilder classNameBuilder = new StringBuilder();
        final int depth = fixedArrayLengthStack.size() - 1;
        for (int i = 0; i < depth; i++) {
            classNameBuilder.append('[');
        }
        this.className = classNameBuilder.append(javaBaseType).toString();

        // fixedArrayLengthStack.empty()
        if (!fixedArrayLengthStack.contains(null)) { // static
            this.baseTypeByteLen = fixedArrayLengthStack.get(fixedArrayLengthStack.size() - 1);
            int product = baseTypeByteLen + (32 - (baseTypeByteLen % 32)); // round up to next 32
//            final int lim = fixedArrayLengthStack.size() - 1;
            for (int i = 0; i < depth; i++) {
                product *= fixedArrayLengthStack.get(i);
            }
            this.byteLen = product;
            System.out.println(toString() + " : static len = " + byteLen + " (base len " + baseTypeByteLen + ")");
        } else {
            switch (javaBaseType) {
            case "B":
            case "java.lang.String":
                this.baseTypeByteLen = 8; break;
                default: this.baseTypeByteLen = null;
            }
            this.byteLen = null;
        }

        if(byteLen == null) {
            System.out.println(typeString + ", " + abiBaseType + ", " + javaBaseType + ", " + className + " : dynamic len");
        }

        int x = abiBaseType.lastIndexOf("int");
        if(x != -1) {
            if(x < abiBaseType.length() - 3) {
//                this.baseArithmeticLimit = BigInteger.valueOf(2).pow(y);
                this.baseTypeBitLimit = Integer.parseInt(abiBaseType.substring(x + 3));
            } else { // endsWith("int")
//                this.baseArithmeticLimit = UINT_256_ARITHMETIC_LIMIT;
                this.baseTypeBitLimit = 256;
            }
        } else if(abiBaseType.equals("address")) {
//            this.baseArithmeticLimit = ADDRESS_ARITHMETIC_LIMIT;
            this.baseTypeBitLimit = 160;
        } else {
//            this.baseArithmeticLimit = null;
            this.baseTypeBitLimit = null;
        }

    }

    // uint[][3][]
    // String typeString, StringBuilder sb, ArrayList[] fixedLengthsHolder
    private String buildJavaClassName(final int i) { //
//        final int len = typeString.length();
//        final int lastIndex = len - 1;

        Integer fixedLength;

        if(typeString.charAt(i) == ']') {
            final int arrayOpenIndex = typeString.lastIndexOf('[', i - 1);
//            System.out.println("i = " + i);
            if(i - arrayOpenIndex > 1) {
                fixedLength = Integer.parseInt(typeString.substring(arrayOpenIndex + 1, i));
            } else {
                fixedLength = null;
            }
//            if(arrayType) {
//                classNameBuilder.append('[');
//            }

            fixedArrayLengthStack.push(fixedLength);

            return buildJavaClassName(arrayOpenIndex - 1); // , true
        } else {
            this.abiBaseType = typeString.substring(0, i + 1);
            return type(abiBaseType, !fixedArrayLengthStack.isEmpty());
//            classNameBuilder.append();
//            fixedLength = null;
        }
//        if(arrayType) {
//            fixedArrayLengthStack.add(fixedLength);
//        }
//        fixedArrayLengthStack.add(fixedLength);
    }

    private String type(String abiBaseType, boolean element) {
        switch (abiBaseType) {
        case "uint8": fixedArrayLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_BYTE : CLASS_NAME_BYTE;
        case "uint16": fixedArrayLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_SHORT : CLASS_NAME_SHORT;
        case "uint24":
        case "uint32": fixedArrayLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_INT : CLASS_NAME_INT;
        case "uint40":
        case "uint48":
        case "uint56":
        case "uint64": fixedArrayLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_LONG : CLASS_NAME_LONG;
        case "uint72":
        case "uint80":
        case "uint88":
        case "uint96":
        case "uint104":
        case "uint112":
        case "uint120":
        case "uint128":
        case "uint136":
        case "uint144":
        case "uint152":
        case "uint160":
        case "uint168":
        case "uint176":
        case "uint184":
        case "uint192":
        case "uint200":
        case "uint208":
        case "uint216":
        case "uint224":
        case "uint232":
        case "uint240":
        case "uint248":
        case "uint256": fixedArrayLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_BIG_INTEGER : CLASS_NAME_BIG_INTEGER;
        case "int8": fixedArrayLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_BYTE : CLASS_NAME_BYTE; // signed // TODO
        case "int16": fixedArrayLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_SHORT : CLASS_NAME_SHORT;
        case "int24":
        case "int32": fixedArrayLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_INT : CLASS_NAME_INT;
        case "int40":
        case "int48":
        case "int56":
        case "int64": fixedArrayLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_LONG : CLASS_NAME_LONG;
        case "int72":
        case "int80":
        case "int88":
        case "int96":
        case "int104":
        case "int112":
        case "int120":
        case "int128":
        case "int136":
        case "int144":
        case "int152":
        case "int160":
        case "int168":
        case "int176":
        case "int184":
        case "int192":
        case "int200":
        case "int208":
        case "int216":
        case "int224":
        case "int232":
        case "int240":
        case "int248":
        case "int256":
        case "address": fixedArrayLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_BIG_INTEGER : CLASS_NAME_BIG_INTEGER;
//        case "uint":
//        case "int": return element ? CLASS_NAME_ELEMENT_BIG_INTEGER : CLASS_NAME_BIG_INTEGER;
        case "bool": fixedArrayLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_BOOLEAN : CLASS_NAME_BOOLEAN;
//        case "ufixed":
//        case "fixed": return element ? CLASS_NAME_ELEMENT_BIG_DECIMAL : CLASS_NAME_BIG_DECIMAL;
        case "bytes1": fixedArrayLengthStack.push(1); return CLASS_NAME_ARRAY_BYTE;
        case "bytes2": fixedArrayLengthStack.push(2); return CLASS_NAME_ARRAY_BYTE;
        case "bytes3": fixedArrayLengthStack.push(3); return CLASS_NAME_ARRAY_BYTE;
        case "bytes4": fixedArrayLengthStack.push(4); return CLASS_NAME_ARRAY_BYTE;
        case "bytes5": fixedArrayLengthStack.push(5); return CLASS_NAME_ARRAY_BYTE;
        case "bytes6": fixedArrayLengthStack.push(6); return CLASS_NAME_ARRAY_BYTE;
        case "bytes7": fixedArrayLengthStack.push(7); return CLASS_NAME_ARRAY_BYTE;
        case "bytes8": fixedArrayLengthStack.push(8); return CLASS_NAME_ARRAY_BYTE;
        case "bytes9": fixedArrayLengthStack.push(9); return CLASS_NAME_ARRAY_BYTE;
        case "bytes10": fixedArrayLengthStack.push(10); return CLASS_NAME_ARRAY_BYTE;
        case "bytes11": fixedArrayLengthStack.push(11); return CLASS_NAME_ARRAY_BYTE;
        case "bytes12": fixedArrayLengthStack.push(12); return CLASS_NAME_ARRAY_BYTE;
        case "bytes13": fixedArrayLengthStack.push(13); return CLASS_NAME_ARRAY_BYTE;
        case "bytes14": fixedArrayLengthStack.push(14); return CLASS_NAME_ARRAY_BYTE;
        case "bytes15": fixedArrayLengthStack.push(15); return CLASS_NAME_ARRAY_BYTE;
        case "bytes16": fixedArrayLengthStack.push(16); return CLASS_NAME_ARRAY_BYTE;
        case "bytes17": fixedArrayLengthStack.push(17); return CLASS_NAME_ARRAY_BYTE;
        case "bytes18": fixedArrayLengthStack.push(18); return CLASS_NAME_ARRAY_BYTE;
        case "bytes19": fixedArrayLengthStack.push(19); return CLASS_NAME_ARRAY_BYTE;
        case "bytes20": fixedArrayLengthStack.push(20); return CLASS_NAME_ARRAY_BYTE;
        case "bytes21": fixedArrayLengthStack.push(21); return CLASS_NAME_ARRAY_BYTE;
        case "bytes22": fixedArrayLengthStack.push(22); return CLASS_NAME_ARRAY_BYTE;
        case "bytes23": fixedArrayLengthStack.push(23); return CLASS_NAME_ARRAY_BYTE;
        case "function":
        case "bytes24": fixedArrayLengthStack.push(24); return CLASS_NAME_ARRAY_BYTE;
        case "bytes25": fixedArrayLengthStack.push(25); return CLASS_NAME_ARRAY_BYTE;
        case "bytes26": fixedArrayLengthStack.push(26); return CLASS_NAME_ARRAY_BYTE;
        case "bytes27": fixedArrayLengthStack.push(27); return CLASS_NAME_ARRAY_BYTE;
        case "bytes28": fixedArrayLengthStack.push(28); return CLASS_NAME_ARRAY_BYTE;
        case "bytes29": fixedArrayLengthStack.push(29); return CLASS_NAME_ARRAY_BYTE;
        case "bytes30": fixedArrayLengthStack.push(30); return CLASS_NAME_ARRAY_BYTE;
        case "bytes31": fixedArrayLengthStack.push(31); return CLASS_NAME_ARRAY_BYTE;
        case "bytes32": fixedArrayLengthStack.push(32); return CLASS_NAME_ARRAY_BYTE; // CLASS_NAME_ARRAY_BYTE; // CLASS_NAME_ELEMENT_ARRAY_BYTE;
        case "bytes": /* dynamic*/
            fixedArrayLengthStack.push(null);
            return CLASS_NAME_ARRAY_BYTE;
        case "string": /* dynamic*/
            fixedArrayLengthStack.push(null);
            return element ? CLASS_NAME_ELEMENT_STRING : CLASS_NAME_STRING;
        default: {
            throw new IllegalArgumentException("abi base type " + abiBaseType + " not yet supported");
            // ufixed<M>x<N>
            // fixed<M>x<N>
//            BigDecimal
            // (T1,T2,...,Tn)
        }
        }
//        return null;
    }

    public void validate(Object param) {
        validate(param, className, 0);
    }

    private void validate(final Object param, final String expectedClassName, final int expectedLengthIndex) {
//        if(param == null) {
//            throw new NullPointerException("object is null");
//        }
        if(!className.equals(param.getClass().getName())) {
            boolean isAssignable;
            try {
                isAssignable = Class.forName(expectedClassName).isAssignableFrom(param.getClass());
            } catch (ClassNotFoundException cnfe) {
                isAssignable = false;
            }
            if(!isAssignable) {
                throw new IllegalArgumentException("class mismatch: " + param.getClass().getName() + " not assignable to " + expectedClassName + " (" + typeString + ")");
            }
        }
        System.out.print("class valid, ");
        final Integer fixedArrayLength;
        if(param instanceof Number) {
            Number numberParam = (Number) param;
            final int bitLen;
            if(param instanceof BigInteger) {
                BigInteger bigIntParam = (BigInteger) param;
                bitLen = bigIntParam.bitLength();
            } else {
//                bigIntParam = BigInteger.valueOf(numberParam.longValue());
                final long longVal = numberParam.longValue();
                bitLen = longVal >= 0 ? RLPIntegers.bitLen(longVal) : BizarroIntegers.bitLen(longVal);
                if(longVal > 0) {
                    Assert.assertEquals(Long.toBinaryString(longVal).length(), bitLen);
                } else if(longVal == 0) {
                    Assert.assertEquals(0, bitLen);
                } else if(longVal == -1) {
                    Assert.assertEquals(0, bitLen);
                } else { // < -1
                    String bin = Long.toBinaryString(longVal);
                    String minBin = bin.substring(bin.indexOf('0'));
                    Assert.assertEquals(bitLen, minBin.length());
                }
                Assert.assertEquals(BigInteger.valueOf(longVal).bitLength(), bitLen);
            }

            if(bitLen > baseTypeBitLimit) {
                throw new IllegalArgumentException("exceeds bit limit: " + bitLen + " > " + baseTypeBitLimit);
            }

//            bigIntParam.bitLength()
//            if (bigIntParam.compareTo(baseArithmeticLimit) >= 0) {
//                throw new IllegalArgumentException("exceeds arithmetic limit: " + bigIntParam + " > " + baseArithmeticLimit);
//            }
            return;
        }
        if(param instanceof byte[]) {
            fixedArrayLength = fixedArrayLengthStack.get(expectedLengthIndex);
            if(fixedArrayLength != null) {
                checkLength(((byte[]) param).length, fixedArrayLength);
            }
            return;
        }
        if(param instanceof short[]) {
            fixedArrayLength = fixedArrayLengthStack.get(expectedLengthIndex);
            if(fixedArrayLength != null) {
                checkLength(((short[]) param).length, fixedArrayLength);
            }
            return;
        }
        if(param instanceof int[]) {
            fixedArrayLength = fixedArrayLengthStack.get(expectedLengthIndex);
            if(fixedArrayLength != null) {
                checkLength(((int[]) param).length, fixedArrayLength);
            }
            return;
        }
        if(param instanceof long[]) {
            fixedArrayLength = fixedArrayLengthStack.get(expectedLengthIndex);
            if(fixedArrayLength != null) {
                checkLength(((long[]) param).length, fixedArrayLength);
            }
            return;
        }
        if(param instanceof Object[]) {
            fixedArrayLength = fixedArrayLengthStack.get(expectedLengthIndex);
            Object[] objectArray = (Object[]) param;
            final int len = objectArray.length;
            if(fixedArrayLength != null) {
                checkLength(len, fixedArrayLength);
            }
            final int nextExpectedLengthIndex = expectedLengthIndex + 1;
            int i = 0;
            try {
                for ( ; i < len; i++) {
                    String nextExpectedClassName;
                    if(expectedClassName.charAt(1) == 'L') {
                        nextExpectedClassName = expectedClassName.substring(2, expectedClassName.length() - 1);
                    } else {
                        nextExpectedClassName = expectedClassName.substring(1);
                    }
                    validate(objectArray[i], nextExpectedClassName, nextExpectedLengthIndex);
                }
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new IllegalArgumentException("index " + i + ": " + e.getMessage(), e);
            }
        }
    }

    private static void checkLength(int actual, int expected) {
        if(actual != expected) {
            throw new IllegalArgumentException("array length mismatch: " + actual + " != " + expected);
        }
        System.out.println("length valid ");
    }

    public void encode(Object value, ByteBuffer dest) {
        if (value instanceof Number) {
            Encoder.insertInt(((Number) value).longValue(), dest);
        } else if (value instanceof byte[]) {
            Encoder.insertBytes((byte[]) value, dest);
        } else if (value instanceof int[]) {

        } else if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            for (int j = 0; j < arr.length; j++) {
                encode(arr[j], dest);
            }
        }
    }

    public int calcDynamicByteLen(Object param) {
//        int len =
        for (Integer e : fixedArrayLengthStack) {
//            product *= e;
        }

        return 0;
    }

    @Override
    public String toString() {
        return typeString + ", " + className + ", " + Arrays.toString(fixedArrayLengthStack.toArray());
    }
}


//if(!fixedArrayLengthStack.contains(null)) { // static
////            switch (javaBaseType) {
////            case "B":
////            case "S":
////            case "I":
////            case "J": // J for jumbo?
////            case "Ljava.math.BigInteger;":
////            case "java.lang.Byte":
////            case "java.lang.Short":
////            case "java.lang.Integer":
////            case "java.lang.Long":
////            case "java.math.BigInteger": this.baseTypeByteLen = 32; break;
////            case "[B": this.baseTypeByteLen = fixedArrayLengthStack.get(fixedArrayLengthStack.size() - 1); break;
////            default: this.baseTypeByteLen = null;
////            }
//
//// int roundedUp = t.byteLen + (32 - (t.byteLen % 32));
//        this.baseTypeByteLen = fixedArrayLengthStack.get(fixedArrayLengthStack.size() - 1);
//
//        if (baseTypeByteLen != null) {
//        if (fixedArrayLengthStack.empty()) {
//        this.byteLen = baseTypeByteLen;
//        } else {
//        int product = baseTypeByteLen;
//final int size = fixedArrayLengthStack.size();
//        for (int i = 1; i < size; i++) {
//        product *= fixedArrayLengthStack.get(i);
//        }
//        this.byteLen = product;
//        }
//        System.out.println(toString() + " : static len = " + byteLen + " (base len " + baseTypeByteLen + ")");
//        } else {
//        this.byteLen = null;
//        }
//        } else {
//        switch (javaBaseType) {
//        case "B":
//        case "java.lang.String":
//        this.baseTypeByteLen = 8; break;
//default: this.baseTypeByteLen = null;
//        }
//        this.byteLen = null;
//        }