package com.esaulpaugh.headlong.abi;

import com.joemelsha.crypto.hash.Keccak;
import sun.nio.cs.US_ASCII;

import java.nio.ByteBuffer;
import java.security.DigestException;
import java.text.ParseException;
import java.util.ArrayList;

public class Function {

    public static final Type[] EMPTY_TYPE_ARRAY = new Type[0];
    public static final int FUNCTION_ID_LEN = 4;

    private final String canonicalSignature;

    transient final byte[] selector = new byte[FUNCTION_ID_LEN];

    transient final TupleType paramTypes;

    boolean requiredCanonicalization;

    Function(String signature) throws ParseException {
        StringBuilder canonicalSignature = new StringBuilder();
        ArrayList<Type> types = new ArrayList<>();
        this.requiredCanonicalization = ABI.parseFunctionSignature(signature, canonicalSignature, types);
        this.canonicalSignature = canonicalSignature.toString();

        try {
            Keccak keccak256 = new Keccak(256);
            keccak256.update(this.canonicalSignature.getBytes(US_ASCII.INSTANCE));
            keccak256.digest(selector, 0, selector.length);
        } catch (DigestException de) {
            throw new RuntimeException(de);
        }

        this.paramTypes = TupleType.create(types.toArray(EMPTY_TYPE_ARRAY));
    }

    public ByteBuffer encodeCall(Object... params) {
        return Encoder.encodeFunctionCall(this, params);
    }

    public String getCanonicalSignature() {
        return canonicalSignature;
    }

    public byte[] getSelector() {
        byte[] out = new byte[selector.length];
        System.arraycopy(selector, 0, out, 0, out.length);
        return out;
    }

    public TupleType getParamTypes() {
        return paramTypes;
//        Type[] out = new Type[types.length];
//        System.arraycopy(types, 0, out, 0, out.length);
//        return out;
    }

    public boolean requiredCanonicalization() {
        return requiredCanonicalization;
    }
}
