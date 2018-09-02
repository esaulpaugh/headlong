package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.type.StackableType;
import com.esaulpaugh.headlong.abi.beta.type.TupleType;
import com.joemelsha.crypto.hash.Keccak;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.text.ParseException;
import java.util.List;

import static com.esaulpaugh.headlong.abi.beta.type.StackableType.EMPTY_TYPE_ARRAY;

/**
 * Represents a function in an Ethereum contract. Can encode function calls matching the function's signature.
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
        try {
            paramTypes.validate(new com.esaulpaugh.headlong.abi.beta.util.Tuple(args));
            return null;
        } catch (Throwable t) {
//            System.err.println(t.getMessage());
            return t;
        }
    }

    public ByteBuffer encodeCall(Object... args) {
        return GoodEncoder.encodeFunctionCall(this, args);
    }

    public Object[] decodeCall(byte[] abi) {
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

    public static ByteBuffer encodeFunctionCall(String signature, Object... arguments) throws ParseException {
        return GoodEncoder.encodeFunctionCall(new Function(signature), arguments);
    }
}
