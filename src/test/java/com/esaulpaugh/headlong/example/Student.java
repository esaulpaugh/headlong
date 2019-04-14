package com.esaulpaugh.headlong.example;

import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.rlp.exception.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.rlp.RLPIterator;
import com.esaulpaugh.headlong.rlp.util.FloatingPoint;
import com.esaulpaugh.headlong.rlp.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class Student implements RLPEncodeable, ABIEncodeable {

    private final String name;
    private final float gpa;
    private final byte[] publicKey;
    private final BigDecimal balance;

    public Student(String name, float gpa, byte[] publicKey, BigDecimal balance) {
        this.name = name;
        this.gpa = gpa;
        this.publicKey = Arrays.copyOf(publicKey, publicKey.length);
        this.balance = balance;
    }

    public Student(byte[] rlp) throws DecodeException {
        RLPIterator iter = RLP_STRICT.sequenceIterator(rlp);

        this.name = iter.next().asString(UTF_8);
        this.gpa = iter.next().asFloat();
        this.publicKey = iter.next().data();
        this.balance = new BigDecimal(iter.next().asBigInt(), iter.next().asInt());
    }

//    public Student(byte[] rlp) throws DecodeException {
//        List<RLPItem> items = RLP_STRICT.collectAll(rlp);
//
//        this.name = items.get(0).asString(UTF_8);
//        this.gpa = items.get(1).asFloat();
//        this.publicKey = items.get(2).asBigInt();
//        this.balance = new BigDecimal(items.get(3).asBigInt(), items.get(4).asInt());
//    }

    public Student(byte[] rlp, int index) throws DecodeException {
        RLPItem item = RLP_STRICT.wrap(rlp, index);
        this.name = item.asString(UTF_8);
        this.gpa = (item = RLP_STRICT.wrap(rlp, item.endIndex)).asFloat();
        this.publicKey = (item = RLP_STRICT.wrap(rlp, item.endIndex)).data();
        BigInteger intVal = (item = RLP_STRICT.wrap(rlp, item.endIndex)).asBigInt();
        this.balance = new BigDecimal(intVal, RLP_STRICT.wrap(rlp, item.endIndex).asInt());
    }

    public Student(Tuple values) {
        this.name = (String) values.get(0);
        this.gpa = ((BigDecimal) values.get(1)).floatValue();
        this.publicKey = (byte[]) values.get(2);
        this.balance = new BigDecimal(new BigInteger((byte[]) values.get(3)), (int) values.get(4));
    }

    public String getName() {
        return name;
    }

    public float getGpa() {
        return gpa;
    }

    public byte[] getPublicKey() {
        return Arrays.copyOf(publicKey, publicKey.length);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    @Override
    public String toString() {
        return name + ", " + gpa + ", " + new BigInteger(publicKey) + ", $" + balance;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[] { name, gpa, publicKey, balance });
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Student)) {
            return false;
        }

        Student other = (Student) obj;

        return Objects.equals(name, other.name)
                && Math.abs(gpa - other.gpa) < 0.00005f
                && Arrays.equals(publicKey, other.publicKey)
                && Objects.equals(balance, other.balance);
    }

    @Override
    public Object[] toObjectArray() {
        return new Object[] {
                Strings.decode(name, UTF_8),
                FloatingPoint.toBytes(gpa),
                publicKey,
                balance.unscaledValue().toByteArray(),
                Integers.toBytes(balance.scale())
        };
    }

    @Override
    public byte[] toRLP() {
        return RLPEncoder.encodeSequentially(toObjectArray());
    }

    @Override
    public void toRLP(byte[] dest, int destIndex) {
        RLPEncoder.encodeSequentially(toObjectArray(), dest, destIndex);
    }

    @Override
    public Tuple toTuple() {
        return new Tuple(name, BigDecimal.valueOf(gpa), publicKey, balance.unscaledValue().toByteArray(), balance.scale());
    }
}
