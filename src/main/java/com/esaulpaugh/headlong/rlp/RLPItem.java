package com.esaulpaugh.headlong.rlp;

import com.esaulpaugh.headlong.rlp.util.FloatingPoint;
import com.esaulpaugh.headlong.rlp.util.Integers;
import com.esaulpaugh.headlong.rlp.util.ObjectNotation;
import com.esaulpaugh.headlong.rlp.util.Strings;

import java.math.BigInteger;
import java.util.Arrays;

import static com.esaulpaugh.headlong.rlp.DataType.MIN_LONG_DATA_LEN;
import static com.esaulpaugh.headlong.rlp.RLPCodec.wrap;

/**
 * Created by Evo on 1/19/2017.
 */
public abstract class RLPItem {

    final byte[] buffer;
    public final int index;

    public final transient DataType type;
    public final transient int dataIndex;
    public final transient int dataLength;

    RLPItem(byte[] buffer, int index, int containerEnd) throws DecodeException {
        containerEnd = Math.min(buffer.length, containerEnd);

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
            _dataLength = Integers.getLong(buffer, lengthIndex, lengthOfLength);
            if(_dataLength < MIN_LONG_DATA_LEN) {
                throw new IllegalStateException("long element data length must be " + MIN_LONG_DATA_LEN + " or greater; found: " + _dataLength + " for element @ " + index);
            }
            break;
        default: throw new RuntimeException();
        }

        final long end = _dataIndex + _dataLength;
        if(end > containerEnd) {
            throw new IllegalStateException("element @ index " + index + " exceeds its container: " + end + " > " + containerEnd);
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

    public String asString(int encoding) {
        return Strings.encode(buffer, dataIndex, dataLength, encoding);
    }

    public int asInt() throws DecodeException {
        return Integers.getInt(buffer, dataIndex, dataLength);
    }

    public long asLong() throws DecodeException {
        return Integers.getLong(buffer, dataIndex, dataLength);
    }

    public BigInteger asBigInt() {
        return new BigInteger(data());
    }

    public float asFloat() throws DecodeException {
        return FloatingPoint.getFloat(data(), 0, dataLength);
    }

    public double asDouble() throws DecodeException {
        return FloatingPoint.getDouble(data(), 0, dataLength);
    }

    public String encodeRange(int from, int to, int encoding) {
        checkRangeBounds(from, to);
        return Strings.encode(buffer, from, to - from, encoding);
    }

    private void checkRangeBounds(int from, int to) {
        if(from < dataIndex) {
            throw new IndexOutOfBoundsException(from + " < " + dataIndex);
        }
        if(to > endIndex()) {
            throw new IndexOutOfBoundsException(to + " > " + endIndex());
        }
    }

    /**
     * Clones this object.
     * @return
     */
    public RLPItem duplicate() {
        try {
            return wrap(encoding(), 0, Integer.MAX_VALUE);
        } catch (DecodeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        try {
            return ObjectNotation.fromEncoding(buffer, index, endIndex()).toString();
        } catch (DecodeException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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
}
