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
                            ArrayType.BYTE_ARRAY_CLASS,
                            ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME_STUB,
                            i,
                            ByteType.UNSIGNED_BYTE_OBJECT
                    )
            );
        }

        map.put(
                "bytes",
                new BaseTypeInfo(ArrayType.BYTE_ARRAY_CLASS, ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME_STUB, -1, ByteType.UNSIGNED_BYTE_OBJECT)
        );
        map.put(
                "function",
                new BaseTypeInfo(
                        ArrayType.BYTE_ARRAY_CLASS,
                        ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME_STUB,
                        24 * Byte.SIZE,
                        0,
                        24,
                        ByteType.UNSIGNED_BYTE_OBJECT
                )
        );
        map.put(
                "string",
                new BaseTypeInfo(ArrayType.STRING_CLASS, ArrayType.STRING_ARRAY_CLASS_NAME_STUB, -1, ByteType.UNSIGNED_BYTE_OBJECT)
        );
        map.put(
                "address",
                new BaseTypeInfo(BigIntegerType.CLASS, BigIntegerType.ARRAY_CLASS_NAME_STUB, 160, 0, -1, null)
        );
        map.put(
                "decimal",
                new BaseTypeInfo(BigDecimalType.CLASS, BigDecimalType.ARRAY_CLASS_NAME_STUB, 128, 10, -1, null)
        );
        map.put(
                "bool",
                new BaseTypeInfo(BooleanType.CLASS, BooleanType.ARRAY_CLASS_NAME_STUB, 1)
        );

        TYPE_INFO_MAP = Collections.unmodifiableMap(map);
    }

    private static void putSignedInts(final Map<String, BaseTypeInfo> map) {
        int n;
        for(n=8;n <= 8; n+=8) {
            map.put("int" + n, new BaseTypeInfo(ByteType.CLASS, ByteType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 32; n+=8) {
            map.put("int" + n, new BaseTypeInfo(IntType.CLASS, IntType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 64; n+=8) {
            map.put("int" + n, new BaseTypeInfo(LongType.CLASS, LongType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 256; n+=8) {
            map.put("int" + n, new BaseTypeInfo(BigIntegerType.CLASS, BigIntegerType.ARRAY_CLASS_NAME_STUB, n));
        }
    }

    private static void putUnsignedInts(final Map<String, BaseTypeInfo> map) {
        int n;
        for(n=8;n <= 8; n+=8) {
            map.put("uint" + n, new BaseTypeInfo(IntType.CLASS, ByteType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 24; n+=8) {
            map.put("uint" + n, new BaseTypeInfo(IntType.CLASS, IntType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 32; n+=8) {
            map.put("uint" + n, new BaseTypeInfo(LongType.CLASS, IntType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 56; n+=8) {
            map.put("uint" + n, new BaseTypeInfo(LongType.CLASS, LongType.ARRAY_CLASS_NAME_STUB, n));
        }
        // special case -- allow long for array elements
        for ( ; n <= 64; n+=8) {
            map.put("uint" + n, new BaseTypeInfo(BigIntegerType.CLASS, LongType.ARRAY_CLASS_NAME_STUB, n));
        }
        for ( ; n <= 256; n+=8) {
            map.put("uint" + n, new BaseTypeInfo(BigIntegerType.CLASS, BigIntegerType.ARRAY_CLASS_NAME_STUB, n));
        }
    }

    static void putFixed(Map<String, BaseTypeInfo> map, boolean unsigned) {
        final String stub = unsigned ? "ufixed" : "fixed";
        for(int M = 8; M <= 256; M+=8) {
            for (int N = 1; N <= 80; N++) {
                map.put(
                        stub + M + 'x' + N,
                        new BaseTypeInfo(BigDecimalType.CLASS, BigDecimalType.ARRAY_CLASS_NAME_STUB, M, N, -1, null)
                );
            }
        }
    }

    public final Class<?> clazz; // e.g. java.lang.Boolean.class
    public final String arrayClassNameStub; // e.g. "Z", e.g. "Ljava.lang.BigInteger;"

    public final int bitLen;
    public final int scale;

    public final ABIType<?> elementType;

    public final int arrayLen;

    public BaseTypeInfo(Class<?> clazz, String arrayClassNameStub, int arrayLen, ABIType<?> elementType) {
        this(clazz, arrayClassNameStub, -1, 0, arrayLen, elementType);
    }

    public BaseTypeInfo(Class<?> clazz, String arrayClassNameStub, int bitLen) {
        this(clazz, arrayClassNameStub, bitLen, 0, -1, null);
    }

    public BaseTypeInfo(Class<?> clazz,
                        String arrayClassNameStub,
                        int bitLen,
                        int scale,
                        int arrayLen,
                        ABIType<?> elementType) {
        this.clazz = clazz;
        this.arrayClassNameStub = arrayClassNameStub.intern();
        this.bitLen = bitLen;
        this.arrayLen = arrayLen;
        this.scale = scale;
        this.elementType = elementType;
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
        return Objects.hash(clazz, arrayClassNameStub, bitLen, scale, elementType, arrayLen);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseTypeInfo that = (BaseTypeInfo) o;
        return clazz == that.clazz &&
                bitLen == that.bitLen &&
                scale == that.scale &&
                arrayLen == that.arrayLen &&
                Objects.equals(arrayClassNameStub, that.arrayClassNameStub) &&
                Objects.equals(elementType, that.elementType);
    }
}
