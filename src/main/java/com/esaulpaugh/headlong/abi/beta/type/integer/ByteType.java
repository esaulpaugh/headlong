package com.esaulpaugh.headlong.abi.beta.type.integer;

import com.esaulpaugh.headlong.abi.beta.type.StackableType;

import java.math.BigInteger;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.beta.type.array.ArrayType.roundUp;

public class ByteType extends AbstractInt256Type<Byte> {

    public static final String CLASS_NAME = Byte.class.getName();
    public static final String CLASS_NAME_ELEMENT = byte[].class.getName().replaceFirst("\\[", "");

    public static final int MAX_BIT_LEN = 8;


    public static final ByteType BYTE_OBJECT = new ByteType("uint8", CLASS_NAME);
    public static final ByteType BYTE_PRIMITIVE = new ByteType("uint8", "B");

    public ByteType(String canonicalAbiType, String className) {
        super(canonicalAbiType, className, MAX_BIT_LEN);
    }

//    protected ByteType(String canonicalAbiType, String className, int bitLength) {
//        super(canonicalAbiType, className, bitLength);
//    }

    @Override
    public Byte decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        return bi.byteValueExact();
//        validate(b);
//        return b;
    }

    @Override
    public int byteLength(Object value) {
        return roundUp(1);
    }

//    @Override
//    public void validate(Object value) {
//        super.validate(value);
//        if(bitLength != BYTE_LENGTH_BITS) {
//            throw new AssertionError();
//        }
////        if(bitLength == 1) {
////            if(!(value instanceof Boolean)) {
////                throw new IllegalArgumentException("bitLength 1, expected Boolean. found " + value.getClass().getName());
////            }
////        }
//    }
}
