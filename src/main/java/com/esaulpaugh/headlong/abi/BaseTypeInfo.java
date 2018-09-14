package com.esaulpaugh.headlong.abi;

import java.io.Serializable;
import java.util.*;

// saves ~1 MB of memory by not including fixed/ufixed types in the map
public class BaseTypeInfo {

    private static final int HASH_MAP_INITIAL_CAPACITY = 256;

    private static final Map<String, BaseTypeInfo> TYPE_INFO_MAP;

    static {
        Map<String, BaseTypeInfo> map = new HashMap<>(HASH_MAP_INITIAL_CAPACITY);

        try {

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

        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
    }

//    public final String canonical; // e.g. address
//    public final String nonCanonical; // e.g. fixed
//    public final String effective; // e.g. uint160
    public final String className; // e.g. java.lang.Boolean
    public final String arrayClassNameStub; // e.g. Z, e.g. Ljava.lang.BigInteger;

    public final int bitLength;
    public final int scale;
//    public final boolean unsigned;

    public final StackableType<?> elementType;

    public final int arrayLength;

    public BaseTypeInfo(String canonical, String objectClassName, String arrayClassNameStub, int arrayLength, StackableType<?> elementType) {
        this(canonical, null, canonical, objectClassName, arrayClassNameStub, -1, 0, true, arrayLength, elementType);
    }

    public BaseTypeInfo(String canonical, String objectClassName, String arrayClassNameStub, int bitLength, boolean unsigned) {
        this(canonical, null, canonical, objectClassName, arrayClassNameStub, bitLength, 0, unsigned, -1,null);
    }

    public BaseTypeInfo(String canonical,
                        String nonCanonical,
                        String effective,
                        String objectClassName,
                        String arrayClassNameStub,
                        int bitLength,
                        int scale,
                        boolean unsigned,
                        int arrayLength,
                        StackableType<?> elementType) {
//        this.ordinal = ordinal;
//        this.canonical = canonical;
//        this.nonCanonical = nonCanonical == null ? null : nonCanonical.intern();
//        this.effective = effective.intern();

        this.className = objectClassName.intern();
        this.arrayClassNameStub = arrayClassNameStub.intern();

        this.bitLength = bitLength;
        this.arrayLength = arrayLength;
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

    public static void main(String[] args0) {

        System.out.println(int[].class.getSuperclass());

//        if(true)return;

        final int capacity = 256;

        Set<String> keySet = TYPE_INFO_MAP.keySet();

        int c, code, index;

        System.out.println(
                ((capacity - 1) & ("bool".hashCode() ^ ("bool".hashCode() >>> 16)))
                        + " == "
                        +  ((capacity - 1) & ("string".hashCode() ^ ("string".hashCode() >>> 16)))
        );

        int count = 0;
        HashSet<Integer> hashCodes = new HashSet<>();
        for(String key : keySet) {
            code = key.hashCode();
            index = (capacity - 1) & code;
            if(!hashCodes.add(index)) {
                System.out.print(key + "(" + index + "),");
                count++;
            }
        }
        System.out.println("\ncount = " + count);

        count = 0;
        hashCodes = new HashSet<>();

        for(String key : keySet) {
            code = (c = key.hashCode()) ^ (c >>> 16);
            index = (capacity - 1) & code;
            if(!hashCodes.add(index)) {
                System.out.print(key + "(" + index + "),");
                count++;
            }
        }
        System.out.println("\ncount = " + count);

        @SuppressWarnings("unchecked")
        Map.Entry<String, BaseTypeInfo>[] entries = TYPE_INFO_MAP.entrySet().toArray(new Map.Entry[0]);
        Arrays.sort(entries, new MapComparator());
        for(Map.Entry<String, BaseTypeInfo> e : entries) {
            System.out.println(e.getKey() + " --> " + e.getValue());
        }
    }

    private static class MapComparator implements Comparator<Map.Entry<String, BaseTypeInfo>>, Serializable {
        private static final long serialVersionUID = -765405845176007435L;

        public int compare(Map.Entry<String, BaseTypeInfo> a, Map.Entry<String, BaseTypeInfo> b) {
            return a.getKey().compareTo(b.getKey());
        }
    }

    private static void putSignedInts(final Map<String, BaseTypeInfo> map) {
        final String stub = "int";
        int bitLength;
        String canonical;
//        for(i = 8; i <= 8; i+=8) {
//            canonical = stub + i;
//            map.put(canonical, new BaseTypeInfo(canonical, Byte.class, byte.class, i, false));
//        }
//        for( ; i <= 16; i+=8) {
//            canonical = stub + i;
//            map.put(canonical, new BaseTypeInfo(canonical, Short.class, short.class, i, false));
//        }
        for( bitLength = 8 ; bitLength <= 32; bitLength+=8) {
            canonical = stub + bitLength;
            map.put(canonical, new BaseTypeInfo(canonical, IntType.CLASS_NAME, IntType.ARRAY_CLASS_NAME_STUB, bitLength, false));
        }
        for( ; bitLength <= 64; bitLength+=8) {
            canonical = stub + bitLength;
            map.put(canonical, new BaseTypeInfo(canonical, LongType.CLASS_NAME, LongType.ARRAY_CLASS_NAME_STUB, bitLength, false));
        }
        for( ; bitLength <= 248; bitLength+=8) {
            canonical = stub + bitLength;
            map.put(canonical, new BaseTypeInfo(canonical, BigIntegerType.CLASS_NAME, BigIntegerType.ARRAY_CLASS_NAME_STUB, bitLength, false));
        }

        // 256 added separately
        String special = stub + "256";
        map.put(special, new BaseTypeInfo(special, stub, special, BigIntegerType.CLASS_NAME, BigIntegerType.ARRAY_CLASS_NAME_STUB, 256, 0, false, -1, null));
    }

    private static void putUnsignedInts(final Map<String, BaseTypeInfo> map) throws ClassNotFoundException {
        final String stub = "uint";
        int i;
        String canonical;
        for( i = 8; i <= 8; i+=8) {
            canonical = stub + i;
            map.put(canonical, new BaseTypeInfo(canonical, IntType.CLASS_NAME, ByteType.ARRAY_CLASS_NAME_STUB, i, true));
        }
//        for( ; i <= 16; i+=8) {
//            canonical = stub + i;
//            map.put(canonical, new BaseTypeInfo(canonical, Integer.class, int.class, i, true));
//        }
        for( ; i <= 32; i+=8) {
            canonical = stub + i;
            map.put(canonical, new BaseTypeInfo(canonical, LongType.CLASS_NAME, IntType.ARRAY_CLASS_NAME_STUB, i, true));
        }
        for( ; i < 64; i+=8) {
            canonical = stub + i;
            map.put(canonical, new BaseTypeInfo(canonical, LongType.CLASS_NAME, LongType.ARRAY_CLASS_NAME_STUB, i, true));
        }
        for( ; i <= 64; i+=8) {
            canonical = stub + i;
            map.put(canonical, new BaseTypeInfo(canonical, BigIntegerType.CLASS_NAME, LongType.ARRAY_CLASS_NAME_STUB, i, true));
        }
        for( ; i < 256; i+=8) {
            canonical = stub + i;
            map.put(canonical, new BaseTypeInfo(canonical, BigIntegerType.CLASS_NAME, BigIntegerType.ARRAY_CLASS_NAME_STUB, i, true));
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
        return Objects.hash(className, arrayClassNameStub, bitLength, scale, elementType, arrayLength);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseTypeInfo that = (BaseTypeInfo) o;
        return bitLength == that.bitLength &&
                scale == that.scale &&
                arrayLength == that.arrayLength &&
                Objects.equals(className, that.className) &&
                Objects.equals(arrayClassNameStub, that.arrayClassNameStub) &&
                Objects.equals(elementType, that.elementType);
    }
}
