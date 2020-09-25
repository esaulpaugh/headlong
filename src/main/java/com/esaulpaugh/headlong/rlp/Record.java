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

    public Record(long seq, List<KeyValuePair> pairs, Signer signer) {
        final int signatureLen = signer.signatureLength();
        final int payloadLen = rlpEncodedLen(seq) + RLPEncoder.dataLen(pairs);
        final int recordListDataLen = RLPEncoder.prefixLength(signatureLen) + signatureLen + payloadLen;
        final int recordListPrefixLen = RLPEncoder.prefixLength(recordListDataLen);
        final int recordLen = recordListPrefixLen + recordListDataLen;

        checkRecordLen(recordLen);

        final ByteBuffer bb = ByteBuffer.allocate(recordLen);
        RLPEncoder.insertListPrefix(recordListDataLen, bb);

        final int contentListLen = RLPEncoder.prefixLength(payloadLen) + payloadLen;
        final int contentListOffset = recordLen - contentListLen;
        bb.position(contentListOffset);
        RLPEncoder.insertRecordContentList(payloadLen, seq, pairs, bb);

        final byte[] signature = signer.sign(bb.array(), contentListOffset, contentListLen);
        bb.position(recordListPrefixLen);
        RLPEncoder.encodeItem(signature, bb);

        this.rlp = RLP_STRICT.wrapList(bb.array());
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
        byte[] content = record.getContentBytes(signatureItem.endIndex);
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
        return RLP_STRICT.wrapList(getContentBytes(getSignature().endIndex));
    }

    public long getSeq() {
        Iterator<RLPItem> iter = getRLP().iterator();
        iter.next();
        return iter.next().asLong();
    }

    private byte[] getContentBytes(int index) {
        int contentDataLen = rlp.encodingLength() - index;
        ByteBuffer bb = ByteBuffer.allocate(RLPEncoder.prefixLength(contentDataLen) + contentDataLen);
        RLPEncoder.insertListPrefix(contentDataLen, bb);
        rlp.exportRange(index, index + contentDataLen, bb.array(), bb.position());
        return bb.array();
    }

    public interface Signer {
        int signatureLength();
        byte[] sign(byte[] message, int off, int len);
    }

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

    private static int rlpEncodedLen(long val) {
        int dataLen = Integers.len(val);
        if (dataLen == 1) {
            return (byte) val >= 0x00 ? 1 : 2;
        }
        return 1 + dataLen;
    }
}
