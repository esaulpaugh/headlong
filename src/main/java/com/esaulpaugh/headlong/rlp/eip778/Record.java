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

import com.esaulpaugh.headlong.rlp.RLPDecoder;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.rlp.RLPList;
import com.esaulpaugh.headlong.rlp.RLPListIterator;
import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.util.Strings;

import java.security.SignatureException;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.BASE_64_URL_SAFE;

/**
 * Implementation of EIP 778: Ethereum Node Records (ENR), https://eips.ethereum.org/EIPS/eip-778
 */
public final class Record {

    private static final int MAX_RECORD_LEN = 300;

    private static final String ENR_PREFIX = "enr:";

    private final RLPList rlp;

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
            this.rlp = RLP_STRICT.wrapList(record);
        } catch (DecodeException e) {
            throw new RuntimeException(e); // shouldn't happen
        }
    }

    private Record(RLPList recordRLP) {
        this.rlp = recordRLP;
    }

    public static Record parse(String enrString) throws DecodeException {
        if(enrString.startsWith(ENR_PREFIX)) {
            return decode(Strings.decode(enrString.substring(ENR_PREFIX.length()), BASE_64_URL_SAFE));
        }
        throw new IllegalArgumentException("prefix \"" + ENR_PREFIX + "\" not found");
    }

    public static Record decode(byte[] record) throws DecodeException {
        return new Record(RLP_STRICT.wrapList(record));
    }

    public RLPList getRLP() {
        return rlp;
    }

    public RLPItem getSignature() throws DecodeException {
        return getRLP().iterator(RLP_STRICT).next();
    }

    public RLPList getContent() throws DecodeException {
        return RLP_STRICT.wrapList(getContentBytes(getSignature().endIndex));
    }

    public long getSeq() throws DecodeException {
        RLPListIterator iter = new RLPListIterator(getRLP(), RLP_STRICT);
        iter.next();
        return iter.next().asLong();
    }

    private byte[] getContentBytes(int index) {
        int contentDataLen = rlp.encodingLength() - index;
        byte[] content = new byte[RLPEncoder.prefixLength(contentDataLen) + contentDataLen];
        int prefixLen = RLPEncoder.insertListPrefix(contentDataLen, content, 0);
        rlp.exportRange(index, index + contentDataLen, content, prefixLen);
        return content;
    }

    public RLPList decode(Verifier verifier) throws DecodeException, SignatureException {
        RLPItem signatureItem = getSignature();
        byte[] content = getContentBytes(signatureItem.endIndex);
        verifier.verify(signatureItem.data(), content); // verify content
        return RLPDecoder.RLP_STRICT.wrapList(content);
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return rlp.equals(((Record) o).rlp);
    }

    @Override
    public String toString() {
        return ENR_PREFIX + rlp.toString(BASE_64_URL_SAFE);
    }
}
