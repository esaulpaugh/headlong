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

import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonObject;
import com.joemelsha.crypto.hash.Keccak;

import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntFunction;

import static com.esaulpaugh.headlong.abi.ABIType.ID_LABEL_PADDED;
import static com.esaulpaugh.headlong.abi.ABIType.PADDED_LABEL_LEN;
import static com.esaulpaugh.headlong.abi.TypeEnum.ORDINAL_CONSTRUCTOR;
import static com.esaulpaugh.headlong.abi.TypeEnum.ORDINAL_ERROR;
import static com.esaulpaugh.headlong.abi.TypeEnum.ORDINAL_EVENT;
import static com.esaulpaugh.headlong.abi.TypeEnum.ORDINAL_FALLBACK;
import static com.esaulpaugh.headlong.abi.TypeEnum.ORDINAL_FUNCTION;
import static com.esaulpaugh.headlong.abi.TypeEnum.ORDINAL_RECEIVE;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

/**
 * Represents a function in an Ethereum contract. Can encode and decode calls matching this function's signature.
 * Can decode the function's return values.
 */
public final class Function implements ABIObject {

    public static final int SELECTOR_LEN = 4;
    private static final int MAX_NAME_CHARS = 2048;

    private final TypeEnum type;
    private final String name;
    private final TupleType<?> inputTypes;
    private final TupleType<?> outputTypes;
    private final String stateMutability;

    private final String hashAlgorithm;
    private final byte[] selector = new byte[SELECTOR_LEN];

    public Function(String signature) {
        this(signature, signature.indexOf('('), TupleType.EMPTY, ABIType.FLAGS_NONE);
    }

    public Function(String signature, String outputs) {
        this(ABIType.FLAGS_NONE, signature, outputs);
    }

    private Function(int flags, String signature, String outputs) {
        this(signature, signature.indexOf('('), outputs != null ? TupleType.parse(flags, outputs) : TupleType.empty(flags), flags);
    }

    private Function(final String signature, final int nameLength, final TupleType<?> outputs, final int flags) {
        this(
                TypeEnum.FUNCTION,
                signature.substring(0, nameLength),
                TupleType.parse(flags, signature.substring(nameLength)),
                outputs,
                null,
                Function.newDefaultDigest()
        );
    }

    /**
     * @param type          enum denoting one of: function, receive, fallback, constructor
     * @param name          this function's name, being the first part of the function signature
     * @param inputs        {@link TupleType} describing this function's input parameters
     * @param outputs       {@link TupleType} describing this function's return types
     * @param stateMutability   "pure", "view", "payable" etc.
     * @param messageDigest hash function with which to generate the 4-byte selector
     * @throws IllegalArgumentException if the arguments do not specify a valid function
     */
    public Function(TypeEnum type, String name, TupleType<?> inputs, TupleType<?> outputs, String stateMutability, MessageDigest messageDigest) {
        this.type = Objects.requireNonNull(type);
        this.name = name != null ? validateName(name) : null;
        this.inputTypes = Objects.requireNonNull(inputs);
        this.outputTypes = Objects.requireNonNull(outputs);
        this.stateMutability = stateMutability;
        this.hashAlgorithm = Objects.requireNonNull(messageDigest.getAlgorithm());
        validateFunction();
        generateSelector(messageDigest);
    }

    @Override
    public TypeEnum getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Tuple> TupleType<T> getInputs() {
        return (TupleType<T>) inputTypes;
    }

    @SuppressWarnings("unchecked")
    public <T extends Tuple> TupleType<T> getOutputs() {
        return (TupleType<T>) outputTypes;
    }

    public String getStateMutability() {
        return stateMutability;
    }

    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    public byte[] selector() {
        return Arrays.copyOf(selector, selector.length);
    }

    public String selectorHex() {
        return Strings.encode(selector);
    }

    @Override
    public String getCanonicalSignature() {
        return name != null
                ? name + inputTypes.canonicalType
                : inputTypes.canonicalType;
    }

    private void validateFunction() {
        switch (type.ordinal()) {
        case ORDINAL_FUNCTION:
            if (name == null) {
                throw validationErr("define name");
            }
            return;
        case ORDINAL_RECEIVE:
            if (!ABIJSON.PAYABLE.equals(stateMutability)) {
                throw validationErr("define stateMutability as \"" + ABIJSON.PAYABLE + '"');
            }
            /* fall through */
        case ORDINAL_FALLBACK:
            if (!inputTypes.isEmpty()) {
                throw validationErr("define no inputs");
            }
            /* fall through */
        case ORDINAL_CONSTRUCTOR:
            if (!outputTypes.isEmpty()) {
                throw validationErr("define no outputs");
            }
            if (name != null) {
                throw validationErr("not define name");
            }
            return;
        case ORDINAL_EVENT:
        case ORDINAL_ERROR:
        default: throw TypeEnum.unexpectedType(type.toString());
        }
    }

    private IllegalArgumentException validationErr(String typeRuleStr) {
        return new IllegalArgumentException("type is \"" + type + "\"; functions of this type must " + typeRuleStr);
    }

    private void generateSelector(MessageDigest messageDigest) {
        messageDigest.reset();
        messageDigest.update(Strings.decode(getCanonicalSignature(), Strings.ASCII));
        try {
            messageDigest.digest(selector, 0, SELECTOR_LEN);
        } catch (DigestException de) {
            throw new AssertionError(de);
        }
    }

    private int validatedCallLength(Tuple args) {
        return Function.SELECTOR_LEN + inputTypes.validate(args);
    }

    public int measureCallLength(Tuple args) {
        return validatedCallLength(args);
    }

    public ByteBuffer encodeCallWithArgs(Object... args) {
        return encodeCall(new Tuple(args));
    }

    public ByteBuffer encodeCall(Tuple args) {
        ByteBuffer dest = ByteBuffer.allocate(validatedCallLength(args)); // ByteOrder.BIG_ENDIAN by default
        dest.put(selector);
        inputTypes.encodeTail(args, dest);
        dest.flip();
        return dest;
    }

    public void encodeCall(Tuple args, ByteBuffer dest) {
        inputTypes.validate(args);
        dest.put(selector);
        inputTypes.encodeTail(args, dest);
    }

    @SuppressWarnings("unchecked")
    public <T extends Tuple> T decodeCall(byte[] call) {
        checkSelector(Arrays.copyOf(call, SELECTOR_LEN));
        return (T) inputTypes.decode(call, SELECTOR_LEN, call.length - SELECTOR_LEN);
    }

    /**
     * The inverse of {@link #encodeCall}.
     *
     * @param buffer the encoded function call
     * @return  the decoded arguments
     */
    @SuppressWarnings("unchecked")
    public <T extends Tuple> T decodeCall(ByteBuffer buffer) {
        checkSelector(buffer);
        return (T) inputTypes.decode(buffer);
    }

    public <T> T decodeCall(byte[] call, int... indices) {
        return decodeCall(ByteBuffer.wrap(call), indices);
    }

    public <T> T decodeCall(ByteBuffer buffer, int... indices) {
        checkSelector(buffer);
        return inputTypes.decode(buffer, indices);
    }

    private void checkSelector(ByteBuffer bb) {
        final byte[] four = new byte[SELECTOR_LEN];
        bb.get(four, 0, four.length);
        checkSelector(four);
    }

    private void checkSelector(byte[] found) {
        if (!MessageDigest.isEqual(found, selector)) {
                throw new IllegalArgumentException("given selector does not match: expected: " + selectorHex()
                        + ", found: " + Strings.encode(found));
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Tuple> T decodeReturn(byte[] returnVals) {
        return (T) outputTypes.decode(returnVals);
    }

    @SuppressWarnings("unchecked")
    public <T extends Tuple> T decodeReturn(ByteBuffer buf) {
        return (T) outputTypes.decode(buf);
    }

    public <T> T decodeReturn(byte[] returnVals, int... indices) {
        return decodeReturn(ByteBuffer.wrap(returnVals), indices);
    }

    /**
     * Decodes and returns the elements at the specified indices.
     * NOTE: This method does not advance the {@link ByteBuffer}'s {@code position}.
     *
     * @param buf   the buffer containing the return values
     * @param indices   the index of each of the elements to decode, in ascending order and between 0 (inclusive) and
     *                  {@code getOutputs().size()} (exclusive)
     * @param <T>   {@link Tuple} if decoding multiple elements
     * @return  the decoded elements
     */
    public <T> T decodeReturn(ByteBuffer buf, int... indices) {
        return outputTypes.decode(buf, indices);
    }

    @SuppressWarnings("unchecked")
    public <J> J decodeSingletonReturn(byte[] singleton) {
        if (outputTypes.size() == 1) {
            return (J) outputTypes.get(0).decode(singleton);
        }
        throw new IllegalArgumentException("return type not a singleton: " + outputTypes.canonicalType);
    }

    @SuppressWarnings("unchecked")
    public <J> J decodeSingletonReturn(ByteBuffer buf) {
        if (outputTypes.size() == 1) {
            return (J) outputTypes.get(0).decode(buf);
        }
        throw new IllegalArgumentException("return type not a singleton: " + outputTypes.canonicalType);
    }

    @Override
    public boolean isFunction() {
        return true;
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(type, name, inputTypes, outputTypes, stateMutability, hashAlgorithm)
                + Arrays.hashCode(selector);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Function)) return false;
        Function other = (Function) o;
        return other.type == this.type &&
                Objects.equals(other.name, this.name) &&
                other.inputTypes.equals(this.inputTypes) &&
                other.outputTypes.equals(this.outputTypes) &&
                Objects.equals(other.stateMutability, this.stateMutability) &&
                other.hashAlgorithm.equals(this.hashAlgorithm) &&
                MessageDigest.isEqual(other.selector, this.selector);
    }

    @Override
    public String toString() {
        return toJson(true);
    }

    private static String validateName(String input) {
        final int len = input.length();
        if (len > MAX_NAME_CHARS) {
            throw new IllegalArgumentException("function name is too long: " + len + " > " + MAX_NAME_CHARS);
        }
        for (int i = 0; i < len; i++) {
            final char c = input.charAt(i);
            if (c >= 0x80 || c == '(') {
                throw new IllegalArgumentException("illegal char 0x" + Integer.toHexString(c) + " '" + c + "' @ index " + i);
            }
        }
        return input;
    }

    /**
     * Experimental. Annotates the given function call and returns an informational formatted String. This method is
     * subject to change or removal in a future release.
     */
    public String annotateCall(byte[] call) {
        return annotateCall(decodeCall(call));
    }

    /**
     * Experimental. Annotates the function call given the {@code args} and returns an informational formatted String. This
     * method is subject to change or removal in a future release.
     */
    public String annotateCall(Tuple args) {
        return inputTypes.annotate(
                args,
                new StringBuilder(768).append(name).append(":\n")
                        .append(ID_LABEL_PADDED).append(selectorHex())
        );
    }
// ---------------------------------------------------------------------------------------------------------------------
    public static Function parse(String signature) {
        return new Function(signature);
    }

    public static Function parse(String signature, String outputs) {
        return new Function(signature, outputs);
    }

    public static Function parse(int flags, String signature, String outputs) {
        return new Function(flags, signature, outputs);
    }

    /**
     * @return a {@link MessageDigest}
     */
    public static MessageDigest newDefaultDigest() {
        return new Keccak(256); // replace this with your preferred impl
    }

    public static String formatCall(byte[] call) {
        return formatCall(call, (int row) -> ABIType.padLabel(0, Integer.toString(row)));
    }

    /**
     * Returns a formatted string for a given ABI-encoded function call.
     *
     * @param buffer the buffer containing the ABI call
     * @param labeler code to generate the row label
     * @return the formatted string
     * @throws IllegalArgumentException if {@code length} mod 32 is not equal to four
     */
    public static String formatCall(byte[] buffer, IntFunction<String> labeler) {
        final int bodyLen = buffer.length - SELECTOR_LEN;
        Integers.checkIsMultiple(bodyLen, UNIT_LENGTH_BYTES);
        return ABIType.finishFormat(
                buffer,
                SELECTOR_LEN,
                buffer.length,
                labeler,
                new StringBuilder(PADDED_LABEL_LEN + (SELECTOR_LEN * FastHex.CHARS_PER_BYTE) + (bodyLen / UNIT_LENGTH_BYTES) * ABIType.CHARS_PER_LINE)
                        .append(ID_LABEL_PADDED)
                        .append(FastHex.encodeToString(buffer, 0, SELECTOR_LEN))
        );
    }

    public static Function fromJson(String objectJson) {
        return fromJsonObject(ABIType.FLAGS_NONE, ABIJSON.parseObject(objectJson));
    }

    /** @see ABIObject#fromJson(int, String) */
    public static Function fromJson(int flags, String objectJson) {
        return fromJsonObject(flags, ABIJSON.parseObject(objectJson));
    }

    /** @see ABIObject#fromJsonObject(int, JsonObject) */
    public static Function fromJsonObject(int flags, JsonObject function) {
        return fromJsonObject(flags, function, Function.newDefaultDigest());
    }

    public static Function fromJsonObject(int flags, JsonObject function, MessageDigest digest) {
        return ABIJSON.parseFunction(function, digest, flags);
    }
}
