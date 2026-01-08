/*
   Copyright 2019-2026 Evan Saulpaugh

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
package com.esaulpaugh.headlong.abi.example;

import com.esaulpaugh.headlong.abi.Quintuple;
import com.esaulpaugh.headlong.abi.Tuple;
import com.esaulpaugh.headlong.abi.TupleType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

public class ABIStudent implements ABIEncodeable<Quintuple<String, BigDecimal, byte[], byte[], Integer>> {

    public static final TupleType<Quintuple<String, BigDecimal, byte[], byte[], Integer>> TYPE = TupleType.parse("(string,fixed128x2,bytes,bytes,uint16)");

    private final String name;
    private final float gpa;
    private final byte[] publicKey;
    private final BigDecimal balance;
    private transient final Quintuple<String, BigDecimal, byte[], byte[], Integer> tuple;

    public ABIStudent(String name, float gpa, byte[] publicKey, BigDecimal balance) {
        byte[] keyCopy = Arrays.copyOf(publicKey, publicKey.length);
        this.tuple = toTuple(name, gpa, keyCopy, balance);
        this.name = name;
        this.gpa = tuple.get1().floatValue();
        this.publicKey = keyCopy;
        this.balance = balance;
    }

    public ABIStudent(Quintuple<String, BigDecimal, byte[], byte[], Integer> values) {
        this(
                values.get0(),
                values.get1().floatValue(),
                values.get2(),
                new BigDecimal(new BigInteger(values.get3()), values.get4())
        );
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
    public int hashCode() {
        return toTuple().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(!getClass().isInstance(o)) return false;
        ABIStudent other = (ABIStudent) o;
        return other.toTuple().equals(toTuple());
    }

    @Override
    public String toString() {
        return toTuple().toString();
    }

    @Override
    public Quintuple<String, BigDecimal, byte[], byte[], Integer> toTuple() {
        return tuple;
    }

    private static Quintuple<String, BigDecimal, byte[], byte[], Integer> toTuple(String name, float gpa, byte[] publicKey, BigDecimal balance) {
        BigDecimal gpaBD = new BigDecimal(Float.toString(gpa))
                .setScale(2, RoundingMode.HALF_UP);
        return Tuple.of(name, gpaBD, publicKey, balance.unscaledValue().toByteArray(), balance.scale());
    }

    public static ABIStudent decode(byte[] arr) {
        return new ABIStudent(TYPE.decode(arr));
    }
}
