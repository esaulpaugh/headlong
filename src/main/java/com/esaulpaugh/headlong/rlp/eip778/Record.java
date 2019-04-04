package com.esaulpaugh.headlong.rlp.eip778;

import com.esaulpaugh.headlong.rlp.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPListIterator;
import com.esaulpaugh.headlong.util.FastHex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.HEX;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

/**
 * Implementation of EIP 778: Ethereum Node Records (ENR), https://eips.ethereum.org/EIPS/eip-778
 */
public class Record {

    private static final int MAX_RECORD_LEN = 300;

    private final long seq;

    private final KeyValuePair[] pairs;

    private final byte[] record;

    public Record(long seq, KeyValuePair[] pairs, Signer signer) {

        byte[] content = RLPEncoder.encodeEIP778RecordContent(seq, pairs);
        byte[] signature = signer.sign(RLPEncoder.encodeAsList((Object) content));

        byte[] record = RLPEncoder.encodeEIP778Record(signature, content);

        if(record.length > MAX_RECORD_LEN) {
            throw new IllegalArgumentException("record length exceeds maximum: " + record.length + " > " + MAX_RECORD_LEN);
        }

        this.seq = seq;
        this.pairs = Arrays.copyOf(pairs, pairs.length);
        this.record = record;
    }

    public Record(byte[] record) throws DecodeException {
        RLPListIterator iter = RLP_STRICT.listIterator(record);

        iter.next(); // signature
        this.seq = iter.next().asLong();

        List<KeyValuePair> pairs =  new ArrayList<>();
        while (iter.hasNext()) {
            pairs.add(new KeyValuePair(iter.next().data(), iter.next().data()));
        }
        this.pairs = pairs.toArray(KeyValuePair.EMPTY_ARRAY);

        this.record = Arrays.copyOf(record, record.length);
    }

    public byte[] getSignature() throws DecodeException {
        return RLP_STRICT.listIterator(record).next().data();
    }

    public byte[] getContent() {
        return RLPEncoder.encodeEIP778RecordContent(seq, pairs);
    }

    public byte[] getRecord() {
        return Arrays.copyOf(record, record.length);
    }

    public interface Signer {
        byte[] sign(byte[] message);
    }

    @Override
    public String toString() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("record len = ").append(record.length).append('\n');
            sb.append("record = ").append(FastHex.encodeToString(record)).append('\n');

            RLPListIterator iter = RLP_STRICT.listIterator(record);

            sb.append("signature = ").append(iter.next().asString(HEX)).append('\n');
            sb.append("seq = ").append(iter.next().asLong()).append('\n');

            while (iter.hasNext()) {
                sb.append(iter.next().asString(UTF_8)).append(" --> ").append(iter.next().asString(HEX)).append('\n');
            }
            return sb.toString();
        } catch (DecodeException de) {
            throw new RuntimeException(de);
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getRecord());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Record record1 = (Record) o;
        return Arrays.equals(getRecord(), record1.getRecord());
    }
}