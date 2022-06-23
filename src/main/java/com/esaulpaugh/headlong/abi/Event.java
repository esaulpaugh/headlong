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

import com.esaulpaugh.headlong.abi.util.JsonUtils;
import com.esaulpaugh.headlong.util.FastHex;
import com.esaulpaugh.headlong.util.Strings;
import com.google.gson.JsonObject;
import com.joemelsha.crypto.hash.Keccak;

import java.util.Arrays;
import java.util.Objects;

/** Represents an event in Ethereum. */
public final class Event implements ABIObject {

    private static final ArrayType<byte[], Byte, ByteType> BYTES_32 = TypeFactory.create("bytes32");
    public static final byte[][] EMPTY_TOPICS = new byte[0][];

    private final String name;
    private final boolean anonymous;
    private final TupleType inputs;
    private final TupleType indexedParams;
    private final TupleType nonIndexedParams;
    private final boolean[] indexManifest;
    private final byte[] signatureHash;


    public static Event create(String name, TupleType inputs, boolean... indexed) {
        return new Event(name, false, inputs, indexed);
    }

    public static Event createAnonymous(String name, TupleType inputs, boolean... indexed) {
        return new Event(name, true, inputs, indexed);
    }

    public Event(String name, boolean anonymous, TupleType inputs, boolean... indexed) {
        this.name = Objects.requireNonNull(name);
        this.inputs = Objects.requireNonNull(inputs);
        if(indexed.length != inputs.size()) {
            throw new IllegalArgumentException("indexed.length doesn't match number of inputs");
        }
        this.indexManifest = Arrays.copyOf(indexed, indexed.length);
        this.indexedParams = inputs.select(indexManifest);
        this.nonIndexedParams = inputs.exclude(indexManifest);
        this.anonymous = anonymous;
        this.signatureHash = new Keccak(256).digest(Strings.decode(getCanonicalSignature(), Strings.ASCII));
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.EVENT;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TupleType getInputs() {
        return inputs;
    }

    public boolean[] getIndexManifest() {
        return Arrays.copyOf(indexManifest, indexManifest.length);
    }

    public boolean isElementIndexed(int position) {
        return indexManifest[position];
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    public String getCanonicalSignature() {
        return name + inputs.canonicalType;
    }

    public TupleType getIndexedParams() {
        return indexedParams;
    }

    public TupleType getNonIndexedParams() {
        return nonIndexedParams;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * (31 * name.hashCode() + inputs.hashCode()) + Arrays.hashCode(indexManifest)) + Boolean.hashCode(anonymous);
    }

    @Override
    public boolean equals(Object o) {
        if(o == this) return true;
        if(!(o instanceof Event)) return false;
        Event other = (Event) o;
        return other.anonymous == this.anonymous
                && other.name.equals(this.name)
                && other.inputs.equals(this.inputs)
                && Arrays.equals(other.indexManifest, this.indexManifest);
    }

    public static Event fromJson(String eventJson) {
        return fromJsonObject(JsonUtils.parseObject(eventJson));
    }

    public static Event fromJsonObject(JsonObject event) {
        return ABIJSON.parseEvent(event);
    }

    @Override
    public String toString() {
        return toJson(true);
    }

    @Override
    public boolean isEvent() {
        return true;
    }

    public Tuple decodeTopics(byte[][] topics) {
        return new Tuple(decodeTopicsArray(topics));
    }

    public Tuple decodeData(byte[] data) {
        return data == null && nonIndexedParams.isEmpty()
                ? Tuple.EMPTY
                : nonIndexedParams.decode(Objects.requireNonNull(data));
    }

    /**
     * Decodes Event arguments.
     *
     * @param topics indexed parameters to decode. If the event is anonymous, the first element is a Keccak hash of the
     *               canonical signature of the event (see <a href="https://docs.soliditylang.org/en/v0.8.11/abi-spec.html#events">https://docs.soliditylang.org/en/v0.8.11/abi-spec.html#events</a>)
     * @param data non-indexed parameters to decode
     * @return  the decoded arguments
     */
    public Tuple decodeArgs(byte[][] topics, byte[] data) {
        return mergeDecodedArgs(decodeTopicsArray(topics), decodeData(data));
    }

    private Tuple mergeDecodedArgs(Object[] decodedTopics, Tuple decodedData) {
        Object[] result = new Object[inputs.size()];
        for (int i = 0, topicIndex = 0, dataIndex = 0; i < indexManifest.length; i++) {
            if (indexManifest[i]) {
                result[i] = decodedTopics[topicIndex++];
            } else {
                result[i] = decodedData.get(dataIndex++);
            }
        }
        return new Tuple(result);
    }

    private Object[] decodeTopicsArray(byte[][] topics) {
        if(anonymous) {
            checkAnonymousTopics(topics);
        } else {
            checkNonAnonymousTopics(topics);
        }
        final int offset = anonymous ? 0 : 1;
        final Object[] decodedTopics = new Object[indexedParams.size()];
        for (int i = 0; i < decodedTopics.length; i++) {
            ABIType<?> abiType = indexedParams.get(i);
            byte[] topic = topics[i + offset];
            if (abiType.isDynamic()) {
                // Dynamic indexed types are not decodable in Events. Only a special hash is stored for fast querying of records
                // See https://docs.soliditylang.org/en/v0.8.11/abi-spec.html#indexed-event-encoding
                decodedTopics[i] = BYTES_32.decode(topic);
            } else {
                decodedTopics[i] = abiType.decode(topic);
            }
        }
        return decodedTopics;
    }

    private void checkAnonymousTopics(byte[][] topics) {
        topics = indexedParams.isEmpty() && topics == null
                ? EMPTY_TOPICS
                : Objects.requireNonNull(topics, "non-null topics expected");
        checkTopicsLength(topics.length, 0);
    }

    private void checkNonAnonymousTopics(byte[][] topics) {
        Objects.requireNonNull(topics, "non-null topics expected");
        checkTopicsLength(topics.length, 1);
        byte[] decodedSignatureHash = BYTES_32.decode(topics[0]);
        if (!Arrays.equals(signatureHash, decodedSignatureHash)) {
            throw new IllegalArgumentException("unexpected topics[0]: event " + getCanonicalSignature()
                    + " expects " + FastHex.encodeToString(signatureHash)
                    + " but found " + FastHex.encodeToString(decodedSignatureHash));
        }
    }

    private void checkTopicsLength(int len, int offset) {
        final int expectedTopics = indexedParams.size() + offset;
        if(len != expectedTopics) {
            throw new IllegalArgumentException("expected topics.length == " + expectedTopics + " but found length " + len);
        }
    }
}
