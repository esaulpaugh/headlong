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

import com.esaulpaugh.headlong.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.BASE_64_URL_SAFE;

/** Implementation of <a href="https://eips.ethereum.org/EIPS/eip-778">EIP-778: Ethereum Node Records (ENR)</a> */
public final class Record implements Iterable<KVP>, Comparable<Record> {

    private static final int MAX_RECORD_LEN = 300;

    private static final String ENR_PREFIX = "enr:";

    private final long seq;
    private final RLPList rlp;

    public static ByteBuffer encode(Signer signer, long seq, KVP... pairs) {
        return encode(signer, seq, Arrays.asList(pairs));
    }

    public static ByteBuffer encode(Signer signer, final long seq, List<KVP> pairs) {
        final int signatureLen = signer.signatureLength();
        if (signatureLen <= 1) {
            throw new InvalidParameterException("invalid signature length");
        }
        if (seq < 0) {
            throw new IllegalArgumentException("negative seq");
        }

        Collections.sort(pairs);

        final byte[] seqBytes = Integers.toBytes(seq);
        final int payloadLen = RLPEncoder.payloadLen(seqBytes, pairs); // content list prefix not included
        final int recordDataLen = RLPEncoder.itemLen(signatureLen) + payloadLen;

        final ByteBuffer recordBuf = ByteBuffer.allocate(checkRecordLen(RLPEncoder.itemLen(recordDataLen)));
        final byte[] content = RLPEncoder.encodeRecordContent(seqBytes, pairs, payloadLen);

        // copy payload to record before sending to signer
        byte[] record = recordBuf.array();
        System.arraycopy(content, content.length - payloadLen, record, record.length - payloadLen, payloadLen);

        final byte[] signature = signer.sign(content);
        if (signature.length != signatureLen) {
            throw new InvalidParameterException("unexpected signature length: " + signature.length + " != " + signatureLen);
        }

        RLPEncoder.insertListPrefix(recordDataLen, recordBuf);
        RLPEncoder.putString(signature, recordBuf);
        recordBuf.rewind();
        return recordBuf;
    }

    public Record(Signer signer, long seq, KVP... pairs) {
        this(signer, seq, Arrays.asList(pairs));
    }

    public Record(Signer signer, long seq, List<KVP> pairs) {
        this(seq, RLP_STRICT.wrapList(encode(signer, seq, pairs).array()));
    }

    private Record(long seq, RLPList recordRLP) { // validate before calling
        this.seq = seq;
        this.rlp = recordRLP;
    }

    private static int checkRecordLen(int recordLen) {
        if (recordLen > MAX_RECORD_LEN) {
            throw new IllegalArgumentException("record length exceeds maximum: " + recordLen + " > " + MAX_RECORD_LEN);
        }
        return recordLen;
    }

    public Record with(Signer signer, long seq, KVP... newPairs) {
        final HashSet<KVP> pairSet = new HashSet<>();
        for (KVP pair : newPairs) {
            if (!pairSet.add(pair)) {
                throw KVP.duplicateKeyErr(pair.key);
            }
        }
        for (KVP pair : this) {
            pairSet.add(pair);
        }
        return new Record(signer, seq, pairSet.toArray(KVP.EMPTY_ARRAY));
    }

    public static Record parse(String enrString, Verifier verifier) throws SignatureException {
        if (enrString.startsWith(ENR_PREFIX)) {
            return decode(Strings.decode(enrString.substring(ENR_PREFIX.length()), BASE_64_URL_SAFE), verifier);
        }
        throw new IllegalArgumentException("prefix \"" + ENR_PREFIX + "\" not found");
    }

    public static Record decode(byte[] bytes, Verifier verifier) throws SignatureException {
        checkRecordLen(bytes.length);
        final RLPList rlpList = RLP_STRICT.wrapList(Arrays.copyOf(bytes, bytes.length)); // defensive copy
        if (rlpList.encodingLength() != bytes.length) {
            throw new IllegalArgumentException("unconsumed trailing bytes");
        }
        final Iterator<RLPItem> rlpIter = rlpList.iterator();
        if (verifier != null) {
            final RLPString signatureItem = rlpIter.next().asRLPString(); // signature
            verifier.verify(signatureItem.asBytes(), Record.content(rlpList, signatureItem.endIndex));
        } else {
            rlpIter.next(); // skip signature
        }
        final long seq = rlpIter.next().asRLPString().asLong();
        RLPString prevKey = null;
        while (rlpIter.hasNext()) {
            final RLPString key = rlpIter.next().asRLPString();
            rlpIter.next(); // value
            if (prevKey != null && key.compareTo(prevKey) <= 0) {
                throw key.compareTo(prevKey) == 0
                        ? KVP.duplicateKeyErr(key)
                        : new IllegalArgumentException("key out of order");
            }
            prevKey = key;
        }
        return new Record(seq, rlpList);
    }

    public RLPList getRLP() {
        return rlp;
    }

    public RLPString getSignature() {
        return rlp.iterator(RLP_STRICT).next().asRLPString();
    }

    public RLPList getContent() {
        return RLP_STRICT.wrapList(Record.content(this.rlp, getSignature().endIndex));
    }

    public long getSeq() {
        return seq;
    }

    public List<KVP> getPairs() {
        final List<KVP> list = new ArrayList<>();
        for (KVP pair : this) {
            list.add(pair);
        }
        return list;
    }

    public LinkedHashMap<String, RLPItem> orderedMap() {
        final LinkedHashMap<String, RLPItem> map = new LinkedHashMap<>();
        for (KVP pair : this) {
            map.put(pair.key.asString(Strings.UTF_8), pair.value());
        }
        return map;
    }

    @Override
    public Iterator<KVP> iterator() {
        final Iterator<RLPItem> rlpIter = rlp.iterator();
        rlpIter.next(); // signature
        rlpIter.next(); // seq
        return new Iterator<KVP>() {
            @Override
            public boolean hasNext() {
                return rlpIter.hasNext();
            }

            @Override
            public KVP next() {
                return new KVP(rlpIter.next().asRLPString(), rlpIter.next());
            }
        };
    }

    @Override
    public int compareTo(Record o) {
        return Long.compare(this.seq, o.seq);
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
