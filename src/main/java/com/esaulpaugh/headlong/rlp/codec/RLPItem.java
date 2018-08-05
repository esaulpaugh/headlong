package com.esaulpaugh.headlong.rlp.codec;

import com.esaulpaugh.headlong.rlp.codec.decoding.ObjectNotation;
import com.esaulpaugh.headlong.rlp.codec.util.Integers;
import com.esaulpaugh.headlong.rlp.codec.util.Strings;

import java.util.Arrays;

import static com.esaulpaugh.headlong.rlp.codec.DataType.MIN_LONG_DATA_LEN;

/**
 * Created by Evo on 1/19/2017.
 */
public abstract class RLPItem {

    final byte[] buffer;
    public final int index;

    public final transient DataType type;
    public final transient int dataIndex;
    public final transient int dataLength;

    RLPItem(byte[] buffer, int index, int containerLimit) {
        containerLimit = Math.min(buffer.length, containerLimit);

        final int _dataIndex;
        final long _dataLength;

        final byte leadByte = buffer[index];
        final DataType type = DataType.type(leadByte);
        switch (type) {
        case SINGLE_BYTE: _dataIndex = index; _dataLength = 1; break;
        case STRING_SHORT:
        case LIST_SHORT: _dataIndex = index + 1; _dataLength = leadByte - type.offset;
            break;
        case STRING_LONG:
        case LIST_LONG:
            int lengthIndex = index + 1;
            int lengthOfLength = leadByte - type.offset; // DataType dictates that lengthOfLength guaranteed to be in [1,8]
            _dataIndex = lengthIndex + lengthOfLength;
            _dataLength = Integers.get(buffer, lengthIndex, lengthOfLength);
            if(_dataLength < MIN_LONG_DATA_LEN) {
                throw new IllegalStateException("long element data length must be " + MIN_LONG_DATA_LEN + " or greater; found: " + _dataLength + " for element @ " + index);
            }
            break;
        default: throw new RuntimeException();
        }

        final long end = _dataIndex + _dataLength;
        if(end > containerLimit) {
            throw new IllegalStateException("element @ index " + index + " exceeds its container: " + end + " > " + containerLimit);
        }
        if(end < 0) {
            throw new IllegalStateException("end of element @ " + index + " is out of range: " + end);
        }

        this.buffer = buffer;
        this.index = index;
        this.type = type;
        this.dataIndex = _dataIndex;
        this.dataLength = (int) _dataLength;
    }

    public abstract boolean isList();

    public int encodingLength() {
        return endIndex() - index;
    }

    public int endIndex() {
        return dataIndex + dataLength;
    }

    public byte[] encoding() {
        final int len = encodingLength();
        byte[] copy = new byte[len];
        System.arraycopy(buffer, index, copy, 0, len);
        return copy;
    }

    public int exportEncoding(byte[] dest, int destIndex) {
        int encodingLen = encodingLength();
        System.arraycopy(buffer, index, dest, destIndex, encodingLen);
        return destIndex + encodingLen;
    }

    public byte[] data() {
        byte[] copy = new byte[dataLength];
        System.arraycopy(buffer, dataIndex, copy, 0, dataLength);
        return copy;
    }

    public byte[] copyOfRange(int from, int to) {
        checkRangeBounds(from, to);
        final int len = to - from;
        byte[] range = new byte[len];
        System.arraycopy(buffer, from, range, 0, len);
        return range;
    }

    public int exportData(byte[] dest, int destIndex) {
        System.arraycopy(buffer, dataIndex, dest, destIndex, dataLength);
        return endIndex();
    }

    public String data(int encoding) {
        return Strings.encodeToString(buffer, dataIndex, dataLength, encoding);
    }

    public int asInt() {
        return Integers.getInt(buffer, dataIndex, dataLength);
    }

    public long asLong() {
        return Integers.get(buffer, dataIndex, dataLength);
    }

    public String encodeRange(int from, int to, int encoding) {
        checkRangeBounds(from, to);
        return Strings.encodeToString(buffer, from, to - from, encoding);
    }

    private void checkRangeBounds(int from, int to) {
        if(from < dataIndex) {
            throw new IndexOutOfBoundsException(from + " < " + dataIndex);
        }
        if(to > endIndex()) {
            throw new IndexOutOfBoundsException(to + " > " + endIndex());
        }
    }

    @Override
    public String toString() {
        return ObjectNotation.fromEncoding(buffer, index, endIndex()).toString();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof RLPItem)) {
            return false;
        }

        RLPItem other = (RLPItem) obj;

        final int encLen = this.encodingLength();

        if(encLen != other.encodingLength()) {
            return false;
        }

        final int end = this.index + encLen;
        for (int i = this.index, j = other.index; i < end; i++, j++) {
            if(this.buffer[i] != other.buffer[j]) {
                return false;
            }
        }

        return true;
    }

    /**
     * @see Arrays#hashCode(byte[])
     * @return
     */
    @Override
    public int hashCode() {

        int result = 1;
        final int end = endIndex();
        for (int i = index; i < end; i++) {
            result = 31 * result + buffer[i];
        }
        return result;
    }

    static RLPItem fromEncoding(byte[] buffer, int index, int containerLimit) {
        DataType type = DataType.type(buffer[index]);
        switch (type) {
        case SINGLE_BYTE:
        case STRING_SHORT:
        case STRING_LONG:
            return new RLPString(buffer, index, containerLimit);
        case LIST_SHORT:
        case LIST_LONG:
            return new RLPList(buffer, index, containerLimit);
        default:
            throw new RuntimeException("???");
        }
    }
}
