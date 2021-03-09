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
import java.security.SignatureException;
import java.util.Iterator;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.BASE_64_URL_SAFE;

/** Implementation of EIP-778: Ethereum Node Records (ENR), https://eips.ethereum.org/EIPS/eip-778 */
public final class Record {

    private static final long MAX_RECORD_LEN = 300;

    private static final String ENR_PREFIX = "enr:";

    private final RLPList rlp;

    public static byte[] encode(final long seq, List<KeyValuePair> pairs, Signer signer) {
        if(seq < 0) {
            throw new IllegalArgumentException("negative seq");
        }
        final int signatureLen = signer.signatureLength();
        if(signatureLen < 0) {
            throw new RuntimeException("signer specifies negative signature length");
        }
        final int contentDataLen = RLPEncoder.measureEncodedLen(seq) + RLPEncoder.dataLen(pairs); // content list prefix not included
        final int recordDataLen = RLPEncoder.itemLen(signatureLen) + contentDataLen;
        final int recordLen = RLPEncoder.itemLen(recordDataLen);
        checkRecordLen(recordLen);

        final int contentLen = RLPEncoder.itemLen(contentDataLen);
        final int contentOffset = recordLen - contentLen;

        final ByteBuffer record = ByteBuffer.allocate(recordLen);
        RLPEncoder.insertRecordContent(contentDataLen, seq, pairs, record.position(contentOffset));

        byte[] recordArr = record.array();
        byte[] signature = signer.sign(recordArr, contentOffset, contentLen);
        if(signature.length != signatureLen) {
            throw new RuntimeException("incorrect signature length: " + signature.length + " != " + signatureLen);
        }
        RLPEncoder.encodeItem(signature, record.position(recordLen - recordDataLen)); // end of signature will overwrite content list prefix
        RLPEncoder.insertListPrefix(recordDataLen, record.position(0));
        return recordArr;
    }

    public Record(final long seq, List<KeyValuePair> pairs, Signer signer) {
        this.rlp = RLP_STRICT.wrapList(encode(seq, pairs, signer));
    }

    private Record(RLPList recordRLP) { // validate before calling
        this.rlp = recordRLP;
    }

    private static void checkRecordLen(int recordLen) {
        if(recordLen > MAX_RECORD_LEN) {
            throw new IllegalArgumentException("record length exceeds maximum: " + recordLen + " > " + MAX_RECORD_LEN);
        }
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
                .duplicate(RLP_STRICT); // defensive copy
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
        return getRLP().iterator(RLP_STRICT).next().asRLPString();
    }

    public RLPList getContent() {
        return RLP_STRICT.wrapList(content(getSignature().endIndex));
    }

    public long getSeq() {
        Iterator<RLPItem> iter = getRLP().iterator();
        iter.next();
        return iter.next().asLong();
    }

    // reconstruct the content list from the content data
    private byte[] content(int index) {
        int contentDataLen = rlp.encodingLength() - index;
        ByteBuffer bb = ByteBuffer.allocate(RLPEncoder.itemLen(contentDataLen));
        RLPEncoder.insertListPrefix(contentDataLen, bb);
        rlp.exportRange(index, index + contentDataLen, bb.array(), bb.position());
        return bb.array();
    }

    public interface Signer {
        int signatureLength();
        byte[] sign(byte[] message, int off, int len);
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
        return ENR_PREFIX + rlp.toString(BASE_64_URL_SAFE);
    }
}
