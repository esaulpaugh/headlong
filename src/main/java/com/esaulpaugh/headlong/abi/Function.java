package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.Strings;
import com.joemelsha.crypto.hash.Keccak;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.encode;

/**
 * Represents a function in an Ethereum contract. Can encode and decode calls matching this function's signature.
 */
public class Function implements Serializable {

    private static final Pattern HAS_NON_ASCII_CHARS = Pattern.compile("[^\\p{ASCII}]+");

    public static final int SELECTOR_LEN = 4;

    final String canonicalSignature;
    private final String hashAlgorithm;

    private final boolean requiredCanonicalization;
    final byte[] selector;
    final TupleType inputTypes;

    private final TupleType outputTypes;

    {
        selector = new byte[SELECTOR_LEN];
    }

    public Function(String signature) throws ParseException {
        this(signature, null);
    }

    public Function(String signature, String outputs) throws ParseException {
        this(signature, outputs, new Keccak(256));
    }

    /**
     * Note that {@code messageDigest} must be given in an {@link MessageDigest#INITIAL} (i.e. not
     * {@link MessageDigest#IN_PROGRESS}) state.
     *
     * @param signature the function signature
     * @param messageDigest the hash function with which to generate the 4-byte selector
     * @throws ParseException   if the signature is malformed
     */
    public Function(final String signature, String outputs, final MessageDigest messageDigest) throws ParseException {

        final int split = signature.indexOf('(');

        if(split < 0) {
            throw new ParseException("params start not found", signature.length());
        }

        final String functionName = signature.substring(0, split);

        final Matcher matcher = HAS_NON_ASCII_CHARS.matcher(functionName);
        if(matcher.find()) {
            char c = signature.charAt(matcher.start());
            throw new ParseException("non-ascii char, \'" + c + "\' " + TupleTypeParser.escapeChar(c) + ", @ index " + matcher.start(), matcher.start());
        }

        final String rawTupleTypeString = signature.substring(split);

        final TupleType tupleType = TupleTypeParser.parseTupleType(rawTupleTypeString);
        final String canonicalSig = functionName + tupleType.canonicalType;
        try {
            messageDigest.update(canonicalSig.getBytes(Strings.CHARSET_ASCII));
            messageDigest.digest(selector, 0, SELECTOR_LEN);
        } catch (DigestException de) {
            throw new RuntimeException(de);
        }
        this.canonicalSignature = canonicalSig;
        this.requiredCanonicalization = !signature.equals(canonicalSig);
        this.inputTypes = tupleType;
        this.hashAlgorithm = messageDigest.getAlgorithm();
        this.outputTypes = outputs == null ? null : TupleType.parse(outputs);
    }

    public String getName() {
        return canonicalSignature.substring(0, canonicalSignature.indexOf('('));
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

    public TupleType getInputTypes() {
        return inputTypes;
    }

    public TupleType getOutputTypes() {
        return outputTypes;
    }

    public ByteBuffer encodeCallWithArgs(Object... args) {
        return encodeCall(new Tuple(args));
    }

    public ByteBuffer encodeCall(Tuple args) {
        return CallEncoder.encodeCall(this, args);
    }

    public Function encodeCall(Tuple args, ByteBuffer dest, boolean validate) {
        if(validate) {
            inputTypes.validate(args);
        }
        CallEncoder.encodeCall(this, args, dest);
        return this;
    }

    public Tuple decodeReturnValues(byte[] returnVals) {
        return outputTypes.decode(returnVals);
    }

    public Tuple decodeReturnValues(ByteBuffer returnVals) {
        return outputTypes.decode(returnVals);
    }

    public int callLength(Tuple args) {
        return CallEncoder.calcEncodingLength(this, args, true);
    }

    public int callLength(Tuple args, boolean validate) {
        return CallEncoder.calcEncodingLength(this, args, validate);
    }

    public Tuple decodeCall(byte[] array) {
        return decodeCall(ByteBuffer.wrap(array));
    }

    public Tuple decodeCall(ByteBuffer abiBuffer) {
        byte[] unitBuffer = ABIType.newUnitBuffer();
        abiBuffer.get(unitBuffer, 0, SELECTOR_LEN);
        final byte[] selector = this.selector;
        for(int i = 0; i < SELECTOR_LEN; i++) {
            if(unitBuffer[i] != selector[i]) {
                throw new IllegalArgumentException("given selector does not match: expected: " + this.selectorHex()
                        + ", found: " + encode(unitBuffer, 0, SELECTOR_LEN, HEX));
            }
        }
        return inputTypes.decode(abiBuffer, unitBuffer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(canonicalSignature, hashAlgorithm, outputTypes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Function function = (Function) o;
        return canonicalSignature.equals(function.canonicalSignature) &&
                hashAlgorithm.equals(function.hashAlgorithm) &&
                Objects.equals(outputTypes, function.outputTypes);
    }

    public static String formatCall(byte[] abiCall) {
        return formatCall(abiCall, 0, abiCall.length);
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
    public static String formatCall(byte[] buffer, int offset, final int length) {

        if(length < 4 || ((length - 4) & 0b111) != 0) {
            int mod = length % UNIT_LENGTH_BYTES;
            throw new IllegalArgumentException("expected length mod " + UNIT_LENGTH_BYTES + " == 4, found: " + mod);
        }

        StringBuilder sb = new StringBuilder("ID\t")
                .append(encode(Arrays.copyOfRange(buffer, offset, SELECTOR_LEN), HEX))
                .append('\n');
        int idx = offset + SELECTOR_LEN;
        while(idx < length) {
            sb.append(idx >>> UnitType.LOG_2_UNIT_LENGTH_BYTES)
                    .append('\t')
                    .append(encode(Arrays.copyOfRange(buffer, idx, idx + UNIT_LENGTH_BYTES), HEX))
                    .append('\n');
            idx += UNIT_LENGTH_BYTES;
        }
        return sb.toString();
    }

    public static String hexOf(byte[] bytes) {
        return encode(bytes, HEX);
    }

    public static Function parse(String signature) throws ParseException {
        return new Function(signature);
    }

    public static Function fromJson(String functionJson) throws ParseException {
        return ContractJSONParser.parseFunction(functionJson);
    }
}
