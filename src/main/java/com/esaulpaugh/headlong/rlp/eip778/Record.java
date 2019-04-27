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
package com.esaulpaugh.headlong.rlp.eip778;

import com.esaulpaugh.headlong.rlp.*;
import com.esaulpaugh.headlong.rlp.exception.DecodeException;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.HEX;

/**
 * Implementation of EIP 778: Ethereum Node Records (ENR), https://eips.ethereum.org/EIPS/eip-778
 */
public final class Record {

    private static final int MAX_RECORD_LEN = 300;

    private final RLPList record;

    public Record(long seq, KeyValuePair[] pairs, Signer signer) {
        final int signatureLen = signer.signatureLength();
        final int signatureItemLen = RLPEncoder.prefixLength(signatureLen) + signatureLen;
        final long payloadLenLong = RLPEncoder.encodedLen(seq) + RLPEncoder.dataLen(pairs);
        final long recordListPayloadLenLong = signatureItemLen + payloadLenLong;
        final int recordPrefixLen = RLPEncoder.prefixLength(recordListPayloadLenLong);
        final long recordLenLong = recordPrefixLen + recordListPayloadLenLong;
        if(recordLenLong > MAX_RECORD_LEN) {
            throw new IllegalArgumentException("record length exceeds maximum: " + recordLenLong + " > " + MAX_RECORD_LEN);
        }

        final int recordLen = (int) recordLenLong;
        byte[] record = new byte[recordLen];
        RLPEncoder.insertListPrefix((int) recordListPayloadLenLong, record, 0);
        int contentListOffset = recordPrefixLen + signatureItemLen - RLPEncoder.prefixLength(payloadLenLong);
        RLPEncoder.insertRecordContentList((int) payloadLenLong, seq, pairs, record, contentListOffset);
        byte[] signature = signer.sign(record, contentListOffset, recordLen - contentListOffset);
        RLPEncoder.insertRecordSignature(signature, record, recordPrefixLen);

        try {
            this.record = RLP_STRICT.wrapList(record);
        } catch (DecodeException e) {
            throw new RuntimeException(e);
        }
    }

    private Record(RLPList record) {
        this.record = record;
    }

    public static Record decode(byte[] record) throws DecodeException {
        return new Record(RLP_STRICT.wrapList(record));
    }

    public RLPList getRecord() {
        return record;
    }

    public RLPItem getSignature() throws DecodeException {
        return getRecord().iterator(RLP_STRICT).next();
    }

    public RLPList getContent() throws DecodeException {
        return RLP_STRICT.wrapList(getContentBytes(getSignature().endIndex));
    }

    public long getSeq() throws DecodeException {
        RLPListIterator iter = new RLPListIterator(getRecord(), RLP_STRICT);
        iter.next();
        return iter.next().asLong();
    }

    private byte[] getContentBytes(int index) {
        int contentDataLen = record.encodingLength() - index;
        byte[] content = new byte[RLPEncoder.prefixLength(contentDataLen) + contentDataLen];
        int prefixLen = RLPEncoder.insertListPrefix(contentDataLen, content, 0);
        record.exportRange(index, index + contentDataLen, content, prefixLen);
        return content;
    }

    public RLPList decode(Verifier verifier) throws DecodeException {
        RLPItem signatureItem = getSignature();
        byte[] signature = signatureItem.data();
        byte[] content = getContentBytes(signatureItem.endIndex);
        if(verifier.verify(signature, content)) { // verify content
            return RLPDecoder.RLP_STRICT.wrapList(content);
        }
        return null;
    }

    public interface Signer {
        int signatureLength();
        byte[] sign(byte[] message, int off, int len);
    }

    public interface Verifier {
        boolean verify(byte[] signature, byte[] content) throws DecodeException;
    }

    @Override
    public int hashCode() {
        return record.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return record.equals(((Record) o).record);
    }

    @Override
    public String toString() {
        return record.asString(HEX);
    }
}