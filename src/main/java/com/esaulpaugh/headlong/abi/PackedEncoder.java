package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.rlp.util.BizarroIntegers;
import com.esaulpaugh.headlong.rlp.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.esaulpaugh.headlong.abi.StackableType.*;

public class PackedEncoder {

    static int insertTuple(TupleType tupleType, Tuple tuple, byte[] dest, int idx) {

        final StackableType<?>[] types = tupleType.elementTypes;
        final Object[] values = tuple.elements;

        final int len = types.length;
        int i;
        for (i = 0; i < len; i++) {
            idx = encode(types[i], values[i], dest, idx);
        }

        return idx;
    }

    private static int encode(StackableType<?> type, Object value, byte[] dest, int idx) {
        switch (type.typeCode()) {
        case TYPE_CODE_BOOLEAN: return insertBool((boolean) value, dest, idx);
        case TYPE_CODE_BYTE:
        case TYPE_CODE_SHORT:
        case TYPE_CODE_INT:
        case TYPE_CODE_LONG: return insertInt(((Number) value).longValue(), dest, idx);
        case TYPE_CODE_BIG_INTEGER: return insertInt(((BigInteger) value), dest, idx);
        case TYPE_CODE_BIG_DECIMAL: return insertInt(((BigDecimal) value).unscaledValue(), dest, idx);
        case TYPE_CODE_ARRAY:
            return encodeArray((ArrayType<?, ?>) type, value, dest, idx);
        case TYPE_CODE_TUPLE:
            return insertTuple((TupleType) type, (Tuple) value, dest, idx);
        default:
            throw new IllegalArgumentException("unexpected array type: " + type.toString());
        }
    }

    private static int encodeArray(ArrayType<?,?> arrayType, Object value, byte[] dest, int idx) {
        switch (arrayType.elementType.typeCode()) {
        case TYPE_CODE_BOOLEAN: return insertBooleans((boolean[]) value, dest, idx);
        case TYPE_CODE_BYTE:
            byte[] arr = arrayType.isString ? ((String) value).getBytes(Strings.CHARSET_UTF_8) : (byte[]) value;
            return insertBytes(arr, dest, idx);
        case TYPE_CODE_SHORT: return insertShorts((short[]) value, dest, idx);
        case TYPE_CODE_INT: return insertInts((int[]) value, dest, idx);
        case TYPE_CODE_LONG: return insertLongs((long[]) value, dest, idx);
        case TYPE_CODE_BIG_INTEGER: return insertBigIntegers((BigInteger[]) value, dest, idx);
        case TYPE_CODE_BIG_DECIMAL: return insertBigDecimals((BigDecimal[]) value, dest, idx);
        case TYPE_CODE_ARRAY:
        case TYPE_CODE_TUPLE:
            final StackableType<?> elementType = arrayType.elementType;
            for(Object e : (Object[]) value) {
                idx = encode(elementType, e, dest, idx);
            }
            return idx;
        default: throw new IllegalArgumentException("unexpected array type: " + arrayType.toString());
        }
    }

    // ------------------------

    private static int insertBooleans(boolean[] bools, byte[] dest, int idx) {
        final int len = bools.length;
        for (int i = idx; i < len; i++) {
            dest[idx + i] = bools[i] ? (byte) 1 : (byte) 0;
        }
        return idx + len;
    }

    private static int insertBytes(byte[] bytes, byte[] dest, int idx) {
        final int len = bytes.length;
        System.arraycopy(bytes, 0, dest, idx, len);
        return idx + len;
    }

    private static int insertShorts(short[] shorts, byte[] dest, int idx) {
        for (short e : shorts) {
            idx = insertInt(e, dest, idx);
        }
        return idx;
    }

    private static int insertInts(int[] ints, byte[] dest, int idx) {
        for (int e : ints) {
            idx = insertInt(e, dest, idx);
        }
        return idx;
    }

    private static int insertLongs(long[] longs, byte[] dest, int idx) {
        for (long e : longs) {
            idx = insertInt(e, dest, idx);
        }
        return idx;
    }

    private static int insertBigIntegers(BigInteger[] bigInts, byte[] dest, int idx) {
        for (BigInteger e : bigInts) {
            idx = insertInt(e, dest, idx);
        }
        return idx;
    }

    private static int insertBigDecimals(BigDecimal[] bigDecs, byte[] dest, int idx) {
        for (BigDecimal e : bigDecs) {
            idx = insertInt(e.unscaledValue(), dest, idx);
        }
        return idx;
    }

    // ---------------------------

    private static int insertBool(boolean value, byte[] dest, int idx) {
        dest[idx] = value ? (byte) 1 : (byte) 0;
        return 1;
    }

    private static int insertInt(long value, byte[] dest, int idx) {
        if(value >= 0) {
            return idx + Integers.putLong(value, dest, idx);
        }
        if(value == -1L) {
            dest[idx] = CallEncoder.NEGATIVE_ONE_BYTE;
            return idx + 1;
        }
        return idx + BizarroIntegers.putLong(value, dest, idx);
    }

    private static int insertInt(BigInteger bigGuy, byte[] dest, int idx) {
        byte[] arr = bigGuy.toByteArray();
        final int len = arr.length;
        System.arraycopy(arr, 0, dest, idx, len);
        return idx + len;
    }
}
