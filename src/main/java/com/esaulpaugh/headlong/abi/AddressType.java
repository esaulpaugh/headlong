/*
   Copyright 2021 Evan Saulpaugh

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
package com.esaulpaugh.headlong.abi;

import java.nio.ByteBuffer;

/** The {@link ABIType} for {@link Address}. Corresponds to the "address" type. */
public final class AddressType extends UnitType<Address> {

    private static final BigIntegerType ADDRESS_INNER = new BigIntegerType("ADDRESS_INNER", TypeFactory.ADDRESS_BIT_LEN, true, null);

    AddressType(String name) {
        super("address", Address.class, TypeFactory.ADDRESS_BIT_LEN, true, name);
    }

    @Override
    Class<?> arrayClass() {
        return Address[].class;
    }

    @Override
    public int typeCode() {
        return ABIType.TYPE_CODE_ADDRESS;
    }

    @Override
    void validateInternal(Address value) {
        ADDRESS_INNER.validateInternal(value.value());
    }

    @Override
    void encodeTail(Address value, ByteBuffer dest) {
        ADDRESS_INNER.encodeTail(value.value(), dest);
    }

    @Override
    Address decode(ByteBuffer bb, byte[] unitBuffer) {
        return new Address(ADDRESS_INNER.decode(bb, unitBuffer));
    }

    @Override
    void encodePackedUnchecked(Address value, ByteBuffer dest) {
        ADDRESS_INNER.encodePackedUnchecked(value.value(), dest);
    }

    @Override
    public Address parseArgument(String s) {
        Address address = Address.wrap(s);
        validate(address);
        return address;
    }
}
