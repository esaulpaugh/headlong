package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.Encoder;
import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.RLPIntegers;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.spongycastle.util.encoders.Hex;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

import static com.esaulpaugh.headlong.abi.util.ClassNames.toFriendly;

class Type {

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

//    private static final BigInteger ADDRESS_ARITHMETIC_LIMIT = BigInteger.valueOf(2).pow(160);
//    private static final BigInteger UINT_256_ARITHMETIC_LIMIT = BigInteger.valueOf(2).pow(256);

    private final String canonicalAbiType;

    private transient final String javaClassName;

    private transient final  Stack<Integer> fixedLengthStack = new Stack<>();
    private transient final int arrayDepth;

    private transient final Integer baseTypeByteLen;
    private transient final Integer byteLen;

    private transient final String abiBaseType;
    private transient final Integer baseTypeBitLimit;

    private transient final Integer scaleN; // TODO more subclasses of Type

    protected Type(String canonicalAbiType) {
        this.canonicalAbiType = canonicalAbiType;
        Pair<String, String> baseTypeNames = buildBaseTypeNames(canonicalAbiType.length() - 1);

        this.abiBaseType = baseTypeNames.getLeft();
        String javaBaseType = baseTypeNames.getRight();
        StringBuilder classNameBuilder = new StringBuilder();
        int depth = fixedLengthStack.size() - 1;
        for (int i = 0; i < depth; i++) {
            classNameBuilder.append('[');
        }
        this.javaClassName = classNameBuilder.append(baseTypeNames.getRight()).toString();

        if (!fixedLengthStack.contains(null)) { // static
            this.baseTypeByteLen = fixedLengthStack.get(fixedLengthStack.size() - 1);

            int rounded = roundUp(baseTypeByteLen);

            if(baseTypeByteLen == 1 && !canonicalAbiType.startsWith("bytes1")) { // typeString.startsWith("int8") || typeString.startsWith("uint8")
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

        this.arrayDepth = depth;

        if(byteLen == null) {
            System.out.println(canonicalAbiType + ", " + abiBaseType + ", " + javaBaseType + ", " + javaClassName + " : dynamic len");
        }

        Integer baseBitLimit = null;
        Integer scale = null;
        int t;
        if(!abiBaseType.startsWith("(")) {
            if ((t = abiBaseType.lastIndexOf("int")) != -1) {
                if (t < abiBaseType.length() - 3) {
                    baseBitLimit = Integer.parseInt(abiBaseType.substring(t + 3));
                } else { // endsWith("int")
                    baseBitLimit = 256;
                }
            } else if (abiBaseType.equals("address")) {
                baseBitLimit = 160;
            } else if ((t = abiBaseType.indexOf("fixed")) >= 0) {
                int x = abiBaseType.indexOf('x', t + 5);
                Integer m = Integer.parseInt(abiBaseType.substring(t + 5, x));
                Integer n = Integer.parseInt(abiBaseType.substring(x + 1)); // error due to tuple not parsed
                System.out.println(m + "x" + n);
                baseBitLimit = m;
                scale = n;
            }
        }
        this.baseTypeBitLimit = baseBitLimit;
        this.scaleN = scale;
    }

    public Integer getByteLen() {
        return byteLen;
    }

    private Pair<String, String> buildBaseTypeNames(final int i) {
        Integer fixedLength;

        if(canonicalAbiType.charAt(i) == ']') {
            final int arrayOpenIndex = canonicalAbiType.lastIndexOf('[', i - 1);
            if(i - arrayOpenIndex > 1) {
                fixedLength = Integer.parseInt(canonicalAbiType.substring(arrayOpenIndex + 1, i));
            } else {
                fixedLength = null;
            }

            fixedLengthStack.push(fixedLength);

            return buildBaseTypeNames(arrayOpenIndex - 1); // , true
        } else {
            String abiBaseType = canonicalAbiType.substring(0, i + 1);
            String javaBaseType = getJavaBaseTypeName(abiBaseType, !fixedLengthStack.isEmpty());
            return new ImmutablePair<>(abiBaseType, javaBaseType);
        }
    }

    private String getJavaBaseTypeName(String abiBaseType, boolean element) {

        if(abiBaseType.charAt(0) == '(') {
            fixedLengthStack.push(null);
            return Tuple.class.getName();
        }

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
        case "bool": fixedLengthStack.push(32); return element ? CLASS_NAME_ELEMENT_BOOLEAN : CLASS_NAME_BOOLEAN;
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
                return element ? CLASS_NAME_ELEMENT_BIG_DECIMAL : CLASS_NAME_BIG_DECIMAL;
            }
            throw new IllegalArgumentException("unrecognized type: " + abiBaseType + " (" + Hex.toHexString(abiBaseType.getBytes()) + ")");
        }
        }
    }

    public void validate(Object param) {
        validate(param, javaClassName, 0);
    }

    private void validate(final Object param, final String expectedClassName, final int expectedLengthIndex) {
//        if(param == null) {
//            throw new NullPointerException("object is null");
//        }
        if(!javaClassName.equals(param.getClass().getName())) {
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
                        + " (" + toFriendly(param.getClass().getName()) + " not instanceof " + toFriendly(expectedClassName) + "/" + canonicalAbiType + ")");
            }
        }
        System.out.print("class valid, ");

        if(param.getClass().isArray()) {
            if(param instanceof Object[]) {
                validateArray((Object[]) param, expectedClassName, expectedLengthIndex);
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
        } else if (param instanceof Tuple) {
            validateTuple((Tuple) param);
        } else {
            throw new IllegalArgumentException("unrecognized type: " + param.getClass().getName());
        }
    }

    private void validateTuple(Tuple tuple) {
        Type[] types = ((TupleType) this).getTypes();
        final int typesLen = types.length;
        if(typesLen != tuple.elements.length) {
            throw new IllegalArgumentException("tuple length mismatch: expected: " + typesLen + ", actual: " + tuple.elements.length);
        }
        System.out.println("length valid;");
        for (int i = 0; i < typesLen; i++) {
            validate(tuple.elements[i], types[i].javaClassName, 0);
        }
    }

    private void validateArray(final Object[] arr, final String expectedClassName, final int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));

        final String nextExpectedClassName;
        if(expectedClassName.charAt(1) == 'L') {
            nextExpectedClassName = expectedClassName.substring(2, expectedClassName.length() - 1);
        } else {
            nextExpectedClassName = expectedClassName.substring(1);
        }

        int i = 0;
        try {
            for ( ; i < len; i++) {
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
    }

    private void validateShortArray(short[] arr, int expectedLengthIndex) {
        final int len = arr.length;
        checkLength(len, fixedLengthStack.get(expectedLengthIndex));
//        int i = 0;
//        try {
//            for ( ; i < len; i++) {
//                validate(arr[i], CLASS_NAME_SHORT, expectedLengthIndex + 1);
//            }
//        } catch (IllegalArgumentException | NullPointerException re) {
//            throw new IllegalArgumentException("index " + i + ": " + re.getMessage(), re);
//        }
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
        if(expected == null) {
            System.out.println("dynamic length");
            return;
        }
        if(actual != expected) {
            throw new IllegalArgumentException("array length mismatch: " + actual + " != " + expected);
        }
        System.out.println("fixed length valid;");
    }

    private void validateNumber(Number number) {
        final int bitLen;
        if(number instanceof BigInteger) {
            BigInteger bigIntParam = (BigInteger) number;
            bitLen = bigIntParam.bitLength();
        } else if(number instanceof BigDecimal) {
            BigDecimal bigIntParam = (BigDecimal) number;
            if(bigIntParam.scale() != 0) {
                throw new IllegalArgumentException("scale must be 0");
            }
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
        } else if(value instanceof Tuple) {
            Encoder.insertTuple((Tuple) value);
        }
    }

    public int calcDynamicByteLen(Object param) {


        Stack<Integer> dynamicLengthStack = new Stack<>();
        buildLengthStack(param, dynamicLengthStack);

        int dynamicLen = 1;
        for (int i = arrayDepth - 1; i >= 0; i--) {
            int len;
            Integer fixedLen = fixedLengthStack.get(i);
            if(fixedLen != null) {
                len = fixedLen;
            } else {
                len = dynamicLengthStack.get(i);
            }
            dynamicLen *= len;
        }

        return roundUp(32 + 32 + dynamicLen);
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
        } else if(value instanceof Tuple) {
            throw new AssertionError("override expected");
//            dynamicLengthStack.push(((Tuple) value).byteLen);
        }
        // shouldn't happen if type checks/validation already occurred
        throw new IllegalArgumentException("unknown type: " + value.getClass().getName());
    }

    private static int roundUp(int len) {
        int mod = len % 32;
        return mod == 0 ? len : len + (32 -mod);
//        return newLen == 0 ? 32 : newLen;
    }

    @Override
    public String toString() {
        return canonicalAbiType;
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
