package com.esaulpaugh.headlong.abi;

import java.nio.ByteBuffer;

public final class AddressType extends UnitType<Address> {

    static final BigIntegerType ADDRESS_INNER = new BigIntegerType("ADDRESS_INNER", TypeFactory.ADDRESS_BIT_LEN, true);

    AddressType() {
        super("address", Address.class, TypeFactory.ADDRESS_BIT_LEN, true);
    }

    @Override
    Class<?> arrayClass() throws ClassNotFoundException {
        return Address[].class;
    }

    @Override
    public int typeCode() {
        return ABIType.TYPE_CODE_ADDRESS;
    }

    @Override
    public int validate(Address value) {
        return ADDRESS_INNER.validate(value.value);
    }

    @Override
    void encodeTail(Object value, ByteBuffer dest) {
        ADDRESS_INNER.encodeTail(((Address) value).value, dest);
    }

    @Override
    Address decode(ByteBuffer bb, byte[] unitBuffer) {
        return new Address(ADDRESS_INNER.decode(bb, unitBuffer));
    }

    @Override
    void encodePackedUnchecked(Address value, ByteBuffer dest) {
        ADDRESS_INNER.encodePackedUnchecked(value.value, dest);
    }

    @Override
    public Address parseArgument(String s) {
        Address address = Address.wrap(s);
        validate(address);
        return address;
    }
}
