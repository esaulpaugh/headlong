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
            String canonical = "bytes" + i;
            map.put(canonical, new BaseTypeInfo(canonical, ArrayType.BYTE_ARRAY_CLASS_NAME, ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME_STUB, i, ByteType.UNSIGNED_BYTE_OBJECT));
        }

        map.put(
                "function",
                new BaseTypeInfo(
                        "function",
                        null,
                        "bytes24",
                        ArrayType.BYTE_ARRAY_CLASS_NAME,
                        ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME_STUB,
                        24 * Byte.SIZE,
                        0,
                        true,
                        24, ByteType.UNSIGNED_BYTE_OBJECT
                )
        );

        map.put("bytes", new BaseTypeInfo("bytes", ArrayType.BYTE_ARRAY_CLASS_NAME, ArrayType.BYTE_ARRAY_ARRAY_CLASS_NAME_STUB, -1, ByteType.UNSIGNED_BYTE_OBJECT));
        map.put("string", new BaseTypeInfo("string", ArrayType.STRING_CLASS_NAME, ArrayType.STRING_ARRAY_CLASS_NAME_STUB, -1, ByteType.UNSIGNED_BYTE_OBJECT));

        map.put(
                "address",
                new BaseTypeInfo(
                        "address", null,
                        "uint160",
                        BigIntegerType.CLASS_NAME, BigIntegerType.ARRAY_CLASS_NAME_STUB,
                        160,
                        0,
                        true,
                        -1, null
                )
        );
        map.put(
                "decimal",
                new BaseTypeInfo(
                        "decimal", null,
                        "fixed128x10",
                        BigDecimalType.CLASS_NAME, BigDecimalType.ARRAY_CLASS_NAME_STUB,
                        128,
                        10,
                        false,
                        -1, null
                )
        );
        map.put(
                "bool",
                new BaseTypeInfo(
                        "bool",
                        BooleanType.CLASS_NAME, BooleanType.ARRAY_CLASS_NAME_STUB,
                        1,
                        true
                )
        );

        TYPE_INFO_MAP = Collections.unmodifiableMap(map);
    }

//    public final String canonical; // e.g. address
//    public final String nonCanonical; // e.g. fixed
//    public final String effective; // e.g. uint160
    public final String className; // e.g. java.lang.Boolean
    public final String arrayClassNameStub; // e.g. Z, e.g. Ljava.lang.BigInteger;

    public final int bitLen;
    public final int scale;
//    public final boolean unsigned;

    public final ABIType<?> elementType;

    public final int arrayLen;

    public BaseTypeInfo(String canonical, String objectClassName, String arrayClassNameStub, int arrayLen, ABIType<?> elementType) {
        this(canonical, null, canonical, objectClassName, arrayClassNameStub, -1, 0, true, arrayLen, elementType);
    }

    public BaseTypeInfo(String canonical, String objectClassName, String arrayClassNameStub, int bitLen, boolean unsigned) {
        this(canonical, null, canonical, objectClassName, arrayClassNameStub, bitLen, 0, unsigned, -1,null);
    }

    public BaseTypeInfo(String canonical,
                        String nonCanonical,
                        String effective,
                        String objectClassName,
                        String arrayClassNameStub,
                        int bitLen,
                        int scale,
                        boolean unsigned,
                        int arrayLen,
                        ABIType<?> elementType) {
//        this.ordinal = ordinal;
//        this.canonical = canonical;
//        this.nonCanonical = nonCanonical == null ? null : nonCanonical.intern();
//        this.effective = effective.intern();

        this.className = objectClassName.intern();
        this.arrayClassNameStub = arrayClassNameStub.intern();

        this.bitLen = bitLen;
        this.arrayLen = arrayLen;
        this.scale = scale;
//        this.unsigned = unsigned;
        this.elementType = elementType;
    }

    @Override
    public String toString() {
        return
//                canonical + ", "
//                + (nonCanonical == null ? '-' : nonCanonical) + ", "
//                + (effective.equals(canonical) ? '-' : effective) + ", "
                "\"" +className + "\", "
                + "\""+ arrayClassNameStub + '\"';
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

    /**
     * May be used to modify (mangle) type attributes at runtime. Throws UnsupportedOperationException if TYPE_INFO_MAP
     * is unmodifiable.
     *
     * @param canonical the key
     * @param info  the value
     * @return  the previous value
     */
    public static BaseTypeInfo put(String canonical, BaseTypeInfo info) {
        return TYPE_INFO_MAP.put(canonical, info);
    }

    /**
     * Throws UnsupportedOperationException if TYPE_INFO_MAP is unmodifiable.
     *
     * @param canonical the key
     * @return  the value from the removed entry
     */
    public static BaseTypeInfo remove(String canonical) {
        return TYPE_INFO_MAP.remove(canonical);
    }

    public static Set<String> keySet() {
        return TYPE_INFO_MAP.keySet();
    }

    public static Map<String, BaseTypeInfo> getBaseTypeInfoMap() {
        return TYPE_INFO_MAP;
    }

    private static void putSignedInts(final Map<String, BaseTypeInfo> map) {
        final String stub = "int";
        int n;
        String canonical;
        for( n = 8; n <= 8; n+=8) {
            canonical = stub + n;
            map.put(canonical, new BaseTypeInfo(canonical, ByteType.CLASS_NAME, ByteType.ARRAY_CLASS_NAME_STUB, n, false));
        }
        for( ; n <= 32; n+=8) {
            canonical = stub + n;
            map.put(canonical, new BaseTypeInfo(canonical, IntType.CLASS_NAME, IntType.ARRAY_CLASS_NAME_STUB, n, false));
        }
        for( ; n <= 64; n+=8) {
            canonical = stub + n;
            map.put(canonical, new BaseTypeInfo(canonical, LongType.CLASS_NAME, LongType.ARRAY_CLASS_NAME_STUB, n, false));
        }
        for( ; n <= 248; n+=8) {
            canonical = stub + n;
            map.put(canonical, new BaseTypeInfo(canonical, BigIntegerType.CLASS_NAME, BigIntegerType.ARRAY_CLASS_NAME_STUB, n, false));
        }

        // 256 added separately
        String special = stub + "256";
        map.put(special, new BaseTypeInfo(special, stub, special, BigIntegerType.CLASS_NAME, BigIntegerType.ARRAY_CLASS_NAME_STUB, 256, 0, false, -1, null));
    }

    private static void putUnsignedInts(final Map<String, BaseTypeInfo> map) {
        final String stub = "uint";
        int n;
        String canonical;
        for( n = 8; n <= 8; n+=8) {
            canonical = stub + n;
            map.put(canonical, new BaseTypeInfo(canonical, IntType.CLASS_NAME, ByteType.ARRAY_CLASS_NAME_STUB, n, true));
        }
        for( ; n <= 24; n+=8) {
            canonical = stub + n;
            map.put(canonical, new BaseTypeInfo(canonical, IntType.CLASS_NAME, IntType.ARRAY_CLASS_NAME_STUB, n, true));
        }
        for( ; n <= 32; n+=8) {
            canonical = stub + n;
            map.put(canonical, new BaseTypeInfo(canonical, LongType.CLASS_NAME, IntType.ARRAY_CLASS_NAME_STUB, n, true));
        }
        for( ; n < 64; n+=8) {
            canonical = stub + n;
            map.put(canonical, new BaseTypeInfo(canonical, LongType.CLASS_NAME, LongType.ARRAY_CLASS_NAME_STUB, n, true));
        }
        for( ; n <= 64; n+=8) {
            canonical = stub + n;
            map.put(canonical, new BaseTypeInfo(canonical, BigIntegerType.CLASS_NAME, LongType.ARRAY_CLASS_NAME_STUB, n, true));
        }
        for( ; n < 256; n+=8) {
            canonical = stub + n;
            map.put(canonical, new BaseTypeInfo(canonical, BigIntegerType.CLASS_NAME, BigIntegerType.ARRAY_CLASS_NAME_STUB, n, true));
        }

        // 256 added separately
        String special = stub + "256";
        map.put(special, new BaseTypeInfo(special, stub, special, BigIntegerType.CLASS_NAME, BigIntegerType.ARRAY_CLASS_NAME_STUB, 256, 0, true, -1, null));
    }

    static void putFixed(Map<String, BaseTypeInfo> map, boolean unsigned) {
        final String stub = unsigned ? "ufixed" : "fixed";
        for(int M = 8; M <= 256; M+=8) {
            for (int N = 1; N <= 80; N++) {
                String canonical = stub + M + 'x' + N;
                map.put(canonical, new BaseTypeInfo(canonical, null, canonical, BigDecimalType.CLASS_NAME, BigDecimalType.ARRAY_CLASS_NAME_STUB, M, N, unsigned, -1, null));
            }
        }

        // overwrite 128x18 entry
        String special = stub + "128x18";
        map.put(special, new BaseTypeInfo(special, stub, special, BigDecimalType.CLASS_NAME, BigDecimalType.ARRAY_CLASS_NAME_STUB, 128, 18, unsigned, -1, null));
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
