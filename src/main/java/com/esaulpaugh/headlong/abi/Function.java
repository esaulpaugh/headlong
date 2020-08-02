/*
   Copyright 2019 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.WrappedKeccak;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.JsonUtils;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonObject;
import com.joemelsha.crypto.hash.Keccak;

import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

/**
 * Represents a function in an Ethereum contract. Can encode and decode calls matching this function's signature.
 * Can decode the function's return values.
 */
public final class Function implements ABIObject {

    /** The various variants of {@link Function}. */
    public enum Type {

        FUNCTION,
        RECEIVE,
        FALLBACK,
        CONSTRUCTOR;

        @Override
        public String toString() {
            return toString(this);
        }

        public static String toString(Function.Type type) {
            switch (type) {
            case FUNCTION: return ABIJSON.FUNCTION;
            case RECEIVE: return ABIJSON.RECEIVE;
            case FALLBACK: return ABIJSON.FALLBACK;
            case CONSTRUCTOR: return ABIJSON.CONSTRUCTOR;
            default: throw new Error();
            }
        }
    }

    private static final Pattern ALL_ASCII_NO_OPEN_PAREN = Pattern.compile("^[[^(]&&\\p{ASCII}]*$");
    private static final Pattern OPEN_PAREN_OR_NON_ASCII = Pattern.compile("[([^\\p{ASCII}]]");

    public static final int SELECTOR_LEN = 4;

    private final Type type;
    private final String name;
    private final TupleType inputTypes;
    private final TupleType outputTypes;
    private final String stateMutability;

    private final byte[] selector = new byte[SELECTOR_LEN];
    private final String hashAlgorithm;

    public Function(String signature) {
        this(signature, null);
    }

    public Function(String signature, String outputs) {
        this(Type.FUNCTION, signature, outputs, newDefaultDigest());
    }

    public Function(String signature, String outputs, MessageDigest messageDigest) {
        this(Type.FUNCTION, signature, outputs, messageDigest);
    }

    /**
     * @param type          to denote function, receive, fallback, or constructor
     * @param signature     the function's signature e.g. "foo(int,bool)"
     * @param outputs       the signature of the tuple containing this function's return types
     * @param messageDigest the hash function with which to generate the 4-byte selector
     * @throws IllegalArgumentException if {@code signature} or {@code outputs} is malformed
     */
    public Function(Type type, String signature, String outputs, MessageDigest messageDigest) {
        final int split = signature.indexOf('(');
        if (split >= 0) {
            try {
                this.inputTypes = (TupleType) TypeFactory.create(signature.substring(split), null);
            } catch (ClassCastException cce) {
                throw new IllegalArgumentException("illegal signature termination", cce); // e.g. "foo()[]"
            }
            this.type = Objects.requireNonNull(type);
            String name = Utils.regexValidate(ALL_ASCII_NO_OPEN_PAREN, OPEN_PAREN_OR_NON_ASCII, signature.substring(0, split)); // guaranteed not to contain '(' bc of split
            this.name = name.isEmpty() && (type == Type.FALLBACK || type == Type.CONSTRUCTOR) ? null : name;
            this.outputTypes = outputs != null ? TupleType.parse(outputs) : TupleType.EMPTY;
            this.stateMutability = null;
            this.hashAlgorithm = messageDigest.getAlgorithm();
            validateFunction();
            generateSelector(messageDigest);
        } else {
            throw new IllegalArgumentException("params start not found");
        }
    }

    public Function(Type type, String name, TupleType inputTypes, TupleType outputTypes, String stateMutability, MessageDigest messageDigest) {
        this.type = Objects.requireNonNull(type);
        this.name = name != null ? Utils.regexValidate(ALL_ASCII_NO_OPEN_PAREN, OPEN_PAREN_OR_NON_ASCII, name) : null;
        this.inputTypes = Objects.requireNonNull(inputTypes);
        this.outputTypes = Objects.requireNonNull(outputTypes);
        this.stateMutability = stateMutability;
        this.hashAlgorithm = messageDigest.getAlgorithm();
        validateFunction();
        generateSelector(messageDigest);
    }

    public Type getType() {
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

    public String getStateMutability() {
        return stateMutability;
    }

    public byte[] selector() {
        return Arrays.copyOf(selector, selector.length);
    }

    public String selectorHex() {
        return Strings.encode(selector);
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    @Override
    public String getCanonicalSignature() {
        return name != null
                ? name + inputTypes.canonicalType
                : inputTypes.canonicalType;
    }

    private void validateFunction() {
        switch (type) {
        case FUNCTION:
            if(name == null) {
                throw nameNullabilityException(false);
            }
            break;
        case RECEIVE:
            if (!ABIJSON.RECEIVE.equals(name)) {
                throw new IllegalArgumentException("functions of this type must be named \"" + ABIJSON.RECEIVE + "\"");
            }
            if (!ABIJSON.PAYABLE.equals(stateMutability)) {
                throw new IllegalArgumentException("functions of this type must be " + ABIJSON.PAYABLE);
            }
            /* fall through */
        case FALLBACK:
            assertNoElements(inputTypes, "inputs");
            /* fall through */
        case CONSTRUCTOR:
            assertNoElements(outputTypes, "outputs");
            if (type != Type.RECEIVE && name != null) {
                throw nameNullabilityException(true);
            }
            /* fall through */
        default:
        }
    }

    private static IllegalArgumentException nameNullabilityException(boolean mustBeNull) {
        return new IllegalArgumentException("functions of this type must be " + (mustBeNull ? "un" : "") + "named");
    }

    private static void assertNoElements(TupleType tupleType, String description) {
        if(tupleType.elementTypes.length > 0) {
            throw new IllegalArgumentException("functions of this type cannot have " + description);
        }
    }

    private void generateSelector(MessageDigest messageDigest) {
        messageDigest.reset();
        messageDigest.update(Strings.decode(getCanonicalSignature(), Strings.UTF_8));
        try {
            messageDigest.digest(selector, 0, SELECTOR_LEN);
        } catch (DigestException de) {
            throw new RuntimeException(de);
        }
    }

    public int measureCallLength(Tuple args) {
        return Function.SELECTOR_LEN + inputTypes.measureEncodedLength(args);
    }

    public ByteBuffer encodeCallWithArgs(Object... args) {
        return encodeCall(new Tuple(args));
    }

    public ByteBuffer encodeCall(Tuple args) {
        ByteBuffer dest = ByteBuffer.wrap(new byte[measureCallLength(args)]); // ByteOrder.BIG_ENDIAN by default
        encodeCall(args, dest);
        return dest;
    }

    public Function encodeCall(Tuple args, ByteBuffer dest) {
        inputTypes.validate(args);
        dest.put(selector);
        inputTypes.encodeTail(args, dest);
        return this;
    }

    public Tuple decodeCall(byte[] array) {
        return decodeCall(ByteBuffer.wrap(array));
    }

    public Tuple decodeCall(ByteBuffer abiBuffer) {
        final byte[] unitBuffer = ABIType.newUnitBuffer();
        abiBuffer.get(unitBuffer, 0, SELECTOR_LEN);
        for(int i = 0; i < SELECTOR_LEN; i++) {
            if(unitBuffer[i] != selector[i]) {
                throw new IllegalArgumentException("given selector does not match: expected: " + selectorHex()
                        + ", found: " + Strings.encode(unitBuffer, 0, SELECTOR_LEN, Strings.HEX));
            }
        }
        return inputTypes.decode(abiBuffer, unitBuffer);
    }

    public Tuple decodeReturn(byte[] returnVals) {
        return outputTypes.decode(returnVals);
    }

    public Tuple decodeReturn(ByteBuffer returnVals) {
        return outputTypes.decode(returnVals);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(type, name, inputTypes, outputTypes, hashAlgorithm, stateMutability)
                + Arrays.hashCode(selector);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Function function = (Function) o;
        return type == function.type &&
                Objects.equals(name, function.name) &&
                inputTypes.equals(function.inputTypes) &&
                outputTypes.equals(function.outputTypes) &&
                Arrays.equals(selector, function.selector) &&
                hashAlgorithm.equals(function.hashAlgorithm) &&
                Objects.equals(stateMutability, function.stateMutability);
    }

    @Override
    public String toString() {
        return toJson(true);
    }

    @Override
    public String toJson(boolean pretty) {
        return ABIJSON.toJson(this, pretty);
    }
// ---------------------------------------------------------------------------------------------------------------------
    public static Function parse(String signature) {
        return new Function(signature);
    }

    public static Function parse(String signature, MessageDigest messageDigest) {
        return new Function(signature, null, messageDigest);
    }

    public static Function fromJson(String objectJson) {
        return fromJsonObject(JsonUtils.parseObject(objectJson));
    }

    public static Function fromJson(String objectJson, MessageDigest messageDigest) {
        return fromJsonObject(JsonUtils.parseObject(objectJson), messageDigest);
    }

    public static Function fromJsonObject(JsonObject function) {
        return fromJsonObject(function, Function.newDefaultDigest());
    }

    public static Function fromJsonObject(JsonObject function, MessageDigest messageDigest) {
        return ABIJSON.parseFunction(function, messageDigest);
    }

    /**
     * @return a {@link MessageDigest}
     * @see WrappedKeccak
     */
    public static MessageDigest newDefaultDigest() {
        return new Keccak(256); // replace this with your preferred impl
    }

    public static String formatCall(byte[] abiCall) {
        return formatCall(abiCall, 0, abiCall.length);
    }

    public static String formatCall(byte[] buffer, int offset, final int length) {
        return formatCall(buffer, offset, length, new TupleType.LabelMaker());
    }

    /**
     * Returns a formatted string for a given ABI-encoded function call.
     *
     * @param buffer the buffer containing the ABI call
     * @param offset the offset into the input buffer of the ABI call
     * @param length the length of the ABI call
     * @param labelMaker code to generate the row labels
     * @return the formatted string
     * @throws IllegalArgumentException if the input length mod 32 != 4
     */
    public static String formatCall(byte[] buffer, int offset, final int length, TupleType.LabelMaker labelMaker) {
        Integers.checkIsMultiple(length - 4, UNIT_LENGTH_BYTES);
        StringBuilder sb = new StringBuilder();
        sb.append("ID");
        int n = 9 /* arbitrary magic number */ - "ID".length();
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }
        sb.append(Strings.encode(buffer, offset, SELECTOR_LEN, Strings.HEX));
        int idx = offset + SELECTOR_LEN;
        while (idx < length) {
            sb.append('\n');
            sb.append(labelMaker.make(idx));
            sb.append(Strings.encode(buffer, idx, UNIT_LENGTH_BYTES, Strings.HEX));
            idx += UNIT_LENGTH_BYTES;
        }
        return sb.toString();
    }
}
