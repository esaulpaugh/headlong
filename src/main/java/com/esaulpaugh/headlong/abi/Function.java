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

import com.esaulpaugh.headlong.util.Strings;
import com.esaulpaugh.headlong.abi.util.Utils;
import com.google.gson.JsonObject;
import com.joemelsha.crypto.hash.Keccak;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.Arrays;
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
        FUNCTION;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        static Type get(String value) {
            if(value != null) {
                switch (value) {
                case ContractJSONParser.FALLBACK: return Type.FALLBACK;
                case ContractJSONParser.CONSTRUCTOR: return Type.CONSTRUCTOR;
                case ContractJSONParser.FUNCTION: return Type.FUNCTION;
                }
            }
            return null;
        }
    }

    private static final Pattern NON_ASCII_CHAR = Pattern.compile("[^\\p{ASCII}]+");

    private static final Pattern ILLEGAL_NAME_CHAR = Pattern.compile("[^\\p{ASCII}&&[^(]]+");

    public static final int SELECTOR_LEN = 4;

    private final Type type;
    private final String name;
    private final TupleType inputTypes;
    private final TupleType outputTypes;

    private final byte[] selector;
    private final String hashAlgorithm;

    private final String stateMutability;

    {
        selector = new byte[SELECTOR_LEN];
    }

    Function(Type type, String name, TupleType inputTypes, TupleType outputTypes, String stateMutability, MessageDigest messageDigest) throws ParseException {
        this.type = Objects.requireNonNull(type);
        this.name = name != null ? Utils.validateChars(ILLEGAL_NAME_CHAR, name) : "";
        this.inputTypes = Objects.requireNonNull(inputTypes);
        this.outputTypes = Objects.requireNonNull(outputTypes);
        this.stateMutability = stateMutability;
        this.hashAlgorithm = messageDigest.getAlgorithm();
        generateSelector(messageDigest);
    }

    public Function(String signature) throws ParseException {
        this(signature, null);
    }

    public Function(String signature, String outputs) throws ParseException {
        this(Type.FUNCTION, signature, outputs, newDefaultDigest());
    }

    public Function(String signature, String outputs, MessageDigest messageDigest) throws ParseException {
        this(Type.FUNCTION, signature, outputs, messageDigest);
    }

    /**
     * @param type  to denote function, constructor, or fallback
     * @param signature the function signature
     * @param outputs   the signature of the tuple containing the return types
     * @param messageDigest the hash function with which to generate the 4-byte selector
     * @throws ParseException   if {@code signature} or {@code outputs} is malformed
     */
    public Function(Type type, String signature, String outputs, MessageDigest messageDigest) throws ParseException {
        final int split = signature.indexOf('(');
        if(split < 0) {
            throw new ParseException("params start not found", signature.length());
        }
        final TupleType tupleType = (TupleType) TypeFactory.create(signature.substring(split));

        this.type = Objects.requireNonNull(type);
        this.name = Utils.validateChars(NON_ASCII_CHAR, signature.substring(0, split));
        this.inputTypes = tupleType;
        this.outputTypes = outputs != null ? TupleType.parse(outputs) : TupleType.EMPTY;
        this.stateMutability = null;
        this.hashAlgorithm = messageDigest.getAlgorithm();
        generateSelector(messageDigest);
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

    public Tuple decodeReturn(byte[] returnVals) { // TODO allow decoding of non-calls without a Function
        return outputTypes.decode(returnVals);
    }

    public Tuple decodeReturn(ByteBuffer returnVals) {
        return outputTypes.decode(returnVals);
    }

    public int callLength(Tuple args) {
        return callLength(args, true);
    }

    public int callLength(Tuple args, boolean validate) {
        return Function.SELECTOR_LEN + (validate ? inputTypes.validate(args) : inputTypes.byteLength(args));
    }

    public ByteBuffer encodeCallWithArgs(Object... args) {
        return encodeCall(new Tuple(args));
    }

    public ByteBuffer encodeCall(Tuple args) {
        ByteBuffer dest = ByteBuffer.wrap(new byte[callLength(args, true)]); // ByteOrder.BIG_ENDIAN by default
        encodeCall(args, dest);
        return dest;
    }

    public Function encodeCall(Tuple args, ByteBuffer dest, boolean validate) {
        if(validate) {
            inputTypes.validate(args);
        }
        encodeCall(args, dest);
        return this;
    }

    private void encodeCall(Tuple args, ByteBuffer dest) {
        dest.put(selector);
        inputTypes.encodeTail(args, dest);
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

    public static String hexOf(ByteBuffer buffer) {
        return encode(buffer.array(), HEX);
    }
}
