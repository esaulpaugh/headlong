package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.Encoder;
import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.RLPIntegers;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Stack;

import static com.esaulpaugh.headlong.abi.util.ClassNames.toFriendly;

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

    private transient final  Stack<Integer> fixedLengthStack = new Stack<>(); // TODO parse typeString from left to right?
    private transient final String className;

    transient final Integer baseTypeByteLen;
    transient final Integer byteLen;

    transient String abiBaseType;
//    transient final BigInteger baseArithmeticLimit; // signed vs unsigned
    transient final Integer baseTypeBitLimit;

    transient Integer scaleN;

    Type(String typeString) {
        this.typeString = typeString;
        String javaBaseType = buildJavaClassName(typeString.length() - 1);
        StringBuilder classNameBuilder = new StringBuilder();
        int depth = fixedLengthStack.size() - 1;
        for (int i = 0; i < depth; i++) {
            classNameBuilder.append('[');
        }
        this.className = classNameBuilder.append(javaBaseType).toString();

        // fixedLengthStack.empty()
        if (!fixedLengthStack.contains(null)) { // static
            this.baseTypeByteLen = fixedLengthStack.get(fixedLengthStack.size() - 1);

            int rounded = roundUp(baseTypeByteLen);

//            int product;
//            int mod = baseTypeByteLen % 32;
//            if(mod != 0) {
//                product = baseTypeByteLen + (32 - mod); // round up to next 32
//            } else {
//                product = baseTypeByteLen;
//            }

            if(baseTypeByteLen == 1 && !typeString.startsWith("bytes1")) { // typeString.startsWith("int8") || typeString.startsWith("uint8")
                depth--;
            }

            int product = rounded;
            StringBuilder sb = new StringBuilder("(" + baseTypeByteLen + " --> " + product + ")");
            for (int i = depth - 1; i >= 0; i--) {
                product *= fixedLengthStack.get(i);
                sb.append(" * ").append(fixedLengthStack.get(i));
            }
            this.byteLen = product;
            System.out.println(toString() + " : static len: " + sb.toString() + " = " + byteLen);
        } else {
            switch (javaBaseType) {
            case "B":
            case "[B":
            case "java.lang.String":
                this.baseTypeByteLen = 1; break;
            default: this.baseTypeByteLen = null;
            }
            this.byteLen = null;
        }

        if(byteLen == null) {
            System.out.println(typeString + ", " + abiBaseType + ", " + javaBaseType + ", " + className + " : dynamic len");
        }

        int t;
        if((t = abiBaseType.lastIndexOf("int")) != -1) {
            if(t < abiBaseType.length() - 3) {
//                this.baseArithmeticLimit = BigInteger.valueOf(2).pow(y);
                this.baseTypeBitLimit = Integer.parseInt(abiBaseType.substring(t + 3));
            } else { // endsWith("int")
//                this.baseArithmeticLimit = UINT_256_ARITHMETIC_LIMIT;
                this.baseTypeBitLimit = 256;
            }
        } else if(abiBaseType.equals("address")) {
//            this.baseArithmeticLimit = ADDRESS_ARITHMETIC_LIMIT;
            this.baseTypeBitLimit = 160;
        } else if((t = abiBaseType.indexOf("fixed")) >= 0) {
            int x = abiBaseType.lastIndexOf('x');
            Integer m = Integer.parseInt(abiBaseType.substring(t + 5, x));
            Integer n = Integer.parseInt(abiBaseType.substring(x + 1));
            System.out.println(m + "x" + n);
            this.baseTypeBitLimit = m;
            this.scaleN = n;
        } else {
            this.abiBaseType = null;
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

            fixedLengthStack.push(fixedLength);

            return buildJavaClassName(arrayOpenIndex - 1); // , true
        } else {
            this.abiBaseType = typeString.substring(0, i + 1);
            return type(abiBaseType, !fixedLengthStack.isEmpty());
//            classNameBuilder.append();
//            fixedLength = null;
        }
//        if(arrayType) {
//            fixedLengthStack.add(fixedLength);
//        }
//        fixedLengthStack.add(fixedLength);
    }

    private String type(String abiBaseType, boolean element) {
        switch (abiBaseType) {
        case "uint8": fixedLengthStack.push(1); return element ? CLASS_NAME_ELEMENT_BYTE : CLASS_NAME_BYTE;
        case "uint16": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_SHORT : CLASS_NAME_SHORT;
        case "uint24":
        case "uint32": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_INT : CLASS_NAME_INT;
        case "uint40":
        case "uint48":
        case "uint56":
        case "uint64": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_LONG : CLASS_NAME_LONG;
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
        case "uint256": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_BIG_INTEGER : CLASS_NAME_BIG_INTEGER;
        case "int8": fixedLengthStack.push(1); return element ? CLASS_NAME_ELEMENT_BYTE : CLASS_NAME_BYTE; // signed // TODO
        case "int16": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_SHORT : CLASS_NAME_SHORT;
        case "int24":
        case "int32": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_INT : CLASS_NAME_INT;
        case "int40":
        case "int48":
        case "int56":
        case "int64": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_LONG : CLASS_NAME_LONG;
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
        case "address": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_BIG_INTEGER : CLASS_NAME_BIG_INTEGER;
//        case "uint":
//        case "int": return element ? CLASS_NAME_ELEMENT_BIG_INTEGER : CLASS_NAME_BIG_INTEGER;
        case "bool": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_BOOLEAN : CLASS_NAME_BOOLEAN;
//        case "ufixed":
//        case "fixed": return element ? CLASS_NAME_ELEMENT_BIG_DECIMAL : CLASS_NAME_BIG_DECIMAL;
        case "bytes1": fixedLengthStack.push(1); return CLASS_NAME_ARRAY_BYTE;
        case "bytes2": fixedLengthStack.push(2); return CLASS_NAME_ARRAY_BYTE;
        case "bytes3": fixedLengthStack.push(3); return CLASS_NAME_ARRAY_BYTE;
        case "bytes4": fixedLengthStack.push(4); return CLASS_NAME_ARRAY_BYTE;
        case "bytes5": fixedLengthStack.push(5); return CLASS_NAME_ARRAY_BYTE;
        case "bytes6": fixedLengthStack.push(6); return CLASS_NAME_ARRAY_BYTE;
        case "bytes7": fixedLengthStack.push(7); return CLASS_NAME_ARRAY_BYTE;
        case "bytes8": fixedLengthStack.push(8); return CLASS_NAME_ARRAY_BYTE;
        case "bytes9": fixedLengthStack.push(9); return CLASS_NAME_ARRAY_BYTE;
        case "bytes10": fixedLengthStack.push(10); return CLASS_NAME_ARRAY_BYTE;
        case "bytes11": fixedLengthStack.push(11); return CLASS_NAME_ARRAY_BYTE;
        case "bytes12": fixedLengthStack.push(12); return CLASS_NAME_ARRAY_BYTE;
        case "bytes13": fixedLengthStack.push(13); return CLASS_NAME_ARRAY_BYTE;
        case "bytes14": fixedLengthStack.push(14); return CLASS_NAME_ARRAY_BYTE;
        case "bytes15": fixedLengthStack.push(15); return CLASS_NAME_ARRAY_BYTE;
        case "bytes16": fixedLengthStack.push(16); return CLASS_NAME_ARRAY_BYTE;
        case "bytes17": fixedLengthStack.push(17); return CLASS_NAME_ARRAY_BYTE;
        case "bytes18": fixedLengthStack.push(18); return CLASS_NAME_ARRAY_BYTE;
        case "bytes19": fixedLengthStack.push(19); return CLASS_NAME_ARRAY_BYTE;
        case "bytes20": fixedLengthStack.push(20); return CLASS_NAME_ARRAY_BYTE;
        case "bytes21": fixedLengthStack.push(21); return CLASS_NAME_ARRAY_BYTE;
        case "bytes22": fixedLengthStack.push(22); return CLASS_NAME_ARRAY_BYTE;
        case "bytes23": fixedLengthStack.push(23); return CLASS_NAME_ARRAY_BYTE;
        case "function":
        case "bytes24": fixedLengthStack.push(24); return CLASS_NAME_ARRAY_BYTE;
        case "bytes25": fixedLengthStack.push(25); return CLASS_NAME_ARRAY_BYTE;
        case "bytes26": fixedLengthStack.push(26); return CLASS_NAME_ARRAY_BYTE;
        case "bytes27": fixedLengthStack.push(27); return CLASS_NAME_ARRAY_BYTE;
        case "bytes28": fixedLengthStack.push(28); return CLASS_NAME_ARRAY_BYTE;
        case "bytes29": fixedLengthStack.push(29); return CLASS_NAME_ARRAY_BYTE;
        case "bytes30": fixedLengthStack.push(30); return CLASS_NAME_ARRAY_BYTE;
        case "bytes31": fixedLengthStack.push(31); return CLASS_NAME_ARRAY_BYTE;
        case "bytes32": fixedLengthStack.push(32); return CLASS_NAME_ARRAY_BYTE; // CLASS_NAME_ARRAY_BYTE; // CLASS_NAME_ELEMENT_ARRAY_BYTE;
        case "bytes": /* dynamic*/
            fixedLengthStack.push(null);
            return CLASS_NAME_ARRAY_BYTE;
        case "string": /* dynamic*/
            fixedLengthStack.push(null);
            return element ? CLASS_NAME_ELEMENT_STRING : CLASS_NAME_STRING;
        case "fixed": throw new IllegalArgumentException("fixed not supported. use fixed128x18");
        case "ufixed": throw new IllegalArgumentException("ufixed not supported. use ufixed128x18");
        case "int": throw new IllegalArgumentException("int not supported. use int256");
        case "uint": throw new IllegalArgumentException("uint not supported. use uint256");
        default: {
            if(abiBaseType.contains("fixed")) {
                fixedLengthStack.push(32);
                return CLASS_NAME_BIG_DECIMAL;
            }
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
                throw new IllegalArgumentException("class mismatch: "
                        + param.getClass().getName()
                        + " not assignable to "
                        + expectedClassName
                        + " (" + toFriendly(param.getClass().getName()) + " not instanceof " + toFriendly(expectedClassName) + "/" + typeString + ")");
            }
        }
        System.out.print("class valid, ");

        if(param.getClass().isArray()) {
            if (param instanceof Object[]) {
                validateObjectArray((Object[]) param, expectedClassName, expectedLengthIndex);
//            } else {
//                validateArray(param, expectedLengthIndex);
//            }
            } else if (param instanceof byte[]) {
                validateByteArray((byte[]) param, expectedLengthIndex);
            } else if (param instanceof int[]) {
                validateIntArray((int[]) param, expectedLengthIndex);
            } else if (param instanceof long[]) {
                validateLongArray((long[]) param, expectedLengthIndex);
            } else if (param instanceof short[]) {
                validateShortArray((short[]) param, expectedLengthIndex);
            } else if (param instanceof boolean[]) {
                validateBooleanArray((boolean[]) param, expectedLengthIndex);
            }
        } else if(param instanceof Number) {
            validateNumber((Number) param);
//            Number numberParam = (Number) param;
//            final int bitLen;
//            if(param instanceof BigInteger) {
//                BigInteger bigIntParam = (BigInteger) param;
//                bitLen = bigIntParam.bitLength();
//            } else {
////                bigIntParam = BigInteger.valueOf(numberParam.longValue());
//                final long longVal = numberParam.longValue();
//                bitLen = longVal >= 0 ? RLPIntegers.bitLen(longVal) : BizarroIntegers.bitLen(longVal);
//                if(longVal > 0) {
//                    Assert.assertEquals(Long.toBinaryString(longVal).length(), bitLen);
//                } else if(longVal == 0) {
//                    Assert.assertEquals(0, bitLen);
//                } else if(longVal == -1) {
//                    Assert.assertEquals(0, bitLen);
//                } else { // < -1
//                    String bin = Long.toBinaryString(longVal);
//                    String minBin = bin.substring(bin.indexOf('0'));
//                    Assert.assertEquals(bitLen, minBin.length());
//                }
//                Assert.assertEquals(BigInteger.valueOf(longVal).bitLength(), bitLen);
//            }
//
//            if(bitLen > baseTypeBitLimit) {
//                throw new IllegalArgumentException("exceeds bit limit: " + bitLen + " > " + baseTypeBitLimit);
//            }
//            System.out.println("length valid, ");
//
////            bigIntParam.bitLength()
////            if (bigIntParam.compareTo(baseArithmeticLimit) >= 0) {
////                throw new IllegalArgumentException("exceeds arithmetic limit: " + bigIntParam + " > " + baseArithmeticLimit);
////            }
//            return;
        }


//            Object[] objectArray = (Object[]) param;
//            final int len = objectArray.length;
//            checkLength(len, fixedLengthStack.get(expectedLengthIndex));
//            int i = 0;
//            try {
//                for ( ; i < len; i++) {
//                    String nextExpectedClassName;
//                    if(expectedClassName.charAt(1) == 'L') {
//                        nextExpectedClassName = expectedClassName.substring(2, expectedClassName.length() - 1);
//                    } else {
//                        nextExpectedClassName = expectedClassName.substring(1);
//                    }
//                    validate(objectArray[i], nextExpectedClassName, expectedLengthIndex + 1);
//                }
//            } catch (IllegalArgumentException | NullPointerException re) {
//                throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
//            }
    }

    private void validateObjectArray(Object[] arr, String expectedClassName, int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));
        int i = 0;
        try {
            for ( ; i < len; i++) {
                String nextExpectedClassName;
                if(expectedClassName.charAt(1) == 'L') {
                    nextExpectedClassName = expectedClassName.substring(2, expectedClassName.length() - 1);
                } else {
                    nextExpectedClassName = expectedClassName.substring(1);
                }
                validate(arr[i], nextExpectedClassName, expectedLengthIndex + 1);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private void validateBooleanArray(boolean[] arr, int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));
        final int nextExpectedLengthIndex = expectedLengthIndex + 1;
        int i = 0;
        try {
            for ( ; i < len; i++) {
                validate(arr[i], CLASS_NAME_BOOLEAN, nextExpectedLengthIndex);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private void validateByteArray(byte[] arr, int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));
//        int i = 0;
//        try {
//            for ( ; i < len; i++) {
//                validate(arr[i], CLASS_NAME_ELEMENT_BYTE, expectedLengthIndex + 1);
//            }
//        } catch (IllegalArgumentException | NullPointerException re) {
//            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
//        }
    }

    private void validateShortArray(short[] arr, int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));
        int i = 0;
        try {
            for ( ; i < len; i++) {
                validate(arr[i], CLASS_NAME_SHORT, expectedLengthIndex + 1);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private void validateIntArray(int[] arr, int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));
        int i = 0;
        try {
            for ( ; i < len; i++) {
                validate(arr[i], CLASS_NAME_INT, expectedLengthIndex + 1);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private void validateLongArray(long[] arr, int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));
        int i = 0;
        try {
            for ( ; i < len; i++) {
                validate(arr[i], CLASS_NAME_LONG, expectedLengthIndex + 1);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }

    private static void checkLength(int actual, Integer expected) {
        if(expected != null && actual != expected) {
            throw new IllegalArgumentException("array length mismatch: " + actual + " != " + expected);
        }
        System.out.println("length valid;");
    }

    private void validateNumber(Number number) {
        final int bitLen;
        if(number instanceof BigInteger) {
            BigInteger bigIntParam = (BigInteger) number;
            bitLen = bigIntParam.bitLength();
        } else if(number instanceof BigDecimal) {
            BigDecimal bigIntParam = (BigDecimal) number;
            bitLen = bigIntParam.unscaledValue().bitLength();
        } else {
            final long longVal = number.longValue();
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
        System.out.println("length valid;");
    }

    public void encode(Object value, ByteBuffer dest) {
        if(value instanceof String) {
            Encoder.insertBytes(((String) value).getBytes(StandardCharsets.UTF_8), dest);
        } else if(value.getClass().isArray()) {
            if (value instanceof Object[]) {
                Object[] arr = (Object[]) value;
                for (Object obj : arr) {
                    encode(obj, dest);
                }
            } else if (value instanceof byte[]) {
                System.out.println("byte[] " + dest.position());
                Encoder.insertBytes((byte[]) value, dest);
            } else if (value instanceof int[]) {
                Encoder.insertInts((int[]) value, dest);
            } else if (value instanceof long[]) {
                Encoder.insertLongs((long[]) value, dest);
            } else if (value instanceof short[]) {
                Encoder.insertShorts((short[]) value, dest);
            } else if(value instanceof boolean[]) {
                Encoder.insertBooleans((boolean[]) value, dest);
            }
        } else if (value instanceof Number) {
            if(value instanceof BigInteger) {
                Encoder.insertInt(((BigInteger) value), dest);
            } else if(value instanceof BigDecimal) {
                Encoder.insertInt(((BigDecimal) value).unscaledValue(), dest);
            } else {
                Encoder.insertInt(((Number) value).longValue(), dest);
            }
        } else if(value instanceof Boolean) {
            Encoder.insertBool((boolean) value, dest);
        }
    }

    public int calcDynamicByteLen(Object param) {

        Stack<Integer> dynamicLengthStack = new Stack<>();
        buildLengthStack(param, dynamicLengthStack);

        int product = baseTypeByteLen;
        System.out.print(product);
        final int lim = fixedLengthStack.size();
        for (int i = 0; i < lim; i++) {
            int len;
            Integer fixedLen = fixedLengthStack.get(i);
            if(fixedLen != null) {
                len = fixedLen;
            } else {
                len = dynamicLengthStack.get(i);
            }
            System.out.print(" * " + len);
            product *= len;
        }
        System.out.println();
        return product;
    }

    private void buildLengthStack(Object value, Stack<Integer> dynamicLengthStack) {
        if(value instanceof String) {
            dynamicLengthStack.push(roundUp(((String) value).length()));
        } else if(value.getClass().isArray()) {
            if (value instanceof Object[]) {
                Object[] arr = (Object[]) value;
                for (Object obj : arr) {
                    buildLengthStack(obj, dynamicLengthStack);
                }
            } else if (value instanceof byte[]) {
                dynamicLengthStack.push(roundUp(((byte[]) value).length));
            } else if (value instanceof int[]) {
                dynamicLengthStack.push(((int[]) value).length << 5); // mul 32
            } else if (value instanceof long[]) {
                dynamicLengthStack.push(((long[]) value).length << 5);
            } else if (value instanceof short[]) {
                dynamicLengthStack.push(((short[]) value).length << 5);
            } else if(value instanceof boolean[]) {
                dynamicLengthStack.push(((boolean[]) value).length << 5);
            }
        } else if (value instanceof Number) {
            dynamicLengthStack.push(32);
        } else if(value instanceof Boolean) {
            dynamicLengthStack.push(32);
        }
    }

    private static int roundUp(int len) {
        int mod = len % 32;
        return mod == 0 ? len : len + (32 -mod);
//        return newLen == 0 ? 32 : newLen;
    }

    @Override
    public String toString() {
        return typeString + ", " + className + ", " + Arrays.toString(fixedLengthStack.toArray());
    }

    private void validateArray(Object arr, int expectedLengthIndex) {
        Object[] elements;
        String newClassName;
        final int len;
        if(arr instanceof byte[]) {
            byte[] casted = (byte[]) arr;
            len = casted.length;
            elements = new Object[len];
            for (int i = 0; i < len; i++) {
                elements[i] = casted[i];
            }
            newClassName = CLASS_NAME_BYTE;
        } else if(arr instanceof short[]) {
            short[] casted = (short[]) arr;
            len = casted.length;
            elements = new Object[len];
            for (int i = 0; i < len; i++) {
                elements[i] = casted[i];
            }
            newClassName = CLASS_NAME_SHORT;
        } else if(arr instanceof int[]) {
            int[] casted = (int[]) arr;
            len = casted.length;
            elements = new Object[len];
            for (int i = 0; i < len; i++) {
                elements[i] = casted[i];
            }
            newClassName = CLASS_NAME_INT;
        } else if(arr instanceof long[]) {
            long[] casted = (long[]) arr;
            len = casted.length;
            elements = new Object[len];
            for (int i = 0; i < len; i++) {
                elements[i] = casted[i];
            }
            newClassName = CLASS_NAME_LONG;
        } else if(arr instanceof boolean[]) {
            boolean[] casted = (boolean[]) arr;
            len = casted.length;
            elements = new Object[len];
            for (int i = 0; i < len; i++) {
                elements[i] = casted[i];
            }
            newClassName = CLASS_NAME_BOOLEAN;
        } else {
            throw new RuntimeException(new ClassNotFoundException(arr.getClass().getName()));
        }
        Integer fixedArrayLength = fixedLengthStack.get(expectedLengthIndex);
        if(fixedArrayLength != null) {
            checkLength(len, fixedArrayLength);
        }
        int i = 0;
        try {
            for ( ; i < len; i++) {
                validate(elements[i], newClassName, expectedLengthIndex + 1);
            }
        } catch (IllegalArgumentException | NullPointerException re) {
            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
        }
    }
}


//if(!fixedLengthStack.contains(null)) { // static
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
////            case "[B": this.baseTypeByteLen = fixedLengthStack.get(fixedLengthStack.size() - 1); break;
////            default: this.baseTypeByteLen = null;
////            }
//
//// int roundedUp = t.byteLen + (32 - (t.byteLen % 32));
//        this.baseTypeByteLen = fixedLengthStack.get(fixedLengthStack.size() - 1);
//
//        if (baseTypeByteLen != null) {
//        if (fixedLengthStack.empty()) {
//        this.byteLen = baseTypeByteLen;
//        } else {
//        int product = baseTypeByteLen;
//final int size = fixedLengthStack.size();
//        for (int i = 1; i < size; i++) {
//        product *= fixedLengthStack.get(i);
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