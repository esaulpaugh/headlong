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
package com.esaulpaugh.headlong.abi.example;

import com.esaulpaugh.headlong.abi.Tuple;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

public class ABIStudent implements ABIEncodeable {

    private final String name;
    private final float gpa;
    private final byte[] publicKey;
    private final BigDecimal balance;

    public ABIStudent(String name, float gpa, byte[] publicKey, BigDecimal balance) {
        this.name = name;
        this.gpa = gpa;
        this.publicKey = Arrays.copyOf(publicKey, publicKey.length);
        this.balance = balance;
    }

    public ABIStudent(Tuple values) {
        this(
                values.get(0),
                ((BigDecimal) values.get(1)).floatValue(),
                values.get(2),
                new BigDecimal(new BigInteger((byte[]) values.get(3)), (int) values.get(4))
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
        return Arrays.deepHashCode(new Object[] { name, gpa, publicKey, balance });
    }

    @Override
    public boolean equals(Object o) {
        if(!getClass().isInstance(o)) return false;
        ABIStudent other = (ABIStudent) o;
        return Objects.equals(other.name, this.name)
                && Math.abs(other.gpa - this.gpa) < 0.00005f
                && Arrays.equals(other.publicKey, this.publicKey)
                && Objects.equals(other.balance, this.balance);
    }

    @Override
    public String toString() {
        return name + ", " + gpa + ", " + new BigInteger(publicKey) + ", $" + balance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Tuple> T toTuple() {
        return (T) Tuple.of(name, BigDecimal.valueOf(gpa), publicKey, balance.unscaledValue().toByteArray(), balance.scale());
    }
}
