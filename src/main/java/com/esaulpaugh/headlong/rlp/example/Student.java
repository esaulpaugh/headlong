package com.esaulpaugh.headlong.rlp.example;

import com.esaulpaugh.headlong.rlp.DecodeException;
import com.esaulpaugh.headlong.rlp.RLPEncoder;
import com.esaulpaugh.headlong.rlp.RLPItem;
import com.esaulpaugh.headlong.rlp.SequenceIterator;
import com.esaulpaugh.headlong.rlp.util.FloatingPoint;
import com.esaulpaugh.headlong.rlp.util.Integers;
import com.esaulpaugh.headlong.util.Strings;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

import static com.esaulpaugh.headlong.rlp.RLPDecoder.RLP_STRICT;
import static com.esaulpaugh.headlong.util.Strings.UTF_8;

public class Student implements RLPEncodeable {

    private String name;
    private float gpa;
    private BigInteger publicKey;
    private BigDecimal balance;

    public Student(String name, float gpa, BigInteger publicKey, BigDecimal balance) {
        this.name = name;
        this.gpa = gpa;
        this.publicKey = publicKey;
        this.balance = balance;
    }

    public Student(byte[] rlp) throws DecodeException {
        SequenceIterator iter = RLP_STRICT.sequenceIterator(rlp);

        this.name = iter.next().asString(UTF_8);
        this.gpa = iter.next().asFloat();
        this.publicKey = iter.next().asBigInt();
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
        this.publicKey = (item = RLP_STRICT.wrap(rlp, item.endIndex)).asBigInt();
        BigInteger intVal = (item = RLP_STRICT.wrap(rlp, item.endIndex)).asBigInt();
        this.balance = new BigDecimal(intVal, RLP_STRICT.wrap(rlp, item.endIndex).asInt());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getGpa() {
        return gpa;
    }

    public void setGpa(float gpa) {
        this.gpa = gpa;
    }

    public BigInteger getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(BigInteger publicKey) {
        this.publicKey = publicKey;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return name + ", " + gpa + ", " + publicKey + ", $" + balance;
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
                && Objects.equals(publicKey, other.publicKey)
                && Objects.equals(balance, other.balance);
    }

    @Override
    public Object[] toObjectArray() {
        return new Object[] {
                Strings.decode(name, UTF_8),
                FloatingPoint.toBytes(gpa),
                publicKey.toByteArray(),
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
}
