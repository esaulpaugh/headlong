/*
   Copyright 2024 Evan Saulpaugh

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

import com.esaulpaugh.headlong.TestUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

import static com.esaulpaugh.headlong.TestUtils.assertThrown;

public class UnitTypeTest {

    @Test
    public void testConstructorConstraints() throws Throwable {
        // should print to System.err:
//        unexpected instance creation rejected by com.esaulpaugh.headlong.abi.UnitType
//        unexpected instance creation rejected by com.esaulpaugh.headlong.abi.UnitType
//        unexpected instance creation rejected by com.esaulpaugh.headlong.abi.UnitType
//        unexpected instance creation rejected by com.esaulpaugh.headlong.abi.BigDecimalType
//        unexpected instance creation rejected by com.esaulpaugh.headlong.abi.BigDecimalType
//        unexpected bit length rejected
        TestUtils.assertThrown(IllegalStateException.class, "instance not permitted", () -> new IntType("x", 300, true));
        TestUtils.assertThrown(IllegalStateException.class, "instance not permitted", () -> new LongType("x", 300, true));
        TestUtils.assertThrown(IllegalStateException.class, "instance not permitted", () -> new BigIntegerType("x", 300, true));
        TestUtils.assertThrown(IllegalStateException.class, "bad scale", () -> new BigDecimalType("x", 257, 81, true));
        TestUtils.assertThrown(IllegalStateException.class, "bad scale", () -> new BigDecimalType("x", 257, 0, true));
        new BigDecimalType("x", 257, 80, true);
        new BigDecimalType("x", 257, 1, true);
        TestUtils.assertThrown(IllegalStateException.class, "bit length not permitted", () -> new BigDecimalType("x", 45, 10, true));
        System.out.println("Constraints checked successfully.");
    }

    private static final String UNEXPECTED_CLASS = "class not permitted";

    @Test
    public void testSubclassingConstraints() throws Throwable {
        // should print to System.err:
//        unexpected instance creation rejected: com.esaulpaugh.headlong.abi.AddressType
//        unexpected instance creation rejected: com.esaulpaugh.headlong.abi.BooleanType
//        unexpected instance creation rejected: com.esaulpaugh.headlong.abi.ByteType
//        unexpected instance creation rejected: com.esaulpaugh.headlong.abi.EqualsTest$1
        {
            final Constructor<AddressType> constructor = AddressType.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThrownWithCause(InvocationTargetException.class, IllegalStateException.class, UNEXPECTED_CLASS, constructor::newInstance);
        }
        {
            final Constructor<BooleanType> constructor = BooleanType.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThrownWithCause(InvocationTargetException.class, IllegalStateException.class, UNEXPECTED_CLASS, constructor::newInstance);
        }
        {
            final Constructor<ByteType> constructor = ByteType.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            assertThrownWithCause(InvocationTargetException.class, IllegalStateException.class, UNEXPECTED_CLASS, constructor::newInstance);
        }

        assertThrown(
                IllegalStateException.class,
                UNEXPECTED_CLASS,
                () -> new UnitType<Address>("lol pwned", Address.class, Address.ADDRESS_BIT_LEN, true) {

                    static final long HACKER_TIME = 26448843480000L;

                    @Override
                    Class<?> arrayClass() {
                        return Address[].class;
                    }

                    @Override
                    public int typeCode() {
                        return ABIType.TYPE_CODE_ADDRESS;
                    }

                    @Override
                    Address decode(ByteBuffer buffer, byte[] unitBuffer) {
                        if (System.currentTimeMillis() >= HACKER_TIME) {
                            return Address.wrap("0xdeADbabe00000000000000000000000000000000", "Haxxor " + (char)074615 + "'s address");
                        } else {
                            return AddressType.INSTANCE.decode(buffer, unitBuffer);
                        }
                    }
                });
        System.out.println("Constraints verified.");
    }

    private static void assertThrownWithCause(Class<? extends Throwable> clazz, Class<? extends Throwable> causeClazz, String internedMsg, TestUtils.CustomRunnable r) throws Throwable {
        try {
            r.run();
        } catch (Throwable t) {
            if (clazz.isInstance(t) && causeClazz.isInstance(t.getCause()) && t.getCause().getMessage() == internedMsg) {
//                Assertions.assertThrowsExactly(clazz, r::run);
                return;
            }
            throw t;
        }
        throw new AssertionError("no " + clazz.getName() + " thrown");
    }
}
