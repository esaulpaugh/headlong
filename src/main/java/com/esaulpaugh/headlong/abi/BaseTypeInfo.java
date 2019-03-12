package com.esaulpaugh.headlong.abi;

import java.util.*;

/**
 * An object to hold metadata about a base type, such as the type's Java class name. A metadata object for each type is
 * stored in {@link #TYPE_INFO_MAP} which is used by {@link TypeFactory#resolveBaseType(String, boolean, ABIType)}
 * to generate a base type.
 *
 * fixed/ufixed types, which number in the thousands, are not included in the map, saving about 1 MB of memory. These
 * are parsed by {@link TypeFactory}.
 */
class BaseTypeInfo {

    private static final Map<String, BaseTypeInfo> TYPE_INFO_MAP;

    static {
        Map<String, BaseTypeInfo> map = new HashMap<>(256);

        putSignedInts(map);
        putUnsignedInts(map);

        for (int i = 1; i <= 32; i++) {
            map.put(
                    "bytes" + i,
                    new BaseTypeInfo(
                            ArrayType.BYTE_ARRAY_CLASS_NAME,
                            ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME_STUB,
                            i,
                            ByteType.UNSIGNED_BYTE_OBJECT
                    )
            );
        }

        map.put(
                "bytes",
                new BaseTypeInfo(ArrayType.BYTE_ARRAY_CLASS_NAME, ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME_STUB, -1, ByteType.UNSIGNED_BYTE_OBJECT)
        );
        map.put(
                "function",
                new BaseTypeInfo(
                        ArrayType.BYTE_ARRAY_CLASS_NAME,
                        ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME_STUB,
                        24 * Byte.SIZE,
                        0,
                        24,
                        ByteType.UNSIGNED_BYTE_OBJECT
                )
        );
        map.put(
                "string",
                new BaseTypeInfo(ArrayType.STRING_CLASS_NAME, ArrayType.STRING_ARRAY_CLASS_NAME_STUB, -1, ByteType.UNSIGNED_BYTE_OBJECT)
        );
        map.put(
                "address",
                new BaseTypeInfo(BigIntegerType.CLASS_NAME, BigIntegerType.ARRAY_CLASS_NAME_STUB, 160, 0, -1, null)
        );
        map.put(
                "decimal",
                new BaseTypeInfo(BigDecimalType.CLASS_NAME, BigDecimalType.ARRAY_CLASS_NAME_STUB, 128, 10, -1, null)
        );
        map.put(
                "bool",
                new BaseTypeInfo(BooleanType.CLASS_NAME, BooleanType.ARRAY_CLASS_NAME_STUB, 1)
        );

        TYPE_INFO_MAP = Collections.unmodifiableMap(map);
    }

    private static void putSignedInts(final Map<String, BaseTypeInfo> map) {
        int n;
        for(n=8;n <= 8; n+=8) {
            map.put("int" + n, new BaseTypeInfo(ByteType.CLASS_NAME, ByteType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 32; n+=8) {
            map.put("int" + n, new BaseTypeInfo(IntType.CLASS_NAME, IntType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 64; n+=8) {
            map.put("int" + n, new BaseTypeInfo(LongType.CLASS_NAME, LongType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 256; n+=8) {
            map.put("int" + n, new BaseTypeInfo(BigIntegerType.CLASS_NAME, BigIntegerType.ARRAY_CLASS_NAME_STUB, n));
        }
    }

    private static void putUnsignedInts(final Map<String, BaseTypeInfo> map) {
        int n;
        for(n=8;n <= 8; n+=8) {
            map.put("uint" + n, new BaseTypeInfo(IntType.CLASS_NAME, ByteType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 24; n+=8) {
            map.put("uint" + n, new BaseTypeInfo(IntType.CLASS_NAME, IntType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 32; n+=8) {
            map.put("uint" + n, new BaseTypeInfo(LongType.CLASS_NAME, IntType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n < 64; n+=8) {
            map.put("uint" + n, new BaseTypeInfo(LongType.CLASS_NAME, LongType.ARRAY_CLASS_NAME_STUB, n));
        }
        // special case -- allow long for array elements
        for ( ; n <= 64; n+=8) {
            map.put("uint" + n, new BaseTypeInfo(BigIntegerType.CLASS_NAME, LongType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 256; n+=8) {
            map.put("uint" + n, new BaseTypeInfo(BigIntegerType.CLASS_NAME, BigIntegerType.ARRAY_CLASS_NAME_STUB, n));
        }
    }

    static void putFixed(Map<String, BaseTypeInfo> map, boolean unsigned) {
        final String stub = unsigned ? "ufixed" : "fixed";
        for(int M = 8; M <= 256; M+=8) {
            for (int N = 1; N <= 80; N++) {
                map.put(
                        stub + M + 'x' + N,
                        new BaseTypeInfo(BigDecimalType.CLASS_NAME, BigDecimalType.ARRAY_CLASS_NAME_STUB, M, N, -1, null)
                );
            }
        }
    }

    public final String className; // e.g. "java.lang.Boolean"
    public final String arrayClassNameStub; // e.g. "Z", e.g. "Ljava.lang.BigInteger;"

    public final int bitLen;
    public final int scale;

    public final ABIType<?> elementType;

    public final int arrayLen;

    public BaseTypeInfo(String objectClassName, String arrayClassNameStub, int arrayLen, ABIType<?> elementType) {
        this(objectClassName, arrayClassNameStub, -1, 0, arrayLen, elementType);
    }

    public BaseTypeInfo(String objectClassName, String arrayClassNameStub, int bitLen) {
        this(objectClassName, arrayClassNameStub, bitLen, 0, -1, null);
    }

    public BaseTypeInfo(String objectClassName,
                        String arrayClassNameStub,
                        int bitLen,
                        int scale,
                        int arrayLen,
                        ABIType<?> elementType) {
        this.className = objectClassName.intern();
        this.arrayClassNameStub = arrayClassNameStub.intern();
        this.bitLen = bitLen;
        this.arrayLen = arrayLen;
        this.scale = scale;
        this.elementType = elementType;
    }

    @Override
    public String toString() {
        return "\"" +className + "\", " + "\""+ arrayClassNameStub + '\"';
    }

    /**
     * Returns the canonical base type's metadata object if it exists.
     *
     * @param canonical the canonical type string for the base type
     * @return  the metadata object
     */
    public static BaseTypeInfo get(String canonical) {
        return TYPE_INFO_MAP.get(canonical);
    }

    public static Map<String, BaseTypeInfo> getBaseTypeInfoMap() {
        return TYPE_INFO_MAP;
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, arrayClassNameStub, bitLen, scale, elementType, arrayLen);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseTypeInfo that = (BaseTypeInfo) o;
        return bitLen == that.bitLen &&
                scale == that.scale &&
                arrayLen == that.arrayLen &&
                Objects.equals(className, that.className) &&
                Objects.equals(arrayClassNameStub, that.arrayClassNameStub) &&
                Objects.equals(elementType, that.elementType);
    }
}
