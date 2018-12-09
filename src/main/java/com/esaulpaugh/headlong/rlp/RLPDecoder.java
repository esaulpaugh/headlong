package com.esaulpaugh.headlong.rlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Decodes RLP-formatted data.
 */
public class RLPDecoder {

    public static final RLPDecoder RLP_STRICT = new RLPDecoder(false);
    public static final RLPDecoder RLP_LENIENT = new RLPDecoder(true);

    public final boolean lenient;

    private RLPDecoder(boolean lenient) {
        this.lenient = lenient;
    }

    public class SequenceIterator {

        private final byte[] rlp;
        private int index;
        private final int end;

        SequenceIterator(byte[] rlp, int start, int end) {
            this.rlp = rlp;
            this.index = start;
            this.end = end;
        }

        public boolean hasNext() {
            return index < end;
        }

        public RLPItem next() throws DecodeException {
            RLPItem item = wrap(rlp, index);
            this.index = item.endIndex;
            return item;
        }
    }

    public SequenceIterator sequenceIterator(byte[] buffer) {
        return sequenceIterator(buffer, 0);
    }

    /**
     * Returns an iterator over the sequence of RLP items starting at {@code index}.
     *
     * @param buffer    the array containing the sequence
     * @param index the index of the sequence
     * @return  an iterator over the elements in the sequence
     */
    public SequenceIterator sequenceIterator(byte[] buffer, int index) {
        return new SequenceIterator(buffer, index, buffer.length);
    }

    public RLPList.Iterator listIterator(byte[] buffer) throws DecodeException {
        return listIterator(buffer, 0);
    }

    /**
     * Returns an iterator over the elements in the RLP list item at {@code index}.
     *
     * @param buffer    the array containing the list item
     * @param index the index of the RLP list item
     * @return  the iterator over the elements in the list
     * @throws DecodeException  if the RLP list failed to decode
     */
    public RLPList.Iterator listIterator(byte[] buffer, int index) throws DecodeException {
        return ((RLPList) wrap(buffer, index))
                .iterator(this);
//        RLPList rlpList = (RLPList) RLP_STRICT.wrap(buffer, index);
//        return rlpList.elements(RLP_STRICT).iterator();
    }

    /**
     * Returns an {@link RLPItem} for a length-one encoding (e.g. 0xc0)
     *
     * @param lengthOneRLP  the encoding
     * @return  the item
     * @throws DecodeException  if the byte fails to decode
     */
    public RLPItem wrap(byte lengthOneRLP) throws DecodeException {
        return wrap(new byte[] { lengthOneRLP }, 0);
    }

    public RLPItem wrap(byte[] encoding) throws DecodeException {
        return wrap(encoding, 0);
    }

    public RLPItem wrap(byte[] buffer, int index) throws DecodeException {
        return wrap(buffer, index, buffer.length);
    }

    RLPItem wrap(byte[] buffer, int index, int containerEnd) throws DecodeException {
        switch (DataType.type(buffer[index])) {
        case SINGLE_BYTE:
        case STRING_SHORT:
        case STRING_LONG:
            return new RLPString(buffer, index, containerEnd, lenient);
        case LIST_SHORT:
        case LIST_LONG:
            return new RLPList(buffer, index, containerEnd, lenient);
        default:
            throw new AssertionError();
        }
    }

    public List<RLPItem> collectAll(byte[] encodings) throws DecodeException {
        return collectAll(0, encodings);
    }

    public List<RLPItem> collectAll(int index, byte[] encodings) throws DecodeException {
        return collectBefore(index, encodings, encodings.length);
    }

    public List<RLPItem> collectBefore(byte[] encodings, int endIndex) throws DecodeException {
        return collectBefore(0, encodings, endIndex);
    }

    public List<RLPItem> collectBefore(int index, byte[] encodings, int endIndex) throws DecodeException {
        ArrayList<RLPItem> dest = new ArrayList<>();
        collectBefore(index, encodings, endIndex, dest);
        return dest;
    }

    public List<RLPItem> collectN(byte[] encodings, int n) throws DecodeException {
        return collectN(0, encodings, n);
    }

    public List<RLPItem> collectN(int index, byte[] encodings, int n) throws DecodeException {
        ArrayList<RLPItem> dest = new ArrayList<>(n);
        collect(index, encodings, (count, idx) -> count < n, dest);
        return dest;
    }
    // --------
    public int collectAll(int index, byte[] encodings, Collection<RLPItem> dest) throws DecodeException {
        return collectBefore(index, encodings, encodings.length, dest);
    }

    public int collectBefore(int index, byte[] encodings, int endIndex, Collection<RLPItem> dest) throws DecodeException {
        return collect(index, encodings, (count, idx) -> idx < endIndex, dest);
    }

    public void collectN(byte[] encodings, int index, int n, Collection<RLPItem> dest) throws DecodeException {
        collect(index, encodings, (count, idx) -> count < n, dest);
    }
    // -------
    public int collect(int index, byte[] encodings, BiPredicate<Integer, Integer> predicate, Collection<RLPItem> collection) throws DecodeException {
        int count = 0;
        while (predicate.test(count, index)) {
            RLPItem item = wrap(encodings, index);
            collection.add(item);
            count++;
            index = item.endIndex;
        }
        return count;
    }
}
