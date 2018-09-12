package com.esaulpaugh.headlong.abi;

import com.joemelsha.crypto.hash.Keccak;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.DigestException;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.esaulpaugh.headlong.abi.AbstractUnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.rlp.util.Strings.HEX;
import static com.esaulpaugh.headlong.rlp.util.Strings.encode;

/**
 * Represents a function in an Ethereum contract. Can encode and decode function calls matching this function's signature.
 */
public class Function implements Serializable {

    private static final long serialVersionUID = -1151351210657573607L;

    private static final Charset ASCII = Charset.forName("US-ASCII");

    public static final int SELECTOR_LEN = 4;

    final String canonicalSignature;
    private final String hashAlgorithm;

    private transient final boolean requiredCanonicalization;
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
        this.paramTypes = TupleType.create(canonicalSig.substring(canonicalSig.indexOf('(')), types.toArray(StackableType.EMPTY_TYPE_ARRAY));
        this.hashAlgorithm = messageDigest.getAlgorithm();
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

    public String getHashAlgorithm() {
        return hashAlgorithm;
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

    public static String format(byte[] abiCall) {
        return format(abiCall, 0, abiCall.length);
    }

    /**
     * Returns a formatted string for a given ABI-encoded function call.
     *
     * @param buffer   the buffer containing the ABI call
     * @param offset    the offset into the input buffer of the ABI call
     * @param length    the length of the ABI call
     * @return  the formatted string
     * @throws  IllegalArgumentException    if the input length mod 32 != 4
     */
    public static String format(byte[] buffer, int offset, final int length) {

        if(length < 4 || ((length - 4) & 0b111) != 0) {
            int mod = length % UNIT_LENGTH_BYTES;
            throw new IllegalArgumentException("expected length mod " + UNIT_LENGTH_BYTES + " == 4, found: " + mod);
        }

        StringBuilder sb = new StringBuilder("ID\t")
                .append(encode(Arrays.copyOfRange(buffer, offset, SELECTOR_LEN), HEX))
                .append('\n');
        int idx = offset + SELECTOR_LEN;
        while(idx < length) {
            sb.append(idx >>> AbstractUnitType.LOG_2_UNIT_LENGTH_BYTES)
                    .append('\t')
                    .append(encode(Arrays.copyOfRange(buffer, idx, idx + UNIT_LENGTH_BYTES), HEX))
                    .append('\n');
            idx += UNIT_LENGTH_BYTES;
        }
        return sb.toString();
    }

    public static String hex(byte[] bytes) {
        return encode(bytes, HEX);
    }

    // use readResolve to avoid setting transient final fields
    private Object readResolve() {
        try {
            return new Function(this.canonicalSignature);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public int hashCode() {
        // do not hash requiredCanonicalization
        return Objects.hash(
                canonicalSignature,
                hashAlgorithm,
                paramTypes // hash transient paramTypes just to be sure TODO
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Function other = (Function) o;
        // do not check requiredCanonicalization
        return Objects.equals(canonicalSignature, other.canonicalSignature)
                && Objects.equals(hashAlgorithm, other.hashAlgorithm)
                && Objects.equals(paramTypes, other.paramTypes); // check transient paramTypes just to be sure TODO
    }
}
