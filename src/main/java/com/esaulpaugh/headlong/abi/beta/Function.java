package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Tuple;
import com.joemelsha.crypto.hash.Keccak;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.List;

import static com.esaulpaugh.headlong.abi.beta.StackableType.EMPTY_TYPE_ARRAY;
import static com.esaulpaugh.headlong.rlp.util.Strings.HEX;
import static com.esaulpaugh.headlong.rlp.util.Strings.encode;

/**
 * Represents a function in an Ethereum contract. Can encode and decode function calls matching this function's signature.
 */
public class Function {

    private static final Charset ASCII = StandardCharsets.US_ASCII;

    public static final int SELECTOR_LEN = 4;

    final String canonicalSignature;
    private boolean requiredCanonicalization;
    transient final byte[] selector;
    transient final TupleType paramTypes;

    {
        selector = new byte[SELECTOR_LEN];
    }

    public Function(String signature) throws ParseException {
        this(signature, new Keccak(256));
    }

    /**
     * Beware that {@code messageDigest} must be given in an {@link MessageDigest#INITIAL} (i.e. not
     * {@link MessageDigest#IN_PROGRESS}) state.
     *
     * @param signature
     * @param messageDigest
     * @throws ParseException
     */
    public Function(String signature, MessageDigest messageDigest) throws ParseException {
        StringBuilder canonicalBuilder = new StringBuilder();
        List<StackableType<?>> types = SignatureParser.parseFunctionSignature(signature, canonicalBuilder);
        final String canonicalSig = canonicalBuilder.toString();
        try {
            messageDigest.update(canonicalSig.getBytes(ASCII));
            messageDigest.digest(selector, 0, SELECTOR_LEN);
        } catch (DigestException de) {
            throw new RuntimeException(de);
        }
        this.canonicalSignature = canonicalSig;
        this.requiredCanonicalization = !signature.equals(canonicalSig);
        this.paramTypes = TupleType.create(canonicalSig.substring(canonicalSig.indexOf('(')), types.toArray(EMPTY_TYPE_ARRAY));
    }

    public ByteBuffer encodeCall(Object... args) {
        return Function.encodeCall(this, new Tuple(args));
    }

    public ByteBuffer encodeCall(Tuple argsTuple) {
        return Function.encodeCall(this, argsTuple);
    }

    public Tuple decodeCall(byte[] array) {
        return decodeCall(ByteBuffer.wrap(array));
    }

    public Tuple decodeCall(ByteBuffer abiBuffer) {
        byte[] unitBuffer = StackableType.newUnitBuffer();
        abiBuffer.get(unitBuffer, 0, SELECTOR_LEN);
        for(int i = 0; i < SELECTOR_LEN; i++) {
            if(unitBuffer[i] != this.selector[i]) {
                throw new IllegalArgumentException("given selector does not match: expected: " + this.selectorHex()
                        + ", found: " + encode(unitBuffer, 0, SELECTOR_LEN, HEX));
            }
        }
        return paramTypes.decode(abiBuffer, unitBuffer);
    }

    public String getCanonicalSignature() {
        return canonicalSignature;
    }

    public boolean requiredCanonicalization() {
        return requiredCanonicalization;
    }

    public byte[] selector() {
        byte[] out = new byte[selector.length];
        System.arraycopy(selector, 0, out, 0, out.length);
        return out;
    }

    public String selectorHex() {
        return encode(selector, HEX);
    }

    public static ByteBuffer encodeCall(String signature, Object... args) throws ParseException {
        return Function.encodeCall(new Function(signature), new Tuple(args));
    }

    public static ByteBuffer encodeCall(String signature, MessageDigest messageDigest, Object... args) throws ParseException {
        return Function.encodeCall(new Function(signature, messageDigest), new Tuple(args));
    }

    public static ByteBuffer encodeCall(Function function, Object... args) {
        return Function.encodeCall(function, new Tuple(args));
    }

    public static ByteBuffer encodeCall(Function function, Tuple argsTuple) {
        return Encoder.encodeFunctionCall(function, argsTuple);
    }
}
