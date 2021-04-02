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
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.esaulpaugh.headlong.abi.ABIJSON.CONSTRUCTOR;
import static com.esaulpaugh.headlong.abi.ABIJSON.EVENT;
import static com.esaulpaugh.headlong.abi.ABIJSON.FALLBACK;
import static com.esaulpaugh.headlong.abi.ABIJSON.FUNCTION;
import static com.esaulpaugh.headlong.abi.ABIJSON.RECEIVE;
import static com.esaulpaugh.headlong.abi.UnitType.UNIT_LENGTH_BYTES;

/**
 * Represents a function in an Ethereum contract. Can encode and decode calls matching this function's signature.
 * Can decode the function's return values.
 */
public final class Function implements ABIObject {

    private static final Pattern ALL_ASCII_NO_OPEN_PAREN = Pattern.compile("^[[^(]&&\\p{ASCII}]*$");
    private static final Pattern OPEN_PAREN_OR_NON_ASCII = Pattern.compile("[([^\\p{ASCII}]]");

    public static final int SELECTOR_LEN = 4;

    private final TypeEnum type;
    private final String name;
    private final TupleType inputTypes;
    private final TupleType outputTypes;
    private final String stateMutability;

    private final byte[] selector;
    private final String hashAlgorithm;

    { // instance initializer
        this.selector = new byte[SELECTOR_LEN];
    }

    public Function(String signature) {
        this(signature, null);
    }

    public Function(String signature, String outputs) {
        this(
                TypeEnum.FUNCTION,
                signature.substring(0, signature.indexOf('(')),
                TupleType.parse(signature.substring(signature.indexOf('('))),
                outputs != null ? TupleType.parse(outputs) : TupleType.EMPTY,
                null,
                Function.newDefaultDigest()
        );
    }

    /**
     * @param type          enum denoting one of: function, receive, fallback, or constructor
     * @param name          this function's name
     * @param inputs        {@link TupleType} describing this function's input parameters
     * @param outputs       {@link TupleType} type describing this function's return types
     * @param stateMutability   "pure", "view", "payable" etc.
     * @param messageDigest hash function with which to generate the 4-byte selector
     * @throws IllegalArgumentException if {@code signature} or {@code outputs} is malformed
     */
    public Function(TypeEnum type, String name, TupleType inputs, TupleType outputs, String stateMutability, MessageDigest messageDigest) {
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

    @Override
    public TupleType getInputs() {
        return inputTypes;
    }

    public TupleType getOutputs() {
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
        switch (type.name) {
        case EVENT:
            throw TypeEnum.unexpectedType(type.toString());
        case FUNCTION:
            if(name == null) {
                throw validationErr("define name");
            }
            return;
        case RECEIVE:
            if (!RECEIVE.equals(name)) {
                throw validationErr("define name as \"" + RECEIVE + '"');
            }
            if (!ABIJSON.PAYABLE.equals(stateMutability)) {
                throw validationErr("define stateMutability as \"" + ABIJSON.PAYABLE + '"');
            }
            /* fall through */
        case FALLBACK:
            if(inputTypes.size() != 0) {
                throw validationErr("define no inputs");
            }
            /* fall through */
        case CONSTRUCTOR:
            if(outputTypes.size() != 0) {
                throw validationErr("define no outputs");
            }
            if (name != null && type != TypeEnum.RECEIVE) {
                throw validationErr("not define name");
            }
            return;
        default: throw new Error();
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
            throw new Error(de);
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
        ByteBuffer dest = ByteBuffer.wrap(new byte[validatedCallLength(args)]); // ByteOrder.BIG_ENDIAN by default
        dest.put(selector);
        inputTypes.encodeTail(args, dest);
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

    /**
     * The inverse of {@link #encodeCall}.
     *
     * @param abiBuffer the encoded function call
     * @return  the decoded arguments
     */
    public Tuple decodeCall(ByteBuffer abiBuffer) {
        final byte[] unitBuffer = ABIType.newUnitBuffer();
        abiBuffer.get(unitBuffer, 0, SELECTOR_LEN);
        for(int i = 0; i < SELECTOR_LEN; i++) {
            if(unitBuffer[i] != selector[i]) {
                throw new IllegalArgumentException("given selector does not match: expected: " + selectorHex()
                        + ", found: " + Strings.encode(unitBuffer, 0, SELECTOR_LEN, Strings.HEX));
            }
        }
        return inputTypes.decode(abiBuffer, unitBuffer); // unitBuffer contents are ignored, overwritten during decode
    }

    public Tuple decodeReturn(byte[] returnVals) {
        return outputTypes.decode(returnVals);
    }

    public Tuple decodeReturn(ByteBuffer returnVals) {
        return outputTypes.decode(returnVals);
    }

    @SuppressWarnings("unchecked")
    public <J> J decodeSingletonReturn(byte[] singleton) {
        return (J) outputTypes.get(0).decode(singleton);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(type, name, inputTypes, outputTypes, hashAlgorithm, stateMutability)
                + Arrays.hashCode(selector);
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof Function)) return false;
        Function other = (Function) o;
        return other.type == this.type &&
                Objects.equals(other.name, this.name) &&
                other.inputTypes.equals(this.inputTypes) &&
                other.outputTypes.equals(this.outputTypes) &&
                Arrays.equals(other.selector, this.selector) &&
                other.hashAlgorithm.equals(this.hashAlgorithm) &&
                Objects.equals(other.stateMutability, this.stateMutability);
    }

    @Override
    public String toString() {
        return toJson(true);
    }

    @Override
    public String toJson(boolean pretty) {
        return ABIJSON.toJson(this, true, pretty);
    }

    private static String validateName(String input) {
        if(ALL_ASCII_NO_OPEN_PAREN.matcher(input).matches()) {
            return input;
        }
        Matcher badChar = OPEN_PAREN_OR_NON_ASCII.matcher(input);
        if (badChar.find()) {
            int idx = badChar.start();
            char c = input.charAt(idx);
            throw new IllegalArgumentException("illegal char 0x" + Integer.toHexString(c) + " '" + c + "' @ index " + idx);
        }
        throw new Error("regex mismatch");
    }
// ---------------------------------------------------------------------------------------------------------------------
    public static Function parse(String signature) {
        return new Function(signature);
    }

    public static Function parse(String signature, String outputs) {
        return new Function(signature, outputs);
    }

    public static Function fromJson(String objectJson) {
        return fromJsonObject(JsonUtils.parseObject(objectJson));
    }

    public static Function fromJsonObject(JsonObject function) {
        return ABIJSON.parseFunction(function);
    }

    /**
     * @return a {@link MessageDigest}
     */
    public static MessageDigest newDefaultDigest() {
        return new Keccak(256); // replace this with your preferred impl
    }

    public static String formatCall(byte[] abiCall) {
        return formatCall(abiCall, 0, abiCall.length);
    }

    public static String formatCall(byte[] buffer, int offset, final int length) {
        return formatCall(buffer, offset, length, (row) -> TupleType.pad(0, "" + row));
    }

    /**
     * Returns a formatted string for a given ABI-encoded function call.
     *
     * @param buffer the buffer containing the ABI call
     * @param offset the offset into the input buffer of the ABI call
     * @param length the length of the ABI call
     * @param labeler code to generate the row label
     * @return the formatted string
     * @throws IllegalArgumentException if the input length mod 32 != 4
     */
    public static String formatCall(byte[] buffer, int offset, final int length, IntFunction<String> labeler) {
        Integers.checkIsMultiple(length - SELECTOR_LEN, UNIT_LENGTH_BYTES);
        StringBuilder sb = new StringBuilder(TupleType.pad(0, "ID"))
                .append(Strings.encode(buffer, offset, SELECTOR_LEN, Strings.HEX));
        return ABIType.finishFormat(buffer, offset + SELECTOR_LEN, offset + length, labeler, sb);
    }
}
