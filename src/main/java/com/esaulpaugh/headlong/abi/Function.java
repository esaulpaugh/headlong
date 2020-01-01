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

import com.esaulpaugh.headlong.util.JsonUtils;
import com.esaulpaugh.headlong.abi.util.Utils;
import com.google.gson.JsonObject;
import com.joemelsha.crypto.hash.Keccak;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.encode;

/**
 * Represents a function in an Ethereum contract. Can encode and decode calls matching this function's signature.
 * Can decode the function's return values.
 */
public final class Function implements ABIObject, Serializable {

    public enum Type {

        FALLBACK,
        CONSTRUCTOR,
        FUNCTION,
        RECEIVE;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        static Type get(String value) {
            if(value != null) {
                switch (value) {
                case ABIJSON.FALLBACK: return Type.FALLBACK;
                case ABIJSON.CONSTRUCTOR: return Type.CONSTRUCTOR;
                case ABIJSON.FUNCTION: return Type.FUNCTION;
                case ABIJSON.RECEIVE: return RECEIVE;
                }
            }
            return value == null ? Type.FUNCTION : null;
        }
    }

    private static final Pattern NON_ASCII_CHAR = Pattern.compile("[^\\p{ASCII}]+");

    private static final Pattern ILLEGAL_NAME_CHAR = Pattern.compile("[^\\p{ASCII}&&[^(]]+");

    public static final int SELECTOR_LEN = 4;

    private final Type type;
    private final String name;
    private final TupleType inputTypes;
    private final TupleType outputTypes;

    private final byte[] selector = new byte[SELECTOR_LEN];
    private final String hashAlgorithm;

    private final String stateMutability;

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
     * @param type  to denote function, constructor, or fallback
     * @param signature the function signature
     * @param outputs   the signature of the tuple containing the return types
     * @param messageDigest the hash function with which to generate the 4-byte selector
     * @throws IllegalArgumentException   if {@code signature} or {@code outputs} is malformed
     */
    public Function(Type type, String signature, String outputs, MessageDigest messageDigest) {
        try {
            final int split = signature.indexOf('(');
            if (split < 0) {
                throw new ParseException("params start not found", signature.length());
            }
            final TupleType tupleType;
            try {
                tupleType = (TupleType) TypeFactory.create(signature.substring(split));
            } catch (ClassCastException cce) {
                throw new ParseException("illegal signature termination", signature.length()); // e.g. "foo()[]"
            }

            this.type = Objects.requireNonNull(type);
            String name = Utils.validateChars(NON_ASCII_CHAR, signature.substring(0, split));
            this.name = name.isEmpty() && (type == Type.FALLBACK || type == Type.CONSTRUCTOR) ? null : name;
            this.inputTypes = tupleType;
            this.outputTypes = outputs != null ? TupleType.parse(outputs) : TupleType.EMPTY;
            this.stateMutability = null;
            this.hashAlgorithm = messageDigest.getAlgorithm();
            validateFunction();
            generateSelector(messageDigest);
        } catch (ParseException pe) {
            throw new IllegalArgumentException(pe);
        }
    }

    public Function(Type type, String name, TupleType inputTypes, TupleType outputTypes, String stateMutability, MessageDigest messageDigest) {
        try {
            this.type = Objects.requireNonNull(type);
            this.name = name != null ? Utils.validateChars(ILLEGAL_NAME_CHAR, name) : null;
            this.inputTypes = Objects.requireNonNull(inputTypes);
            this.outputTypes = Objects.requireNonNull(outputTypes);
            this.stateMutability = stateMutability;
            this.hashAlgorithm = messageDigest.getAlgorithm();
            validateFunction();
            generateSelector(messageDigest);
        } catch (ParseException pe) {
            throw new IllegalArgumentException(pe);
        }
    }

    private void generateSelector(MessageDigest messageDigest) {
        try {
            messageDigest.reset();
            messageDigest.update(getCanonicalSignature().getBytes(StandardCharsets.UTF_8));
            messageDigest.digest(selector, 0, SELECTOR_LEN);
        } catch (DigestException de) {
            throw new RuntimeException(de);
        }
    }

    private void validateFunction() {
        switch (type) {
        case FUNCTION:
            assertNameNullability(name, false);
            break;
        case RECEIVE:
            if (!ABIJSON.RECEIVE.equals(name)) {
                throw new IllegalArgumentException("functions of this type must be named \"" + ABIJSON.RECEIVE + "\"");
            }
            if (!ABIJSON.PAYABLE.equals(stateMutability)) {
                throw new IllegalArgumentException("functions of this type must be " + ABIJSON.PAYABLE);
            }
            /* falls through */
        case FALLBACK:
            assertNoElements(inputTypes, "inputs");
            /* falls through */
        case CONSTRUCTOR:
            assertNoElements(outputTypes, "outputs");
            if (type != Type.RECEIVE) {
                assertNameNullability(name, true);
            }
        }
    }

    private static void assertNameNullability(String name, boolean _null) {
        if(_null ^ (name == null)) { // if not matching
            throw new IllegalArgumentException("functions of this type must be " + (_null ? "un" : "") + "named");
        }
    }

    private static void assertNoElements(TupleType tupleType, String description) {
        if(tupleType.elementTypes.length > 0) {
            throw new IllegalArgumentException("functions of this type cannot have " + description);
        }
    }

    public String getCanonicalSignature() {
        if(name == null) {
            return inputTypes.canonicalType;
        }
        return name + inputTypes.canonicalType;
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

    public Tuple decodeReturn(byte[] returnVals) throws ABIException { // TODO allow decoding of non-calls without a Function
        return outputTypes.decode(returnVals);
    }

    public Tuple decodeReturn(ByteBuffer returnVals) throws ABIException {
        return outputTypes.decode(returnVals);
    }

    public int measureCallLength(Tuple args) throws ABIException {
        return Function.SELECTOR_LEN + inputTypes.validate(args);
    }

    public ByteBuffer encodeCallWithArgs(Object... args) throws ABIException {
        return encodeCall(new Tuple(args));
    }

    public ByteBuffer encodeCall(Tuple args) throws ABIException {
        ByteBuffer dest = ByteBuffer.wrap(new byte[measureCallLength(args)]); // ByteOrder.BIG_ENDIAN by default
        encodeCall(args, dest);
        return dest;
    }

    public Function encodeCall(Tuple args, ByteBuffer dest) throws ABIException {
        inputTypes.validate(args);
        dest.put(selector);
        inputTypes.encodeTail(args, dest);
        return this;
    }

    public Tuple decodeCall(byte[] array) throws ABIException {
        return decodeCall(ByteBuffer.wrap(array));
    }

    public Tuple decodeCall(ByteBuffer abiBuffer) throws ABIException {
        final byte[] unitBuffer = ABIType.newUnitBuffer();
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
        return new Keccak(256); // replace this with your preferred impl
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

    public static Function parse(String signature) {
        return new Function(signature);
    }

    public static Function fromJson(String functionJson) throws ParseException {
        return ABIJSON.parseFunction(functionJson);
    }

    public static Function fromJsonObject(JsonObject function) throws ParseException {
        return ABIJSON.parseFunction(function);
    }

    @Override
    public String toJson(boolean pretty) {
        JsonObject object = ABIJSON.buildFunctionJson(this);
        return pretty ? JsonUtils.toPrettyPrint(object) : object.toString();
    }

    @Override
    public String toString() {
        return toJson(true);
    }
// ---------------------------------------------------------------------------------------------------------------------
    public static String hexOf(byte[] bytes) {
        return encode(bytes, HEX);
    }

    public static String hexOf(ByteBuffer buffer) {
        return encode(buffer.array(), HEX);
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
}
