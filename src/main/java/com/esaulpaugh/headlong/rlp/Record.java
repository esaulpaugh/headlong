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
package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.util.Strings;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.BASE_64_URL_SAFE;

/** Implementation of EIP-778: Ethereum Node Records (ENR), https://eips.ethereum.org/EIPS/eip-778 */
public final class Record {

    private static final long MAX_RECORD_LEN = 300;

    private static final String ENR_PREFIX = "enr:";

    private final RLPList rlp;

    public static ByteBuffer encode(Signer signer, long seq, KVP... pairs) {
        return encode(signer, seq, Arrays.asList(pairs));
    }

    public static ByteBuffer encode(Signer signer, final long seq, List<KVP> pairs) {
        if(seq < 0) {
            throw new IllegalArgumentException("negative seq");
        }
        final int signatureLen = signer.signatureLength();
        if(signatureLen < 0) {
            throw new InvalidParameterException("signer specifies negative signature length");
        }
        final int payloadLen = RLPEncoder.payloadLen(seq, pairs); // content list prefix not included
        final int recordDataLen = RLPEncoder.itemLen(signatureLen) + payloadLen;

        final byte[] record = new byte[checkRecordLen(RLPEncoder.itemLen(recordDataLen))];
        final byte[] content = RLPEncoder.encodeRecordContent(payloadLen, seq, pairs);

        // copy payload to record before sending to signer
        System.arraycopy(content, content.length - payloadLen, record, record.length - payloadLen, payloadLen);

        final byte[] signature = signer.sign(content);
        if(signature.length != signatureLen) {
            throw new InvalidParameterException("unexpected signature length: " + signature.length + " != " + signatureLen);
        }

        final ByteBuffer bb = ByteBuffer.wrap(record);
        RLPEncoder.insertListPrefix(recordDataLen, bb);
        RLPEncoder.encodeString(signature, bb);
        return bb;
    }

    public Record(Signer signer, long seq, KVP... pairs) {
        this(signer, seq, Arrays.asList(pairs));
    }

    public Record(Signer signer, long seq, List<KVP> pairs) {
        this.rlp = RLP_STRICT.wrapList(encode(signer, seq, pairs).array());
    }

    private Record(RLPList recordRLP) { // validate before calling
        this.rlp = recordRLP;
    }

    private static int checkRecordLen(int recordLen) {
        if(recordLen > MAX_RECORD_LEN) {
            throw new IllegalArgumentException("record length exceeds maximum: " + recordLen + " > " + MAX_RECORD_LEN);
        }
        return recordLen;
    }

    public Record with(Signer signer, long seq, KVP... newPairs) {
        final HashSet<KVP> pairSet = new HashSet<>();
        for (KVP pair : newPairs) {
            if(!pairSet.add(pair)) {
                throw pair.duplicateKeyErr();
            }
        }
        visitAll((k, v) -> pairSet.add(new KVP(k, v)));
        return new Record(signer, seq, pairSet.toArray(KVP.EMPTY_ARRAY));
    }

    public static Record parse(String enrString, Verifier verifier) throws SignatureException {
        if(enrString.startsWith(ENR_PREFIX)) {
            byte[] bytes = Strings.decode(enrString.substring(ENR_PREFIX.length()), BASE_64_URL_SAFE);
            return decode(bytes, verifier);
        }
        throw new IllegalArgumentException("prefix \"" + ENR_PREFIX + "\" not found");
    }

    public static Record decode(byte[] bytes, Verifier verifier) throws SignatureException {
        checkRecordLen(bytes.length);
        RLPList rlpList = RLP_STRICT.wrapList(bytes)
                .duplicate(); // defensive copy
        if(rlpList.encodingLength() != bytes.length) {
            throw new IllegalArgumentException("unconsumed trailing bytes");
        }
        Record record = new Record(rlpList);
        RLPString signatureItem = record.getSignature();
        byte[] content = record.content(signatureItem.endIndex);
        verifier.verify(signatureItem.asBytes(), content); // verify signature
        return record;
    }

    public RLPList getRLP() {
        return rlp;
    }

    public RLPString getSignature() {
        return rlp.iterator(RLP_STRICT).next().asRLPString();
    }

    public RLPList getContent() {
        return RLP_STRICT.wrapList(content(getSignature().endIndex));
    }

    public long getSeq() {
        return visitAll(null);
    }

    public List<KVP> getPairs() {
        List<KVP> list = new ArrayList<>();
        visitAll((k, v) -> list.add(new KVP(k, v)));
        return list;
    }

    public Map<String, byte[]> map() {
        Map<String, byte[]> map = new LinkedHashMap<>();
        visitAll((k, v) -> map.put(k.asString(Strings.UTF_8), v.asBytes()));
        return map;
    }

    /**
     * @param visitor   pair-consuming code
     * @return seq
     * */
    public long visitAll(BiConsumer<RLPItem, RLPItem> visitor) {
        Iterator<RLPItem> iter = rlp.iterator();
        iter.next(); // skip signature
        long seq = iter.next().asLong();
        if (visitor != null) {
            while (iter.hasNext()) {
                visitor.accept(iter.next(), iter.next());
            }
        }
        return seq;
    }

    private byte[] content(int index) {
        // reconstruct the content list from the content data
        final int contentDataLen = rlp.encodingLength() - index;
        final ByteBuffer bb = ByteBuffer.allocate(RLPEncoder.itemLen(contentDataLen));
        RLPEncoder.insertListPrefix(contentDataLen, bb);
        final byte[] arr = bb.array();
        System.arraycopy(rlp.buffer, index, arr, bb.position(), contentDataLen);
        return arr;
    }

    public interface Signer {
        int signatureLength();
        byte[] sign(byte[] content);
    }

    @FunctionalInterface
    public interface Verifier {
        void verify(byte[] signature, byte[] content) throws SignatureException;
    }

    @Override
    public int hashCode() {
        return rlp.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Record && ((Record) o).rlp.equals(this.rlp);
    }

    @Override
    public String toString() {
        return ENR_PREFIX + rlp.encodingString(BASE_64_URL_SAFE);
    }
}
