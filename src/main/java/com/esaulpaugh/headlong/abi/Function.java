package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.util.Strings;
import com.esaulpaugh.headlong.util.Utils;
import com.google.gson.JsonObject;
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
 * Can decode the function's return values.
 */
public class Function implements ABIObject, Serializable {

    public enum FunctionType {

        FALLBACK,
        CONSTRUCTOR,
        FUNCTION;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        static FunctionType get(String value) {
            switch (value) {
            case ContractJSONParser.FALLBACK: return FunctionType.FALLBACK;
            case ContractJSONParser.CONSTRUCTOR: return FunctionType.CONSTRUCTOR;
            case ContractJSONParser.FUNCTION: return FunctionType.FUNCTION;
            default: throw new IllegalArgumentException("no " + FunctionType.class.getSimpleName() + " found for " + value);
            }
        }
    }

    private static final Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]+");

    private static final Pattern ILLEGAL_NAME = Pattern.compile("[^\\p{ASCII}&&[^(]]+");

    public static final int SELECTOR_LEN = 4;

    private final FunctionType type;
    private final String name;
    private final TupleType inputTypes;
    private final TupleType outputTypes;

    private final byte[] selector;
    private final String hashAlgorithm;

    private final String stateMutability;

    {
        selector = new byte[SELECTOR_LEN];
    }

    Function(FunctionType type, String name, TupleType inputTypes, TupleType outputTypes, String stateMutability, MessageDigest messageDigest) throws ParseException {
        this.type = Objects.requireNonNull(type);
        this.name = validateNameNullable(ILLEGAL_NAME, name);
        this.inputTypes = inputTypes != null ? inputTypes : TupleType.EMPTY;
        this.outputTypes = outputTypes != null ? outputTypes : TupleType.EMPTY;
        this.stateMutability = stateMutability;
        this.hashAlgorithm = messageDigest.getAlgorithm();
        generateSelector(messageDigest);
    }

    public Function(String signature) throws ParseException {
        this(signature, null);
    }

    public Function(String signature, String outputs) throws ParseException {
        this(FunctionType.FUNCTION, signature, outputs, newDefaultDigest());
    }

    public Function(String signature, String outputs, MessageDigest messageDigest) throws ParseException {
        this(FunctionType.FUNCTION, signature, outputs, messageDigest);
    }

    /**
     * @param functionType  to denote function, constructor, or fallback
     * @param signature the function signature
     * @param outputs   the signature of the tuple containing the return types
     * @param messageDigest the hash function with which to generate the 4-byte selector
     * @throws ParseException   if {@code signature} or {@code outputs} is malformed
     */
    public Function(FunctionType functionType, String signature, String outputs, MessageDigest messageDigest) throws ParseException {
        final int split = signature.indexOf('(');
        if(split < 0) {
            throw new ParseException("params start not found", signature.length());
        }
        final TupleType tupleType = TupleTypeParser.parseTupleType(signature.substring(split));

        this.type = Objects.requireNonNull(functionType);
        this.name = validateNameNonNull(NON_ASCII, signature.substring(0, split));
        this.inputTypes = tupleType;
        this.outputTypes = outputs != null ? TupleType.parse(outputs) : TupleType.EMPTY;
        this.stateMutability = null;
        this.hashAlgorithm = messageDigest.getAlgorithm();
        generateSelector(messageDigest);
    }

    private static String validateNameNullable(Pattern pattern, String name) throws ParseException {
        return name != null ? validateNameNonNull(pattern, name) : "";
    }

    private static String validateNameNonNull(Pattern pattern, String name) throws ParseException {
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            final char c = name.charAt(matcher.start());
            throw new ParseException(
                    "illegal char " + Utils.escapeChar(c) + " \'" + c + "\' @ index " + matcher.start(),
                    matcher.start()
            );
        }
        return name;
    }

    private void generateSelector(MessageDigest messageDigest) {
        try {
            messageDigest.reset();
            messageDigest.update(getCanonicalSignature().getBytes(Strings.CHARSET_UTF_8));
            messageDigest.digest(selector, 0, SELECTOR_LEN);
        } catch (DigestException de) {
            throw new RuntimeException(de);
        }
    }

    public String getCanonicalSignature() {
        return name + inputTypes.canonicalType;
    }

    public FunctionType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public TupleType getParamTypes() {
        return inputTypes;
    }

    public TupleType getOutputTypes() {
        return outputTypes;
    }

    public byte[] selector() {
        return Arrays.copyOf(selector, selector.length);
    }

    public String selectorHex() {
        return encode(selector, HEX);
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public String getStateMutability() {
        return stateMutability;
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

    public Tuple decodeReturn(byte[] returnVals) {
        return outputTypes.decode(returnVals);
    }

    public Tuple decodeReturn(ByteBuffer returnVals) {
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

    public static MessageDigest newDefaultDigest() {
        return new Keccak(256);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, name, inputTypes, outputTypes, hashAlgorithm, stateMutability);
        result = 31 * result + Arrays.hashCode(selector);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Function function = (Function) o;
        return type == function.type &&
                name.equals(function.name) &&
                inputTypes.equals(function.inputTypes) &&
                outputTypes.equals(function.outputTypes) &&
                Arrays.equals(selector, function.selector) &&
                hashAlgorithm.equals(function.hashAlgorithm) &&
                Objects.equals(stateMutability, function.stateMutability);
    }

    @Override
    public int objectType() {
        return ABIObject.FUNCTION;
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

    public static Function parse(String signature) throws ParseException {
        return new Function(signature);
    }

    public static Function fromJson(String functionJson) throws ParseException {
        return ContractJSONParser.parseFunction(functionJson);
    }

    public static Function fromJsonObject(JsonObject function) throws ParseException {
        return ContractJSONParser.parseFunction(function);
    }

    public static String hexOf(byte[] bytes) {
        return encode(bytes, HEX);
    }
}
