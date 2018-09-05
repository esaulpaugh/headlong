package com.esaulpaugh.headlong.abi.beta;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

// saves ~1 MB of memory by not including fixed/ufixed types in the map
public class BaseTypeInfo {

    private static final int HASH_MAP_INITIAL_CAPACITY = 256;

    private static final Map<String, BaseTypeInfo> TYPE_INFO_MAP;

    static {
        int o = 0;
        Map<String, BaseTypeInfo> map = new HashMap<>(HASH_MAP_INITIAL_CAPACITY);

        o = putInts(o, map, false);
        o = putInts(o, map, true);

        for (int i = 1; i <= 32; i++) {
            String canonical = "bytes" + i;
            map.put(canonical, new BaseTypeInfo(o++, canonical, byte[].class, i, ByteType.UNSIGNED_BYTE_OBJECT));
        }

        map.put(
                "function",
                new BaseTypeInfo(o++, "function", null, "bytes24", byte[].class, null, 24 * Byte.SIZE, 0, false, 24, ByteType.UNSIGNED_BYTE_OBJECT)
        );

        map.put("bool", new BaseTypeInfo(o++, "bool", Boolean.class, boolean.class, 1, false));
        map.put("address", new BaseTypeInfo(o++, "address", null, "uint160", BigInteger.class, null, 160, 0, false, -1, null));
        map.put("bytes", new BaseTypeInfo(o++, "bytes", byte[].class, -1, ByteType.UNSIGNED_BYTE_OBJECT));
        map.put("string", new BaseTypeInfo(o++, "string", String.class, -1, ByteType.UNSIGNED_BYTE_OBJECT));
        map.put("decimal", new BaseTypeInfo(o++, "decimal", null, "fixed256x10", BigDecimal.class, null, 256, 10, true, -1, null));

        TYPE_INFO_MAP = Collections.unmodifiableMap(map);
    }

    private transient final Integer ordinal; // for sorting

    public final String canonical; // e.g. address
    public final String nonCanonical; // e.g. fixed
    public final String effective; // e.g. uint160
    public final String className; // e.g. java.lang.Boolean
    public final String arrayClassNameStub; // e.g. Z, e.g. Ljava.lang.BigInteger;

    public final int bitLength;
    public final int scale;
    public final boolean signed;

    public final StackableType elementType;

    public final int arrayLength;

    public BaseTypeInfo(Integer ordinal, String canonical, Class<?> objectClass, int arrayLength, StackableType elementType) {
        this(ordinal, canonical, null, canonical, objectClass, null, -1, 0, false, arrayLength, elementType);
    }

    public BaseTypeInfo(Integer ordinal, String canonical, Class<?> objectClass, Class<?> primitiveClass, int bitLength, boolean signed) {
        this(ordinal, canonical, null, canonical, objectClass, primitiveClass, bitLength, 0, signed, -1,null);
    }

    public BaseTypeInfo(Integer ordinal,
                        String canonical,
                        String nonCanonical,
                        String effective,
                        Class<?> objectClass,
                        Class<?> primitiveClass,
                        int bitLength,
                        int scale,
                        boolean signed,
                        int arrayLength,
                        StackableType elementType) {
        this.ordinal = ordinal;
        this.canonical = canonical.intern();
        this.nonCanonical = nonCanonical == null ? null : nonCanonical.intern();
        this.effective = effective.intern();
        this.className = objectClass.getName().intern();
        Object array = Array.newInstance(primitiveClass != null ? primitiveClass : objectClass, 0);
        this.arrayClassNameStub = array.getClass().getName().replaceFirst("\\[", "").intern();
        this.bitLength = bitLength;
        this.arrayLength = arrayLength;
        this.scale = scale;
        this.signed = signed;
        this.elementType = elementType;
    }

    @Override
    public String toString() {
        return ordinal + ") "
                + canonical + ", "
                + (nonCanonical == null ? '-' : nonCanonical) + ", "
                + (effective.equals(canonical) ? '-' : effective)
                + ", \"" + className + "\", "
                + "\""+ arrayClassNameStub + '\"';
    }

    private static String canonicalize(String raw) {
        switch (raw) {
        case "int": return "int256";
        case "uint": return "uint256";
        case "fixed": return "fixed128x18";
        case "ufixed": return "ufixed128x18";
        default: return raw;
        }
    }

    public static BaseTypeInfo get(String canonical) {
//        String canonical = canonicalize(raw);
        return TYPE_INFO_MAP.get(canonical);
    }

    /**
     * Throws UnsupportedOperationException if TYPE_INFO_MAP is unmodifiable.
     *
     * @param info
     * @return
     */
    public static BaseTypeInfo put(BaseTypeInfo info) {
        return TYPE_INFO_MAP.put(info.canonical, info);
    }

    /**
     * Throws UnsupportedOperationException if TYPE_INFO_MAP is unmodifiable.
     *
     * @param canonical
     * @return
     */
    public static BaseTypeInfo remove(String canonical) {
        return TYPE_INFO_MAP.remove(canonical);
    }

    @SuppressWarnings("unchecked")
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

        Map.Entry<String, BaseTypeInfo>[] entries = TYPE_INFO_MAP.entrySet().toArray(new Map.Entry[0]);
        Arrays.sort(entries, new MapComparator());
        for(Map.Entry<String, BaseTypeInfo> e : entries) {
            System.out.println(e.getKey() + " --> " + e.getValue());
        }
    }

    private static class MapComparator implements Comparator<Map.Entry<String, BaseTypeInfo>>, Serializable {
        public int compare(Map.Entry<String, BaseTypeInfo> a, Map.Entry<String, BaseTypeInfo> b) {
            return Integer.compare(a.getValue().ordinal, b.getValue().ordinal);
        }
    }

    private static int putInts(int o, final Map<String, BaseTypeInfo> map, final boolean unsigned) {
        final String stub = unsigned ? "uint" : "int";

        final boolean signed = !unsigned;

        int i;
        String canonical;

        for(i = 8; i <= 8; i+=8) {
            canonical = stub + i;
            map.put(canonical, new BaseTypeInfo(o++, canonical, Byte.class, byte.class, i, signed));
        }
        for( ; i <= 16; i+=8) {
            canonical = stub + i;
            map.put(canonical, new BaseTypeInfo(o++, canonical, Short.class, short.class, i, signed));
        }
        for( ; i <= 32; i+=8) {
            canonical = stub + i;
            map.put(canonical, new BaseTypeInfo(o++, canonical, Integer.class, int.class, i, signed));
        }
        for( ; i <= 64; i+=8) {
            canonical = stub + i;
            map.put(canonical, new BaseTypeInfo(o++, canonical, Long.class, long.class, i, signed));
        }
        for( ; i < 256; i+=8) {
            canonical = stub + i;
            map.put(canonical, new BaseTypeInfo(o++, canonical, BigInteger.class, null, i, signed));
        }

        String special = stub + "256";
        map.put(special, new BaseTypeInfo(o++, special, stub, special, BigInteger.class, null, 256, 0, signed, -1, null));

        return o;
    }

//    private static BaseTypeInfo fixedType(String canonical, int bitLength, int scale) {
//        boolean signed = ;
//        String nonCanonical;
//        if(canonical.contains("fixed128x18")) {
//            nonCanonical = canonical.charAt(0) == 'u' ? "ufixed" : "fixed";
//        } else {
//            nonCanonical = null;
//        }
//        return ;
//        return new BaseTypeInfo(null, canonical, nonCanonical, canonical, BigDecimal.class, null);
//    }

//    private static int putFixed(int o, Map<String, BaseTypeInfo> map, boolean unsigned) {
//        final String stub = unsigned ? "ufixed" : "fixed";
//        for(int M = 8; M <= 256; M+=8) {
//            for (int N = 1; N <= 80; N++) {
//                String canonical = stub + M + 'x' + N;
//                map.put(canonical, new BaseTypeInfo(o++, canonical, null, canonical, BigDecimal.class, null, M, N, true, -1, null));
//            }
//        }
//
//        // overwrite 128x18 entry
//        String special = stub + "128x18";
//        map.put(special, new BaseTypeInfo(o++, special, stub, special, BigDecimal.class, null, 128, 18, true, -1, null));
//
//        return o;
//    }
}
