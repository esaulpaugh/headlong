package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Tuple;
import com.joemelsha.crypto.hash.Keccak;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.text.ParseException;
import java.util.List;

import static com.esaulpaugh.headlong.abi.beta.StackableType.EMPTY_TYPE_ARRAY;

/**
 * Represents a function in an Ethereum contract. Can encode and decode function calls matching this function's signature.
 */
public class Function {

    private static final Charset ASCII = StandardCharsets.US_ASCII;

    public static final int SELECTOR_LEN = 4;

    private final String canonicalSignature;
    private boolean requiredCanonicalization;
    transient final byte[] selector = new byte[SELECTOR_LEN];
    transient final TupleType paramTypes;

    public Function(String signature) throws ParseException {
        StringBuilder canonicalSignature = new StringBuilder();
        List<StackableType> types = SignatureParser.parseFunctionSignature(signature, canonicalSignature);
        this.canonicalSignature = canonicalSignature.toString();
        this.requiredCanonicalization = !signature.equals(this.canonicalSignature);

        try {
            Keccak keccak256 = new Keccak(256);
            keccak256.update(this.canonicalSignature.getBytes(ASCII));
            keccak256.digest(selector, 0, selector.length);
        } catch (DigestException de) {
            throw new RuntimeException(de);
        }

        this.paramTypes = TupleType.create(canonicalSignature.substring(canonicalSignature.indexOf("(")), types.toArray(EMPTY_TYPE_ARRAY));
    }

    public Throwable error(Object... args) {
        return error(new Tuple(args));
    }

    public Throwable error(Tuple args) {
        try {
            paramTypes.validate(args);
        } catch (Throwable t) {
            return t;
        }
        return null;
    }

    public ByteBuffer encodeCall(Object... args) {
        return Function.encodeCall(this, new Tuple(args));
    }

    public ByteBuffer encodeCall(Tuple argsTuple) {
        return Function.encodeCall(this, argsTuple);
    }

    public Tuple decodeCall(byte[] abi) {
        return paramTypes.decode(abi, SELECTOR_LEN);
    }

    public String getCanonicalSignature() {
        return canonicalSignature;
    }

    public boolean requiredCanonicalization() {
        return requiredCanonicalization;
    }

    public byte[] getSelector() {
        byte[] out = new byte[selector.length];
        System.arraycopy(selector, 0, out, 0, out.length);
        return out;
    }

    public String getSelectorHex() {
        return String.format("%040x", new BigInteger(selector));
    }

    public static ByteBuffer encodeCall(String signature, Object... args) throws ParseException {
        return Function.encodeCall(new Function(signature), new Tuple(args));
    }

    public static ByteBuffer encodeCall(Function function, Object... args) {
        return Function.encodeCall(function, new Tuple(args));
    }

    public static ByteBuffer encodeCall(Function function, Tuple argsTuple) {
        return Encoder.encodeFunctionCall(function, argsTuple);
    }
}
