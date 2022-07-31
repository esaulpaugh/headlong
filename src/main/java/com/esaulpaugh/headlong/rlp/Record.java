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

/** Implementation of <a href="https://eips.ethereum.org/EIPS/eip-778">EIP-778: Ethereum Node Records (ENR)</a> */
public final class Record {

    private static final long MAX_RECORD_LEN = 300;

    private static final String ENR_PREFIX = "enr:";

    private final long seq;
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
        this.seq = seq;
        this.rlp = RLP_STRICT.wrapList(encode(signer, seq, pairs).array());
    }

    private Record(long seq, RLPList recordRLP) { // validate before calling
        this.seq = seq;
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
                throw KVP.duplicateKeyErr(pair.key);
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
        final RLPList rlpList = RLP_STRICT.wrapList(Arrays.copyOf(bytes, bytes.length)); // defensive copy
        if(rlpList.encodingLength() != bytes.length) {
            throw new IllegalArgumentException("unconsumed trailing bytes");
        }
        final long seq = Record.traverse(rlpList, verifier, new BiConsumer<RLPString, RLPString>() {
                    RLPString prevKey = null;

                    @Override
                    public void accept(RLPString k, RLPString v) {
                        if (prevKey != null && k.compareTo(prevKey) <= 0) {
                            throw k.compareTo(prevKey) == 0
                                    ? KVP.duplicateKeyErr(k)
                                    : new IllegalArgumentException("key out of order: " + k.asString(Strings.UTF_8));
                        }
                        prevKey = k;
                    }
                });
        return new Record(seq, rlpList);
    }

    public RLPList getRLP() {
        return rlp;
    }

    public RLPString getSignature() {
        return Record.getSignature(this.rlp);
    }

    public RLPList getContent() {
        return RLP_STRICT.wrapList(Record.content(this.rlp, getSignature().endIndex));
    }

    public long getSeq() {
        return seq;
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
    public long visitAll(BiConsumer<RLPString, RLPString> visitor) {
        try {
            return Record.traverse(this.rlp, null, visitor);
        } catch (SignatureException se) { // not possible with null verifier
            throw new AssertionError(se);
        }
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

    private static RLPString getSignature(RLPList rlpList) {
        return rlpList.iterator(RLP_STRICT).next().asRLPString();
    }

    private static long traverse(RLPList rlpList, Verifier verifier, BiConsumer<RLPString, RLPString> visitor) throws SignatureException {
        final Iterator<RLPItem> iter = rlpList.iterator();
        final RLPString signatureItem = iter.next().asRLPString();
        if(verifier != null) {
            verifier.verify(signatureItem.asBytes(), Record.content(rlpList, signatureItem.endIndex));
        }
        final long seq = iter.next().asRLPString().asLong();
        if (visitor != null) {
            while (iter.hasNext()) {
                visitor.accept(iter.next().asRLPString(), iter.next().asRLPString());
            }
        }
        return seq;
    }

    private static byte[] content(RLPList rlpList, int index) {
        // reconstruct the content list from the content data
        final int contentDataLen = rlpList.encodingLength() - index;
        final ByteBuffer bb = ByteBuffer.allocate(RLPEncoder.itemLen(contentDataLen));
        RLPEncoder.insertListPrefix(contentDataLen, bb);
        final byte[] arr = bb.array();
        System.arraycopy(rlpList.buffer, index, arr, bb.position(), contentDataLen);
        return arr;
    }
}
